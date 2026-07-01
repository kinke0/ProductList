<template>
  <div class="page">
    <div class="page-header">
      <h3>版本管理</h3>
      <div>
        <el-button type="primary" size="small" :disabled="versions.some(v => v.status === 'draft') || opRunning" @click="handleCreateVersion">创建新版本</el-button>
      </div>
    </div>
    <div v-if="currentVersion" class="status-bar">
      当前版本: {{ currentVersion.versionNo }}
      <el-tag v-if="currentVersion.status === 'draft'" type="warning">编辑中</el-tag>
      <el-tag v-else type="success">已发布</el-tag>
      <el-tag v-if="currentVersion.rollbackCount > 0" type="info" size="small">已退回{{ currentVersion.rollbackCount }}次</el-tag>
    </div>
    <el-table :data="versions" border stripe size="small">
      <el-table-column prop="versionNo" label="版本号" width="100" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'draft'" type="warning">编辑中</el-tag>
          <el-tag v-else type="success">已发布</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布人" width="100">
        <template #default="{ row }">
          {{ row.releasedByName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="发布日期" width="120">
        <template #default="{ row }">
          {{ row.releasedAt ? row.releasedAt.substring(0, 10) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="创建日期" width="120">
        <template #default="{ row }">
          {{ row.createdAt ? row.createdAt.substring(0, 10) : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="200">
        <template #default="{ row }">
          <el-button v-if="row.status === 'draft'" size="small" type="success" @click="handleRelease(row.id)">封板发布</el-button>
          <el-button v-if="row.status === 'released'" size="small" type="warning" @click="handleRollback(row.id, row.versionNo)">退回</el-button>
          <span v-if="row.status === 'released' && row.rollbackCount > 0" style="margin-left: 8px; font-size: 12px; color: #909399;">已退回{{ row.rollbackCount }}次</span>
        </template>
      </el-table-column>
      <el-table-column label="删除" width="80" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="progressVisible" :title="progressTitle" width="560px" :close-on-click-modal="false" :close-on-press-escape="false" :show-close="progressDone">
      <div v-if="progressSteps.length === 0" class="loading-hint">
        <el-icon class="is-loading" :size="24" color="#409eff"><Loading /></el-icon>
        <span>正在准备，请稍候...</span>
      </div>
      <div v-else class="steps-panel">
        <div v-for="step in progressSteps" :key="step.step" class="step-row">
          <span class="step-icon">
            <el-icon v-if="step.status === 'COMPLETED'" color="#67c23a"><CircleCheckFilled /></el-icon>
            <el-icon v-else-if="step.status === 'RUNNING'" class="is-loading" color="#409eff"><Loading /></el-icon>
            <el-icon v-else-if="step.status === 'FAILED'" color="#f56c6c"><CircleCloseFilled /></el-icon>
            <el-icon v-else color="#c0c4cc"><Clock /></el-icon>
          </span>
          <span class="step-label">步骤{{ step.step }}/{{ progressSteps.length }}：{{ step.name }}</span>
          <span class="step-status">{{ stepStatusLabel(step.status) }}</span>
          <span v-if="step.message" class="step-message">{{ step.message }}</span>
        </div>
      </div>
      <div v-if="progressResult" class="progress-result">
        <el-alert :type="progressDone && !progressError ? 'success' : 'error'" :closable="false" show-icon>
          <template #title>{{ progressResult }}</template>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="closeProgress" :disabled="!progressDone">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getVersions, createVersion, releaseVersion, rollbackVersion, deleteVersion, getVersionProgress } from '../../api/version'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheckFilled, CircleCloseFilled, Clock, Loading } from '@element-plus/icons-vue'

const versions = ref([])
const currentVersion = ref(null)
const progressVisible = ref(false)
const progressSteps = ref([])
const progressResult = ref('')
const progressDone = ref(false)
const progressError = ref(false)
const progressOperation = ref('')
let pollTimer = null

const opRunning = computed(() => progressVisible.value && !progressDone.value)
const progressTitle = computed(() => progressOperation.value === 'CREATE' ? '创建新版本' : '删除版本')

async function loadVersions() {
  const res = await getVersions()
  versions.value = res.data || []
  const draft = versions.value.find(v => v.status === 'draft')
  currentVersion.value = draft || versions.value[versions.value.length - 1] || null
}

function stepStatusLabel(status) {
  if (status === 'COMPLETED') return '完成'
  if (status === 'RUNNING') return '进行中...'
  if (status === 'FAILED') return '失败'
  return '等待中'
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const res = await getVersionProgress()
      const data = res.data
      if (!data) return
      progressSteps.value = data.steps || []
      if (data.status === 'COMPLETED') {
        progressDone.value = true
        progressError.value = false
        progressResult.value = data.result || '操作完成'
        stopPolling()
        loadVersions()
      } else if (data.status === 'FAILED') {
        progressDone.value = true
        progressError.value = true
        progressResult.value = data.result || '操作失败'
        stopPolling()
      }
    } catch (e) {
      console.error('轮询进度失败', e)
    }
  }, 1000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function closeProgress() {
  progressVisible.value = false
  stopPolling()
}

async function handleCreateVersion() {
  ElMessageBox.confirm(
    '即将从最新已发布版本克隆全部数据创建新版本，包含清单数据、业务分类、产品分类、基础选项、图片资源、自定义清单等。确认创建？',
    '创建新版本',
    { confirmButtonText: '确认创建', cancelButtonText: '取消', type: 'info' }
  ).then(async () => {
    progressVisible.value = true
    progressDone.value = false
    progressError.value = false
    progressResult.value = ''
    progressSteps.value = []
    progressOperation.value = 'CREATE'
    startPolling()
    try {
      await createVersion()
    } catch (e) {
      stopPolling()
      progressVisible.value = false
      ElMessage.error(e.response?.data?.message || '创建版本失败')
    }
  }).catch(() => {})
}

async function handleDelete(row) {
  const statusWarn = row.status === 'released'
    ? '此版本已发布，删除后该版本的全部数据将不可恢复！'
    : ''
  ElMessageBox.confirm(
    `即将删除版本 ${row.versionNo}，此操作将清理该版本的全部关联数据：清单数据、分类、选项、图片、自定义清单、文档生成记录等。操作不可撤销！${statusWarn}`,
    '删除版本',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error' }
  ).then(async () => {
    progressVisible.value = true
    progressDone.value = false
    progressError.value = false
    progressResult.value = ''
    progressSteps.value = []
    progressOperation.value = 'DELETE'
    startPolling()
    try {
      await deleteVersion(row.id)
    } catch (e) {
      stopPolling()
      progressVisible.value = false
      ElMessage.error(e.response?.data?.message || '删除版本失败')
    }
  }).catch(() => {})
}

async function handleRelease(id) {
  const version = versions.value.find(v => v.id === id)
  const versionNo = version?.versionNo || ''
  const msg = version?.rollbackCount > 0
    ? `确认封板发布？发布后版本号将升级为 ${getNextVersionNo(versionNo, version.rollbackCount)}`
    : '确认封板发布？发布后版本将不可再编辑。'
  ElMessageBox.confirm(msg, '提示', {
    confirmButtonText: '发布',
    type: 'warning'
  }).then(async () => {
    await releaseVersion(id)
    ElMessage.success('发布成功')
    loadVersions()
  }).catch(() => {})
}

function getNextVersionNo(versionNo, rollbackCount) {
  const dotCount = (versionNo.match(/\./g) || []).length
  if (dotCount < 2) return versionNo + '.1'
  const lastDot = versionNo.lastIndexOf('.')
  return versionNo.substring(0, lastDot + 1) + (parseInt(versionNo.substring(lastDot + 1)) + 1)
}

async function handleRollback(id, versionNo) {
  ElMessageBox.confirm(`确认退回版本 ${versionNo}？退回后版本将变为编辑中状态。`, '提示', {
    confirmButtonText: '确认退回',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await rollbackVersion(id)
    ElMessage.success('版本已退回')
    loadVersions()
  }).catch(() => {})
}

onMounted(loadVersions)
onUnmounted(stopPolling)
</script>

<style scoped>
.page { padding: 20px 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--si-border); }
.page-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: var(--si-text-primary); }
.status-bar { margin-bottom: 16px; font-size: 14px; color: var(--si-text-secondary); display: flex; align-items: center; gap: 8px; }
:deep(.el-table) { border-radius: var(--si-radius-md); }
:deep(.el-table th.el-table__cell) { background: var(--si-bg-hover); color: var(--si-text-secondary); font-weight: 600; }

.steps-panel { margin-bottom: 16px; }
.loading-hint { display: flex; align-items: center; justify-content: center; gap: 10px; padding: 40px 0; font-size: 14px; color: #909399; }
.step-row { display: flex; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; gap: 8px; }
.step-row:last-child { border-bottom: none; }
.step-icon { flex-shrink: 0; width: 20px; text-align: center; }
.step-label { font-size: 13px; color: #303133; min-width: 200px; }
.step-status { font-size: 12px; color: #909399; min-width: 60px; }
.step-message { font-size: 12px; color: #67c23a; }
.progress-result { margin-top: 12px; }
</style>
