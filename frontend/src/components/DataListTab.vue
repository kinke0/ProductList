<template>
  <div class="data-list-tab">
    <div class="query-bar">
      <el-form :model="queryForm" inline size="small">
        <el-form-item label="名称">
          <el-input v-model="queryForm.name" placeholder="产品/系统名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="s in statusList" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品经理">
          <el-input v-model="queryForm.productManager" placeholder="产品经理" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="解决方案">
          <el-select v-model="queryForm.solution" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="s in solutions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-select v-model="queryForm.versionDiv" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="v in versionDivList" :key="v" :label="v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-toolbar">
      <div class="toolbar-left">
        <span class="toolbar-title">查询结果</span>
      </div>
      <div class="toolbar-right">
        <el-button v-if="props.selectedNode?.level === 2" type="primary" size="small" :disabled="!props.isEditing" @click="openNewDialog">新建</el-button>
        <el-button type="success" size="small" @click="emit('insertToList', selectedIds)">
          插入待生成清单
        </el-button>
      </div>
    </div>

    <div class="table-body">
    <el-table
      ref="tableRef"
      :data="tableData"
      border
      stripe
      size="small"
      row-key="id"
      style="width: 100%"
      :tree-props="{ children: 'children' }"
      :row-class-name="({ row }) => 'row-id-' + row.id + ' row-level-' + (row.level || 0) + ' row-parent-' + (row.parentId || 0)"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="index" width="40" label="#" class-name="drag-col">
        <template #default="{ $index, row }">
          <span class="row-num" @click.stop="tableRef?.toggleRowSelection(row)">{{ $index + 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column type="selection" width="40" />
      <el-table-column label="名称" min-width="300" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="product-cell">
            <el-tag v-if="row.level && row.level >= 3" :type="levelTagType(row.level)" size="small" class="level-tag">
              {{ levelLabel(row.level) }}
            </el-tag>
            <span class="product-name">{{ row.colProductSystem || '(无名称)' }}</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag v-if="row.colStatus" :type="statusTagType(row.colStatus)" size="small">{{ row.colStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="colProductManager" label="产品经理" min-width="100" show-overflow-tooltip />
      <el-table-column label="版本划分" min-width="150">
        <template #default="{ row }">
          <div class="version-inline">
            <el-checkbox :model-value="hasVer(row, 'A-曜系列')" :disabled="!props.isEditing" @change="toggleVer(row, 'A-曜系列')" size="small">曜</el-checkbox>
            <el-checkbox :model-value="hasVer(row, 'B-远系列')" :disabled="!props.isEditing" @change="toggleVer(row, 'B-远系列')" size="small">远</el-checkbox>
            <el-checkbox :model-value="hasVer(row, 'C-驰系列')" :disabled="!props.isEditing" @change="toggleVer(row, 'C-驰系列')" size="small">驰</el-checkbox>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <span v-if="props.isEditing" class="op-btn op-edit" @click="editRow(row)">编辑</span>
          <span v-if="props.isEditing" class="op-btn op-add" @click="addChildRow(row)">添加</span>
          <span v-if="props.isEditing" class="op-btn op-del" @click="deleteRow(row)">删除</span>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <el-dialog :model-value="showEditDialog" @update:model-value="onDialogChange" :title="editDialogTitle" width="80%" top="5vh">
      <el-form :model="editForm" label-width="120px" size="small">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="productLabel">
              <el-input v-model="editForm.colProductSystem" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="应用角色">
              <el-select v-model="appRoleSelections" multiple size="small" style="width:100%;">
                <el-option v-for="r in appRoles" :key="r" :label="r" :value="r" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="业务分类">
              <el-input :model-value="editForm.colBizCategory" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业务域">
              <el-input :model-value="editForm.colBizDomain" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="editForm.colStatus" style="width: 100%">
                <el-option v-for="s in statusList" :key="s" :label="s" :value="s" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="版本划分">
              <div class="version-options">
                <div class="version-row">
                  <el-checkbox v-model="verYao" size="small">A-曜系列</el-checkbox>
                  <el-checkbox v-if="verYao" v-model="minYao" size="small" style="margin-left:12px;">最小集</el-checkbox>
                </div>
                <div class="version-row">
                  <el-checkbox v-model="verYuan" size="small">B-远系列</el-checkbox>
                  <el-checkbox v-if="verYuan" v-model="minYuan" size="small" style="margin-left:12px;">最小集</el-checkbox>
                </div>
                <div class="version-row">
                  <el-checkbox v-model="verChi" size="small">C-驰系列</el-checkbox>
                  <el-checkbox v-if="verChi" v-model="minChi" size="small" style="margin-left:12px;">最小集</el-checkbox>
                </div>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="产品经理">
              <el-input v-model="editForm.colProductManager" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="解决方案">
              <el-checkbox-group v-model="solutionSelections">
                <el-checkbox v-for="s in solutions" :key="s" :value="s" size="small">{{ s }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="招标参数">
          <el-input v-model="editForm.colBidParamDesc" type="textarea" :rows="10" />
        </el-form-item>
        <el-form-item label="功能说明">
          <el-input v-model="editForm.colFeatureDesc" type="textarea" :rows="10" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="软著">
              <el-input v-model="editForm.colCopyright" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产类型">
              <el-input v-model="editForm.colAssetType" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="editForm.colRemark" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="onDialogChange(false)">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { queryEntries, createEntry, updateEntry, deleteEntry, updateSort } from '../api/data'
import { getOptions } from '../api/option'
import { useAuthStore } from '../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import Sortable from 'sortablejs'

const props = defineProps({
  versionId: [Number, String],
  selectedNode: Object,
  isEditing: Boolean,
  customTabId: { type: Number, default: null }
})

const emit = defineEmits(['insertToList'])

const authStore = useAuthStore()
const tableData = ref([])
const showEditDialog = ref(false)
const isNew = ref(false)
const editingId = ref(null)
const selectedIds = ref([])
const parentRow = ref(null)
const appRoles = ref([])
const solutions = ref([])
const statusList = ref([])
const appRoleSelections = ref([])
const solutionSelections = ref([])
const versionDivList = ref(['A-曜系列', 'B-远系列', 'C-驰系列'])
const tableRef = ref(null)
let sortableInstances = []

function onDialogChange(val) {
  showEditDialog.value = val
}

function hasVer(row, ver) {
  return (row.colVersionDivision || '').includes(ver)
}

async function toggleVer(row, ver) {
  const parts = (row.colVersionDivision || '').split(' ').filter(Boolean)
  const idx = parts.indexOf(ver)
  if (idx >= 0) parts.splice(idx, 1)
  else parts.push(ver)
  row.colVersionDivision = parts.join(' ')
  await updateEntry(row.id, { colVersionDivision: row.colVersionDivision })
  handleQuery()
}

function statusTagType(status) {
  if (!status) return ''
  if (status.includes('可交付')) return 'success'
  if (status.includes('立项')) return 'warning'
  if (status.includes('演示')) return 'primary'
  if (status.includes('缺失')) return 'danger'
  return 'info'
}

function levelLabel(level) {
  if (level === 3) return '产品'
  if (level === 4) return '模块'
  if (level === 5) return '功能'
  return '子功能'
}

function levelTagType(level) {
  if (level === 3) return ''
  if (level === 4) return 'success'
  if (level === 5) return 'warning'
  return 'info'
}

function initSortable() {
  sortableInstances.forEach(s => s.destroy())
  sortableInstances = []
  if (!props.isEditing) return

  nextTick(() => {
    const tbody = document.querySelector('.el-table__body-wrapper tbody')
    if (!tbody) return
    const inst = Sortable.create(tbody, {
      handle: '.drag-col',
      animation: 150,
      onMove(evt) {
        const dragLevel = getRowLevel(evt.dragged)
        const relatedLevel = getRowLevel(evt.related)
        if (dragLevel !== relatedLevel) return false
        if (dragLevel === 3) return true
        return getRowParent(evt.dragged) === getRowParent(evt.related)
      },
      onEnd: async (evt) => {
        const draggedEl = evt.item
        const dragLevel = getRowLevel(draggedEl)
        const dragParent = getRowParent(draggedEl)
        const dragId = Number(draggedEl.className.match(/row-id-(\d+)/)?.[1] || 0)
        const allRowsNow = Array.from(tbody.querySelectorAll('tr.el-table__row'))
        const siblingRows = allRowsNow.filter(tr =>
          getRowLevel(tr) === dragLevel && getRowParent(tr) === dragParent
        )
        const idOrder = siblingRows.map(tr => {
          const m = tr.className.match(/row-id-(\d+)/)
          return m ? Number(m[1]) : null
        }).filter(x => x !== null)
        if (idOrder.length === 0) { handleQuery(); return }
        const payload = idOrder.map((id, i) => ({ id, sortOrder: i }))
        await updateSort(payload)
        ElMessage.success('排序已保存')
        await handleQuery()
      }
    })
    sortableInstances.push(inst)
  })
}

function getRowLevel(tr) {
  const m = tr.className.match(/row-level-(\d+)/)
  return m ? Number(m[1]) : 0
}

function getRowParent(tr) {
  const m = tr.className.match(/row-parent-(\d+)/)
  return m ? Number(m[1]) : 0
}

const verYao = ref(false)
const verYuan = ref(false)
const verChi = ref(false)
const minYao = ref(false)
const minYuan = ref(false)
const minChi = ref(false)
const versionSelections = ref([])

function syncVersionToForm() {
  const parts = []
  if (verYao.value) parts.push('A-曜系列')
  if (verYuan.value) parts.push('B-远系列')
  if (verChi.value) parts.push('C-驰系列')
  editForm.colVersionDivision = parts.join(' ')
  editForm.colYao = minYao.value ? '是' : '否'
  editForm.colYuan = minYuan.value ? '是' : '否'
  editForm.colChi = minChi.value ? '是' : '否'
  editForm.colAppRole = appRoleSelections.value.join(' ')
  editForm.colOtherSolutionTag = solutionSelections.value.join(',')
}

function syncVersionFromForm() {
  const div = editForm.colVersionDivision || ''
  verYao.value = div.includes('A-曜系列')
  verYuan.value = div.includes('B-远系列')
  verChi.value = div.includes('C-驰系列')
  minYao.value = editForm.colYao === '是'
  minYuan.value = editForm.colYuan === '是'
  minChi.value = editForm.colChi === '是'
  versionSelections.value = div.split(' ').filter(Boolean)
  appRoleSelections.value = (editForm.colAppRole || '').split(' ').filter(Boolean)
  solutionSelections.value = (editForm.colOtherSolutionTag || '').split(',').filter(Boolean)
}

async function loadOptions() {
  try {
    const [ar, sol, st] = await Promise.all([
      getOptions('app_role'),
      getOptions('solution'),
      getOptions('status')
    ])
    appRoles.value = (ar.data || []).map(o => o.value)
    solutions.value = (sol.data || []).map(o => o.value)
    statusList.value = (st.data || []).map(o => o.value)
  } catch (e) { /* ignore */ }
}

const queryForm = reactive({
  name: '',
  status: '',
  productManager: '',
  solution: '',
  versionDiv: ''
})

const editForm = reactive({
  colProductSystem: '',
  colAppRole: '',
  colBidParamDesc: '',
  colFeatureDesc: '',
  colStatus: '',
  colBizCategory: '',
  colBizDomain: '',
  colVersionDivision: '',
  colProductManager: '',
  colOtherSolutionTag: '',
  colCopyright: '',
  colAssetType: '',
  colRemark: '',
  colYao: '',
  colYuan: '',
  colChi: ''
})

const editDialogTitle = computed(() => {
  if (!isNew.value) return '编辑'
  if (parentRow.value) return `添加下级 - ${parentRow.value.colProductSystem || '子节点'}`
  return '新建'
})

const productLabel = computed(() => {
  if (isNew.value) {
    if (!parentRow.value) return '产品/系统'
    if (parentRow.value.level >= 5) return '子功能'
    return { 3: '模块', 4: '功能' }[parentRow.value.level] || '名称'
  }
  if (editForm.level >= 6) return '子功能'
  return { 3: '产品/系统', 4: '模块', 5: '功能' }[editForm.level] || '名称'
})

const initialFormState = () => ({
  colProductSystem: '', colAppRole: '', colBidParamDesc: '', colFeatureDesc: '',
  colStatus: '', colBizCategory: '', colBizDomain: '', colVersionDivision: '',
  colProductManager: '', colOtherSolutionTag: '', colCopyright: '', colAssetType: '', colRemark: '',
  colYao: '', colYuan: '', colChi: ''
})

function fillCategoryAndDomain() {
  if (props.selectedNode) {
    editForm.colBizCategory = props.selectedNode.categoryLabel || editForm.colBizCategory
    editForm.colBizDomain = props.selectedNode.domainLabel || editForm.colBizDomain
  }
}

function initEditForm() {
  Object.assign(editForm, initialFormState())
  fillCategoryAndDomain()
}

function onSelectionChange(rows) {
  selectedIds.value = rows.filter(r => r.level === 3).map(r => r.id)
}

function onChildSelectionChange(rows) {
  // child selection handled if needed
}

async function handleQuery() {
  try {
    const res = await queryEntries(props.versionId, {
      customTabId: props.customTabId || undefined,
      name: queryForm.name || undefined,
      status: queryForm.status || undefined,
      productManager: queryForm.productManager || undefined,
      solution: queryForm.solution || undefined,
      versionTag: queryForm.versionDiv || undefined,
      bizCategory: props.selectedNode?.id !== 'all' ? (props.selectedNode?.categoryLabel || undefined) : undefined,
      bizDomain: props.selectedNode?.id !== 'all' ? (props.selectedNode?.domainLabel || undefined) : undefined
    })
    const entries = res.data || []
    tableData.value = buildTree(entries)
    nextTick(initSortable)
  } catch (e) {
    console.error('查询数据失败:', e)
    tableData.value = []
  }
}

function resetQuery() {
  queryForm.name = ''
  queryForm.status = ''
  queryForm.productManager = ''
  queryForm.solution = ''
  queryForm.versionDiv = ''
  handleQuery()
}

function buildTree(entries) {
  const map = {}
  entries.forEach(e => { map[e.id] = { ...e, children: [] } })
  const roots = []
  entries.forEach(e => {
    if (e.parentId && map[e.parentId]) {
      map[e.parentId].children.push(map[e.id])
    } else {
      roots.push(map[e.id])
    }
  })
  return roots
}

function editRow(row) {
  isNew.value = false
  editingId.value = row.id
  parentRow.value = null
  Object.assign(editForm, row)
  syncVersionFromForm()
  showEditDialog.value = true
}

function addChildRow(row) {
  isNew.value = true
  editingId.value = null
  parentRow.value = row
  initEditForm()
  editForm.colProductManager = row.colProductManager || ''
  editForm.colVersionDivision = row.colVersionDivision || ''
  syncVersionFromForm()
  showEditDialog.value = true
}

function openNewDialog() {
  isNew.value = true
  editingId.value = null
  parentRow.value = null
  initEditForm()
  syncVersionFromForm()
  showEditDialog.value = true
}

watch(() => props.selectedNode, () => {
  if (props.selectedNode && props.versionId) {
    handleQuery()
  }
  if (isNew.value) {
    fillCategoryAndDomain()
  }
}, { deep: true })

function deleteRow(row) {
  ElMessageBox.confirm('确认删除该记录？', '提示', { type: 'warning' }).then(async () => {
    await deleteEntry(row.id)
    ElMessage.success('删除成功')
    handleQuery()
  }).catch(() => {})
}

async function saveEdit() {
  syncVersionToForm()
  if (isNew.value) {
    const data = {
      ...editForm,
      versionId: props.versionId,
      level: parentRow.value ? parentRow.value.level + 1 : 3,
      parentId: parentRow.value ? parentRow.value.id : undefined
    }
    await createEntry(data)
    ElMessage.success('创建成功')
    parentRow.value = null
  } else {
    await updateEntry(editingId.value, editForm)
    ElMessage.success('保存成功')
  }
  showEditDialog.value = false
  handleQuery()
}

watch(() => props.versionId, () => {
  handleQuery()
  loadOptions()
}, { immediate: true })

onUnmounted(() => {
  sortableInstances.forEach(s => s.destroy())
})
</script>

<style scoped>
.data-list-tab {
  padding: 12px 12px 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.query-bar {
  flex-shrink: 0;
  background: #fafafa;
  padding: 8px 12px;
  border-radius: 4px;
  margin-bottom: 8px;
}
.table-toolbar {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.table-body {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
.toolbar-title { font-weight: 600; font-size: 14px; }
:deep(.el-table .cell) { padding: 0 6px !important; }
:deep(.drag-col) { cursor: grab; text-align: center !important; }
.row-num { color: #999; font-size: 12px; user-select: none; cursor: pointer; }
.op-btn { cursor: pointer; font-size: 12px; margin-right: 8px; user-select: none; }
.op-edit { color: #409eff; }
.op-add { color: #67c23a; }
.op-del { color: #f56c6c; }
.op-btn:hover { text-decoration: underline; }
.version-inline { display: flex; gap: 2px; white-space: nowrap; }
.version-row { display: flex; align-items: center; }
.product-cell { display: inline; }
.product-name { }
.level-tag { margin: 0 6px; vertical-align: middle; }
</style>
