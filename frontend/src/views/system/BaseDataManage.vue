<template>
  <div class="page">
    <h3>基础数据维护</h3>
    <p class="subtitle">维护左侧层级树的业务分类（L1）与业务域（L2）</p>

    <div class="dual-tables">
      <div class="table-wrapper">
        <div class="table-header">
          <strong>业务分类 (L1)</strong>
          <div>
            <el-button size="small" type="primary" @click="openL1Dialog()" :disabled="versionStatus === 'released'">新增</el-button>
            <el-button size="small" @click="moveUp('l1')" :disabled="versionStatus === 'released'||!selectedL1" style="margin-left:4px;">↑</el-button>
            <el-button size="small" @click="moveDown('l1')" :disabled="versionStatus === 'released'||!selectedL1">↓</el-button>
          </div>
        </div>
        <el-table
          :data="l1List"
          border
          stripe
          size="small"
          highlight-current-row
          @current-change="onL1Select"
          style="cursor: pointer;"
        >
          <el-table-column label="" width="30">
            <template #default="{ row }">
              <span class="drag-icon" @mousedown="startDrag($event, row, 'l1')"
                :style="{ cursor: versionStatus === 'released' ? 'default' : 'grab' }">⠿</span>
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="160">
            <template #default="{ row }">
              {{ row.label }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="openL1Dialog(row)" :disabled="versionStatus === 'released'">编辑</el-button>
              <el-button size="small" type="danger" link @click="deleteL1(row)" :disabled="versionStatus === 'released'">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-wrapper">
        <div class="table-header">
          <strong>业务域 (L2)</strong>
          <div>
            <span v-if="selectedL1" style="font-size:12px;color:#999;margin-right:8px;">
              当前: {{ selectedL1.label }}
            </span>
            <el-button size="small" type="primary" :disabled="!selectedL1 || versionStatus === 'released'" @click="openL2Dialog()">新增</el-button>
            <el-button size="small" @click="moveUp('l2')" :disabled="versionStatus === 'released'||!selectedL2" style="margin-left:4px;">↑</el-button>
            <el-button size="small" @click="moveDown('l2')" :disabled="versionStatus === 'released'||!selectedL2">↓</el-button>
          </div>
        </div>
        <el-table
          v-if="selectedL1"
          :data="l2List"
          border
          stripe
          size="small"
          highlight-current-row
          @current-change="onL2Select"
        >
          <el-table-column label="" width="30">
            <template #default="{ row }">
              <span class="drag-icon" @mousedown="startDrag($event, row, 'l2')"
                :style="{ cursor: versionStatus === 'released' ? 'default' : 'grab' }">⠿</span>
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="160">
            <template #default="{ row }">
              {{ row.label }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="openL2Dialog(row)" :disabled="versionStatus === 'released'">编辑</el-button>
              <el-button size="small" type="danger" link @click="deleteL2(row)" :disabled="versionStatus === 'released'">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="placeholder">
          请先在左侧选择一个业务分类
        </div>
      </div>
    </div>

    <el-dialog v-model="l1Dialog" :title="isNewL1 ? '新增业务分类' : '编辑业务分类'" width="420px">
      <el-form :model="l1Form" label-width="80px" size="small">
        <el-form-item label="名称">
          <el-input v-model="l1Form.colBizCategory" placeholder="如: 5. 智慧医疗" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="l1Dialog = false">取消</el-button>
        <el-button type="primary" @click="saveL1">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="l2Dialog" :title="isNewL2 ? '新增业务域' : '编辑业务域'" width="420px">
      <el-form :model="l2Form" label-width="80px" size="small">
        <el-form-item label="名称">
          <el-input v-model="l2Form.colBizDomain" placeholder="如: 5.1 门诊诊疗业务" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="l2Dialog = false">取消</el-button>
        <el-button type="primary" @click="saveL2">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCategoryTree, createCategory, updateCategory, deleteCategory, createDomain, updateDomain, deleteDomain, updateCategorySort } from '../../api/category'
import { getVersions } from '../../api/version'
import { ElMessage, ElMessageBox } from 'element-plus'

const l1List = ref([])
const l2List = ref([])
const selectedL1 = ref(null)
const selectedL2 = ref(null)
const l1Dialog = ref(false)
const l2Dialog = ref(false)
const isNewL1 = ref(false)
const isNewL2 = ref(false)
const l1Form = ref({ colBizCategory: '' })
const l2Form = ref({ colBizDomain: '' })
const editingL2Id = ref(null)
const editingL1Id = ref(null)

let versionId = null
const versionStatus = ref(null)

async function loadVersion() {
  const res = await getVersions()
  const draft = res.data.find(v => v.status === 'draft')
  versionId = draft ? draft.id : res.data[res.data.length - 1].id
  versionStatus.value = draft ? 'draft' : 'released'
}

async function loadL1() {
  const res = await getCategoryTree(versionId)
  l1List.value = res.data || []
}

async function loadL2(l1Id) {
  const l1 = l1List.value.find(c => c.id === l1Id)
  l2List.value = l1?.children || []
}

function onL1Select(row) {
  selectedL1.value = row
  selectedL2.value = null
  if (row) loadL2(row.id)
}

function onL2Select(row) {
  selectedL2.value = row
}

function startDrag(e, row, type) {
  if (e.button !== 0 || versionStatus.value === 'released') return
  e.preventDefault()
  const list = type === 'l1' ? l1List : l2List
  const idx = list.value.findIndex(r => r.id === row.id)
  if (idx < 0) return
  const tr = e.target.closest('tr')
  if (!tr) return
  const tbody = tr.parentElement
  const allRows = Array.from(tbody.querySelectorAll('tr'))
  const rect = tr.getBoundingClientRect()
  const ghost = tr.cloneNode(true)
  ghost.style.cssText = `position:fixed;top:${rect.top}px;left:${rect.left}px;width:${rect.width}px;z-index:9999;opacity:0.85;pointer-events:none;box-shadow:0 4px 16px rgba(0,0,0,0.15);background:#fff`
  document.body.appendChild(ghost)
  tr.style.opacity = '0.3'

  const offsetY = e.clientY - rect.top
  let targetIdx = idx

  const onMove = (ev) => {
    ghost.style.top = (ev.clientY - offsetY) + 'px'
    for (let i = 0; i < allRows.length; i++) {
      const r = allRows[i]
      const rRect = r.getBoundingClientRect()
      if (ev.clientY < rRect.top + rRect.height / 2) {
        targetIdx = i; break
      }
      targetIdx = allRows.length
    }
    showDropLine(allRows, targetIdx)
  }

  const onUp = async () => {
    ghost.remove(); tr.style.opacity = '1'
    removeDropLine()
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    if (targetIdx !== idx && targetIdx >= 0) {
      await reorderList(type, idx, targetIdx)
    }
  }

  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

function showDropLine(rows, idx) {
  removeDropLine()
  const targetRow = idx < rows.length ? rows[idx] : rows[rows.length - 1]
  if (!targetRow) return
  const r = targetRow.getBoundingClientRect()
  const line = document.createElement('div')
  line.className = 'drop-line'
  line.style.cssText = `position:fixed;left:${r.left}px;width:${r.width}px;top:${idx < rows.length ? r.top : r.bottom}px;height:2px;background:#2563EB;z-index:10000;pointer-events:none`
  document.body.appendChild(line)
}

function removeDropLine() {
  document.querySelectorAll('.drop-line').forEach(el => el.remove())
}

async function reloadKeepingSelection(type) {
  const selL1Id = selectedL1.value?.id
  const selL2Id = selectedL2.value?.id
  await loadL1()
  if (selL1Id) {
    const found = l1List.value.find(c => c.id === selL1Id)
    selectedL1.value = found || null
    if (found) {
      await loadL2(found.id)
      if (selL2Id && type === 'l2') {
        selectedL2.value = l2List.value.find(d => d.id === selL2Id) || null
      }
    }
  }
}

async function reorderList(type, fromIdx, toIdx) {
  const list = type === 'l1' ? l1List : l2List
  const items = [...list.value]
  const [moved] = items.splice(fromIdx, 1)
  items.splice(toIdx, 0, moved)
  const sortList = items.map((item, i) => ({
    type: type === 'l1' ? 'category' : 'domain',
    id: item.id,
    sortOrder: i
  }))
  await updateCategorySort(versionId, sortList)
  await reloadKeepingSelection(type)
}

async function moveUp(type) {
  const list = type === 'l1' ? l1List : l2List
  const selected = type === 'l1' ? selectedL1 : selectedL2
  const idx = list.value.findIndex(r => r.id === selected.value.id)
  if (idx <= 0) return
  const items = [...list.value]
  ;[items[idx - 1], items[idx]] = [items[idx], items[idx - 1]]
  const sortList = items.map((item, i) => ({
    type: type === 'l1' ? 'category' : 'domain',
    id: item.id,
    sortOrder: i
  }))
  await updateCategorySort(versionId, sortList)
  await reloadKeepingSelection(type)
  selectedL2.value = null
}

async function moveDown(type) {
  const list = type === 'l1' ? l1List : l2List
  const selected = type === 'l1' ? selectedL1 : selectedL2
  const idx = list.value.findIndex(r => r.id === selected.value.id)
  if (idx < 0 || idx >= list.value.length - 1) return
  const items = [...list.value]
  ;[items[idx], items[idx + 1]] = [items[idx + 1], items[idx]]
  const sortList = items.map((item, i) => ({
    type: type === 'l1' ? 'category' : 'domain',
    id: item.id,
    sortOrder: i
  }))
  await updateCategorySort(versionId, sortList)
  await reloadKeepingSelection(type)
  selectedL2.value = null
}

function openL1Dialog(row) {
  if (row) {
    isNewL1.value = false
    editingL1Id.value = row.id
    l1Form.value = { colBizCategory: row.label }
  } else {
    isNewL1.value = true
    editingL1Id.value = null
    l1Form.value = { colBizCategory: '' }
  }
  l1Dialog.value = true
}

async function saveL1() {
  if (!l1Form.value.colBizCategory) {
    ElMessage.warning('请输入名称')
    return
  }
  if (isNewL1.value) {
    await createCategory(versionId, l1Form.value.colBizCategory)
    ElMessage.success('创建成功')
  } else {
    await updateCategory(editingL1Id.value, l1Form.value.colBizCategory)
    ElMessage.success('保存成功')
  }
  l1Dialog.value = false
  await loadL1()
}

async function deleteL1(row) {
  ElMessageBox.confirm(`确认删除业务分类"${row.label}"？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteCategory(row.id)
      ElMessage.success('删除成功')
      if (selectedL1.value?.id === row.id) {
        selectedL1.value = null
        l2List.value = []
      }
      await loadL1()
    } catch (e) {
      const msg = e?.response?.data?.message || '删除失败'
      ElMessage.warning(msg)
    }
  }).catch(() => {})
}

function openL2Dialog(row) {
  if (row) {
    isNewL2.value = false
    editingL2Id.value = row.id
    l2Form.value = { colBizDomain: row.label }
  } else {
    isNewL2.value = true
    editingL2Id.value = null
    l2Form.value = { colBizDomain: '' }
  }
  l2Dialog.value = true
}

async function saveL2() {
  if (!l2Form.value.colBizDomain) {
    ElMessage.warning('请输入名称')
    return
  }
  if (isNewL2.value) {
    await createDomain(versionId, selectedL1.value.id, l2Form.value.colBizDomain)
    ElMessage.success('创建成功')
  } else {
    await updateDomain(editingL2Id.value, l2Form.value.colBizDomain)
    ElMessage.success('保存成功')
  }
  l2Dialog.value = false
  await loadL1()
  await loadL2(selectedL1.value.id)
}

async function deleteL2(row) {
  try {
    await deleteDomain(row.id)
    ElMessage.success('删除成功')
    await loadL1()
    await loadL2(selectedL1.value.id)
  } catch (e) {
    const msg = e?.response?.data?.message || '删除失败'
    ElMessage.warning(msg)
  }
}


onMounted(async () => {
  await loadVersion()
  await loadL1()
})
</script>

<style scoped>
.page { padding: 20px 24px; }
h3 { margin: 0 0 4px; font-size: 16px; font-weight: 600; color: var(--si-text-primary); }
.subtitle { margin: 0 0 20px; font-size: 13px; color: var(--si-text-muted); }
.dual-tables {
  display: flex;
  gap: 16px;
}
.table-wrapper {
  flex: 1;
  background: var(--si-bg-card);
  border: 1px solid var(--si-border);
  border-radius: var(--si-radius-lg);
  padding: 16px;
  box-shadow: var(--si-shadow-sm);
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--si-border-light);
}
.table-header strong { color: var(--si-text-primary); font-size: 14px; }
.placeholder {
  text-align: center;
  padding: 40px;
  color: var(--si-text-muted);
  font-size: 14px;
}
:deep(.el-table) { border-radius: var(--si-radius-md); }
:deep(.el-table th.el-table__cell) { background: var(--si-bg-hover); color: var(--si-text-secondary); font-weight: 600; }
</style>
