# 优化：清单数据操作完成后清除选中项

## 目标
每次完成对清单数据的操作后（复制、剪切、粘贴、升级、降级、上移、下移），自动清除所有选中行，避免选中状态残留导致后续误操作。

## 根因
当前只在批量删除、批量修改分类/域、移除、非保留刷新时清除 selectedIds。复制/剪切/粘贴/升级/降级/上移/下移等操作完成后不清除选中状态。

## 涉及文件
- `frontend/src/components/DataListTab.vue` — 在以下函数成功后追加 `selectedIds.value = []`：
  - onCtxCopy、onCtxCut、onCtxPasteSibling、onCtxPasteChild
  - onCtxLevelUp、onCtxLevelDown、onCtxMoveUp、onCtxMoveDown
- `VERSION.md` — 追加变更说明

## 分步实施

### Step 1: 修改各操作函数，成功后清除 selectedIds

在以下函数中，`handleQuery(true)` 调用前加 `selectedIds.value = []`：

- onCtxCopy（第2291行后）
- onCtxCut（第2306行后）
- onCtxPasteSibling（第2329行前）
- onCtxPasteChild（第2356行前）
- onCtxLevelUp（第2371行前）
- onCtxLevelDown（第2384行前）
- onCtxMoveUp（第2396行前）
- onCtxMoveDown（第2408行前）

### Step 2: 更新 VERSION.md

### Step 3: 验证
