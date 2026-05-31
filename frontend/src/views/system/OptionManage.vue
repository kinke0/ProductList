<template>
  <div class="page">
    <div class="page-header">
      <h3>{{ title }}</h3>
      <el-button size="small" type="primary" @click="openDialog()">新增</el-button>
    </div>
    <el-table :data="items" border stripe size="small" @row-click="onRowClick" highlight-current-row ref="tableRef">
      <el-table-column label="排序" width="100" align="center">
        <template #default="{ row }">
          <span class="drag-icon" @mousedown="startDrag($event, row)" :style="{ cursor: versionStatus === 'released' ? 'default' : 'grab' }">⠿</span>
        </template>
      </el-table-column>
      <el-table-column prop="value" label="名称" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" link @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="isEdit ? '编辑' : '新增'" width="420px">
      <el-form :model="form" label-width="80px" size="small">
        <el-form-item label="名称">
          <el-input v-model="form.value" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getOptions, createOption, updateOption, deleteOption, updateOptionSort } from '../../api/option'
import { getVersions } from '../../api/version'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const type = computed(() => route.meta.type || 'status')
const title = computed(() => route.meta.title || '选项维护')

const versionId = ref(null)
const versionStatus = ref('draft')
const items = ref([])
const selectedRow = ref(null)
const tableRef = ref(null)
const showDialog = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const form = ref({ value: '' })

async function loadVersion() {
  const res = await getVersions()
  const draft = res.data.find(v => v.status === 'draft')
  const ver = draft || res.data[res.data.length - 1]
  versionId.value = ver.id
  versionStatus.value = ver.status
}

async function loadData() {
  if (!versionId.value) return
  const res = await getOptions(versionId.value, type.value)
  items.value = res.data || []
  selectedRow.value = null
}

function onRowClick(row) {
  selectedRow.value = row
}

function openDialog(row) {
  if (row) { isEdit.value = true; editingId.value = row.id; form.value = { value: row.value } }
  else { isEdit.value = false; editingId.value = null; form.value = { value: '' } }
  showDialog.value = true
}

async function handleSave() {
  if (!form.value.value) { ElMessage.warning('请输入名称'); return }
  if (isEdit.value) await updateOption(editingId.value, form.value.value)
  else await createOption(versionId.value, type.value, form.value.value)
  ElMessage.success('保存成功')
  showDialog.value = false
  loadData()
}

async function handleDelete(id) {
  ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }).then(async () => {
    await deleteOption(id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

async function reorderList(fromIdx, toIdx) {
  const list = [...items.value]
  const [moved] = list.splice(fromIdx, 1)
  list.splice(toIdx, 0, moved)
  const sortList = list.map((item, i) => ({ id: item.id, sortOrder: i }))
  await updateOptionSort(sortList)
  await loadData()
}

function startDrag(e, row) {
  if (e.button !== 0 || versionStatus.value === 'released') return
  e.preventDefault()
  const idx = items.value.findIndex(r => r.id === row.id)
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
      const rRect = allRows[i].getBoundingClientRect()
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
      await reorderList(idx, targetIdx)
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

onMounted(async () => {
  await loadVersion()
  await loadData()
})

watch(() => route.path, async () => {
  await loadVersion()
  await loadData()
})
</script>

<style scoped>
.page { padding: 20px 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--si-border); }
.page-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: var(--si-text-primary); }
:deep(.el-table) { border-radius: var(--si-radius-md); }
:deep(.el-table th.el-table__cell) { background: var(--si-bg-hover); color: var(--si-text-secondary); font-weight: 600; }

.drag-icon {
  cursor: grab;
  font-size: 14px;
  color: #94A3B8;
  user-select: none;
  margin-right: 6px;
}

.drag-icon:hover {
  color: #2563EB;
}

.sort-btns {
  display: inline-flex;
  flex-direction: column;
  gap: 0;
  margin-left: 2px;
}

.sort-btn {
  font-size: 11px;
  color: #94A3B8;
  cursor: pointer;
  line-height: 14px;
  user-select: none;
}

.sort-btn:hover {
  color: #2563EB;
}
</style>
