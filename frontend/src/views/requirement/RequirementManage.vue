<template>
  <div class="page">
    <div class="page-header">
      <h3>需求管理</h3>
      <el-button size="small" type="primary" @click="openCreateDialog">提交需求</el-button>
    </div>

    <div class="filter-bar">
      <el-radio-group v-model="queryForm.scope" size="small" @change="onScopeChange">
        <el-radio-button value="my">我的需求</el-radio-button>
        <el-radio-button value="all">全部需求</el-radio-button>
      </el-radio-group>
      <el-select v-model="queryForm.status" placeholder="状态" size="small" clearable style="width: 100px;" @change="loadData">
        <el-option v-for="s in statusList" :key="s" :label="s" :value="s" />
      </el-select>
      <el-select v-model="queryForm.category" placeholder="所属模块" size="small" clearable style="width: 120px;" @change="loadData">
        <el-option v-for="m in moduleList" :key="m" :label="m" :value="m" />
      </el-select>
      <el-select v-model="queryForm.type" placeholder="需求类型" size="small" clearable style="width: 110px;" @change="loadData">
        <el-option v-for="t in typeList" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="queryForm.priority" placeholder="优先级" size="small" clearable style="width: 90px;" @change="loadData">
        <el-option label="高" value="高" />
        <el-option label="中" value="中" />
        <el-option label="低" value="低" />
      </el-select>
      <el-input v-model="queryForm.creator" placeholder="提出人" size="small" clearable style="width: 100px;" @clear="loadData" />
      <el-radio-group v-model="queryForm.dateRange" size="small" @change="loadData">
        <el-radio-button value="3">近3天</el-radio-button>
        <el-radio-button value="7">近7天</el-radio-button>
        <el-radio-button value="30">近30天</el-radio-button>
        <el-radio-button value="90">近90天</el-radio-button>
        <el-radio-button value="all">全部</el-radio-button>
      </el-radio-group>
      <el-button size="small" type="primary" @click="loadData">搜索</el-button>
      <el-button size="small" @click="resetQuery">重置</el-button>
    </div>

    <div class="main-body">
      <div class="stats-panel">
        <div ref="statsChartRef" class="stats-chart"></div>
        <div ref="moduleChartRef" class="stats-chart" style="margin-top: 16px;"></div>
        <div ref="typeChartRef" class="stats-chart" style="margin-top: 16px;"></div>
      </div>
      <div class="table-panel">
        <el-table :data="filteredData" border stripe size="small" max-height="calc(100vh - 260px)">
          <el-table-column prop="reqNo" label="需求编号" width="130" />
          <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
          <el-table-column prop="category" label="所属模块" width="180">
            <template #default="{ row }">
              <span>{{ row.category || '-' }}{{ row.domain ? ' / ' + row.domain : '' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="typeTagType(row.type)">{{ row.type || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="priority" label="优先级" width="60" align="center">
            <template #default="{ row }">
              <span :style="{ color: priorityColor(row.priority), fontWeight: 600 }">{{ row.priority }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="70" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)" style="cursor:pointer" @click="showApprovalFlow(row)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="提出时间" width="95">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="creatorName" label="提出人" width="80" align="center" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <template v-if="isAdmin">
                <el-button v-if="row.status === '提出'" size="small" type="success" link @click="handleAction(row, 'confirm')">确认</el-button>
                <el-button v-if="row.status === '已确认'" size="small" type="warning" link @click="handleAction(row, 'develop')">开发</el-button>
                <el-button v-if="row.status === '开发中'" size="small" type="primary" link @click="handleAction(row, 'ready')">待上线</el-button>
                <el-button v-if="row.status === '待上线'" size="small" type="success" link @click="handleAction(row, 'release')">上线</el-button>
                <el-button v-if="row.status === '提出' || row.status === '已确认'" size="small" type="danger" link @click="handleAction(row, 'reject')">驳回</el-button>
                <el-button size="small" type="danger" link @click="handleDeleteReq(row)">删除</el-button>
              </template>
              <template v-if="isMyReq(row) && row.status === '提出'">
                <el-button size="small" type="primary" link @click="openEditDialog(row)">编辑</el-button>
                <el-button size="small" type="info" link @click="handleCancel(row)">撤销</el-button>
              </template>
              <el-button size="small" link @click="showDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <RequirementFormDialog v-model="showFormDialog" :edit-id="editingId" :initial-data="editingData" @saved="onFormSaved" />

    <el-dialog v-model="showActionDialog" :title="actionTitle" width="440px">
      <el-form :model="actionForm" label-width="80px" size="small">
        <el-form-item v-if="pendingAction === 'release'" label="版本号" required>
          <el-select v-model="actionForm.releasedVersion" placeholder="请选择版本" style="width: 100%;">
            <el-option v-for="v in versions" :key="v.id" :label="v.versionNo" :value="v.versionNo" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="pendingAction === 'reject'" label="驳回原因" required>
          <el-input v-model="actionForm.rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
        </el-form-item>
        <el-form-item v-if="pendingAction !== 'release' && pendingAction !== 'reject'" label="备注">
          <el-input v-model="actionForm.comment" type="textarea" :rows="2" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showActionDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAction">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="需求详情" width="850px">
      <template v-if="detailData">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="需求编号">{{ detailData.item.reqNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="statusType(detailData.item.status)">{{ detailData.item.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">
            <span style="display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;" :title="detailData.item.title">{{ detailData.item.title }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="所属模块">{{ detailData.item.category || '-' }}{{ detailData.item.domain ? ' / ' + detailData.item.domain : '' }}</el-descriptions-item>
          <el-descriptions-item label="需求类型">{{ detailData.item.type || '-' }}</el-descriptions-item>
          <el-descriptions-item label="优先级">{{ detailData.item.priority }}</el-descriptions-item>
          <el-descriptions-item label="提出人">{{ detailData.creatorName }}</el-descriptions-item>
          <el-descriptions-item label="提出时间">{{ formatDate(detailData.item.createdAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.item.releasedVersion" label="上线版本">{{ detailData.item.releasedVersion }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.item.rejectReason" label="驳回原因" :span="2">{{ detailData.item.rejectReason }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            <div class="detail-desc" v-html="detailData.item.description || '无'" @click="onDetailDescClick"></div>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="detailData.logs && detailData.logs.length" style="margin-top: 16px;">
          <h4 style="font-size: 13px; color: #64748B; margin-bottom: 8px;">操作记录</h4>
          <el-timeline>
            <el-timeline-item v-for="log in detailData.logs" :key="log.id" :timestamp="log.createdAt?.substring(0, 16)" placement="top">
              {{ log.action }} <span v-if="log.comment" style="color: #94A3B8;">- {{ log.comment }}</span>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailImgPreviewVisible" title="图片预览" width="auto" top="2vh" :style="{ maxWidth: '90vw' }">
      <div style="display:flex;align-items:center;justify-content:center;">
        <img v-if="detailImgPreviewUrl" :src="detailImgPreviewUrl" style="max-width:85vw;max-height:78vh;object-fit:contain;" />
      </div>
    </el-dialog>

    <el-dialog v-model="flowVisible" :title="'审批流 - ' + flowReqNo" width="550px">
      <el-timeline>
        <el-timeline-item v-for="log in flowLogs" :key="log.id" :timestamp="log.createdAt?.substring(0, 16)" placement="top">
          {{ log.operatorName || '未知' }}  {{ log.action }}
          <span v-if="log.action === '驳回' && log.comment" style="color: #F56C6C;">，原因: {{ log.comment }}</span>
          <span v-else-if="log.comment" style="color: #94A3B8;"> ({{ log.comment }})</span>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount, inject, watch } from 'vue'
import * as echarts from 'echarts'
import { useAuthStore } from '../../store/auth'
import {
  getRequirements, getRequirementDetail, getRequirementStats, getModuleStats, getTypeStats,
  confirmRequirement, developRequirement, readyRequirement, releaseRequirement,
  rejectRequirement, cancelRequirement, deleteRequirement
} from '../../api/requirement'
import { getVersions } from '../../api/version'
import RequirementFormDialog from '../../components/RequirementFormDialog.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.isAdmin())

const statusList = ['提出', '已确认', '开发中', '待上线', '已上线', '驳回', '撤销']
const statusConfig = {
  '提出': { type: '', color: '#409EFF' },
  '已确认': { type: 'success', color: '#8B5CF6' },
  '开发中': { type: 'warning', color: '#E6A23C' },
  '待上线': { type: '', color: '#3B82F6' },
  '已上线': { type: 'success', color: '#67C23A' },
  '驳回': { type: 'danger', color: '#F56C6C' },
  '撤销': { type: 'info', color: '#909399' }
}
const typeList = ['功能增强', 'Bug修复', '新功能', '性能优化']
const moduleList = ['产品清单', '需求管理', '版本管理', '系统管理', '图床管理']

const allData = ref([])
const queryForm = ref({ scope: 'my', status: '', creator: '', dateRange: '30', category: '', type: '', priority: '' })
const versions = ref([])

const showFormDialog = ref(false)
const showActionDialog = ref(false)
const showDetailDialog = ref(false)
const editingId = ref(null)
const editingData = ref(null)
const pendingAction = ref('')
const pendingRow = ref(null)
const actionForm = ref({ comment: '', rejectReason: '', releasedVersion: '' })
const detailData = ref(null)
const detailImgPreviewVisible = ref(false)
const detailImgPreviewUrl = ref('')
const flowVisible = ref(false)
const flowLogs = ref([])
const flowReqNo = ref('')

const statsChartRef = ref(null)
let statsChartInstance = null
const moduleChartRef = ref(null)
let moduleChartInstance = null
const typeChartRef = ref(null)
let typeChartInstance = null

const filteredData = computed(() => allData.value)

function statusType(s) { return statusConfig[s]?.type || '' }
function typeTagType(t) {
  if (t === 'Bug修复') return 'danger'
  if (t === '新功能') return 'success'
  if (t === '性能优化') return 'warning'
  return ''
}
function priorityColor(p) {
  if (p === '高') return '#F56C6C'
  if (p === '低') return '#909399'
  return '#E6A23C'
}
function isMyReq(row) {
  const userId = localStorage.getItem('userId')
  return String(row.createdBy) === String(userId)
}
function formatDate(d) {
  if (!d) return ''
  return d.substring(0, 10)
}

const actionTitle = computed(() => {
  const map = { confirm: '确认需求', develop: '设为开发中', ready: '设为待上线', release: '上线', reject: '驳回需求' }
  return map[pendingAction.value] || ''
})

function clearOtherFilters() {
  queryForm.value.status = ''
  queryForm.value.category = ''
  queryForm.value.type = ''
  queryForm.value.priority = ''
  queryForm.value.creator = ''
  queryForm.value.dateRange = 'all'
}

function resetQuery() {
  queryForm.value = { scope: 'my', status: '', creator: '', dateRange: '30', category: '', type: '', priority: '' }
  loadData()
}

async function loadData() {
  const params = {}
  if (queryForm.value.status) params.status = queryForm.value.status
  if (queryForm.value.creator) params.creatorName = queryForm.value.creator
  const dr = queryForm.value.dateRange
  if (dr && dr !== 'all') {
    const days = Number(dr)
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - days)
    params.startDate = start.toISOString().substring(0, 10)
    params.endDate = end.toISOString().substring(0, 10)
  }
  if (queryForm.value.category) params.category = queryForm.value.category
  if (queryForm.value.type) params.type = queryForm.value.type
  if (queryForm.value.priority) params.priority = queryForm.value.priority
  if (queryForm.value.scope === 'my') {
    params.scope = 'my'
  }
  const res = await getRequirements(params)
  allData.value = res.data || []
  loadStats(params)
  loadModuleStats(params)
  loadTypeStats(params)
}

async function loadStats(params = {}) {
  const res = await getRequirementStats(params)
  renderStatsChart(res.data || {})
}

function renderStatsChart(stats) {
  if (!statsChartRef.value) return
  if (!statsChartInstance) {
    statsChartInstance = echarts.init(statsChartRef.value)
    statsChartInstance.on('click', (params) => {
      clearOtherFilters()
      queryForm.value.status = queryForm.value.status === params.name ? '' : params.name
      loadData()
    })
  }
  const labels = statusList
  const values = labels.map(l => stats[l] || 0)
  statsChartInstance.setOption({
    title: { text: '状态分布', left: 'center', textStyle: { fontSize: 12, color: '#64748B' } },
    tooltip: { trigger: 'axis' },
    grid: { left: 30, right: 10, top: 30, bottom: 24, containLabel: false },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 9, color: '#64748B', rotate: 30 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10, color: '#94A3B8' }, splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } } },
    series: [{
      type: 'bar',
      data: labels.map((l) => ({
        value: values[labels.indexOf(l)],
        itemStyle: { color: statusConfig[l].color }
      })),
      barWidth: 20,
      label: { show: true, position: 'top', formatter: '{c}', color: '#606266', fontSize: 10 }
    }]
  })
}

async function loadModuleStats(params = {}) {
  const res = await getModuleStats(params)
  renderModuleChart(res.data || {})
}

function renderModuleChart(stats) {
  if (!moduleChartRef.value) return
  if (!moduleChartInstance) {
    moduleChartInstance = echarts.init(moduleChartRef.value)
    moduleChartInstance.on('click', (params) => {
      clearOtherFilters()
      queryForm.value.category = queryForm.value.category === params.name ? '' : params.name
      loadData()
    })
  }
  const labels = Object.keys(stats)
  const values = Object.values(stats)
  moduleChartInstance.setOption({
    title: { text: '模块分布', left: 'center', textStyle: { fontSize: 12, color: '#64748B' } },
    tooltip: { trigger: 'axis' },
    grid: { left: 30, right: 10, top: 30, bottom: 30, containLabel: false },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 9, color: '#64748B', rotate: 30 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10, color: '#94A3B8' }, splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } } },
    series: [{
      type: 'bar',
      data: values,
      barWidth: 20,
      label: { show: true, position: 'top', formatter: '{c}', color: '#606266', fontSize: 10 },
      itemStyle: { color: '#8B5CF6' }
    }]
  })
}

async function loadTypeStats(params = {}) {
  const res = await getTypeStats(params)
  renderTypeChart(res.data || {})
}

function renderTypeChart(stats) {
  if (!typeChartRef.value) return
  if (!typeChartInstance) {
    typeChartInstance = echarts.init(typeChartRef.value)
    typeChartInstance.on('click', (params) => {
      clearOtherFilters()
      queryForm.value.type = queryForm.value.type === params.name ? '' : params.name
      loadData()
    })
  }
  const colorMap = { '功能增强': '#409EFF', 'Bug修复': '#F56C6C', '新功能': '#67C23A', '性能优化': '#E6A23C' }
  const labels = Object.keys(stats)
  const values = Object.values(stats)
  typeChartInstance.setOption({
    title: { text: '类型分布', left: 'center', textStyle: { fontSize: 12, color: '#64748B' } },
    tooltip: { trigger: 'axis' },
    grid: { left: 30, right: 10, top: 30, bottom: 24, containLabel: false },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 9, color: '#64748B' } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10, color: '#94A3B8' }, splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } } },
    series: [{
      type: 'bar',
      data: labels.map((l, i) => ({ value: values[i], itemStyle: { color: colorMap[l] || '#409EFF' } })),
      barWidth: 20,
      label: { show: true, position: 'top', formatter: '{c}', color: '#606266', fontSize: 10 }
    }]
  })
}

function onScopeChange() { loadData() }

function openCreateDialog() {
  editingId.value = null
  editingData.value = null
  showFormDialog.value = true
}

function openEditDialog(row) {
  editingId.value = row.id
  editingData.value = { title: row.title, description: row.description, priority: row.priority, category: row.category, domain: row.domain || '', type: row.type || '' }
  showFormDialog.value = true
}

function onFormSaved() {
  loadData()
}

function handleAction(row, action) {
  pendingAction.value = action
  pendingRow.value = row
  actionForm.value = { comment: '', rejectReason: '', releasedVersion: '' }
  if (action === 'release' || action === 'reject') {
    showActionDialog.value = true
  } else {
    submitAction()
  }
}

async function submitAction() {
  const id = pendingRow.value.id
  const action = pendingAction.value
  const data = { ...actionForm.value }
  try {
    if (action === 'confirm') await confirmRequirement(id, data)
    else if (action === 'develop') await developRequirement(id, data)
    else if (action === 'ready') await readyRequirement(id, data)
    else if (action === 'release') {
      if (!data.releasedVersion) { ElMessage.warning('请选择版本号'); return }
      await releaseRequirement(id, data)
    } else if (action === 'reject') {
      if (!data.rejectReason) { ElMessage.warning('请输入驳回原因'); return }
      await rejectRequirement(id, data)
    }
    ElMessage.success('操作成功')
    showActionDialog.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

function handleCancel(row) {
  ElMessageBox.confirm('确认撤销该需求？', '提示', { type: 'warning' }).then(async () => {
    await cancelRequirement(row.id)
    ElMessage.success('已撤销')
    loadData()
  }).catch(() => {})
}

function handleDeleteReq(row) {
  ElMessageBox.confirm(`确认删除需求 "${row.reqNo}" 及其关联的图片？`, '危险操作', { type: 'error', confirmButtonText: '确认删除' }).then(async () => {
    await deleteRequirement(row.id)
    ElMessage.success('已删除')
    loadData()
  }).catch(() => {})
}

async function showApprovalFlow(row) {
  const res = await getRequirementDetail(row.id)
  flowLogs.value = res.data.logs || []
  flowReqNo.value = res.data.item.reqNo
  flowVisible.value = true
}

async function showDetail(row) {
  const res = await getRequirementDetail(row.id)
  detailData.value = res.data
  showDetailDialog.value = true
}

function onDetailDescClick(e) {
  const actionBtn = e.target.closest('[data-action]')
  const card = e.target.closest('.image-card')
  const url = card?.getAttribute('data-url')
  if (actionBtn && url) {
    const action = actionBtn.getAttribute('data-action')
    if (action === 'preview') {
      detailImgPreviewUrl.value = url
      detailImgPreviewVisible.value = true
    } else if (action === 'delete') {
      if (detailData.value.item.status !== '提出') {
        ElMessage.warning('需求已确认，不允许修改图片')
        return
      }
      ElMessageBox.confirm('确认删除该图片？', '提示', { type: 'warning' }).then(() => {
        card.remove()
        ElMessage.success('已删除')
      }).catch(() => {})
    }
    return
  }
  const thumb = e.target.closest('.image-thumb')
  if (thumb && url) {
    detailImgPreviewUrl.value = url
    detailImgPreviewVisible.value = true
  }
}

function handleResize() {
  statsChartInstance && statsChartInstance.resize()
  moduleChartInstance && moduleChartInstance.resize()
  typeChartInstance && typeChartInstance.resize()
}

const refreshKey = inject('reqRefreshKey', ref(0))
watch(refreshKey, () => { loadData() })

onMounted(async () => {
  const vRes = await getVersions()
  versions.value = vRes.data || []
  await loadData()
  await nextTick()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (statsChartInstance) { statsChartInstance.dispose(); statsChartInstance = null }
  if (moduleChartInstance) { moduleChartInstance.dispose(); moduleChartInstance = null }
  if (typeChartInstance) { typeChartInstance.dispose(); typeChartInstance = null }
})
</script>

<style scoped>
.page { padding: 20px 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--si-border); }
.page-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: var(--si-text-primary); }

.filter-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.main-body {
  display: flex;
  gap: 16px;
  min-height: 0;
}

.stats-panel {
  flex-shrink: 0;
  width: 280px;
  background: #FFFFFF;
  border: 1px solid var(--si-border);
  border-radius: var(--si-radius-lg);
  padding: 12px;
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}
.stats-chart { height: 180px; width: 100%; }

.table-panel {
  flex: 1;
  min-width: 0;
}

:deep(.el-table) { border-radius: var(--si-radius-md); }
:deep(.el-table th.el-table__cell) { background: var(--si-bg-hover); color: var(--si-text-secondary); font-weight: 600; }

:deep(.detail-desc) { max-height: 260px; overflow-y: auto; }
:deep(.detail-desc .image-card) {
  display: inline-block; width: 200px; vertical-align: top;
  border: 1px solid #e2e8f0; border-radius: 6px;
  overflow: hidden; background: #fff; margin: 4px 8px 4px 0;
  transition: box-shadow 0.2s;
}
:deep(.detail-desc .image-card:hover) { box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
:deep(.detail-desc .image-thumb) {
  height: 120px; overflow: hidden; cursor: pointer;
  display: flex; align-items: center; justify-content: center; background: #f5f5f5;
}
:deep(.detail-desc .image-thumb img) { max-width: 100%; max-height: 100%; object-fit: contain; }
:deep(.detail-desc .image-info) { display: flex; padding: 4px 8px; justify-content: space-between; align-items: center; }
:deep(.detail-desc .image-name) {
  font-size: 12px; color: #334155; overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0;
}
:deep(.detail-desc .image-size) { font-size: 11px; color: #94A3B8; flex-shrink: 0; margin-left: 4px; }
:deep(.detail-desc .image-actions) {
  display: flex; border-top: 1px solid #e2e8f0;
}
:deep(.detail-desc .image-action-btn) {
  flex: 1; font-size: 12px; border: none; background: none; cursor: pointer;
  padding: 5px 0; color: #409eff; text-align: center; transition: background 0.2s;
}
:deep(.detail-desc .image-action-btn:hover) { background: #ecf5ff; }
:deep(.detail-desc .image-action-danger) { color: #f56c6c; }
:deep(.detail-desc .image-action-danger:hover) { background: #fef0f0; }
:deep(.detail-desc [data-action="edit"]),
:deep(.detail-desc [data-action="rename"]) { display: none; }
</style>
