# 修复：Bug 2 — 智能化复选框勾选后未保存就生效

## 目标
修复编辑表单中勾选"智能化"复选框后，表单尚未保存但修改直接生效的问题。

## 根因分析

`onDialogChange()` 函数（第1222-1237行）在对话框关闭时自动调用 `updateEntry(editingId.value, editForm)` 保存所有表单数据。这意味着用户任何关闭对话框的操作（点击右上角 X、按 ESC、点击遮罩层），都会触发自动保存。用户没有点"保存"按钮，但关闭对话框时数据已经被自动提交到后端。

```javascript
async function onDialogChange(val) {
  if (!val && showEditDialog.value && !autoSaving && !isNew.value && editingId.value) {
    autoSaving = true
    try {
      editForm.colFeatureDesc = editorRef.value?.innerHTML || ''
      await updateEntry(editingId.value, editForm)  // ← 自动保存！
      flushPendingImageUpdates()
      handleQuery(true)
    } catch (e) { /* ignore */ }
    autoSaving = false
  }
  showEditDialog.value = val
  ...
}
```

`saveEdit()` 函数在保存时设置 `autoSaving = true`，然后设置 `showEditDialog.value = false`。此时 `onDialogChange` 检测到 `!autoSaving` 为 false，跳过自动保存。所以点击"保存"按钮时不会重复保存。

但用户关闭对话框时（不点保存），`autoSaving` 为 false，`onDialogChange` 就会执行自动保存。这违背了用户预期。

## 涉及文件
- `frontend/src/components/DataListTab.vue`

## 实施步骤

### Step 1：移除 `onDialogChange` 的自动保存逻辑

将 `onDialogChange` 改为仅关闭对话框，不自动保存：

```javascript
// 修改前
async function onDialogChange(val) {
  if (!val && showEditDialog.value && !autoSaving && !isNew.value && editingId.value) {
    autoSaving = true
    try {
      editForm.colFeatureDesc = editorRef.value?.innerHTML || ''
      await updateEntry(editingId.value, editForm)
      flushPendingImageUpdates()
      handleQuery(true)
    } catch (e) { /* ignore */ }
    autoSaving = false
  }
  showEditDialog.value = val
  if (!val) {
    pendingImageUpdates.value = []
  }
}

// 修改后
function onDialogChange(val) {
  showEditDialog.value = val
  if (!val) {
    pendingImageUpdates.value = []
  }
}
```

注意：移除自动保存后，`autoSaving` 变量仅在 `saveEdit()` 中使用，可以保留不影响。

### Step 2：验证构建
- 前端 `npm run build`
- 后端 `mvn compile`
