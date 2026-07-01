# 业务分类/域与清单数据行关联改用ID而非名称

## Context
用户明确指出："业务分类与清单数据行的关联逻辑应该是ID，而不是靠名称"。

当前代码中存在多处使用文本名称（colBizCategory/colBizDomain）而非ID（categoryId/domainId）来查找关联的 L1/L2 分隔行条目，导致以下问题：
1. 域名修改后（如加了"（待删除）"后缀），名称匹配可能失败或不一致
2. 删除域/分类时用名称查找分隔行，如果名称不匹配就找不到正确的关联条目
3. 重命名域/分类时靠名称查找来同步更新，不可靠

## 涉及文件
1. `DataEntryRepository.java` — 新增 ID-based Repository 方法
2. `CategoryService.java` — deleteCategory/deleteDomain/updateCategory/updateDomain/createDomain/findL1EntryId 改用 ID
3. `DataEntryService.java` — getDomainTree/batchUpdateCategory 改用 ID（去掉名称 fallback）
4. `ImageResourceService.java` — resolveL3Product 改用 ID
5. `VERSION.md` — 变更说明

## 修复步骤

### 步骤 1: DataEntryRepository.java 新增 ID-based 方法

新增方法：
```java
List<DataEntry> findByVersionIdAndCategoryIdAndLevel(Long versionId, Long categoryId, Integer level);
```

已有方法（无需新增）：
- `findByVersionIdAndDomainIdAndLevel(Long versionId, Long domainId, Integer level)` ✅
- `findByVersionIdAndParentId(Long versionId, Long parentId)` ✅

### 步骤 2: CategoryService.java 改用 ID 关联

1. **deleteCategory** (line 161):
   - 旧: `findByVersionIdAndLevelAndColBizCategory(ver, 1, cat.getName())`
   - 新: `findByVersionIdAndCategoryIdAndLevel(ver, cat.getId(), 1)`
   - 同时检查：删除分类前，应检查该分类下所有域的 L2 分隔行是否都没有子条目

2. **deleteDomain** (line 223):
   - 旧: `findByVersionIdAndLevelAndColBizDomain(ver, 2, dom.getName())`
   - 新: `findByVersionIdAndDomainIdAndLevel(ver, dom.getId(), 2)`
   - 这是导致"1.4 数据产品"无法删除的根因——名称含括号后缀导致匹配失败

3. **updateCategory** (line 138-151):
   - 旧: `findByVersionIdAndColBizCategory(ver, oldName)` 找到所有条目更新名称
   - 新: 应保留名称同步逻辑（因为 colBizCategory 是冗余的文本字段，改名时确实需要同步），但**查找应该基于 categoryId** 而非名称
   - 改为: `findByVersionIdAndCategoryIdAndLevel(ver, cat.getId(), null)` — 但 level 参数不能为 null，需要分步查找所有有 categoryId 的条目
   - 实际实现: 需要新增 `findByVersionIdAndCategoryId(Long versionId, Long categoryId)` 方法

4. **updateDomain** (line 204-217):
   - 旧: `findByVersionIdAndColBizDomain(ver, oldName)`
   - 新: `findByVersionIdAndDomainId(Long versionId, Long domainId)` — 需新增此方法
   - 同样保留名称同步逻辑，但查找基于 domainId

5. **createDomain** (line 185):
   - 旧: `findL1EntryId(ver, cat.getName())` — 名称匹配
   - 新: `findByVersionIdAndCategoryIdAndLevel(ver, categoryId, 1)` — ID 匹配

6. **findL1EntryId** (line 233-240):
   - 重构为基于 categoryId 的查找：`findByVersionIdAndCategoryIdAndLevel(ver, categoryId, 1)`
   - 如果此方法不再需要名称参数，简化接口

### 步骤 3: DataEntryService.java 改用 ID 关联

1. **getDomainTree** (line 164-172):
   - 旧: 先用 domainId 查，失败后用名称 fallback
   - 新: 只用 `findByVersionIdAndDomainIdAndLevel(ver, domainId, 2)`, 去掉名称 fallback

2. **batchUpdateCategory** (line 585-597):
   - 旧: 先用 domainId 查找 L2 分隔行，失败后用 domName fallback
   - 新: 只用 domainId 查找，去掉名称 fallback（`findByVersionIdAndDomainIdAndLevel(ver, domainId, 2)`）

### 步骤 4: ImageResourceService.java resolveL3Product 改用 ID

- 旧: `findByVersionIdAndLevelAndColBizCategoryAndColBizDomain(ver, 3, category, domain)`
- 新: 需要基于 categoryId + domainId 查找，但此方法的输入参数是文本名称，需要先从名称映射到 ID
- 方案: 先通过 BaseCategoryRepository/BaseDomainRepository 查找 ID，然后用 ID 查找 DataEntry

### 步骤 5: 创建新版本流程排查

经排查，`doCreateVersionSteps` 的流程：
1. Step 1: 复制所有 data_entry 条目（包括 L1/L2 分隔行），`cloneWithoutId()` 保留了旧的 categoryId/domainId 和 colBizCategory/colBizDomain 文本
2. Step 2: `CategoryService.copyFromVersion` 创建新版本的 base_category/base_domain 记录，返回 catIdMap/domIdMap
3. Step 7: 用 catIdMap/domIdMap 更新新版本所有条目的 categoryId/domainId

**结论：创建新版本流程本身不会引发 ID 关联异常**。因为：
- categoryId/domainId 在 Step 7 已正确映射到新版本的 ID
- colBizCategory/colBizDomain 文本字段从旧版本复制，与新版本 base_category/base_domain 的 name 字段一致（因为 copyFromVersion 复制了相同的名称）

但存在潜在风险：如果旧版本中 colBizCategory/colBizDomain 与 base_category/base_domain 的 name 已经不一致（因为之前的名称同步逻辑有 bug），创建新版本会复制这种不一致。改为 ID 关联后，这个问题不会再发生。

### 步骤 6: VERSION.md 更新变更说明

在"系统管理 > 基础数据维护"下追加变更说明。

## Repository 新增方法汇总

| 方法 | 用途 |
|------|------|
| `findByVersionIdAndCategoryIdAndLevel(ver, categoryId, level)` | 查找指定版本+分类ID+级别的条目 |
| `findByVersionIdAndCategoryId(ver, categoryId)` | 查找指定版本+分类ID的所有条目（不限级别） |
| `findByVersionIdAndDomainId(ver, domainId)` | 查找指定版本+域ID的所有条目（不限级别） |

已有可用的方法：
- `findByVersionIdAndDomainIdAndLevel(ver, domainId, level)` ✅

## 验证
1. 前端 npm run build
2. 后端 mvn compile
3. 服务重启 + curl 验证
4. 接口测试：删除域"1.4 数据产品"不再提示"存在子级条目"
5. 接口测试：重命名分类/域后数据条目的 colBizCategory/colBizDomain 同步更新
