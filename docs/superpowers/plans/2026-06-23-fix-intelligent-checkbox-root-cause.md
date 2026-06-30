# 修复：智能化复选框两个 Bug

## 目标
修复编辑表单中"智能化"复选框的两个 Bug：
1. 点击 checkbox 方框需要两次才能选中，点击文字一次就能选中
2. 勾选智能化后，表单尚未保存，但修改直接生效

## 根因分析

### Bug 1：点击方框两次才选中

**根因**：el-checkbox 没有自己的 slot 内容（文字标签），当前代码：
```html
<el-checkbox v-model="editForm.colIntelligent" true-value="1" false-value="" />
```

Element Plus 内部（`use-checkbox-event.mjs`）有一个已知 bug (issue #9981)：
- 当 `hasOwnLabel = false`（无 slot 内容）且 `isLabeledByFormItem = true`（在 el-form-item 内）时
- el-checkbox 根元素渲染为 `<span>` 而非 `<label>`
- 点击方框时，`onClickRoot` 和原生 `handleChange` **双重触发**
- 第一次点击：`onClickRoot` 手动设置值 → `handleChange` 也设置值 → 值被处理两次，可能冲突
- 点击文字区域时：文字属于 el-form-item 的 `<label>`，`onClickRoot` 检测到 LABEL 后不执行手动赋值，只有 `handleChange` 正常触发一次

**修复**：给 el-checkbox 添加文字 slot 内容，让 `hasOwnLabel = true`。

### Bug 2：未保存就生效

**可能根因**：可能是 Bug 1 的副作用（双重触发导致值被错误设置后看起来"直接生效"）。
也可能是 `onDialogChange()` 在对话框关闭时自动调用 `updateEntry()` 保存数据，用户关闭对话框后以为没有保存但实际上已保存。

先修复 Bug 1，验证 Bug 2 是否随之解决。

## 涉及文件
- `frontend/src/components/DataListTab.vue`

## 实施步骤

### Step 1：给智能化 checkbox 添加文字 slot
```html
<!-- 修改前 -->
<el-checkbox v-model="editForm.colIntelligent" true-value="1" false-value="" size="small" :disabled="!props.isEditing" />

<!-- 修改后 -->
<el-checkbox v-model="editForm.colIntelligent" true-value="1" false-value="" size="small" :disabled="!props.isEditing">是</el-checkbox>
```

### Step 2：验证构建
- 前端 `npm run build`
- 后端 `mvn compile`
