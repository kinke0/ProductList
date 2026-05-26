<template>
  <div v-if="!selectedVersion" class="version-pick">
    <h3>选择版本</h3>
    <el-table :data="versions" highlight-current-row @current-change="onVersionSelect" style="cursor:pointer;">
      <el-table-column prop="versionNo" label="版本号" width="100" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'draft'" type="warning" size="small">编辑中</el-tag>
          <el-tag v-else type="success" size="small">已发布</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布日期">
        <template #default="{ row }">{{ row.releasedAt ? row.releasedAt.substring(0, 10) : '-' }}</template>
      </el-table-column>
      <el-table-column label="创建日期">
        <template #default="{ row }">{{ row.createdAt ? row.createdAt.substring(0, 10) : '-' }}</template>
      </el-table-column>
    </el-table>
  </div>

  <div v-else class="workbench">
    <el-dialog v-model="showInsertDialog" title="选择目标清单" width="400px">
      <el-table :data="customTabs" highlight-current-row @current-change="onSelectInsertTarget" style="cursor:pointer;">
        <el-table-column prop="name" label="清单名称" />
      </el-table>
      <template #footer>
        <el-button @click="showInsertDialog = false">取消</el-button>
      </template>
    </el-dialog>

    <div class="workbench-header">
      <div class="version-info">
        <span class="version-badge">v{{ selectedVersion.versionNo }}</span>
        <span class="version-date" v-if="selectedVersion.releasedAt">发布: {{ formatDate(selectedVersion.releasedAt) }}</span>
        <el-tag v-if="selectedVersion.status === 'draft'" type="warning" size="small">编辑中</el-tag>
        <el-tag v-else type="success" size="small">已发布</el-tag>
        <span v-if="selectedVersion.status !== 'draft'" class="readonly-tip">已发版版本不可修改</span>
      </div>
      <el-button size="small" @click="showVersionDialog = true">切换版本</el-button>
    </div>

    <el-dialog v-model="showVersionDialog" title="切换版本" width="500px">
      <el-table :data="versions" highlight-current-row @current-change="onVersionSelect" style="cursor:pointer;">
        <el-table-column prop="versionNo" label="版本号" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'draft'" type="warning" size="small">编辑中</el-tag>
            <el-tag v-else type="success" size="small">已发布</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布日期">
          <template #default="{ row }">{{ row.releasedAt ? row.releasedAt.substring(0, 10) : '-' }}</template>
        </el-table-column>
        <el-table-column label="创建日期">
          <template #default="{ row }">{{ row.createdAt ? row.createdAt.substring(0, 10) : '-' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <div class="workbench-body">
      <div class="left-panel">
        <TreePanel :version-id="selectedVersion.id" @select="onTreeSelect" />
      </div>
      <div class="right-panel">
        <div class="tabs-wrapper">
          <el-tabs v-model="activeTab" style="height: 100%; display: flex; flex-direction: column;">
            <el-tab-pane label="统计视图" name="stats">
              <StatsTab :version-id="selectedVersion.id" />
            </el-tab-pane>
            <el-tab-pane label="数据清单" name="list">
              <DataListTab
                :version-id="selectedVersion.id"
                :selected-node="selectedNode"
                :is-editing="selectedVersion.status === 'draft'"
                @insert-to-list="onInsertToList"
              />
            </el-tab-pane>
            <el-tab-pane
              v-for="tab in customTabs"
              :key="'custom-' + tab.id"
              :label="tab.name"
              :name="'custom-' + tab.id"
              :closable="true"
              @tab-remove="onRemoveTab(tab)"
            >
              <DataListTab
                :version-id="selectedVersion.id"
                :selected-node="null"
                :is-editing="selectedVersion.status === 'draft'"
                :custom-tab-id="tab.id"
                @insert-to-list="onInsertToList"
              />
            </el-tab-pane>
          </el-tabs>
          <el-button class="add-list-btn" size="small" @click="onAddList">添加清单</el-button>
        </div>
      </div>
    </div>
  </div>

  <el-dialog v-model="showDocDialog" title="生成文档" width="700px">
    <el-form label-width="100px" size="small">
      <el-form-item label="文档类型">
        <el-radio-group v-model="docType">
          <el-radio value="bid">招标参数</el-radio>
          <el-radio value="feature">功能说明</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="输出格式">
        <el-radio-group v-model="docFormat">
          <el-radio value="word">Word (.docx)</el-radio>
          <el-radio value="excel">Excel (.xlsx)</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="数据范围">
        <el-radio-group v-model="dataScope">
          <el-radio value="selected">仅勾选产品 ({{ selectedEntryIds.length }}条)</el-radio>
          <el-radio value="all">整个版本</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <el-divider content-position="left">生成记录</el-divider>
    <el-table :data="genRecords" size="small" max-height="300" v-loading="recordsLoading">
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'generating' || row.status === 'processing'" type="warning" size="small">
            <span class="spinning-dot" /> 生成中
          </el-tag>
          <el-tag v-else-if="row.status === 'completed' || row.status === 'success'" type="success" size="small">已完成</el-tag>
          <el-tag v-else-if="row.status === 'error'" type="danger" size="small">生成错误</el-tag>
          <el-tag v-else type="info" size="small">{{ row.status || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文档类型" width="140">
        <template #default="{ row }">
          {{ row.format === 'word' ? 'Word' : 'Excel' }}版{{ row.docType === 'bid' ? '招标参数' : '功能说明' }}
        </template>
      </el-table-column>
      <el-table-column label="生成时间" width="160">
        <template #default="{ row }">
          {{ row.createdAt ? row.createdAt.replace('T', ' ').substring(0, 19) : '' }}
        </template>
      </el-table-column>
      <el-table-column label="文档大小" width="90">
        <template #default="{ row }">
          {{ row.fileSize ? (row.fileSize / 1024).toFixed(1) + 'KB' : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="generatedByName" label="生成人" width="80" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <template v-if="row.status === 'completed' || row.status === 'success'">
            <el-button type="primary" link size="small" @click="handlePreview(row)">预览</el-button>
            <el-button type="primary" link size="small" @click="handleDownload(row)">下载</el-button>
            <el-button type="danger" link size="small" @click="handleDeleteRecord(row)">删除</el-button>
          </template>
          <el-button v-else type="danger" link size="small" @click="handleDeleteRecord(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="showDocDialog = false">取消</el-button>
      <el-button type="primary" :loading="docLoading" @click="handleGenerate">生成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import TreePanel from '../../components/TreePanel.vue'
import StatsTab from '../../components/StatsTab.vue'
import DataListTab from '../../components/DataListTab.vue'
import { generateDocument, getDocRecords, downloadDocument, deleteDocRecord } from '../../api/document'
import { getVersions } from '../../api/version'
import { getCustomTabs, createCustomTab, deleteCustomTab, addEntriesToTab } from '../../api/customTab'
import { ElMessage, ElMessageBox } from 'element-plus'

const versions = ref([])
const selectedVersion = ref(null)
const showVersionDialog = ref(false)
const selectedNode = ref(null)
const activeTab = ref('stats')
const showDocDialog = ref(false)
const docType = ref('feature')
const docFormat = ref('word')
const dataScope = ref('all')
const selectedEntryIds = ref([])
const docLoading = ref(false)
const genRecords = ref([])
const recordsLoading = ref(false)
const customTabs = ref([])
const showInsertDialog = ref(false)
const insertEntryIds = ref([])
let pollTimer = null

onMounted(async () => {
  try {
    const res = await getVersions()
    versions.value = res.data || []
  } catch (e) {
    console.error('加载版本列表失败:', e)
  }
})

watch(showDocDialog, (val) => {
  if (val && selectedVersion.value) {
    loadGenRecords()
  } else {
    stopPolling()
  }
})

watch(selectedVersion, async (version) => {
  if (version) {
    await loadCustomTabs()
    activeTab.value = 'stats'
  }
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

function onVersionSelect(version) {
  selectedVersion.value = version
  showVersionDialog.value = false
  selectedNode.value = null
}

function onTreeSelect(node) {
  selectedNode.value = node
}

async function loadCustomTabs() {
  if (!selectedVersion.value) return
  try {
    const res = await getCustomTabs(selectedVersion.value.id)
    customTabs.value = res.data || []
  } catch (e) {
    console.error('加载自定义清单失败:', e)
  }
}

async function onAddList() {
  try {
    const { value } = await ElMessageBox.prompt('请输入清单名称', '添加清单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValidator: (val) => val && val.trim() ? true : '名称不能为空'
    })
    if (value) {
      await createCustomTab({
        name: value.trim(),
        versionId: selectedVersion.value.id
      })
      ElMessage.success('清单创建成功')
      await loadCustomTabs()
      if (customTabs.value.length > 0) {
        const last = customTabs.value[customTabs.value.length - 1]
        activeTab.value = 'custom-' + last.id
      }
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.response?.data?.message || '创建失败')
    }
  }
}

async function onRemoveTab(tab) {
  try {
    await ElMessageBox.confirm(`确认删除清单"${tab.name}"？`, '确认', { type: 'warning' })
    await deleteCustomTab(tab.id)
    ElMessage.success('已删除')
    activeTab.value = 'list'
    await loadCustomTabs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function onInsertToList(entryIds) {
  if (!entryIds || entryIds.length === 0) {
    ElMessage.warning('请先勾选条目')
    return
  }
  if (customTabs.value.length === 0) {
    ElMessage.warning('请先创建自定义清单')
    return
  }
  insertEntryIds.value = entryIds
  showInsertDialog.value = true
}

function onSelectInsertTarget(tab) {
  if (tab) {
    addEntriesToTab(tab.id, insertEntryIds.value).then(() => {
      ElMessage.success('插入成功')
      showInsertDialog.value = false
    }).catch(() => {
      ElMessage.error('插入失败')
    })
  }
}

function onGenerateDoc(ids) {
  selectedEntryIds.value = ids
  showDocDialog.value = true
}

async function loadGenRecords() {
  if (!selectedVersion.value) return
  recordsLoading.value = true
  try {
    const res = await getDocRecords(selectedVersion.value.id)
    genRecords.value = res.data || []
  } finally {
    recordsLoading.value = false
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(() => {
    loadGenRecords()
    const hasGenerating = genRecords.value.some(r => r.status === 'generating')
    if (!hasGenerating) stopPolling()
  }, 2000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

async function handleGenerate() {
  if (dataScope.value === 'selected' && selectedEntryIds.value.length === 0) {
    ElMessage.warning('请先选择数据')
    return
  }
  docLoading.value = true
  try {
    const res = await generateDocument({
      versionId: selectedVersion.value.id,
      docType: docType.value,
      format: docFormat.value,
      dataScope: dataScope.value,
      entryIds: dataScope.value === 'selected' ? selectedEntryIds.value : []
    })
    if (res.code === 200) {
      ElMessage.success('文档正在生成中...')
      loadGenRecords()
      startPolling()
    } else {
      ElMessage.error(res.message || '生成失败')
    }
  } catch (e) {
    ElMessage.error('文档生成请求失败')
  } finally {
    docLoading.value = false
  }
}

async function handleDownload(row) {
  try {
    const res = await downloadDocument(row.id)
    const ext = row.format === 'word' ? 'docx' : 'xlsx'
    const label = row.docType === 'bid' ? '招标参数' : '功能说明'
    const blob = new Blob([res], {
      type: row.format === 'word'
        ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${label}.${ext}`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

async function handlePreview(row) {
  try {
    const res = await downloadDocument(row.id)
    const blob = new Blob([res], {
      type: row.format === 'word'
        ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch (e) {
    ElMessage.error('预览失败')
  }
}

async function handleDeleteRecord(row) {
  try {
    await ElMessageBox.confirm('确定删除这条生成记录吗？', '确认', { type: 'warning' })
    await deleteDocRecord(row.id)
    ElMessage.success('已删除')
    loadGenRecords()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.version-pick {
  padding: 40px;
  max-width: 600px;
  margin: 0 auto;
}
.version-pick h3 {
  margin-bottom: 16px;
  font-size: 18px;
  color: #1a2a3a;
}
.workbench {
  height: calc(100vh - 82px);
  display: flex;
  flex-direction: column;
}
.workbench-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fff;
  border-radius: 4px;
  margin-bottom: 8px;
}
.version-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.version-badge {
  font-weight: 700;
  font-size: 16px;
  color: #1a2a3a;
}
.version-date {
  color: #999;
  font-size: 13px;
}
.readonly-tip {
  color: #f56c6c;
  font-size: 12px;
}
.tabs-wrapper {
  position: relative;
  height: 100%;
}
.tabs-wrapper :deep(.el-tabs__header) {
  padding-right: 100px;
}
.add-list-btn {
  position: absolute;
  top: 5px;
  right: 0;
  z-index: 1;
}
.workbench-body {
  display: flex;
  flex: 1;
  gap: 8px;
  overflow: hidden;
}
.left-panel {
  width: 260px;
  background: #fff;
  border-radius: 4px;
  overflow-y: auto;
  flex-shrink: 0;
}
.right-panel {
  flex: 1;
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
:deep(.el-tabs) { display: flex; flex-direction: column; height: 100%; }
:deep(.el-tabs__header) { flex-shrink: 0; }
:deep(.el-tabs__content) { flex: 1; overflow: hidden; min-height: 0; }
:deep(.el-tab-pane) { height: 100%; overflow: hidden; }

@keyframes spin-glow {
  0% { box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.6); }
  50% { box-shadow: 0 0 6px 3px rgba(230, 162, 60, 0.4); }
  100% { box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.6); }
}

@keyframes spin-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.spinning-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: 2px solid #e6a23c;
  border-top-color: transparent;
  margin-right: 4px;
  vertical-align: -1px;
  animation: spin-rotate 0.8s linear infinite, spin-glow 1.2s ease-in-out infinite;
}
</style>
