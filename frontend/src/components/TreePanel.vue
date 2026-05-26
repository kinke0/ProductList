<template>
  <div class="tree-panel">
    <div class="tree-title">层级导航</div>
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
    >
      <template #default="{ data }">
        <span>{{ data.label }}</span>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { getTree } from '../api/data'

const props = defineProps({ versionId: [Number, String] })
const emit = defineEmits(['select'])
const treeRef = ref(null)
const selectedAll = ref(true)

const treeData = ref([])
const nodeMap = ref({})
const treeProps = { children: 'children', label: 'label', isLeaf: 'isLeaf' }

watch(() => props.versionId, async (val) => {
  if (val) {
    const res = await getTree(val)
    treeData.value = res.data
    nodeMap.value = {}
    buildNodeMap(res.data)
    selectAll()
  }
}, { immediate: true })

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
  padding: 8px;
}
.tree-title {
  font-weight: 600;
  font-size: 14px;
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 4px;
}
.all-node {
  padding: 6px 8px;
  cursor: pointer;
  font-weight: 700;
  color: #409eff;
  border-radius: 4px;
  margin-bottom: 4px;
}
.all-node.active {
  background: #ecf5ff;
}
.all-node:hover {
  background: #f0f7ff;
}
</style>
