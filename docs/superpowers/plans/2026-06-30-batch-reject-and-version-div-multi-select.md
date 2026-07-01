# Bug修复：批量驳回 + 需求：版本划分多选

## 目标

1. **Bug修复**：L3级产品点击批量驳回提示"无可操作项" — `batchReject()` 只允许 `approvalStatus === '待审核'`，但大多数产品状态为'待提交'，且 L3 产品的审批状态与 `colStatus` 包含"可交付"才显示审批标签，因此驳回条件需要包含 `approvalStatus === '待提交'` 的可交付产品
2. **需求1**：创建清单弹窗中，版本划分下拉框缺少"非标配系统"选项，且需要支持多选
3. **需求2**：清单查询过滤中，版本划分需要支持多选

## 涉及文件

1. `frontend/src/components/DataListTab.vue` — 查询栏版本划分改为多选
2. `frontend/src/views/dashboard/DataWorkbench.vue` — 创建清单弹窗版本划分改为多选 + 加"非标配系统"
3. `src/main/java/com/superpower/modules/data/controller/DataEntryController.java` — 版本划分参数改为列表
4. `src/main/java/com/superpower/modules/data/service/DataEntryService.java` — 版本划分过滤逻辑改为多值匹配
5. `src/main/java/com/superpower/modules/data/repository/DataEntryRepository.java` — JPQL LIKE 多值匹配
6. `src/main/java/com/superpower/modules/customtab/controller/CustomTabController.java` — versionTag 参数改为列表
7. `src/main/java/com/superpower/modules/customtab/service/CustomTabService.java` — versionTag 过滤改为多值

## 分步实施

### Step 1: Bug修复 — batchReject 条件放宽

**文件**: `frontend/src/components/DataListTab.vue`

**改动说明**: `batchReject()` 函数中 validStatus 从 `['待审核']` 改为 `['待审核', '待提交']`，允许驳回尚未提交审核的可交付产品。

```javascript
// 原来：只允许驳回 '待审核'
if (s === '待审核') { validIds.push(id) }

// 改为：允许驳回 '待审核' 和 '待提交'
if (s === '待审核' || s === '待提交') { validIds.push(id) }
```

### Step 2: 版本划分多选 — DataListTab 查询栏

**文件**: `frontend/src/components/DataListTab.vue`

**改动说明**:
1. 查询栏版本划分下拉框从单选改为多选（`multiple` 属性）
2. `versionDivList` 已包含 '非标配系统'（line 695）
3. `queryForm.versionDiv` 从 string 改为 array
4. 对应后端 API 参数从 `String versionTag` 改为 `List<String> versionTags`

**前端改动**:
```vue
<!-- 原来 -->
<el-select v-model="queryForm.versionDiv" placeholder="全部" clearable style="width: 110px">
<!-- 改为 -->
<el-select v-model="queryForm.versionDiv" placeholder="全部" clearable multiple style="width: 180px">
```

### Step 3: 版本划分多选 — 创建清单弹窗

**文件**: `frontend/src/views/dashboard/DataWorkbench.vue`

**改动说明**:
1. 弹窗中版本划分下拉框从单选改为多选
2. 增加"非标配系统"选项
3. `addListForm.versionTag` 从 string 改为 array
4. 后端 `createCustomTabWithFilter` API 中 versionTag 参数改为 List

```vue
<!-- 原来 -->
<el-select v-model="addListForm.versionTag" placeholder="全部" clearable style="width:100%">
  <el-option label="A-曜系列" value="A-曜系列" />
  <el-option label="B-远系列" value="B-远系列" />
  <el-option label="C-驰系列" value="C-驰系列" />
</el-select>

<!-- 改为 -->
<el-select v-model="addListForm.versionTag" placeholder="全部" clearable multiple style="width:100%">
  <el-option v-for="v in versionDivList" :key="v" :label="v" :value="v" />
</el-select>
```

### Step 4: 后端 — 版本划分多选过滤

**文件**: `DataEntryRepository.java`, `DataEntryService.java`, `DataEntryController.java`

**改动说明**:
- `versionTag` 参数类型从 `String` 改为 `List<String>`
- JPQL 查询中 LIKE 单值匹配改为多值 OR 匹配
- 对每个 versionTag 值做 `LIKE %:tag%` 匹配

**CustomTabService.java**: `createCustomTabWithFilter` 的 versionTag 参数同样改为 `List<String>`

## 关键注意事项

- `col_版本划分` 字段存储的是空格分隔的多值（如 "A-曜系列 B-远系列 C-驰系列"），所以用 LIKE %:tag% 模式匹配
- 非标配系统也是一个版本划分值，有 507 条数据使用它
- 多选版本划分时，需要匹配任意一个选中值（OR 逻辑）
