# 版本创建/删除重构 + 多步骤进度弹窗

## 目标
1. 创建新版本时补齐缺失的复制步骤（产品分类、自定义清单），并改为异步多步骤流程
2. 新增删除版本功能，反向删除所有关联数据
3. 前端弹窗展示每步进度和数据量

## 创建版本步骤（8步）
| 步骤 | 名称 | 数据来源 | 当前状态 |
|------|------|----------|----------|
| 1 | 复制清单数据 | DataEntry | ✅ 已有 |
| 2 | 复制业务分类 | CategoryService.copyFromVersion | ✅ 已有，需返回ID映射 |
| 3 | 复制产品分类 | ProductService.copyFromVersion | ❌ 方法存在但未调用，需返回ID映射 |
| 4 | 复制基础选项 | DataOptionService.copyOptions | ✅ 已有 |
| 5 | 复制图片资源 | ImageResource + 物理文件 | ✅ 已有 |
| 6 | 复制自定义清单 | CustomTab + CustomTabEntry | ❌ 需新增 |
| 7 | 更新图片URL引用 | DataEntry 图片字段替换 | ✅ 已有 |
| 8 | 更新分类ID引用 | DataEntry categoryId/domainId/productId 回写 | ❌ 需新增 |

## 删除版本步骤（7步）
| 步骤 | 名称 | 清理内容 |
|------|------|----------|
| 1 | 删除自定义清单项 | CustomTabEntry（按 customTabId 批量） |
| 2 | 删除自定义清单 | CustomTab |
| 3 | 删除清单数据 | DataEntry |
| 4 | 删除业务分类 | BaseCategory + BaseDomain + BaseProduct |
| 5 | 删除产品分类 | BaseProductL1 + BaseProductL2 |
| 6 | 删除基础选项 | DataOption |
| 7 | 删除图片资源 | ImageResource + 物理文件目录 |
| 8 | 删除文档生成记录 | DocGenRecord |
| 9 | 删除版本记录 | DataVersion |

## 涉及文件

### 后端
1. `DataVersionService.java` - 重构 createVersion 为异步多步骤；新增 deleteVersion；进度状态模型
2. `DataVersionController.java` - 新增 DELETE /{id}；GET /progress 接口
3. Repository 层新增方法：
   - `DataEntryRepository` - deleteByVersionId, countByVersionId
   - `BaseCategoryRepository` - deleteByVersionId, countByVersionId
   - `BaseDomainRepository` - deleteByVersionId, countByVersionId
   - `BaseProductRepository` - deleteByVersionId, countByVersionId
   - `BaseProductL1Repository` - deleteByVersionId, countByVersionId
   - `BaseProductL2Repository` - deleteByVersionId, countByVersionId
   - `ImageResourceRepository` - deleteByVersionId, countByVersionId
   - `CustomTabRepository` - deleteByVersionId, countByVersionId
   - `CustomTabEntryRepository` - deleteByCustomTabIdIn
   - `DocGenRecordRepository` - deleteByVersionId
4. `CategoryService.java` - copyFromVersion 返回 CategoryCopyResult(catIdMap, domIdMap)
5. `ProductService.java` - copyFromVersion 返回 ProductCopyResult(l1IdMap, l2IdMap)

### 前端
6. `version.js` - 新增 deleteVersion, getVersionProgress API
7. `VersionManage.vue` - 创建按钮触发弹窗；删除按钮；进度弹窗组件

## 关键设计
- 进度状态用内存模型（非数据库），通过 GET /api/versions/progress 轮询
- 删除版本只能删除 draft 状态的版本
- CategoryService/ProductService 的 copyFromVersion 返回 ID 映射，用于步骤8回写 DataEntry
