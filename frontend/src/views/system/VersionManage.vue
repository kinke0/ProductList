<template>
  <div class="page">
    <div class="page-header">
      <h3>版本管理</h3>
      <el-button type="primary" size="small" :disabled="versions.some(v => v.status === 'draft')" @click="handleCreateVersion">创建新版本</el-button>
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
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button v-if="row.status === 'draft'" size="small" type="success" @click="handleRelease(row.id)">封板发布</el-button>
          <el-button v-if="row.status === 'released'" size="small" type="warning" @click="handleRollback(row.id, row.versionNo)">退回</el-button>
          <span v-if="row.status === 'released' && row.rollbackCount > 0" style="margin-left: 8px; font-size: 12px; color: #909399;">已退回{{ row.rollbackCount }}次</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getVersions, createVersion, releaseVersion, rollbackVersion } from '../../api/version'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../../store/auth'

const authStore = useAuthStore()
const versions = ref([])
const currentVersion = ref(null)

async function loadVersions() {
  const res = await getVersions()
  versions.value = res.data || []
  const draft = versions.value.find(v => v.status === 'draft')
  currentVersion.value = draft || versions.value[versions.value.length - 1] || null
}

async function handleCreateVersion() {
  await createVersion()
  ElMessage.success('版本创建成功')
  loadVersions()
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
</script>

<style scoped>
.page { padding: 20px 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--si-border); }
.page-header h3 { margin: 0; font-size: 16px; font-weight: 600; color: var(--si-text-primary); }
.status-bar { margin-bottom: 16px; font-size: 14px; color: var(--si-text-secondary); display: flex; align-items: center; gap: 8px; }
:deep(.el-table) { border-radius: var(--si-radius-md); }
:deep(.el-table th.el-table__cell) { background: var(--si-bg-hover); color: var(--si-text-secondary); font-weight: 600; }
</style>
