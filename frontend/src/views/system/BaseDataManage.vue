<template>
  <div class="page">
    <h3>基础数据维护</h3>
    <p class="subtitle">维护左侧层级树的业务分类（L1）与业务域（L2）</p>

    <div class="filter-section">
      <el-form inline size="small">
        <el-form-item label="名称">
          <el-input v-model="filters.name" placeholder="请输入名称" clearable @change="handleFilter" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="请选择状态" clearable @change="handleFilter">
            <el-option v-for="status in statusOptions" :key="status.value" :label="status.label" :value="status.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品经理">
          <el-input v-model="filters.productManager" placeholder="请输入产品经理" clearable @change="handleFilter" />
        </el-form-item>
        <el-form-item label="解决方案">
          <el-select v-model="filters.solution" placeholder="请选择解决方案" clearable @change="handleFilter">
            <el-option label="智慧医疗" value="智慧医疗" />
            <el-option label="智慧服务" value="智慧服务" />
            <el-option label="智慧管理" value="智慧管理" />
            <el-option label="互联互通" value="互联互通" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-select v-model="filters.versionTag" placeholder="请选择版本" clearable @change="handleFilter">
            <el-option v-for="version in versionOptions" :key="version.value" :label="version.label" :value="version.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="dual-tables">
      <div class="table-wrapper">
        <div class="table-header">
          <strong>业务分类 (L1)</strong>
          <el-button size="small" type="primary" @click="openL1Dialog()" :disabled="versionStatus === 'released'">新增</el-button>
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
          </div>
        </div>
        <el-table
          v-if="selectedL1"
          :data="l2List"
          border
          stripe
          size="small"
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
import { ref, watch, onMounted } from 'vue'
import { getTree, getChildren, createEntry, updateEntry, deleteEntry, queryEntries } from '../../api/data'
import { getVersions } from '../../api/version'
import { ElMessage, ElMessageBox } from 'element-plus'

const l1List = ref([])
const l2List = ref([])
const selectedL1 = ref(null)
const l1Dialog = ref(false)
const l2Dialog = ref(false)
const isNewL1 = ref(false)
const isNewL2 = ref(false)
const l1Form = ref({ colBizCategory: '' })
const l2Form = ref({ colBizDomain: '' })
const editingL2Id = ref(null)
const editingL1Id = ref(null)

const filters = ref({
  name: '',
  status: '',
  productManager: '',
  solution: '',
  versionTag: ''
})

const statusOptions = ref([
  { label: '已开发', value: '已开发' },
  { label: '开发中', value: '开发中' },
  { label: '已规划', value: '已规划' }
])

const versionOptions = ref([])

let versionId = null
const versionStatus = ref(null)

async function loadVersion() {
  const res = await getVersions()
  const draft = res.data.find(v => v.status === 'draft')
  versionId = draft ? draft.id : res.data[res.data.length - 1].id
  versionStatus.value = draft ? 'draft' : 'released'
}

async function loadVersionOptions() {
  try {
    const res = await getVersions()
    versionOptions.value = res.data.map(v => ({
      label: v.versionNo,
      value: v.versionNo
    }))
  } catch (error) {
    console.error('加载版本选项失败:', error)
  }
}

async function handleFilter() {
  if (!versionId) return
  await loadL1()
}

function resetFilters() {
  filters.value = {
    name: '',
    status: '',
    productManager: '',
    solution: '',
    versionTag: ''
  }
  handleFilter()
}

async function loadL1() {
  const hasFilters = filters.value.name || filters.value.status || filters.value.productManager || filters.value.solution || filters.value.versionTag
  if (hasFilters) {
    const res = await queryEntries(versionId, {
      name: filters.value.name,
      status: filters.value.status,
      productManager: filters.value.productManager,
      solution: filters.value.solution,
      versionTag: filters.value.versionTag,
      level: 1
    })
    l1List.value = res.data || []
  } else {
    const res = await getTree(versionId)
    l1List.value = res.data || []
  }
}

async function loadL2(l1Id) {
  if (hasActiveFilters()) {
    const res = await queryEntries(versionId, {
      parentId: l1Id,
      name: filters.value.name,
      status: filters.value.status,
      productManager: filters.value.productManager,
      solution: filters.value.solution,
      versionTag: filters.value.versionTag
    })
    l2List.value = res.data || []
  } else {
    const res = await getChildren(versionId, l1Id)
    l2List.value = res.data || []
  }
}

function hasActiveFilters() {
  return filters.value.name || filters.value.status || filters.value.productManager ||
         filters.value.solution || filters.value.versionTag;
}

function onL1Select(row) {
  selectedL1.value = row
  if (row) loadL2(row.id)
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
    await updateEntry(editingL1Id.value, { colBizCategory: l1Form.value.colBizCategory })
    ElMessage.success('保存成功')
  }
  l1Dialog.value = false
  await loadL1()
}

async function deleteL1(row) {
  ElMessageBox.confirm(`确认删除业务分类"${row.colBizCategory || row.label}"？下面的业务域也会一起删除。`, '提示', {
    type: 'warning'
  }).then(async () => {
    // Delete all L2 children first
    const children = await getChildren(versionId, row.id)
    for (const c of children.data || []) {
      await deleteEntry(c.id)
    }
    await deleteEntry(row.id)
    ElMessage.success('删除成功')
    if (selectedL1.value?.id === row.id) {
      selectedL1.value = null
      l2List.value = []
    }
    await loadL1()
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
    await updateEntry(editingL2Id.value, { colBizDomain: l2Form.value.colBizDomain })
    ElMessage.success('保存成功')
  }
  l2Dialog.value = false
  await loadL2(selectedL1.value.id)
}

async function deleteL2(row) {
  ElMessageBox.confirm(`确认删除业务域"${row.colBizDomain || row.label}"？`, '提示', {
    type: 'warning'
  }).then(async () => {
    await deleteEntry(row.id)
    ElMessage.success('删除成功')
    await loadL2(selectedL1.value.id)
  }).catch(() => {})
}

onMounted(async () => {
  await loadVersion()
  await loadVersionOptions()
  await loadL1()
})
</script>

<style scoped>
.page { padding: 0; }
h3 { margin: 0 0 4px; font-size: 16px; }
.subtitle { margin: 0 0 16px; font-size: 13px; color: #999; }
.filter-section {
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 12px;
}
.dual-tables {
  display: flex;
  gap: 16px;
}
.table-wrapper {
  flex: 1;
  background: #fff;
  border-radius: 4px;
  padding: 12px;
}
.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.placeholder {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
}
</style>
