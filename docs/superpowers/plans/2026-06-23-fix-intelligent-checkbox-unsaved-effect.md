# 修复：智能化复选框勾选后未保存就生效的问题

## 目标
修复编辑表单中勾选"智能化"复选框后，表单尚未保存但修改直接生效的问题。

## 根因分析

### 问题现象
用户勾选"智能化"checkbox 后，无需点保存按钮，修改就已经"生效"了。

### 根因
`editRow()` 函数中使用 `Object.assign(editForm, initialFormState(), row)` 把 `row`（表格行数据，Vue 3 reactive proxy）的所有属性直接拷贝到 `editForm`（也是 reactive proxy）中。

**关键问题**：`row` 来自 `tableData.value`，是 Vue 3 `ref` 数组中的响应式代理对象。虽然对于字符串类型的属性（如 `colIntelligent`），`Object.assign` 是值拷贝，两者应该是独立的，但 Vue 3 的 reactive proxy 机制可能在某些边缘情况下导致属性追踪异常。

更可能的原因是：当 `Object.assign(editForm, initialFormState(), row)` 执行后，`editForm` 上除了表单字段之外，还携带了 `row` 的其他属性（如 `id`、`level`、`parentId`、`children` 等）。其中 `children` 是数组引用，修改 `editForm.children` 就等于修改 `row.children`。虽然 `colIntelligent` 是字符串不受此影响，但整体数据结构的共享可能导致 Vue 的响应式追踪出现异常行为。

此外，之前的修复已在 `editForm` 的 `reactive()` 定义中添加了 `colIntelligent` 属性，解决了第一次点击无效的问题。但"未保存就生效"的问题可能需要额外的措施——确保 `editForm` 和 `row` 之间没有任何引用共享。

## 涉及文件
- `frontend/src/components/DataListTab.vue` — 修改 `editRow()` 和 `viewRow()` 函数

## 实施步骤

### Step 1: 修改 `editRow()` 和 `viewRow()` 函数中的 `Object.assign` 逻辑
将 `Object.assign(editForm, initialFormState(), row)` 改为**只拷贝表单需要的字段**到 `editForm`，而不是拷贝 `row` 的所有属性。

这样确保 `editForm` 只包含表单字段，不包含 `row` 的 `id`、`level`、`parentId`、`children` 等非表单属性，彻底断开 `editForm` 和 `row` 之间的引用共享。

**改动说明**：
```javascript
// 修改前
Object.assign(editForm, initialFormState(), row)

// 修改后：只拷贝 editForm 中已定义的表单字段
const formKeys = Object.keys(initialFormState())
Object.assign(editForm, initialFormState())
formKeys.forEach(key => {
  if (row[key] !== undefined) editForm[key] = row[key]
})
```

同样修改 `viewRow()` 函数中的 `Object.assign`。

### Step 2: 验证构建
- 前端 `npm run build`
- 后端 `mvn compile`
