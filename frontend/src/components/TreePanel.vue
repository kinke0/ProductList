<template>
  <div class="tree-panel">
    <div class="tree-title">层级导航</div>
    <div class="tree-filter">
      <el-input
        v-model="filterText"
        placeholder="搜索分类/领域"
        size="small"
        clearable
        prefix-icon="Search"
      />
    </div>
    <div
      class="all-node"
      :class="{ active: selectedAll }"
      @click="selectAll"
    >
      全部
    </div>
    <el-tree
      ref="treeRef"
      :data="treeData"
      :props="treeProps"
      node-key="id"
      highlight-current
      @node-click="onNodeClick"
      :show-checkbox="false"
      default-expand-all
      :filter-node-method="filterNode"
    >
      <template #default="{ data }">
        <span>{{ data.label }}</span>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { getCategoryTree } from '../api/data'

const props = defineProps({
  versionId: [Number, String],
  highlightNode: { type: Object, default: null }
})
const emit = defineEmits(['select'])
const treeRef = ref(null)
const selectedAll = ref(true)

const treeData = ref([])
const nodeMap = ref({})
const treeProps = { children: 'children', label: 'label', isLeaf: 'isLeaf' }
const filterText = ref('')

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

watch(() => props.versionId, async (val) => {
  if (val) {
    const res = await getCategoryTree(val)
    treeData.value = res.data
    nodeMap.value = {}
    buildNodeMap(res.data)
    selectAll()
  }
}, { immediate: true })

watch(() => props.highlightNode, (node) => {
  if (!node || !treeRef.value) return
  const key = findNodeKeyByLabel(node.categoryLabel, node.domainLabel)
  if (key) {
    selectedAll.value = false
    treeRef.value.setCurrentKey(key)
  }
}, { flush: 'post' })

function findNodeKeyByLabel(categoryLabel, domainLabel) {
  if (domainLabel) {
    for (const [, node] of Object.entries(nodeMap.value)) {
      if (node.label === domainLabel && node.level === 2) {
        return node.id
      }
    }
  }
  if (categoryLabel) {
    for (const [, node] of Object.entries(nodeMap.value)) {
      if (node.label === categoryLabel && node.level === 1) {
        return node.id
      }
    }
  }
  return null
}

function buildNodeMap(nodes) {
  for (const n of nodes) {
    nodeMap.value[n.id] = n
    if (n.children) buildNodeMap(n.children)
  }
}

function selectAll() {
  selectedAll.value = true
  if (treeRef.value) treeRef.value.setCurrentKey(null)
  emit('select', { id: 'all', level: 0, label: '全部', categoryLabel: '', domainLabel: '' })
}

function filterNode(value, data) {
  if (!value) return true
  return data.label?.includes(value)
}


function findAncestor(node, targetLevel) {
  let current = node
  const seen = new Set()
  while (current && current.level > targetLevel) {
    if (seen.has(current.id)) break
    seen.add(current.id)
    const parent = nodeMap.value[current.parentId]
    if (!parent) break
    current = parent
  }
  return current.level === targetLevel ? current : null
}

function onNodeClick(data) {
  selectedAll.value = false
  const l1 = data.level === 1 ? data : findAncestor(data, 1)
  const l2 = data.level === 2 ? data : (data.level > 2 ? findAncestor(data, 2) : null)
  emit('select', {
    ...data,
    categoryLabel: l1 ? l1.label : '',
    domainLabel: l2 ? l2.label : ''
  })
}
</script>

<style scoped>
.tree-panel {
  padding: 12px;
}
.tree-title {
  font-weight: 600;
  font-size: 12px;
  padding: 8px 12px;
  color: var(--si-text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
  border-bottom: 1px solid var(--si-border);
  margin-bottom: 6px;
  font-family: var(--si-font);
}
.tree-filter {
  padding: 0 4px 6px;
}
.all-node {
  padding: 8px 12px;
  cursor: pointer;
  font-weight: 600;
  color: var(--si-text-secondary);
  border-radius: 8px;
  margin: 2px 0;
  transition: background var(--si-transition), color var(--si-transition);
}
.all-node.active {
  background: var(--si-primary-soft);
  color: var(--si-primary);
}
.all-node:hover {
  background: var(--si-bg-hover);
  color: var(--si-text-primary);
}
:deep(.el-tree-node__content) {
  border-radius: 8px;
  margin: 1px 0;
  transition: background-color var(--si-transition);
  color: var(--si-text-secondary);
}
:deep(.el-tree-node__content:hover) {
  background-color: var(--si-bg-hover);
  color: var(--si-text-primary);
}
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: var(--si-primary-soft);
  color: var(--si-primary);
  font-weight: 500;
}
:deep(.el-tree) {
  background: transparent;
  color: var(--si-text-secondary);
}
</style>
