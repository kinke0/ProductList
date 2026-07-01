# 修复清单导航树不匹配与统计视图加载遮罩

## Context

两个问题需要修复：
1. 清单数据加载期间点击左侧导航树，加载完成后出现导航树选中节点与清单内容不匹配
2. 统计视图页签（StatsTab）加载数据时没有加载遮罩

## 根因分析

### 问题1：导航树选中与清单内容不匹配

**当前机制**：DataListTab.vue 已有 `pendingSelectedNode` 机制（line 2850-2862），当 `dataLoading=true` 时保存 `pendingSelectedNode`，加载完成后（line 2727-2738）检测并重新查询。

**根因**：`handleQuery` 发起查询时，使用的是 `props.selectedNode`（line 2697-2698），这是发起查询那一刻的值。当用户在加载期间点击了新导航节点：
- 当前正在执行的查询用的是旧的 selectedNode
- watch 保存了 pendingSelectedNode = 新节点
- 加载完成后 finally 检测到 pendingSelectedNode，调用 `handleQuery()`（无参数）重新查询

**但实际缺陷是**：加载完成后 `pendingSelectedNode` 被清除（line 2729），然后执行 `handleQuery()`。此时 `props.selectedNode` 已经是新值，所以二次查询会正确获取新节点的数据。但二次查询也会设置 `dataLoading=true`，导致 `finally` 又会被执行一次，虽然 `pendingSelectedNode` 已被清除所以不会再次触发。

**真正的问题可能在于时序**：如果用户快速连续点击不同导航节点，watch 会每次触发并更新 `pendingSelectedNode`。但由于 `dataLoading` 为 true，每次都会覆盖 `pendingSelectedNode`，只有最后一次点击被记录。这其实没问题。但还有一个场景：如果 `handleQuery` 的 finally 中 `handleQuery()` 被调用时，`props.selectedNode` 仍然是旧值（Vue响应式还没更新），就会导致查询参数不正确。

**修复方案**：在 `handleQuery` 中增加请求取消机制——当 selectedNode 变化时，如果正在加载中，直接取消当前请求并立即用新 selectedNode 发起新请求。而不是等旧请求完成后才重新查询。

具体改动：
- 引入 `AbortController` 或请求版本号来取消旧请求
- watch selectedNode 时：如果 dataLoading=true，保存 pendingSelectedNode 并**立即发起新查询**（而不是等旧请求完成）
- 或者更简单的方式：在 finally 中用 pendingSelectedNode 中的参数直接查询（不依赖 props.selectedNode 的实时性）

**简化修复方案（推荐）**：将 watch selectedNode 中的逻辑改为——加载中时，不是仅保存 pendingSelectedNode 等待，而是**立即重新发起新查询**，让旧请求的结果被忽略（因为 dataLoading 会在新查询中被重新设为 true）。

修改 DataListTab.vue：
```
watch(() => props.selectedNode, () => {
  if (props.selectedNode && props.versionId) {
    handleQuery()  // 直接查询，不管 dataLoading 状态
  }
  if (isNew.value) {
    fillCategoryAndDomain()
  }
}, { deep: true })
```

同时修改 handleQuery 中的 finally 块：移除 pendingSelectedNode 机制（因为不再需要延迟处理），改为只在 dataLoading 完成后正常结束。

还需要添加请求版本号机制，确保旧请求的结果不会覆盖新请求的数据：
```
let queryVersion = 0  // 请求版本号

async function handleQuery(preserveExpand = false) {
  if (!preserveExpand) {
    selectedIds.value = []
  }
  const currentVersion = ++queryVersion  // 每次查询递增版本号
  dataLoading.value = true
  try {
    const res = await queryEntries(...)
    // 如果版本号不匹配，说明有更新的查询已发起，丢弃本次结果
    if (currentVersion !== queryVersion) return
    ...
  } finally {
    // 只有当前版本仍是最新时才关闭 loading
    if (currentVersion === queryVersion) {
      dataLoading.value = false
    }
  }
}
```

### 问题2：统计视图没有加载遮罩

**根因**：StatsTab.vue 的 `loadStats` 函数（line 75-124）没有任何加载状态变量或加载遮罩。

对比 DataListTab.vue 有 `dataLoading` 和加载遮罩（line 117-120）：
```html
<div v-if="dataLoading" class="batch-overlay">
  <el-icon class="is-loading" style="font-size:28px;color:#409eff;margin-bottom:8px;"><Loading /></el-icon>
  <span style="color:#666;font-size:14px;">数据加载中...</span>
</div>
```

**修复**：在 StatsTab.vue 中添加 `statsLoading` ref，在 `loadStats` 中设置加载状态，并在模板中添加加载遮罩。

## 涉及文件

- `frontend/src/components/DataListTab.vue` — 问题1：修改 watch selectedNode 和 handleQuery
- `frontend/src/components/StatsTab.vue` — 问题2：添加加载遮罩
- `VERSION.md`

## 实施步骤

### 步骤1：修复问题1 - DataListTab.vue

1. 添加 `queryVersion` 变量（line 653附近）
2. 修改 `handleQuery`：
   - 在函数开头递增 queryVersion 并保存 currentVersion
   - 查询结果返回后检查 `currentVersion !== queryVersion`，如果版本不匹配则 return（丢弃旧结果）
   - finally 中只在 `currentVersion === queryVersion` 时才设 `dataLoading = false`
   - 移除 pendingSelectedNode 机制（不再需要）
3. 修改 watch selectedNode（line 2850-2862）：
   - 移除 dataLoading 检查和 pendingSelectedNode 保存逻辑
   - 直接调用 handleQuery()

### 步骤2：修复问题2 - StatsTab.vue

1. 添加 `statsLoading` ref
2. 在 `loadStats` 中设置 `statsLoading = true`（开始时），`statsLoading = false`（结束时）
3. 在模板中添加加载遮罩（类似 DataListTab 的 batch-overlay）

### 步骤3：更新 VERSION.md

在产品清单分类下添加变更说明。

## 验证

1. 前端 npm run build ✅
2. 清单加载期间点击导航树：加载完成后清单内容与新导航树节点匹配
3. 统计视图页签：数据加载时显示加载遮罩
4. 快速连续点击不同导航节点：最终清单内容与最后选中的节点匹配
