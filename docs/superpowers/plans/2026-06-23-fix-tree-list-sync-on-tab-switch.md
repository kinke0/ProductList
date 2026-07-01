# 切换清单时导航树与清单数据不一致修复计划

## 目标
修复切换清单tab时，加载遮罩期间点击左侧导航树导致清单数据与导航树选中项不一致的bug。

## 根因
1. 切换清单tab → `onTabClick()` 触发 `handleQuery()` → `dataLoading=true` → 显示遮罩
2. 遮罩期间点击导航树 → `onTreeSelect()` 更新 `selectedNode` → 触发 DataListTab watch → 又发起一个 `handleQuery()`
3. 两个并发请求，后完成的请求覆盖数据。第二个请求使用了新的 selectedNode，第一个使用旧的，导致数据不一致

## 涉及文件
- `frontend/src/views/dashboard/DataWorkbench.vue` — 禁止加载中点击导航树
- `frontend/src/components/DataListTab.vue` — 防止并发查询，selectedNode watch 中检查 dataLoading

## 实施步骤

### Step 1: DataListTab.vue — selectedNode watch 中防并发
在 `watch(() => props.selectedNode)` 中，如果 `dataLoading` 为 true，记录 pendingSelectedNode，等当前 handleQuery 完成后再执行。

具体改动：
```javascript
// 新增 ref
const pendingSelectedNode = ref(null)

// 修改 watch
watch(() => props.selectedNode, () => {
  if (dataLoading.value) {
    // 加载中，记录等待的节点，等加载完成后执行
    pendingSelectedNode.value = props.selectedNode
    return
  }
  if (props.selectedNode && props.versionId) {
    handleQuery()
  }
  if (isNew.value) {
    fillCategoryAndDomain()
  }
}, { deep: true })

// 在 handleQuery 的 finally 中检查 pendingSelectedNode
async function handleQuery(preserveExpand = false) {
  // ...existing code...
  finally {
    dataLoading.value = false
    // 检查是否有等待中的导航树选择
    if (pendingSelectedNode.value) {
      const pending = pendingSelectedNode.value
      pendingSelectedNode.value = null
      // 用 nextTick 确保 selectedNode 已经更新
      await nextTick()
      if (props.selectedNode && props.versionId) {
        handleQuery()
      }
      if (isNew.value) {
        fillCategoryAndDomain()
      }
    }
  }
}
```

### Step 2: DataWorkbench.vue — 加载中禁止导航树交互
在 TreePanel 上添加 `dataLoading` 传递，或用 CSS pointer-events 禁止点击。

更简洁的方案：在 DataWorkbench 中跟踪加载状态，加载中给 TreePanel 的容器加上 pointer-events: none。

具体改动：DataListTab 暴露 `dataLoading`，DataWorkbench 中获取并控制 TreePanel 的交互性。

但 DataListTab 已有 `dataLoading` ref，需要通过 defineExpose 暴露出去，然后在 DataWorkbench 中通过 ref 读取。

或者更简单的方案：直接在 TreePanel 区域上叠加一个透明遮罩，当 DataListTab 加载中时禁止点击。

最终方案：在 DataListTab watch selectedNode 时，如果正在加载，则延迟到加载完成后才触发查询。这样不需要修改 TreePanel 的交互，用户体验更自然（导航树选中会立即更新，但清单数据会在加载完成后才刷新）。
