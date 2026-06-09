<template>
  <div class="page special-ops-page">
    <div class="page-header">
      <h3>非常规操作</h3>
    </div>
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="一键迁移" name="auto">
        <div class="section-desc">
          <el-alert type="warning" :closable="false" show-icon>
            <template #title>注意：此操作将按顺序执行全部5个步骤，请确保已停止所有编辑操作</template>
          </el-alert>
        </div>
        <div class="steps-panel">
          <div v-for="(step, idx) in status.steps" :key="step.step" class="step-row">
            <span class="step-icon">
              <el-icon v-if="step.status === 'COMPLETED'" color="#67c23a"><CircleCheckFilled /></el-icon>
              <el-icon v-else-if="step.status === 'RUNNING'" class="is-loading" color="#409eff"><Loading /></el-icon>
              <el-icon v-else-if="step.status === 'FAILED'" color="#f56c6c"><CircleCloseFilled /></el-icon>
              <el-icon v-else color="#c0c4cc"><Clock /></el-icon>
            </span>
            <span class="step-label">步骤{{ step.step }}/5：{{ step.name }}</span>
            <span class="step-status">{{ statusLabel(step.status) }}</span>
            <span v-if="step.message" class="step-message">{{ step.message }}</span>
            <span v-if="step.durationMs" class="step-duration">({{ (step.durationMs / 1000).toFixed(1) }}s)</span>
          </div>
        </div>
        <div v-if="status.status === 'RUNNING'" class="progress-bar">
          <el-progress :percentage="progressPercent" :format="() => `${status.processedCount}/${status.totalCount}`" />
        </div>
        <div class="actions">
          <el-button type="primary" :loading="running" :disabled="status.status === 'RUNNING'" @click="startAll">
            {{ running ? '迁移中...' : '一键执行迁移' }}
          </el-button>
          <el-button @click="resetStatus" :disabled="status.status === 'RUNNING'">重置状态</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="分步操作" name="manual">
        <div class="section-desc">
          <el-alert type="info" :closable="false" show-icon>
            <template #title>逐步执行，每一步完成后才能执行下一步。适合排查问题或分阶段操作</template>
          </el-alert>
        </div>
        <div class="steps-panel">
          <div v-for="(step, idx) in manualSteps" :key="step.step" class="step-card">
            <div class="step-card-header">
              <span class="step-num">步骤{{ step.step }}：{{ step.name }}</span>
              <el-tag size="small" :type="stepTagType(step.status)">{{ statusLabel(step.status) }}</el-tag>
            </div>
            <div v-if="step.message" class="step-card-message">{{ step.message }}</div>
            <div v-if="step.durationMs" class="step-card-duration">耗时 {{ (step.durationMs / 1000).toFixed(1) }}s</div>
            <div class="step-card-actions">
              <el-button size="small" type="primary" :loading="step.running" :disabled="!canRunStep(step.step)" @click="runStep(step.step)">
                执行
              </el-button>
            </div>
          </div>
        </div>
        <div class="actions">
          <el-button @click="resetStatus" :disabled="anyRunning">重置状态</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="同步图片文件名" name="sync">
        <div class="section-desc">
          <el-alert type="warning" :closable="false" show-icon>
            <template #title>将图片物理文件名同步为显示名，使文件名与图片名一致。执行后Word/预览中的图片标题将正确显示</template>
          </el-alert>
        </div>
        <div v-if="syncStatus.status === 'RUNNING'" class="progress-bar">
          <el-progress :percentage="syncProgressPercent" :format="() => `${syncStatus.processedCount}/${syncStatus.totalCount}`" />
        </div>
        <div v-if="syncStatus.steps && syncStatus.steps.length" class="steps-panel">
          <div v-for="step in syncStatus.steps" :key="step.step" class="step-row">
            <span class="step-icon">
              <el-icon v-if="step.status === 'COMPLETED'" color="#67c23a"><CircleCheckFilled /></el-icon>
              <el-icon v-else-if="step.status === 'RUNNING'" class="is-loading" color="#409eff"><Loading /></el-icon>
              <el-icon v-else-if="step.status === 'FAILED'" color="#f56c6c"><CircleCloseFilled /></el-icon>
              <el-icon v-else color="#c0c4cc"><Clock /></el-icon>
            </span>
            <span class="step-label">{{ step.name }}</span>
            <span class="step-status">{{ statusLabel(step.status) }}</span>
            <span v-if="step.message" class="step-message">{{ step.message }}</span>
            <span v-if="step.durationMs" class="step-duration">({{ (step.durationMs / 1000).toFixed(1) }}s)</span>
          </div>
        </div>
        <div class="actions">
          <el-button type="primary" :loading="syncRunning" :disabled="syncStatus.status === 'RUNNING'" @click="startSync">
            {{ syncRunning ? '同步中...' : '执行同步' }}
          </el-button>
          <el-button @click="resetSync" :disabled="syncStatus.status === 'RUNNING'">重置状态</el-button>
        </div>
      </el-tab-pane>
      <el-tab-pane label="修复图片引用ID" name="fixid">
        <div class="section-desc">
          <el-alert type="warning" :closable="false" show-icon>
            <template #title>修复 DataEntry 中图片卡片引用的 data-id，使其指向正确的 image_resource 记录。版本隔离迁移后历史数据的 data-id 可能失效</template>
          </el-alert>
        </div>
        <div v-if="fixIdStatus.status === 'RUNNING'" class="progress-bar">
          <el-progress :percentage="fixIdProgressPercent" :format="() => `${fixIdStatus.processedCount}/${fixIdStatus.totalCount}`" />
        </div>
        <div v-if="fixIdStatus.steps && fixIdStatus.steps.length" class="steps-panel">
          <div v-for="step in fixIdStatus.steps" :key="step.step" class="step-row">
            <span class="step-icon">
              <el-icon v-if="step.status === 'COMPLETED'" color="#67c23a"><CircleCheckFilled /></el-icon>
              <el-icon v-else-if="step.status === 'RUNNING'" class="is-loading" color="#409eff"><Loading /></el-icon>
              <el-icon v-else-if="step.status === 'FAILED'" color="#f56c6c"><CircleCloseFilled /></el-icon>
              <el-icon v-else color="#c0c4cc"><Clock /></el-icon>
            </span>
            <span class="step-label">{{ step.name }}</span>
            <span class="step-status">{{ statusLabel(step.status) }}</span>
            <span v-if="step.message" class="step-message">{{ step.message }}</span>
            <span v-if="step.durationMs" class="step-duration">({{ (step.durationMs / 1000).toFixed(1) }}s)</span>
          </div>
        </div>
        <div class="actions">
          <el-button type="primary" :loading="fixIdRunning" :disabled="fixIdStatus.status === 'RUNNING'" @click="startFixId">
            {{ fixIdRunning ? '修复中...' : '执行修复' }}
          </el-button>
          <el-button @click="resetFixIdStatus" :disabled="fixIdStatus.status === 'RUNNING'">重置状态</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { migrateImageAll, migrateStep, getMigrationStatus, resetMigration, syncFilenames, getFilenameSyncStatus, resetFilenameSync, fixImageCardIds, getFixIdStatus, resetFixId } from '../../api/maintenance'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheckFilled, CircleCloseFilled, Loading, Clock } from '@element-plus/icons-vue'

const activeTab = ref('auto')
const running = ref(false)
let pollTimer = null

const status = ref({
  status: 'NOT_STARTED',
  currentStep: 0,
  totalSteps: 5,
  processedCount: 0,
  totalCount: 0,
  steps: [
    { step: 1, name: '备份数据库', status: 'PENDING' },
    { step: 2, name: '复制图片到版本目录', status: 'PENDING' },
    { step: 3, name: '更新数据库URL引用', status: 'PENDING' },
    { step: 4, name: '去重清理重复记录', status: 'PENDING' },
    { step: 5, name: '清理旧文件', status: 'PENDING' }
  ]
})

const manualSteps = computed(() => {
  return status.value.steps.map((s, idx) => ({
    ...s,
    running: running.value && status.value.currentStep === s.step && status.value.status === 'RUNNING'
  }))
})

const anyRunning = computed(() => status.value.status === 'RUNNING')

const progressPercent = computed(() => {
  if (status.value.totalCount === 0) return 0
  return Math.round((status.value.processedCount / status.value.totalCount) * 100)
})

function statusLabel(s) {
  switch (s) {
    case 'PENDING': return '等待中'
    case 'RUNNING': return '进行中'
    case 'COMPLETED': return '已完成'
    case 'FAILED': return '失败'
    case 'SKIPPED': return '已跳过'
    default: return '未开始'
  }
}

function stepTagType(s) {
  switch (s) {
    case 'COMPLETED': return 'success'
    case 'RUNNING': return 'primary'
    case 'FAILED': return 'danger'
    case 'SKIPPED': return 'warning'
    default: return 'info'
  }
}

function canRunStep(step) {
  if (status.value.status === 'RUNNING') return false
  const stepResult = status.value.steps.find(s => s.step === step)
  if (stepResult && stepResult.status === 'COMPLETED') return false
  if (step === 1) return true
  for (let i = 1; i < step; i++) {
    const prev = status.value.steps.find(s => s.step === i)
    if (!prev || prev.status !== 'COMPLETED') return false
  }
  return true
}

async function fetchStatus() {
  try {
    const res = await getMigrationStatus()
    if (res.data) {
      status.value = res.data
      if (res.data.status === 'RUNNING') {
        running.value = true
      } else {
        running.value = false
      }
    }
  } catch {}
}

async function startAll() {
  try {
    await ElMessageBox.confirm(
      '确认执行一键迁移？此操作将按顺序执行全部5个步骤，执行期间请勿进行其他操作。',
      '确认迁移',
      { type: 'warning', confirmButtonText: '确认执行', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await migrateImageAll()
    running.value = true
    startPolling()
    ElMessage.info('迁移任务已启动')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '启动失败')
  }
}

async function runStep(step) {
  try {
    await ElMessageBox.confirm(
      `确认执行步骤${step}：${status.value.steps[step - 1]?.name}？`,
      '确认执行',
      { type: 'warning' }
    )
  } catch { return }
  try {
    await migrateStep(step)
    running.value = true
    startPolling()
    ElMessage.info(`步骤${step}已启动`)
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '启动失败')
  }
}

async function resetStatus() {
  try {
    await resetMigration()
    await fetchStatus()
    ElMessage.success('状态已重置')
  } catch (e) {
    ElMessage.error('重置失败')
  }
}

function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(async () => {
    await fetchStatus()
    if (status.value.status !== 'RUNNING') {
      stopPolling()
      if (status.value.status === 'COMPLETED') {
        ElMessage.success('迁移已完成')
      } else if (status.value.status === 'FAILED') {
        ElMessage.error('迁移失败：' + (status.value.errorMessage || '未知错误'))
      }
    }
  }, 1000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const syncRunning = ref(false)
let syncPollTimer = null

const syncStatus = ref({
  status: 'NOT_STARTED',
  processedCount: 0,
  totalCount: 0,
  steps: []
})

const syncProgressPercent = computed(() => {
  if (syncStatus.value.totalCount === 0) return 0
  return Math.round((syncStatus.value.processedCount / syncStatus.value.totalCount) * 100)
})

async function fetchSyncStatus() {
  try {
    const res = await getFilenameSyncStatus()
    if (res.data) {
      syncStatus.value = res.data
      syncRunning.value = res.data.status === 'RUNNING'
    }
  } catch {}
}

async function startSync() {
  try {
    await ElMessageBox.confirm(
      '确认执行图片文件名同步？此操作将把所有图片的物理文件名改为与显示名一致，执行期间请勿进行其他操作。',
      '确认同步',
      { type: 'warning', confirmButtonText: '确认执行', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await syncFilenames()
    syncRunning.value = true
    startSyncPolling()
    ElMessage.info('同步任务已启动')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '启动失败')
  }
}

async function resetSync() {
  try {
    await resetFilenameSync()
    await fetchSyncStatus()
    ElMessage.success('状态已重置')
  } catch {
    ElMessage.error('重置失败')
  }
}

function startSyncPolling() {
  if (syncPollTimer) return
  syncPollTimer = setInterval(async () => {
    await fetchSyncStatus()
    if (syncStatus.value.status !== 'RUNNING') {
      stopSyncPolling()
      if (syncStatus.value.status === 'COMPLETED') {
        ElMessage.success('文件名同步已完成')
      } else if (syncStatus.value.status === 'FAILED') {
        ElMessage.error('同步失败：' + (syncStatus.value.errorMessage || '未知错误'))
      }
    }
  }, 1000)
}

function stopSyncPolling() {
  if (syncPollTimer) {
    clearInterval(syncPollTimer)
    syncPollTimer = null
  }
}

const fixIdRunning = ref(false)
let fixIdPollTimer = null

const fixIdStatus = ref({
  status: 'NOT_STARTED',
  processedCount: 0,
  totalCount: 0,
  steps: []
})

const fixIdProgressPercent = computed(() => {
  if (fixIdStatus.value.totalCount === 0) return 0
  return Math.round((fixIdStatus.value.processedCount / fixIdStatus.value.totalCount) * 100)
})

async function fetchFixIdStatus() {
  try {
    const res = await getFixIdStatus()
    if (res.data) {
      fixIdStatus.value = res.data
      fixIdRunning.value = res.data.status === 'RUNNING'
    }
  } catch {}
}

async function startFixId() {
  try {
    await ElMessageBox.confirm(
      '确认执行图片引用ID修复？此操作将扫描所有数据条目中的图片卡片，将失效的 data-id 修正为正确的 image_resource ID。',
      '确认修复',
      { type: 'warning', confirmButtonText: '确认执行', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    await fixImageCardIds()
    fixIdRunning.value = true
    startFixIdPolling()
    ElMessage.info('修复任务已启动')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '启动失败')
  }
}

async function resetFixIdStatus() {
  try {
    await resetFixId()
    await fetchFixIdStatus()
    ElMessage.success('状态已重置')
  } catch {
    ElMessage.error('重置失败')
  }
}

function startFixIdPolling() {
  if (fixIdPollTimer) return
  fixIdPollTimer = setInterval(async () => {
    await fetchFixIdStatus()
    if (fixIdStatus.value.status !== 'RUNNING') {
      stopFixIdPolling()
      if (fixIdStatus.value.status === 'COMPLETED') {
        ElMessage.success('图片引用ID修复已完成')
      } else if (fixIdStatus.value.status === 'FAILED') {
        ElMessage.error('修复失败：' + (fixIdStatus.value.errorMessage || '未知错误'))
      }
    }
  }, 1000)
}

function stopFixIdPolling() {
  if (fixIdPollTimer) {
    clearInterval(fixIdPollTimer)
    fixIdPollTimer = null
  }
}

onMounted(() => {
  fetchStatus()
  fetchSyncStatus()
  fetchFixIdStatus()
})

onUnmounted(() => {
  stopPolling()
  stopSyncPolling()
  stopFixIdPolling()
})
</script>

<style scoped>
.special-ops-page { padding: 20px 24px; }
.page-header { margin-bottom: 16px; }
.page-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: var(--si-text-primary); }
.section-desc { margin-bottom: 16px; }
.steps-panel { margin-bottom: 16px; }
.step-row {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 12px; border: 1px solid var(--si-border); border-radius: 6px;
  margin-bottom: 8px; background: var(--si-bg-card);
}
.step-label { font-size: 13px; font-weight: 500; color: var(--si-text-primary); min-width: 200px; }
.step-status { font-size: 12px; color: var(--si-text-secondary); }
.step-message { font-size: 12px; color: var(--si-text-muted); flex: 1; }
.step-duration { font-size: 11px; color: var(--si-text-muted); }
.step-icon { font-size: 16px; }
.progress-bar { margin-bottom: 16px; }
.actions { display: flex; gap: 8px; }
.step-card {
  border: 1px solid var(--si-border); border-radius: 8px; padding: 16px;
  margin-bottom: 12px; background: var(--si-bg-card);
}
.step-card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.step-num { font-size: 14px; font-weight: 600; color: var(--si-text-primary); }
.step-card-message { font-size: 12px; color: var(--si-text-secondary); margin-bottom: 4px; }
.step-card-duration { font-size: 11px; color: var(--si-text-muted); margin-bottom: 8px; }
.step-card-actions { display: flex; gap: 8px; }
</style>
