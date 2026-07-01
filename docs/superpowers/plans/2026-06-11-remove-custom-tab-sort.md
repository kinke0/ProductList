# 移除自定义清单独立排序，与主清单同步

## 目标
移除自定义清单的独立排序机制（customTabEntry.sortOrder），让自定义清单中的条目显示顺序始终与主数据清单保持一致。

## 问题根因
自定义清单中用户拖拽调整顺序只更新 `customTabEntry.sortOrder`，但预览/下载Word使用的是 `DataEntry.sortOrder`（原始排序），导致顺序不一致。

## 涉及文件

### 后端（5个文件）
1. `CustomTabEntry.java` — 移除 sortOrder 字段
2. `CustomTabEntryRepository.java` — 移除 findByCustomTabIdOrderBySortOrder、updateSortOrder
3. `CustomTabService.java` — 简化 addEntries（移除 sortOrder 逻辑），删除 fixNullSort/reorderTabByHierarchy/updateSortOrders
4. `CustomTabController.java` — 删除 PUT /{id}/sort 接口
5. `DataEntryService.java` — 删除 reorderByCustomTabSort，queryEntries 统一走 sortByCategoryOrder
6. `DocumentService.java` — customTabId 分支移除自定义排序，统一走 sortByCategoryOrder

### 前端（2个文件）
1. `customTab.js` — 删除 updateCustomTabSort
2. `DataListTab.vue` — applyDragDrop 统一走 updateSort；buildTree 移除 customTabId 判断统一排序

## 分步实施步骤

### Step 1: 后端实体和仓库层
- CustomTabEntry.java: 移除 sortOrder 字段
- CustomTabEntryRepository.java: 移除排序查询和更新方法

### Step 2: 后端服务层
- CustomTabService.java: 简化 addEntries，删除排序相关方法
- CustomTabController.java: 删除 PUT /{id}/sort

### Step 3: 后端数据查询和文档生成
- DataEntryService.java: 删除 reorderByCustomTabSort，queryEntries 统一走 sortByCategoryOrder
- DocumentService.java: 移除 reorderByCustomTabSort 调用，改为 sortByCategoryOrder

### Step 4: 前端
- customTab.js: 删除 updateCustomTabSort
- DataListTab.vue: 统一拖拽排序和 buildTree

### Step 5: 验证
- mvn compile + npm run build
- 重启服务 + curl 测试

## 关键代码变更说明

### queryEntries 排序统一
```java
// 之前：customTabId != null 时走 reorderByCustomTabSort，否则走 sortByCategoryOrder
// 之后：统一走 sortByCategoryOrder（去掉 customTabId == null 判断）
result = new ArrayList<>(result.stream().filter(...).toList());
result = sortByCategoryOrder(result, versionId); // 不再区分 customTabId
```

### 拖拽排序统一
```javascript
// 之前：customTabId ? updateCustomTabSort : updateSort
// 之后：统一 updateSort（直接修改 DataEntry.sortOrder）
const payload = nonSep.map((d, i) => ({ id: d.id, sortOrder: i }))
await updateSort(payload)
```

### buildTree 统一
```javascript
// 之前：if (!props.customTabId) { sortChildren(roots) }
// 之后：始终执行 sortChildren(roots)
```
