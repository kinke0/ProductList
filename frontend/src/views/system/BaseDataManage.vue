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
          <el-table-column label="名称" min-width="160">
            <template #default="{ row }">
              {{ row.colBizCategory || row.label }}
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
              当前: {{ selectedL1.colBizCategory }}
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
          <el-table-column label="名称" min-width="160">
            <template #default="{ row }">
              {{ row.colBizDomain || row.label }}
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
import { getTree, getChildren, createEntry, updateEntry, deleteEntry, updateCategorySort } from '../../api/data'
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
  const res = await getTree(versionId)
  l1List.value = res.data || []
}

async function loadL2(l1Id) {
  const res = await getChildren(versionId, l1Id)
  l2List.value = res.data || []
}

function onL1Select(row) {
  selectedL1.value = row
  selectedL2.value = null
  if (row) loadL2(row.id)
}

function onL2Select(row) {
  selectedL2.value = row
}

async function moveUp(type) {
  const list = type === 'l1' ? l1List : l2List
  const selected = type === 'l1' ? selectedL1 : selectedL2
  const idx = list.value.findIndex(r => r.id === selected.value.id)
  if (idx <= 0) return
  const idType = type === 'l1' ? 'category' : 'domain'
  const itemA = list.value[idx]
  const itemB = list.value[idx - 1]
  await updateCategorySort(versionId, [
    { type: idType, id: itemA.id, sortOrder: idx - 1 },
    { type: idType, id: itemB.id, sortOrder: idx }
  ])
  if (type === 'l1') await loadL1()
  else await loadL2(selectedL1.value.id)
  selectedL2.value = null
}

async function moveDown(type) {
  const list = type === 'l1' ? l1List : l2List
  const selected = type === 'l1' ? selectedL1 : selectedL2
  const idx = list.value.findIndex(r => r.id === selected.value.id)
  if (idx < 0 || idx >= list.value.length - 1) return
  const idType = type === 'l1' ? 'category' : 'domain'
  const itemA = list.value[idx]
  const itemB = list.value[idx + 1]
  await updateCategorySort(versionId, [
    { type: idType, id: itemA.id, sortOrder: idx + 1 },
    { type: idType, id: itemB.id, sortOrder: idx }
  ])
  if (type === 'l1') await loadL1()
  else await loadL2(selectedL1.value.id)
  selectedL2.value = null
}

function openL1Dialog(row) {
  if (row) {
    isNewL1.value = false
    editingL1Id.value = row.id
    l1Form.value = { colBizCategory: row.colBizCategory || row.label }
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
    await createEntry({
      versionId,
      level: 1,
      sortOrder: l1List.value.length,
      colBizCategory: l1Form.value.colBizCategory,
      colProductSystem: l1Form.value.colBizCategory
    })
    ElMessage.success('创建成功')
  } else {
    await updateEntry(editingL1Id.value, { colBizCategory: l1Form.value.colBizCategory, colProductSystem: l1Form.value.colBizCategory })
    ElMessage.success('保存成功')
  }
  l1Dialog.value = false
  await loadL1()
}

async function deleteL1(row) {
  ElMessageBox.confirm(`确认删除业务分类"${row.colBizCategory || row.label}"？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteEntry(row.id)
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
    l2Form.value = { colBizDomain: row.colBizDomain || row.label }
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
    await createEntry({
      versionId,
      parentId: selectedL1.value.id,
      level: 2,
      sortOrder: l2List.value.length,
      colBizDomain: l2Form.value.colBizDomain,
      colProductSystem: l2Form.value.colBizDomain,
      colBizCategory: selectedL1.value.colBizCategory || selectedL1.value.label
    })
    ElMessage.success('创建成功')
  } else {
    await updateEntry(editingL2Id.value, { colBizDomain: l2Form.value.colBizDomain, colProductSystem: l2Form.value.colBizDomain })
    ElMessage.success('保存成功')
  }
  l2Dialog.value = false
  await loadL2(selectedL1.value.id)
}

async function deleteL2(row) {
  ElMessageBox.confirm(`确认删除业务域"${row.colBizDomain || row.label}"？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteEntry(row.id)
      ElMessage.success('删除成功')
      await loadL2(selectedL1.value.id)
    } catch (e) {
      const msg = e?.response?.data?.message || '删除失败'
      ElMessage.warning(msg)
    }
  }).catch(() => {})
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
