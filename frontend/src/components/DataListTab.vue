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
        <el-button size="small" text @click="expandAll">全部展开</el-button>
        <el-button size="small" text @click="collapseAll">全部折叠</el-button>
        <el-button v-if="props.isEditing && !props.customTabId" type="warning" size="small" plain @click="handleReorder">
          按编号重排序
        </el-button>
        <el-dropdown v-if="props.isEditing && !props.customTabId" @command="handleDedupCommand">
          <el-button type="danger" size="small" plain>
            去除重复<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="exact">精确去重（同名+同级+同父）</el-dropdown-item>
              <el-dropdown-item command="deep">深度去重（忽略编号，按名称核心匹配）</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <template v-if="props.customTabId">
          <el-button type="danger" size="small" :disabled="!props.isEditing || selectedIds.length === 0" @click="onRemoveClick">
            移除
          </el-button>
          <el-button type="success" size="small" @click="emit('generateDoc', selectedIds, props.customTabId)">
            生成文档
          </el-button>
        </template>
        <template v-else>
          <el-button v-if="props.selectedNode?.level === 2" type="primary" size="small" :disabled="!props.isEditing" @click="openNewDialog">新建</el-button>
          <el-button type="success" size="small" @click="onInsertClick">
            插入待生成清单
          </el-button>
        </template>
      </div>
    </div>

    <div class="table-body">
     <div class="vtable-header">
       <div class="vcol vcol-num" style="width:50px;">
         <div class="check-col-inner" :style="{ paddingLeft: props.isEditing ? '22px' : '0' }">
           <el-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate" @change="toggleSelectAll" size="small" />
         </div>
       </div>
       <div class="vcol" style="min-width:200px;flex:1;">名称<span class="record-count" style="margin-left:4px;">{{ totalEntryCount }}条记录</span></div>
      <div class="vcol" style="width:80px;">状态</div>
      <div class="vcol" style="width:100px;">产品经理</div>
      <div class="vcol" style="width:180px;">版本划分</div>
      <div class="vcol" style="width:160px;">操作</div>
    </div>
    <RecycleScroller
      ref="scrollerRef"
      class="virtual-table"
      :items="displayData"
      :item-size="36"
      key-field="id"
      v-slot="{ item: row }"
    >
      <div
        :class="['vrow', row._isSeparator ? 'sep-row' : (displayData.indexOf(row) % 2 === 0 ? 'vrow-even' : 'vrow-odd'), 'row-id-' + row.id, 'row-level-' + (row.level || 0), 'row-parent-' + (row.parentId || 0)]"
      >
        <div class="vcol vcol-num" style="width:50px;">
          <template v-if="row._isSeparator">
            <span class="sep-label" @click.stop="toggleDomainCollapse(row.colBizDomain)">
              <span class="sep-toggle">{{ collapsedDomains.has(row.colBizDomain) ? '▶' : '▼' }}</span>
              <span>{{ row.colBizCategory }} - {{ row.colBizDomain }}</span>
              <el-button v-if="props.isEditing" class="sep-add-btn" size="small" @click.stop="addProductFromSeparator(row)">+ 添加产品/系统</el-button>
            </span>
          </template>
          <template v-else>
            <div class="check-col-inner" :style="{ paddingLeft: props.isEditing ? '22px' : '0' }">
              <span v-if="props.isEditing" class="drag-handle" @mousedown="startDrag($event, displayData.indexOf(row))">⠿</span>
              <el-checkbox :model-value="selectedIds.includes(row.id)" @change="toggleSelect(row)" size="small" />
            </div>
          </template>
        </div>
        <div class="vcol vcol-name" style="min-width:200px;flex:1;">
          <span v-if="!row._isSeparator" class="product-cell" :style="{ paddingLeft: ((row.level || 3) - 3) * 20 + 4 + 'px' }">
            <span
              v-if="row.children && row.children.length > 0"
              class="tree-toggle"
              :class="{ expanded: expandedNodeIds.has(row.id) }"
              @click.stop="toggleNodeExpand(row)"
            >
              <svg width="10" height="10" viewBox="0 0 10 10"><path d="M3.5 1.5 L7 5 L3.5 8.5" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </span>
            <span v-else class="tree-toggle-placeholder"></span>
            <el-tag v-if="row.level && row.level >= 3" :type="levelTagType(row.level)" size="small" class="level-tag">{{ levelLabel(row.level) }}</el-tag>
            <span class="product-name">{{ row.colProductSystem || '(无名称)' }}</span>
            <span v-if="row.children && row.children.length > 0" class="record-count">{{ getDescendantCount(row) }}条记录</span>
          </span>
        </div>
        <div class="vcol" style="width:80px;">
          <el-tag v-if="!row._isSeparator && row.colStatus" :type="statusTagType(row.colStatus)" size="small">{{ row.colStatus }}</el-tag>
        </div>
        <div class="vcol vcol-ellipsis" style="width:100px;">
          <span v-if="!row._isSeparator">{{ row.colProductManager }}</span>
        </div>
        <div class="vcol" style="width:180px;">
          <div v-if="!row._isSeparator" class="version-inline">
            <el-checkbox :model-value="hasVer(row, 'A-曜系列')" :disabled="!props.isEditing" @change="toggleVer(row, 'A-曜系列')" size="small">曜</el-checkbox>
            <el-checkbox :model-value="hasVer(row, 'B-远系列')" :disabled="!props.isEditing" @change="toggleVer(row, 'B-远系列')" size="small">远</el-checkbox>
            <el-checkbox :model-value="hasVer(row, 'C-驰系列')" :disabled="!props.isEditing" @change="toggleVer(row, 'C-驰系列')" size="small">驰</el-checkbox>
          </div>
        </div>
        <div class="vcol vcol-ops" style="width:160px;">
          <template v-if="!row._isSeparator && props.isEditing">
            <span class="op-btn op-edit" @click="editRow(row)">编辑</span>
            <span class="op-btn op-add" @click="addChildRow(row)">添加</span>
             <span v-if="props.customTabId" class="op-btn op-del" @click="emit('removeFromList', collectSelfAndDescendants(row))">移除</span>
            <span v-else class="op-btn op-del" @click="deleteRow(row)">删除</span>
          </template>
        </div>
      </div>
    </RecycleScroller>
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
import { queryEntries, createEntry, updateEntry, deleteEntry, updateSort, reorderAll, dedupEntries, dedupDeepEntries } from '../api/data'
import { updateCustomTabSort } from '../api/customTab'
import { ArrowDown } from '@element-plus/icons-vue'
import { RecycleScroller } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import { getOptions } from '../api/option'
import { useAuthStore } from '../store/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  versionId: [Number, String],
  selectedNode: Object,
  isEditing: Boolean,
  customTabId: { type: Number, default: null },
  refreshTrigger: { type: Number, default: 0 }
})

const emit = defineEmits(['insertToList', 'removeFromList', 'generateDoc'])

const authStore = useAuthStore()
const tableData = ref([])
const totalEntryCount = ref(0)
const showEditDialog = ref(false)
const isNew = ref(false)
const editingId = ref(null)
const selectedIds = ref([])
const manuallySelectedIds = ref(new Set())
const parentRow = ref(null)
const appRoles = ref([])
const solutions = ref([])
const statusList = ref([])
const appRoleSelections = ref([])
 const solutionSelections = ref([])
 const versionDivList = ref(['A-曜系列', 'B-远系列', 'C-驰系列'])
 const collapsedDomains = ref(new Set())
 const displayData = ref([])
 const expandedNodeIds = ref(new Set())
  const scrollerRef = ref(null)
  const dragState = reactive({ active: false, sourceIndex: -1, targetIndex: -1, ghostEl: null })
  let dragMoveHandler = null
  let dragUpHandler = null

 function rebuildDisplayData() {
   const result = []
   const domainGroups = new Map()

   for (const root of tableData.value) {
     const domain = root.colBizDomain || '未分类'
     if (!domainGroups.has(domain)) domainGroups.set(domain, [])
     domainGroups.get(domain).push(root)
   }

   for (const [domain, roots] of domainGroups) {
     const category = roots[0]?.colBizCategory || ''
     result.push({
       _isSeparator: true,
       colBizDomain: domain,
       colBizCategory: category,
       id: 'sep-' + domain
     })
     if (!collapsedDomains.value.has(domain)) {
       function addTree(node) {
         result.push(node)
         if (node.children && node.children.length > 0 && expandedNodeIds.value.has(node.id)) {
           for (const child of node.children) {
             addTree(child)
           }
         }
       }
       for (const root of roots) {
         addTree(root)
       }
     }
   }
     displayData.value = result
  }

  function startDrag(e, rowIndex) {
    if (e.button !== 0) return
    const row = displayData.value[rowIndex]
    if (!row || row._isSeparator) return
    e.preventDefault()
    dragState.active = true
    dragState.sourceIndex = rowIndex
    dragState.targetIndex = rowIndex

    const el = e.target.closest('.vrow')
    if (!el) return
    const rect = el.getBoundingClientRect()
    const ghost = el.cloneNode(true)
    ghost.classList.add('drag-ghost')
    ghost.style.position = 'fixed'
    ghost.style.top = rect.top + 'px'
    ghost.style.left = rect.left + 'px'
    ghost.style.width = rect.width + 'px'
    ghost.style.height = rect.height + 'px'
    ghost.style.zIndex = '9999'
    ghost.style.opacity = '0.85'
    ghost.style.pointerEvents = 'none'
    ghost.style.boxShadow = '0 4px 16px rgba(0,0,0,0.15)'
    ghost.style.background = '#fff'
    document.body.appendChild(ghost)
    dragState.ghostEl = ghost
    const offsetY = e.clientY - rect.top
    el.classList.add('drag-source-hidden')

     dragMoveHandler = (ev) => {
       if (!dragState.active) return
       ghost.style.top = (ev.clientY - offsetY) + 'px'
       const scrollerEl = scrollerRef.value?.$el
       if (!scrollerEl) return
       const wrapper = scrollerEl.querySelector('.vue-recycle-scroller__item-wrapper')
       if (!wrapper) return
       const rows = wrapper.querySelectorAll('.vrow:not(.drag-source-hidden)')
       let targetIdx = -1
       for (const r of rows) {
         const rRect = r.getBoundingClientRect()
         const mid = rRect.top + rRect.height / 2
         const cls = r.className
         const m = cls.match(/row-id-(\S+)/)
         if (!m) continue
         const idStr = m[1]
         const idx = displayData.value.findIndex(d => String(d.id) === idStr)
         if (idx === -1) continue
         if (displayData.value[idx]._isSeparator) continue
         if (ev.clientY < mid) { targetIdx = idx; break }
         let nextIdx = idx + 1
         while (nextIdx < displayData.value.length && displayData.value[nextIdx]._isSeparator) {
           nextIdx++
         }
         targetIdx = nextIdx
       }
       if (targetIdx === -1) targetIdx = dragState.sourceIndex
       dragState.targetIndex = targetIdx
       updateDragIndicator(wrapper)
     }

    dragUpHandler = () => {
      if (!dragState.active) return
      dragState.active = false
      if (dragState.ghostEl) { dragState.ghostEl.remove(); dragState.ghostEl = null }
      const wrapper = scrollerRef.value?.$el?.querySelector('.vue-recycle-scroller__item-wrapper')
      if (wrapper) removeDragIndicator()
      el.classList.remove('drag-source-hidden')
      document.removeEventListener('mousemove', dragMoveHandler)
      document.removeEventListener('mouseup', dragUpHandler)
      dragMoveHandler = null
      dragUpHandler = null
      if (dragState.sourceIndex !== dragState.targetIndex && dragState.targetIndex >= 0) {
        applyDragDrop(dragState.sourceIndex, dragState.targetIndex)
      }
      dragState.sourceIndex = -1
      dragState.targetIndex = -1
    }

    document.addEventListener('mousemove', dragMoveHandler)
    document.addEventListener('mouseup', dragUpHandler)
  }

  function updateDragIndicator(wrapper) {
    removeDragIndicator(wrapper)
    const ti = dragState.targetIndex
    if (ti < 0 || ti > displayData.value.length) return
    let actualTi = ti
    while (actualTi < displayData.value.length && displayData.value[actualTi]?._isSeparator) {
      actualTi++
    }
    if (actualTi >= displayData.value.length && ti > dragState.sourceIndex) {
      actualTi = ti - 1
      while (actualTi >= 0 && displayData.value[actualTi]?._isSeparator) {
        actualTi--
      }
    }
    const targetRow = wrapper.querySelector(`.row-id-${CSS.escape(String(displayData.value[Math.min(actualTi, displayData.value.length - 1)]?.id || ''))}`)
    if (!targetRow) return
    const indicator = document.createElement('div')
    indicator.className = 'drag-indicator'
    const tRect = targetRow.getBoundingClientRect()
    indicator.style.position = 'fixed'
    indicator.style.left = tRect.left + 'px'
    indicator.style.width = tRect.width + 'px'
    indicator.style.height = '2px'
    indicator.style.background = 'var(--si-primary, #2563EB)'
    indicator.style.zIndex = '10000'
    indicator.style.pointerEvents = 'none'
    indicator.style.top = tRect.top + 'px'
    document.body.appendChild(indicator)
  }

  function removeDragIndicator(wrapper) {
    document.querySelectorAll('.drag-indicator').forEach(el => el.remove())
  }

  async function applyDragDrop(fromIdx, toIdx) {
    const arr = [...displayData.value]
    const item = arr.splice(fromIdx, 1)[0]
    if (!item) return
    const insertIdx = toIdx > fromIdx ? toIdx - 1 : toIdx
    arr.splice(insertIdx, 0, item)
    const nonSep = arr.filter(d => !d._isSeparator)
    try {
      if (props.customTabId) {
        const payload = nonSep.map((d, i) => ({ entryId: d.id, sortOrder: i }))
        await updateCustomTabSort(props.customTabId, payload)
      } else {
        const payload = nonSep.map((d, i) => ({ id: d.id, sortOrder: i }))
        await updateSort(payload)
      }
      ElMessage.success('排序已保存')
      handleQuery(true)
    } catch (e) {
      ElMessage.error('排序保存失败')
      rebuildDisplayData()
    }
  }

 function toggleDomainCollapse(domain) {
   const set = collapsedDomains.value
   if (set.has(domain)) {
     set.delete(domain)
   } else {
     set.add(domain)
   }
   collapsedDomains.value = new Set(set)
   rebuildDisplayData()
 }

function addProductFromSeparator(row) {
  isNew.value = true
  editingId.value = null
  parentRow.value = null
  initEditForm()
  editForm.colBizCategory = row.colBizCategory || ''
  editForm.colBizDomain = row.colBizDomain || ''
  syncVersionFromForm()
  showEditDialog.value = true
}

 function expandAll() {
   collapsedDomains.value = new Set()
   const allIds = new Set()
   function collect(nodes) {
     for (const n of nodes) {
       if (n.children && n.children.length > 0) {
         allIds.add(n.id)
         collect(n.children)
       }
     }
   }
   collect(tableData.value)
   expandedNodeIds.value = allIds
   rebuildDisplayData()
 }

 function collapseAll() {
   const domains = new Set()
   for (const row of displayData.value) {
     if (row._isSeparator) domains.add(row.colBizDomain)
   }
   collapsedDomains.value = domains
   expandedNodeIds.value = new Set()
   rebuildDisplayData()
 }

  function toggleNodeExpand(node) {
    const set = expandedNodeIds.value
    if (set.has(node.id)) {
      set.delete(node.id)
    } else {
      set.add(node.id)
    }
    expandedNodeIds.value = new Set(set)
    rebuildDisplayData()
   }
 
 async function handleReorder() {
  if (!props.isEditing) {
    ElMessage.warning('编辑中版本才能进行排序')
    return
  }
  try {
    await ElMessageBox.confirm('将按照编号顺序重新排列所有条目，是否继续？', '确认重排序', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
     await reorderAll(props.versionId)
     ElMessage.success('重排序完成')
     handleQuery(true)
  } catch (e) {
    ElMessage.error('重排序失败')
  }
}

async function handleDedupCommand(mode) {
  const isDeep = mode === 'deep'
  const msg = isDeep
    ? '深度去重将忽略数字编号前缀，按名称核心文字匹配。不同编号但名称相同的条目将被合并（保留编号最小的），其子节点会迁移。是否继续？'
    : '将自动删除同级同名重复条目（保留编号最小的），其子节点会迁移到保留的条目下。是否继续？'
  try {
    await ElMessageBox.confirm(msg, '确认去重', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    const res = isDeep ? await dedupDeepEntries(props.versionId) : await dedupEntries(props.versionId)
    const count = res.data || 0
    if (count === 0) {
      ElMessage.info('未发现重复条目')
    } else {
       ElMessage.success(`已删除 ${count} 条重复记录`)
     }
     handleQuery(true)
  } catch (e) {
    ElMessage.error('去重失败')
  }
}

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
   handleQuery(true)
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
  if (level === 3) return 'primary'
  if (level === 4) return 'success'
  if (level === 5) return 'warning'
  return 'info'
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
  if (!props.versionId) return
  try {
    const [ar, sol, st] = await Promise.all([
      getOptions(props.versionId, 'appRole'),
      getOptions(props.versionId, 'solution'),
      getOptions(props.versionId, 'status')
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

  function collectDescendantIds(row) {
    const ids = []
    function walk(children) {
      for (const c of children) {
        ids.push(c.id)
        if (c.children) walk(c.children)
      }
    }
    if (row.children) walk(row.children)
    return ids
  }

  function collectAncestorIds(row) {
    const ids = []
    let node = nodeMap.value.get(row.id)
    while (node && node.parentId) {
      const parent = nodeMap.value.get(node.parentId)
      if (parent) ids.push(parent.id)
      node = parent
    }
    return ids
  }

  function toggleSelect(row) {
    const isSelected = selectedIds.value.includes(row.id)
    const newSet = new Set(selectedIds.value)
    if (isSelected) {
      newSet.delete(row.id)
      for (const id of collectDescendantIds(row)) newSet.delete(id)
      manuallySelectedIds.value.delete(row.id)
    } else {
      newSet.add(row.id)
      for (const id of collectDescendantIds(row)) newSet.add(id)
      for (const id of collectAncestorIds(row)) newSet.add(id)
      manuallySelectedIds.value.add(row.id)
    }
    selectedIds.value = [...newSet]
  }

  const nodeMap = computed(() => {
    const map = new Map()
    function walk(nodes) {
      for (const n of nodes) {
        map.set(n.id, n)
        if (n.children) walk(n.children)
      }
    }
    walk(tableData.value)
    return map
  })

  const nonSepRows = computed(() => displayData.value.filter(d => !d._isSeparator))
  const isAllSelected = computed(() => {
    const rows = nonSepRows.value
    return rows.length > 0 && rows.every(r => manuallySelectedIds.value.has(r.id))
  })
  const isIndeterminate = computed(() => {
    const rows = nonSepRows.value
    const count = rows.filter(r => manuallySelectedIds.value.has(r.id)).length
    return count > 0 && count < rows.length
  })

  function toggleSelectAll(checked) {
    const visibleIds = new Set(nonSepRows.value.map(r => r.id))
    if (checked) {
      const allIds = new Set(selectedIds.value)
      for (const r of nonSepRows.value) {
        allIds.add(r.id)
        manuallySelectedIds.value.add(r.id)
        for (const id of collectDescendantIds(r)) allIds.add(id)
      }
      selectedIds.value = [...allIds]
    } else {
      const toRemove = new Set()
      for (const id of visibleIds) {
        toRemove.add(id)
        const row = nodeMap.value.get(id)
        if (row) for (const did of collectDescendantIds(row)) toRemove.add(did)
      }
      selectedIds.value = selectedIds.value.filter(id => !toRemove.has(id))
      for (const id of visibleIds) manuallySelectedIds.value.delete(id)
    }
  }

function getDescendantCount(node) {
  let count = 0
  function walk(children) {
    for (const c of children) {
      count++
      if (c.children) walk(c.children)
    }
  }
  if (node.children) walk(node.children)
  return count
}

function onInsertClick() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先勾选条目')
    return
  }
  emit('insertToList', [...selectedIds.value])
}

function onRemoveClick() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先勾选条目')
    return
  }
  const ids = collectSelectedWithDescendants()
  emit('removeFromList', ids)
}

function collectFullBranch() {
  const result = new Set()
  const idIndex = {}
  const parentMap = {}
  function walk(nodes) {
    for (const n of nodes) {
      idIndex[n.id] = n
      if (n.children) {
        n.children.forEach(c => { parentMap[c.id] = n })
        walk(n.children)
      }
    }
  }
  walk(tableData.value)
  function addWithDescendants(id) {
    if (result.has(id)) return
    result.add(id)
    const n = idIndex[id]
    if (n && n.children) {
      for (const c of n.children) addWithDescendants(c.id)
    }
  }
  for (const id of manuallySelectedIds.value) {
    addWithDescendants(id)
    let node = idIndex[id]
    while (node && parentMap[node.id]) {
      const parentId = parentMap[node.id].id
      if (!result.has(parentId)) result.add(parentId)
      node = idIndex[parentId]
    }
  }
  return [...result]
}

function collectSelectedWithDescendants() {
  const result = new Set(selectedIds.value)
  const idIndex = {}
  function walk(nodes) {
    for (const n of nodes) {
      idIndex[n.id] = n
      if (n.children) walk(n.children)
    }
  }
  walk(tableData.value)
  for (const id of selectedIds.value) {
    const node = idIndex[id]
    if (node && node.children) {
      function collect(children) {
        for (const c of children) {
          result.add(c.id)
          if (c.children) collect(c.children)
        }
      }
      collect(node.children)
    }
  }
  return [...result]
}

  function collectSelfAndDescendants(row) {
    const result = [row.id]
    function walk(children) {
      for (const c of children) {
        result.push(c.id)
        if (c.children) walk(c.children)
      }
    }
    if (row.children) walk(row.children)
    return result
  }

function onChildSelectionChange(rows) {
  // child selection handled if needed
}

async function handleQuery(preserveExpand = false) {
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
       totalEntryCount.value = entries.length
       tableData.value = buildTree(entries)
      if (!preserveExpand) {
        collapsedDomains.value = new Set()
        const defaultExpanded = new Set()
        function collectDefaultExpanded(nodes) {
          for (const n of nodes) {
            if (n.children && n.children.length > 0 && (n.level || 3) < 3) {
              defaultExpanded.add(n.id)
              collectDefaultExpanded(n.children)
            }
          }
        }
        collectDefaultExpanded(tableData.value)
        expandedNodeIds.value = defaultExpanded
      }
      rebuildDisplayData()
  } catch (e) {
    console.error('查询数据失败:', e)
    tableData.value = []
    displayData.value = []
    totalEntryCount.value = 0
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
  if (!props.customTabId) {
    function sortChildren(nodes) {
      nodes.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
      for (const n of nodes) {
        if (n.children && n.children.length > 0) sortChildren(n.children)
      }
    }
    sortChildren(roots)
  }
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
     handleQuery(true)
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
   handleQuery(true)
}

watch(() => props.versionId, () => {
  handleQuery()
  loadOptions()
}, { immediate: true })

  watch(() => props.refreshTrigger, () => {
    handleQuery(true)
  })

  onUnmounted(() => {
    if (dragMoveHandler) document.removeEventListener('mousemove', dragMoveHandler)
    if (dragUpHandler) document.removeEventListener('mouseup', dragUpHandler)
    if (dragState.ghostEl) dragState.ghostEl.remove()
  })
 </script>

<style scoped>
.data-list-tab {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.query-bar {
  flex-shrink: 0;
  background: var(--si-bg-card);
  border: 1px solid var(--si-border);
  border-radius: var(--si-radius-md);
  padding: 12px 16px;
  margin: 0 8px 10px;
  display: flex;
  align-items: center;
}
.query-bar :deep(.el-form-item) {
  margin-bottom: 0;
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
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.table-body :deep(.el-table) {
  table-layout: fixed;
}
.toolbar-title { font-weight: 600; font-size: 14px; color: var(--si-text-primary); font-family: var(--si-font); }
.virtual-table { flex: 1; min-height: 0; border: 1px solid var(--si-border); border-radius: 6px; overflow: hidden; }
.vtable-header {
  display: flex; align-items: center; height: 40px; flex-shrink: 0;
  background: var(--si-bg-secondary); border: 1px solid var(--si-border); border-bottom: 2px solid var(--si-primary);
  font-weight: 600; font-size: 12px; color: var(--si-text-secondary);
  padding: 0; user-select: none; border-radius: 6px 6px 0 0;
  letter-spacing: 0.3px;
}
.vtable-header .vcol { padding: 0 8px; display: flex; align-items: center; height: 100%; border-right: 1px solid var(--si-border); }
.vrow {
  display: flex; align-items: center; height: 36px;
  border-bottom: 1px solid var(--si-border); padding: 0;
  font-size: 13px; transition: background-color 0.1s;
  box-sizing: border-box;
}
.vrow:hover { background-color: var(--si-bg-hover); }
.vrow-even { background: #fff; }
.vrow-odd { background: var(--si-bg-secondary); }
.vrow.sep-row {
  background: rgba(37, 99, 235, 0.08) !important;
  border-bottom: 2px solid rgba(37, 99, 235, 0.15);
  cursor: pointer; user-select: none; font-weight: 600;
}
.vrow.sep-row:hover { background: rgba(37, 99, 235, 0.12) !important; }
.vrow.sep-row .vcol-num { flex: 1; }
.vrow.sep-row .vcol-num .sep-label { color: var(--si-primary); font-size: 13px; display: flex; align-items: center; gap: 8px; white-space: nowrap; width: 100%; }
.vrow.sep-row .vcol:not(.vcol-num) { display: none; }
.sep-toggle { font-size: 14px; color: var(--si-primary); }
.tree-toggle {
  display: inline-flex; align-items: center; justify-content: center;
  width: 20px; height: 20px; cursor: pointer; flex-shrink: 0;
  border-radius: 4px; color: #8f959e;
  transition: transform 0.15s ease, background-color 0.15s, color 0.15s;
}
.tree-toggle:hover { background: rgba(0, 0, 0, 0.06); color: #1f2329; }
.tree-toggle.expanded { transform: rotate(90deg); }
.tree-toggle-placeholder { width: 20px; flex-shrink: 0; }
.drag-handle {
  cursor: grab; user-select: none; font-size: 14px;
  color: var(--si-text-muted); flex-shrink: 0; margin-right: 4px;
}
.drag-handle:active { cursor: grabbing; }
.drag-source-hidden { opacity: 0.3; }
.drag-ghost { border-radius: 4px; }
.drag-handle-placeholder { width: 14px; flex-shrink: 0; margin-right: 4px; }
.check-col-inner { display: flex; align-items: center; width: 100%; }
.check-col-inner .drag-handle { position: absolute; left: 6px; }
.vcol-num { position: relative; }
:deep(.el-checkbox__inner) { border-radius: 4px !important; }
:deep(.el-checkbox__input .el-checkbox__inner) { border-radius: 4px !important; }
.sep-add-btn {
  margin-left: auto; height: 24px; font-size: 12px; padding: 0 10px;
  background: rgba(37, 99, 235, 0.1); border: 1px solid rgba(37, 99, 235, 0.25);
  color: var(--si-primary); border-radius: 6px; cursor: pointer;
}
.sep-add-btn:hover { background: rgba(37, 99, 235, 0.18); }
.vcol { padding: 0 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; height: 100%; display: flex; align-items: center; }
.vcol-name { overflow: visible !important; }
.vcol-ellipsis span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.vcol-ops { overflow: visible !important; }
.op-btn { cursor: pointer; font-size: 12px; margin-right: 8px; user-select: none; }
.op-edit { color: var(--si-primary); }
.op-add { color: var(--color-success); }
.op-del { color: var(--si-danger); }
.op-btn:hover { text-decoration: underline; }
.product-cell { display: inline-flex; align-items: center; gap: 4px; width: 100%; }
.product-name { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.version-inline { display: flex; gap: 2px; white-space: nowrap; }
.record-count { color: #8f959e; font-size: 12px; margin-left: auto; white-space: nowrap; flex-shrink: 0; padding-right: 4px; }
.level-tag { margin: 0 6px; vertical-align: middle; }
</style>
