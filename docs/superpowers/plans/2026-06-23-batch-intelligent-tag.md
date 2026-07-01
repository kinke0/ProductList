# 批量智能化标注功能实施计划

## 目标
在"其他批量操作"下拉菜单中新增"智能化标注"选项，弹窗中提供"标记智能化"和"取消智能化"两个操作按钮，将选中条目的 `colIntelligent` 批量设为 `"1"` 或清空。

## 涉及文件
- `frontend/src/components/DataListTab.vue` — 下拉菜单项、弹窗UI、处理逻辑

## 实施步骤

### Step 1: 下拉菜单添加"智能化标注"选项
在两处 `<el-dropdown-menu>`（编辑态 line 57-63 和非编辑态 line 89-95）中，在"版本划分"之后、"批量移除/删除"之前，添加：
```html
<el-dropdown-item command="intelligent">智能化标注</el-dropdown-item>
```

### Step 2: 新增 ref 变量
在 ref 定义区域（约 line 657），新增：
```javascript
const showBatchIntelligentDialog = ref(false)
const batchIntelligentAction = ref('mark')  // 'mark' 或 'unmark'
```

### Step 3: onBatchCommand 处理 intelligent command
在 `onBatchCommand` 函数（line 1738）中，在 `cmd === 'version'` 分支之后添加：
```javascript
} else if (cmd === 'intelligent') {
  batchIntelligentAction.value = 'mark'
  showBatchIntelligentDialog.value = true
```

### Step 4: 新增弹窗UI
在 `showBatchVersionDialog` 的 el-dialog 之后（约 line 511），添加智能化标注弹窗：
```html
<el-dialog v-model="showBatchIntelligentDialog" title="批量智能化标注" width="400px">
  <div style="display:flex;flex-direction:column;gap:12px;padding:10px 0;">
    <el-radio-group v-model="batchIntelligentAction">
      <el-radio value="mark">标记智能化（colIntelligent = 是）</el-radio>
      <el-radio value="unmark">取消智能化标记（colIntelligent = 空）</el-radio>
    </el-radio-group>
  </div>
  <template #footer>
    <el-button @click="showBatchIntelligentDialog = false">取消</el-button>
    <el-button type="primary" @click="confirmBatchIntelligent">确定</el-button>
  </template>
</el-dialog>
```

### Step 5: 新增 confirmBatchIntelligent 函数
参考 `confirmBatchVersion` 的模式，循环逐条 `updateEntry`：
```javascript
async function confirmBatchIntelligent() {
  const intelligentValue = batchIntelligentAction.value === 'mark' ? '1' : ''
  const actionLabel = batchIntelligentAction.value === 'mark' ? '标记智能化' : '取消智能化标记'
  showBatchIntelligentDialog.value = false
  batchLoading.value = true
  try {
    let successCount = 0
    for (const id of selectedIds.value) {
      try {
        const row = findRowById(id, tableData.value)
        if (row) {
          await updateEntry(id, { ...row, colIntelligent: intelligentValue })
          successCount++
        }
      } catch (e) { console.error(`${actionLabel}失败 id=${id}:`, e) }
    }
    ElMessage.success(`成功${actionLabel} ${successCount} 条`)
    handleQuery(true)
  } finally { batchLoading.value = false }
}
```

### Step 6: 更新 VERSION.md
在当前版本的变更说明列表中追加：
```
- 产品清单 > 新增批量智能化标注功能：其他批量操作菜单中新增"智能化标注"选项，支持批量标记/取消智能化
```

## 验证
前端构建 + 后端编译 + 服务重启 + 功能测试
