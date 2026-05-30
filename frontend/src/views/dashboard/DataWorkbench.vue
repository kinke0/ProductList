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
      <el-table :data="customTabs" highlight-current-row @row-click="onSelectInsertTarget" style="cursor:pointer;">
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
        <TreePanel :version-id="selectedVersion.id" :highlight-node="treeHighlight" @select="onTreeSelect" />
      </div>
      <div class="right-panel">
        <div class="tabs-wrapper">
          <el-tabs v-model="activeTab" style="height: 100%; display: flex; flex-direction: column;" @tab-remove="onRemoveTab" @tab-click="onTabClick">
            <el-tab-pane label="产品全景图" name="panorama">
              <PanoramaTab
                v-if="activeTab === 'panorama'"
                :version-id="selectedVersion.id"
                :selected-node="selectedNode"
                @navigate-to-list="onNavigateToList"
                @open-preview="onOpenPreview"
              />
            </el-tab-pane>
            <el-tab-pane label="统计视图" name="stats">
              <StatsTab v-if="activeTab === 'stats'" :version-id="selectedVersion.id" :refresh-trigger="statsRefreshTrigger" />
            </el-tab-pane>
            <el-tab-pane label="数据清单" name="list">
              <DataListTab
                v-show="activeTab === 'list'"
                ref="dataListRef"
                :version-id="selectedVersion.id"
                :selected-node="selectedNode"
                :is-editing="selectedVersion.status === 'draft'"
                :user-role="currentUserRole"
                :refresh-trigger="listRefreshTrigger"
                @insert-to-list="onInsertToList"
                @open-preview="onOpenPreview"
                @preview-reload="onPreviewReload"
              />
            </el-tab-pane>
            <el-tab-pane
              v-for="tab in customTabs"
              :key="'custom-' + tab.id"
              :name="'custom-' + tab.id"
              :closable="true"
            >
              <template #label>
                <span @dblclick.stop="onRenameTab(tab)">{{ tab.name }}</span>
              </template>
              <DataListTab
                v-if="activeTab === 'custom-' + tab.id"
                :ref="el => { if (el) customTabRefs[tab.id] = el }"
                :version-id="selectedVersion.id"
                :selected-node="selectedNode"
                :is-editing="selectedVersion.status === 'draft'"
                :custom-tab-id="tab.id"
                :user-role="currentUserRole"
                :refresh-trigger="customTabRefresh"
                @insert-to-list="onInsertToList"
                @remove-from-list="(ids) => onRemoveFromList(tab.id, ids)"
                @generate-doc="(ids, tabId) => onGenerateDoc(ids, tabId)"
              />
            </el-tab-pane>
            <el-tab-pane name="__add_list" disabled>
              <template #label>
                <span class="add-list-tab-btn" @click.stop.prevent="onAddList">
                  <el-icon><Plus /></el-icon> 添加清单
                </span>
              </template>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
    <PreviewDialog ref="globalPreviewRef" v-model="globalPreviewVisible" :entry-id="globalPreviewEntryId" :batch-entry-ids="globalPreviewBatchIds" @preview-message="onGlobalPreviewMessage" />
  </div>

  <el-dialog v-model="showDocDialog" title="生成文档" width="960px" top="2vh">
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
          <el-radio value="all">{{ docCustomTabId ? '整个清单' : '整个版本' }}</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <el-divider content-position="left">生成记录</el-divider>
    <div style="display:flex;align-items:center;gap:12px;margin-bottom:10px;">
      <span style="font-size:13px;color:var(--si-text-secondary);">文档生成人</span>
      <el-select v-model="filterCreator" placeholder="生成人" clearable size="small" style="width:130px;">
        <el-option v-for="name in creatorList" :key="name" :label="name" :value="name" />
      </el-select>
      <el-radio-group v-model="filterTime" size="small">
        <el-radio-button value="3">近3天</el-radio-button>
        <el-radio-button value="7">近7天</el-radio-button>
        <el-radio-button value="30">近30天</el-radio-button>
        <el-radio-button value="">全部</el-radio-button>
      </el-radio-group>
      <div style="margin-left:auto;">
        <el-button type="danger" size="small" :disabled="selectedRecIds.length === 0" @click="batchDeleteRecords">删除 ({{ selectedRecIds.length }})</el-button>
      </div>
    </div>
    <el-table :data="pagedRecords" size="small" max-height="380" v-loading="recordsLoading" @selection-change="onRecSelectionChange" ref="recTable" style="width:100%">
      <el-table-column type="selection" width="40" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'generating' || row.status === 'processing'" type="warning" size="small">
            {{ getRecordPercent(row) }}%
          </el-tag>
          <el-tag v-else-if="row.status === 'completed' || row.status === 'success'" type="success" size="small">完成</el-tag>
          <el-tag v-else-if="row.status === 'error'" type="danger" size="small">错误</el-tag>
          <el-tag v-else type="info" size="small">{{ row.status || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文档类型" min-width="160">
        <template #default="{ row }">
          {{ row.format === 'word' ? 'Word' : 'Excel' }}版{{ row.docType === 'bid' ? '招标参数' : '功能说明' }}
        </template>
      </el-table-column>
      <el-table-column label="生成时间" min-width="170">
        <template #default="{ row }">
          {{ row.createdAt ? row.createdAt.replace('T', ' ').substring(0, 19) : '' }}
        </template>
      </el-table-column>
      <el-table-column label="大小" width="125">
        <template #default="{ row }">
          {{ row.fileSize ? (row.fileSize / 1024).toFixed(1) + 'KB' : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="generatedByName" label="生成人" width="80" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <template v-if="row.status === 'completed' || row.status === 'success'">
            <el-button type="primary" link size="small" @click="handleDownload(row)">下载</el-button>
            <el-button type="danger" link size="small" @click="handleDeleteRecord(row)">删除</el-button>
          </template>
          <el-button v-else type="danger" link size="small" @click="handleDeleteRecord(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="display:flex;justify-content:flex-end;margin-top:8px;">
      <el-pagination
        v-model:current-page="recPage"
        :page-size="recPageSize"
        :total="filteredRecords.length"
        layout="prev, pager, next"
        small
      />
    </div>

    <template #footer>
      <el-button @click="showDocDialog = false">取消</el-button>
      <el-button type="primary" :loading="docLoading" @click="handleGenerate">生成</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="showAddListDialog" title="添加清单" width="420px" :close-on-click-modal="false">
    <el-form :model="addListForm" label-width="100px" size="small">
      <el-form-item label="清单名称" required>
        <el-input v-model="addListForm.name" placeholder="请输入清单名称" @keyup.enter="handleAddList" />
      </el-form-item>
    </el-form>
    <el-divider style="margin: 12px 0 8px;" />
    <div style="padding: 0 0 4px 16px; font-size: 13px; color: #909399;">可通过条件检索建立清单</div>
    <el-form :model="addListForm" label-width="100px" size="small" style="margin-top: 4px;">
      <el-form-item label="名称">
        <el-input v-model="addListForm.entryName" placeholder="产品/系统名称" clearable @keyup.enter="handleAddList" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="addListForm.statusList" placeholder="全部" clearable multiple style="width:100%">
          <el-option v-for="s in addListStatusList" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item label="产品经理">
        <el-input v-model="addListForm.productManager" placeholder="产品经理" clearable @keyup.enter="handleAddList" />
      </el-form-item>
      <el-form-item label="解决方案">
        <el-select v-model="addListForm.solution" placeholder="全部" clearable style="width:100%">
          <el-option v-for="s in addListSolutions" :key="s" :label="s" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item label="版本划分">
        <el-select v-model="addListForm.versionTag" placeholder="全部" clearable style="width:100%">
          <el-option label="A-曜系列" value="A-曜系列" />
          <el-option label="B-远系列" value="B-远系列" />
          <el-option label="C-驰系列" value="C-驰系列" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showAddListDialog = false">取消</el-button>
      <el-button type="primary" :loading="addListLoading" @click="handleAddList">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import TreePanel from '../../components/TreePanel.vue'
import StatsTab from '../../components/StatsTab.vue'
import DataListTab from '../../components/DataListTab.vue'
import PanoramaTab from '../../components/PanoramaTab.vue'
import PreviewDialog from '../../components/PreviewDialog.vue'
import { generateDocument, getDocRecords, downloadDocument, deleteDocRecord, getDocProgress } from '../../api/document'
import { getVersions } from '../../api/version'
import { getCustomTabs, createCustomTabWithFilter, deleteCustomTab, renameCustomTab, addEntriesToTab, removeEntryFromTab } from '../../api/customTab'
import { getOptions } from '../../api/option'
import { deleteEntry } from '../../api/data'
import { approveEntry } from '../../api/approval'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const versions = ref([])
const currentUserRole = localStorage.getItem('roleCode') || 'USER'
const selectedVersion = ref(null)
const showVersionDialog = ref(false)
const selectedNode = ref(null)
const treeHighlight = ref(null)
const activeTab = ref('panorama')
const showDocDialog = ref(false)
const docType = ref('feature')
const docFormat = ref('word')
const dataScope = ref('all')
const selectedEntryIds = ref([])
 const docCustomTabId = ref(null)
  const docLoading = ref(false)
 const genRecords = ref([])
const recordsLoading = ref(false)
const filterCreator = ref('')
const filterTime = ref('')
const selectedRecIds = ref([])
const recPage = ref(1)
const recPageSize = 15
const recTable = ref(null)
const customTabs = ref([])
const showInsertDialog = ref(false)
const insertEntryIds = ref([])
const customTabRefresh = ref(0)
const statsRefreshTrigger = ref(0)
const listRefreshTrigger = ref(0)
const activeGenRecordId = ref(null)
const showAddListDialog = ref(false)
const addListLoading = ref(false)
 const addListSolutions = ref([])
const addListStatusList = ref([])
 const addListForm = reactive({
  name: '',
  entryName: '',
  statusList: [],
  productManager: '',
  solution: '',
  versionTag: ''
})
const progressTotal = ref(0)
const progressProcessed = ref(0)
const progressStatus = ref('')
let pollTimer = null
let progressTimer = null
let progressFullTimestamp = null

const showProgress = computed(() => activeGenRecordId.value !== null && progressTotal.value > 0)
const progressPercent = computed(() => {
  if (!progressTotal.value) return 0
  return (progressProcessed.value / progressTotal.value) * 100
})

function getRecordPercent(row) {
  if (row.id === activeGenRecordId.value && progressTotal.value > 0) {
    return Math.round((progressProcessed.value / progressTotal.value) * 100)
  }
  if (row.totalEntries > 0) {
    return Math.round(((row.processedEntries || 0) / row.totalEntries) * 100)
  }
  return 0
}

async function pollProgress() {
  if (!activeGenRecordId.value) return
  try {
    const res = await getDocProgress(activeGenRecordId.value)
    const r = res.data
    if (r) {
      progressTotal.value = r.totalEntries || 0
      progressProcessed.value = r.processedEntries || 0
      progressStatus.value = r.status || ''
      if (r.status === 'completed' || r.status === 'error') {
        stopProgressPoll()
        loadGenRecords()
      } else if (progressTotal.value > 0 && progressProcessed.value >= progressTotal.value) {
        if (!progressFullTimestamp) progressFullTimestamp = Date.now()
        else if (Date.now() - progressFullTimestamp > 120000) {
          stopProgressPoll()
          progressStatus.value = 'error'
          loadGenRecords()
          ElMessage.error('文档生成超时，可能因文件过大导致序列化失败')
        }
      }
    }
  } catch (e) {
    // ignore
  }
}

function startProgressPoll() {
  stopProgressPoll()
  progressFullTimestamp = null
  pollProgress()
  progressTimer = setInterval(pollProgress, 100)
}

function stopProgressPoll() {
  if (progressTimer) { clearInterval(progressTimer); progressTimer = null }
}

onBeforeUnmount(() => { stopProgressPoll(); stopPolling() })

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
    loadGenRecords().then(() => {
      const gen = genRecords.value.find(r => r.status === 'generating')
      if (gen) {
        activeGenRecordId.value = gen.id
        progressTotal.value = gen.totalEntries || 0
        progressProcessed.value = gen.processedEntries || 0
        progressStatus.value = gen.status || ''
        startProgressPoll()
      }
    })
  } else {
    stopPolling()
    stopProgressPoll()
    activeGenRecordId.value = null
    progressTotal.value = 0
    progressProcessed.value = 0
    progressStatus.value = ''
  }
})

watch(selectedVersion, async (version) => {
  if (version) {
    await loadCustomTabs()
    activeTab.value = 'panorama'
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
  treeHighlight.value = null
}

function onNavigateToList({ categoryLabel, domainLabel }) {
  selectedNode.value = { id: 'custom', categoryLabel, domainLabel }
  treeHighlight.value = { categoryLabel, domainLabel }
  activeTab.value = 'list'
  listRefreshTrigger.value = Date.now()
}

const globalPreviewVisible = ref(false)
const globalPreviewEntryId = ref(null)
const globalPreviewBatchIds = ref(null)
const globalPreviewRef = ref(null)
const dataListRef = ref(null)
const customTabRefs = reactive({})

function onOpenPreview(entryId) {
  globalPreviewEntryId.value = entryId
  globalPreviewBatchIds.value = null
  globalPreviewVisible.value = true
}

function onPreviewReload(highlightId) {
  if (globalPreviewRef.value) globalPreviewRef.value.reload(highlightId)
}

async function onGlobalPreviewMessage(msg) {
  if (!globalPreviewVisible.value) return
  if (msg?.action === 'edit' || msg?.action === 'addChild') {
    if (dataListRef.value) {
      dataListRef.value.editRowById(msg.entryId, msg.action)
    }
  } else if (msg?.action === 'submit') {
    try {
      await approveEntry(msg.entryId, 'submit', '')
      ElMessage.success('已提交')
      if (globalPreviewRef.value) globalPreviewRef.value.reload()
      refreshCurrentTab()
    } catch (e) { ElMessage.error(e?.response?.data?.message || '操作失败') }
  } else if (msg?.action === 'approve') {
    try {
      await approveEntry(msg.entryId, 'approve', '')
      ElMessage.success('已通过')
      if (globalPreviewRef.value) globalPreviewRef.value.reload()
      refreshCurrentTab()
    } catch (e) { ElMessage.error(e?.response?.data?.message || '操作失败') }
  } else if (msg?.action === 'reject') {
    try {
      const { value } = await ElMessageBox.prompt('请输入驳回原因（非必填）', '驳回', {
        confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '请输入驳回原因，可不填', inputValidator: () => true
      })
      await approveEntry(msg.entryId, 'reject', value || '')
      ElMessage.success('已驳回')
      if (globalPreviewRef.value) globalPreviewRef.value.reload()
      refreshCurrentTab()
    } catch (e) {
      if (e !== 'cancel' && e !== 'close') ElMessage.error(e?.response?.data?.message || '驳回失败')
    }
  } else if (msg?.action === 'delete') {
    try {
      await ElMessageBox.confirm('确认删除该记录？', '提示', { type: 'warning' })
      await deleteEntry(msg.entryId)
      ElMessage.success('删除成功')
      if (globalPreviewRef.value) globalPreviewRef.value.reload()
      refreshCurrentTab()
    } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
  }
}

function refreshCurrentTab() {
  if (activeTab.value === 'list') {
    listRefreshTrigger.value = Date.now()
  } else if (activeTab.value === 'panorama') {
    listRefreshTrigger.value = Date.now()
  }
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
  addListForm.name = ''
  addListForm.entryName = ''
  addListForm.statusList = []
  addListForm.productManager = ''
  addListForm.solution = ''
  addListForm.versionTag = ''
  if (selectedVersion.value) {
    try {
      const [solRes, stRes] = await Promise.all([
        getOptions(selectedVersion.value.id, 'solution'),
        getOptions(selectedVersion.value.id, 'status')
      ])
      addListSolutions.value = (solRes.data || []).map(o => o.value)
      addListStatusList.value = (stRes.data || []).map(o => o.value)
    } catch (e) {
      addListSolutions.value = []
      addListStatusList.value = []
    }
  }
  showAddListDialog.value = true
}

async function handleAddList() {
  if (!addListForm.name || !addListForm.name.trim()) {
    ElMessage.warning('请输入清单名称')
    return
  }
  addListLoading.value = true
  try {
    await createCustomTabWithFilter({
      name: addListForm.name.trim(),
      versionId: selectedVersion.value.id,
      entryName: addListForm.entryName || undefined,
      statusList: addListForm.statusList.length > 0 ? addListForm.statusList : undefined,
      productManager: addListForm.productManager || undefined,
      solution: addListForm.solution || undefined,
      versionTag: addListForm.versionTag || undefined
    })
    ElMessage.success('清单创建成功')
    showAddListDialog.value = false
    await loadCustomTabs()
    if (customTabs.value.length > 0) {
      const last = customTabs.value[customTabs.value.length - 1]
      activeTab.value = 'custom-' + last.id
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '创建失败')
  } finally {
    addListLoading.value = false
  }
}

async function onRemoveTab(targetName) {
  const tab = customTabs.value.find(t => 'custom-' + t.id === targetName)
  if (!tab) return
  try {
    await ElMessageBox.confirm(`确认删除清单"${tab.name}"？清单内的数据关联也将一并删除。`, '确认删除', { type: 'warning' })
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

async function onRenameTab(tab) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新名称', '重命名清单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: tab.name,
      inputValidator: (val) => val && val.trim() ? true : '名称不能为空'
    })
    if (value && value.trim() !== tab.name) {
      await renameCustomTab(tab.id, value.trim())
      ElMessage.success('重命名成功')
      await loadCustomTabs()
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.response?.data?.message || '重命名失败')
    }
  }
}

  function onTabClick(tab) {
   if (tab.paneName === '__add_list') return
   if (tab.paneName === 'stats') {
    statsRefreshTrigger.value = Date.now()
  } else if (tab.paneName === 'list') {
    listRefreshTrigger.value = Date.now()
  } else if (tab.paneName?.startsWith('custom-')) {
    customTabRefresh.value++
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
    showInsertDialog.value = false
    addEntriesToTab(tab.id, insertEntryIds.value).then(() => {
      ElMessage.success('插入成功')
      customTabRefresh.value++
    }).catch(() => {
      ElMessage.error('插入失败')
    }).finally(() => {
      if (dataListRef.value) dataListRef.value.setInserting(false)
    })
  }
}

async function onRemoveFromList(tabId, entryIds) {
  if (!entryIds || entryIds.length === 0) return
  let removed = 0
  for (const entryId of entryIds) {
    try {
      await removeEntryFromTab(tabId, entryId)
      removed++
    } catch {} 
  }
  const tabRef = customTabRefs[tabId]
  if (tabRef) tabRef.setBatchLoading(false)
  else if (dataListRef.value) dataListRef.value.setBatchLoading(false)
  if (removed > 0) {
    ElMessage.success(`已移除 ${removed} 条记录`)
    customTabRefresh.value++
  }
}

function onGenerateDoc(ids, tabId) {
  selectedEntryIds.value = ids
  docCustomTabId.value = tabId || null
  showDocDialog.value = true
}

async function loadGenRecords() {
  if (!selectedVersion.value) return
  recordsLoading.value = true
  try {
    const res = await getDocRecords(selectedVersion.value.id)
    genRecords.value = res.data || []
    recPage.value = 1
    selectedRecIds.value = []
  } finally {
    recordsLoading.value = false
  }
}

const creatorList = computed(() => {
  const names = new Set()
  genRecords.value.forEach(r => { if (r.generatedByName) names.add(r.generatedByName) })
  return [...names]
})

const filteredRecords = computed(() => {
  let list = genRecords.value
  if (filterCreator.value) {
    list = list.filter(r => r.generatedByName === filterCreator.value)
  }
  if (filterTime.value) {
    const days = parseInt(filterTime.value)
    const cutoff = Date.now() - days * 86400000
    list = list.filter(r => r.createdAt && new Date(r.createdAt).getTime() > cutoff)
  }
  return list
})

const pagedRecords = computed(() => {
  const start = (recPage.value - 1) * recPageSize
  return filteredRecords.value.slice(start, start + recPageSize)
})

function onRecSelectionChange(rows) {
  selectedRecIds.value = rows.map(r => r.id)
}

async function batchDeleteRecords() {
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedRecIds.value.length} 条记录？`, '批量删除', { type: 'warning' })
  } catch { return }
  for (const id of selectedRecIds.value) {
    try { await deleteDocRecord(id) } catch {}
  }
  ElMessage.success('已删除')
  loadGenRecords()
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(() => {
    loadGenRecords()
    const hasGenerating = genRecords.value.some(r => r.status === 'generating')
    if (!hasGenerating) stopPolling()
  }, 3000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

async function handleGenerate() {
  if (dataScope.value === 'selected' && selectedEntryIds.value.length === 0) {
    ElMessage.warning('请先选择数据')
    return
  }
  if (filteredRecords.value.length >= 10) {
    ElMessage.warning('生成记录已达上限（10条），请先删除旧记录')
    return
  }
  docLoading.value = true
  try {
    const res = await generateDocument({
      versionId: selectedVersion.value.id,
      docType: docType.value,
      format: docFormat.value,
      dataScope: dataScope.value,
      entryIds: dataScope.value === 'selected' ? selectedEntryIds.value : [],
      customTabId: docCustomTabId.value
    })
    if (res.code === 200) {
      const recordId = res.data?.id
      activeGenRecordId.value = recordId || null
      ElMessage.success('文档正在生成中...')
      loadGenRecords()
      if (recordId) startProgressPoll()
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

function handlePreview(row) {
  const idsStr = row.entryIds
  if (!idsStr || idsStr.trim() === '') {
    ElMessage.warning('该记录无生成范围数据')
    return
  }
  const ids = idsStr.split(',').map(s => s.trim()).filter(Boolean).map(Number)
  if (ids.length === 0) {
    ElMessage.warning('该记录无生成范围数据')
    return
  }
  globalPreviewEntryId.value = null
  globalPreviewBatchIds.value = ids
  globalPreviewVisible.value = true
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
  font-family: var(--si-font);
  color: var(--si-text-primary);
}
.workbench {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.workbench-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--si-bg-card);
  border: 1px solid var(--si-border);
  border-radius: var(--si-radius-md);
  margin-bottom: 12px;
  flex-shrink: 0;
  box-shadow: var(--si-shadow-sm);
}
.version-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.version-badge {
  font-weight: 700;
  font-size: 16px;
  font-family: var(--font-heading);
  color: var(--si-primary);
}
.version-date {
  color: var(--si-text-muted);
  font-size: 13px;
}
.readonly-tip {
  color: var(--si-danger);
  font-size: 12px;
}
.tabs-wrapper {
  position: relative;
  height: 100%;
}
.tabs-wrapper :deep(.el-tabs__header) {
  padding-left: 8px;
}
.tabs-wrapper :deep(.el-tabs__item.is-disabled) {
  cursor: pointer !important;
  color: unset !important;
  padding: 0 !important;
}
.add-list-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #2563EB, #1d4ed8);
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3);
}
.add-list-tab-btn:hover {
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.45);
  transform: translateY(-1px);
}
.workbench-body {
  display: flex;
  flex: 1;
  gap: 12px;
  overflow: hidden;
  min-height: 0;
}
.left-panel {
  width: 260px;
  background: var(--si-bg-card);
  border: 1px solid var(--si-border);
  border-radius: var(--si-radius-lg);
  overflow-y: auto;
  flex-shrink: 0;
  box-shadow: var(--si-shadow-sm);
}
.right-panel {
  flex: 1;
  background: var(--si-bg-card);
  border: 1px solid var(--si-border);
  border-radius: var(--si-radius-lg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: var(--si-shadow-sm);
}
:deep(.el-tabs) { display: flex; flex-direction: column; height: 100%; }
:deep(.el-tabs__header) { flex-shrink: 0; }
:deep(.el-tabs__content) { flex: 1; overflow: hidden; min-height: 0; }
:deep(.el-tab-pane) { height: 100%; overflow: hidden; }

@keyframes spin-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.spinning-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: 2px solid var(--si-primary);
  border-top-color: transparent;
  margin-right: 4px;
  vertical-align: -1px;
  animation: spin-rotate 0.8s linear infinite;
}
.record-count { color: #8f959e; font-size: 12px; }
</style>
