# Bug修复：多选后右键复制/剪切只复制一行

## 目标
修复数据清单中多选行后右键复制/剪切，只复制了右键点击的那一行而非所有选中行的问题。

## 根因
`onCtxCopy`（第2282行）和 `onCtxCut`（第2295行）硬编码为只使用右键点击行的 id：
```javascript
clipboard.entryIds = [ctxMenu.row.id]
```
项目中已有 `selectedIds` 用于多选状态，其他批量操作（删除、版本划分等）都使用 `selectedIds.value`，但复制/剪切没有。

## 涉及文件
- `frontend/src/components/DataListTab.vue` — 修改 `onCtxCopy` 和 `onCtxCut` 函数
- `VERSION.md` — 追加变更说明

## 分步实施

### Step 1: 修改 `onCtxCopy` 函数
当 `selectedIds.value` 包含右键点击行的 id 时，使用 `selectedIds.value` 作为复制的 ID 列表；否则只复制右键点击行。

```javascript
function onCtxCopy() {
  if (ctxMenu.row) {
    // 多选时复制所有选中行，单选时只复制右键行
    clipboard.entryIds = selectedIds.value.includes(ctxMenu.row.id)
      ? [...selectedIds.value]
      : [ctxMenu.row.id]
  } else {
    ElMessage.warning('请先勾选条目')
    closeContextMenu()
    return
  }
  clipboard.mode = 'copy'
  closeContextMenu()
  ElMessage.success(`已复制 ${clipboard.entryIds.length} 个节点`)
}
```

### Step 2: 修改 `onCtxCut` 函数
同理，剪切也使用相同的多选逻辑。

```javascript
function onCtxCut() {
  if (ctxMenu.row) {
    clipboard.entryIds = selectedIds.value.includes(ctxMenu.row.id)
      ? [...selectedIds.value]
      : [ctxMenu.row.id]
  } else {
    ElMessage.warning('请先勾选条目')
    closeContextMenu()
    return
  }
  clipboard.mode = 'cut'
  closeContextMenu()
  ElMessage.success(`已剪切 ${clipboard.entryIds.length} 个节点`)
}
```

### Step 3: 更新 VERSION.md
在 V1.0.12 产品清单部分追加修复说明。

### Step 4: 验证
- 前端构建 `npm run build`
- 后端编译 `mvn compile`
- 重启服务并 curl 验证
