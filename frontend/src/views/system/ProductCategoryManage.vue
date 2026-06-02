<template>
  <div class="page">
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px;">
      <h3>产品分类维护</h3>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :show-file-list="false"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
        :disabled="versionStatus === 'released'"
      >
        <el-button type="success" :disabled="versionStatus === 'released'">
          <el-icon><Upload /></el-icon>
          导入Excel
        </el-button>
      </el-upload>
    </div>
    <p class="subtitle">维护两级产品分类：统计分类（L1）→ 核心业务产品（L2）</p>

    <div class="dual-tables">
      <!-- L1 统计分类 -->
      <div class="table-wrapper">
        <div class="table-header">
          <strong>统计分类 (L1)</strong>
          <div>
            <el-button size="small" type="primary" @click="openL1Dialog()" :disabled="versionStatus === 'released'">新增</el-button>
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
          <el-table-column label="名称" min-width="200">
            <template #default="{ row }">
              {{ row.name }}
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

      <!-- L2 核心业务产品 -->
      <div class="table-wrapper">
        <div class="table-header">
          <strong>核心业务产品 (L2)</strong>
          <div>
            <span v-if="selectedL1" style="font-size:12px;color:#999;margin-right:8px;">
              当前: {{ selectedL1.name }}
            </span>
            <el-button size="small" type="primary" :disabled="!selectedL1 || versionStatus === 'released'" @click="openL2Dialog()">新增</el-button>
          </div>
        </div>
        <el-table
          v-if="selectedL1"
          :data="l2List"
          border
          stripe
          size="small"
          highlight-current-row
        >
          <el-table-column label="" width="30">
            <template #default="{ row }">
              <span class="drag-icon" @mousedown="startDrag($event, row, 'l2')"
                :style="{ cursor: versionStatus === 'released' ? 'default' : 'grab' }">⠿</span>
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="200">
            <template #default="{ row }">
              {{ row.name }}
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
          请先在左侧选择一个统计分类
        </div>
      </div>
    </div>

    <!-- L1 新增/编辑弹窗 -->
    <el-dialog v-model="l1Dialog" :title="isNewL1 ? '新增统计分类' : '编辑统计分类'" width="420px">
      <el-form :model="l1Form" label-width="80px" size="small">
        <el-form-item label="名称">
          <el-input v-model="l1Form.name" placeholder="如: 医疗健康-数据业务" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="l1Dialog = false">取消</el-button>
        <el-button type="primary" @click="saveL1">保存</el-button>
      </template>
    </el-dialog>

    <!-- L2 新增/编辑弹窗 -->
    <el-dialog v-model="l2Dialog" :title="isNewL2 ? '新增核心业务产品' : '编辑核心业务产品'" width="420px">
      <el-form :model="l2Form" label-width="80px" size="small">
        <el-form-item label="名称">
          <el-input v-model="l2Form.name" placeholder="如: 科研平台与服务" />
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
import {
  getProductL1List, createProductL1, updateProductL1, deleteProductL1, updateProductL1Sort,
  getProductL2List, createProductL2, updateProductL2, deleteProductL2, updateProductL2Sort
} from '../../api/product'
import { getVersions } from '../../api/version'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import request from '../../utils/request'

const l1List = ref([])
const l2List = ref([])
const selectedL1 = ref(null)

const l1Dialog = ref(false)
const l2Dialog = ref(false)
const isNewL1 = ref(false)
const isNewL2 = ref(false)
const l1Form = ref({ name: '' })
const l2Form = ref({ name: '' })
const editingL1Id = ref(null)
const editingL2Id = ref(null)

let versionId = null
const versionStatus = ref(null)

async function loadVersion() {
  const res = await getVersions()
  if (!res.data || res.data.length === 0) {
    versionId = null
    versionStatus.value = null
    return
  }
  const draft = res.data.find(v => v.status === 'draft')
  const ver = draft || res.data[res.data.length - 1]
  versionId = ver ? ver.id : null
  versionStatus.value = ver ? ver.status : null
}

async function loadL1() {
  if (!versionId) {
    l1List.value = []
    return
  }
  const res = await getProductL1List(versionId)
  l1List.value = res.data || []
}

async function loadL2(l1Id) {
  if (!versionId || !l1Id) {
    l2List.value = []
    return
  }
  const res = await getProductL2List(versionId, l1Id)
  l2List.value = res.data || []
}

function onL1Select(row) {
  selectedL1.value = row
  if (row) loadL2(row.id)
}

function startDrag(e, row, type) {
  if (e.button !== 0 || versionStatus.value === 'released') return
  e.preventDefault()
  let list
  if (type === 'l1') list = l1List
  else list = l2List

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

async function reorderList(type, fromIdx, toIdx) {
  if (!versionId) return

  let list, updateSort
  if (type === 'l1') {
    list = l1List
    updateSort = updateProductL1Sort
  } else {
    list = l2List
    updateSort = updateProductL2Sort
  }

  const items = [...list.value]
  const [moved] = items.splice(fromIdx, 1)
  items.splice(toIdx, 0, moved)
  const sortList = items.map((item, i) => ({
    id: item.id,
    sortOrder: i
  }))
  await updateSort(versionId, sortList)

  if (type === 'l1') {
    await loadL1()
    if (selectedL1.value) {
      const found = l1List.value.find(c => c.id === selectedL1.value.id)
      selectedL1.value = found || null
      if (found) await loadL2(found.id)
    }
  } else {
    if (selectedL1.value) await loadL2(selectedL1.value.id)
  }
}

// L1 操作
function openL1Dialog(row) {
  if (row) {
    isNewL1.value = false
    editingL1Id.value = row.id
    l1Form.value = { name: row.name }
  } else {
    isNewL1.value = true
    editingL1Id.value = null
    l1Form.value = { name: '' }
  }
  l1Dialog.value = true
}

async function saveL1() {
  if (!l1Form.value.name) {
    ElMessage.warning('请输入名称')
    return
  }
  if (!versionId) {
    ElMessage.warning('版本信息加载失败，请刷新页面')
    return
  }
  if (isNewL1.value) {
    await createProductL1(versionId, l1Form.value.name)
    ElMessage.success('创建成功')
  } else {
    await updateProductL1(editingL1Id.value, l1Form.value.name)
    ElMessage.success('保存成功')
  }
  l1Dialog.value = false
  await loadL1()
}

async function deleteL1(row) {
  ElMessageBox.confirm(`确认删除统计分类"${row.name}"？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteProductL1(row.id)
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

// L2 操作
function openL2Dialog(row) {
  if (row) {
    isNewL2.value = false
    editingL2Id.value = row.id
    l2Form.value = { name: row.name }
  } else {
    isNewL2.value = true
    editingL2Id.value = null
    l2Form.value = { name: '' }
  }
  l2Dialog.value = true
}

async function saveL2() {
  if (!l2Form.value.name) {
    ElMessage.warning('请输入名称')
    return
  }
  if (!versionId || !selectedL1.value) {
    ElMessage.warning('请先选择统计分类')
    return
  }
  if (isNewL2.value) {
    await createProductL2(versionId, selectedL1.value.id, l2Form.value.name)
    ElMessage.success('创建成功')
  } else {
    await updateProductL2(editingL2Id.value, l2Form.value.name)
    ElMessage.success('保存成功')
  }
  l2Dialog.value = false
  await loadL2(selectedL1.value.id)
}

async function deleteL2(row) {
  try {
    await deleteProductL2(row.id)
    ElMessage.success('删除成功')
    await loadL2(selectedL1.value.id)
  } catch (e) {
    const msg = e?.response?.data?.message || '删除失败'
    ElMessage.warning(msg)
  }
}

async function handleFileChange(file) {
  if (!versionId) {
    ElMessage.warning('版本信息加载失败，请刷新页面')
    return
  }

  const formData = new FormData()
  formData.append('file', file.raw)
  formData.append('versionId', versionId)

  try {
    const res = await request.post('/product/import-excel', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const result = res.data
    if (result.errors && result.errors.length > 0) {
      ElMessage.warning(`导入完成，但有错误：${result.errors.join('; ')}`)
    } else {
      ElMessage.success(`导入成功！共处理 ${result.successRows} 条数据`)
    }
    await loadL1()
  } catch (e) {
    const msg = e?.response?.data?.message || '导入失败'
    ElMessage.error(msg)
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
.drag-icon {
  cursor: grab;
  font-size: 14px;
  color: #94A3B8;
  user-select: none;
}
.drag-icon:hover {
  color: #2563EB;
}
</style>
