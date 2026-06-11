# 实施计划：右键菜单增强 + 多项 Bug 修复

## 右键菜单整行触发 + 路由冲突修复

### 目标
右键菜单触发范围从拖拽手柄扩展到整行，修复 API 路由冲突。

### 改动
- `DataListTab.vue`：`@contextmenu.prevent` 从拖拽手柄移到 `div.vrow`，全局 click/contextmenu 监听改为 onMounted 注册
- `DataEntryController.java`：`/copy` → `/entries/copy`，`/move` → `/entries/move`，避免与 `/{id}` 路由冲突
- `data.js`：API 路径同步更新

## 编辑表单数据残留 Bug

### 目标
打开不同条目的编辑表单时，上一次编辑的数据（如招标参数）不会残留到新表单。

### 根因
`editRow()`/`viewRow()` 用 `Object.assign(editForm, row)` 合并数据，`row` 来自精简 DTO 不含大文本字段，`editForm` 保留了上一次的值。`getEntry()` 返回后只覆盖 `colFeatureDesc`，未覆盖其他大文本字段。

### 改动
- `DataListTab.vue`：先 `Object.assign(editForm, initialFormState(), row)` 重置再填充，`getEntry()` 返回后用 `Object.assign(editForm, res.data)` 覆盖全部字段。

## 分隔行添加产品 L1/L2 信息缺失

### 目标
从分隔行点击"添加产品/系统"时，表单正确带入 L1/L2 下拉选项。

### 根因
1. 所有 L3 节点的 `categoryId`/`domainId` 为 null（Excel 导入时只存了字符串名称）
2. `addProductFromSeparator` 中 `loadCategoryTree()` 在 `categoryId` 赋值前执行，L2 选项为空
3. `resolveCategoryIds()` 是 async 但 `addProductFromSeparator` 没有 await

### 改动
- `DataListTab.vue`：`addProductFromSeparator` 改为 async，用 `await resolveCategoryIds()` 按名称反查 categoryId/domainId，等分类树加载完成后再打开弹窗
- `DataEntryService.java`：新增 `fillCategoryAndDomainIds()` 辅助方法，在 `create()`/`update()`/`importFromExcel()` 保存前调用，按名称自动补全 categoryId/domainId
