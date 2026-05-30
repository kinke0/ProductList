<template>
  <div class="panorama-tab">
    <div v-if="loading" class="panorama-loading">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    <el-empty v-else-if="!bizSection.length && !infraSection.length" description="暂无可交付产品数据" />
    <template v-else>
      <div v-for="section in sections" :key="section.key" class="panorama-section">
        <div class="section-title" :class="section.titleClass">{{ section.title }}<span class="section-count">{{ section.count }} 个产品</span></div>
        <div class="section-body">
          <template v-for="row in section.rows" :key="row.key">
            <div v-if="row.type === 'wide'" class="l1-card l1-wide">
              <div class="l1-header" @click="onL1Click(row.l1.name)">
                <span class="l1-title">{{ stripPrefix(row.l1.name) }}</span>
                <span class="l1-count">{{ countProducts(row.l1) }} 个产品</span>
              </div>
              <div class="l2-groups l2-horizontal">
                <div v-for="l2 in row.l1.children" :key="l2.name" class="l2-group">
                  <div class="l2-tag" @click="onL2Click(row.l1.name, l2.name)">{{ stripPrefix(l2.name) }}</div>
                  <div class="l3-list" :class="{ 'l3-vertical': l2.children.length < 3 }">
                    <div v-for="l3 in l2.children" :key="l3.id" class="l3-card" @click="onL3Click(l3)">{{ l3.displayName }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div v-else class="l1-row">
              <div v-for="l1 in row.items" :key="l1.name" class="l1-card">
                <div class="l1-header" @click="onL1Click(l1.name)">
                  <span class="l1-title">{{ stripPrefix(l1.name) }}</span>
                  <span class="l1-count">{{ countProducts(l1) }} 个产品</span>
                </div>
                <div class="l2-groups">
                  <div v-for="l2 in l1.children" :key="l2.name" class="l2-group">
                    <div class="l2-tag" @click="onL2Click(l1.name, l2.name)">{{ stripPrefix(l2.name) }}</div>
                    <div class="l3-list" :class="{ 'l3-vertical': l2.children.length < 3 }">
                      <div v-for="l3 in l2.children" :key="l3.id" class="l3-card" @click="onL3Click(l3)">{{ l3.displayName }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { queryEntries } from '../api/data'

const props = defineProps({
  versionId: [Number, String],
  selectedNode: Object
})

const emit = defineEmits(['navigate-to-list', 'openPreview'])

const loading = ref(false)
const bizSection = ref([])
const infraSection = ref([])

const sections = computed(() => {
  const result = []
  if (bizSection.value.length > 0) {
    const count = bizSection.value.reduce((s, l1) => s + countProducts(l1), 0)
    result.push({ key: 'biz', title: '业务系统', titleClass: 'biz-title', count, rows: buildRows(bizSection.value) })
  }
  if (infraSection.value.length > 0) {
    const count = infraSection.value.reduce((s, l1) => s + countProducts(l1), 0)
    result.push({ key: 'infra', title: '数智底座', titleClass: 'infra-title', count, rows: buildRows(infraSection.value) })
  }
  return result
})

function stripPrefix(name) {
  if (!name) return ''
  return name.replace(/^[\d.]+\s*/, '')
}

function getPrefixNumber(name) {
  if (!name) return 0
  const match = name.match(/^(\d+)/)
  return match ? parseInt(match[1], 10) : 0
}

function countProducts(l1) {
  let count = 0
  for (const l2 of l1.children) {
    count += l2.children.length
  }
  return count
}

function buildRows(l1List) {
  const rows = []
  const WIDE_THRESHOLD = 6
  const ROW_SIZE = 3
  let i = 0
  while (i < l1List.length) {
    const l1 = l1List[i]
    if (l1.children.length >= WIDE_THRESHOLD) {
      rows.push({ key: 'wide-' + l1.name, type: 'wide', l1 })
      i++
    } else {
      const batch = l1List.slice(i, i + ROW_SIZE)
      if (batch.length === 1 && batch[0].children.length < WIDE_THRESHOLD) {
        rows.push({ key: 'wide-' + batch[0].name, type: 'wide', l1: batch[0] })
      } else {
        rows.push({ key: 'row-' + i, type: 'row', items: batch })
      }
      i += batch.length
    }
  }
  return rows
}

function buildSections(entries) {
  const filtered = entries.filter(e => e.level === 3 && (e.colStatus || '').includes('可交付'))

  const l1Map = {}
  for (const item of filtered) {
    const l1Name = item.colBizCategory || '未分类'
    const l2Name = item.colBizDomain || '未分类'
    const displayName = stripPrefix(item.colProductSystem || '')

    if (!l1Map[l1Name]) l1Map[l1Name] = {}
    if (!l1Map[l1Name][l2Name]) l1Map[l1Name][l2Name] = []
    l1Map[l1Name][l2Name].push({
      id: item.id,
      displayName,
      colProductSystem: item.colProductSystem
    })
  }

  const sections = { biz: [], infra: [] }
  for (const [l1Name, l2Obj] of Object.entries(l1Map)) {
    const l2List = []
    for (const [l2Name, products] of Object.entries(l2Obj)) {
      l2List.push({ name: l2Name, children: products })
    }
    l2List.sort((a, b) => getPrefixNumber(a.name) - getPrefixNumber(b.name))

    const l1Entry = { name: l1Name, children: l2List }
    const num = getPrefixNumber(l1Name)
    if (num >= 5) {
      sections.biz.push(l1Entry)
    } else {
      sections.infra.push(l1Entry)
    }
  }

  sections.biz.sort((a, b) => getPrefixNumber(a.name) - getPrefixNumber(b.name))
  sections.infra.sort((a, b) => getPrefixNumber(a.name) - getPrefixNumber(b.name))

  return { biz: sections.biz, infra: sections.infra }
}

async function loadData() {
  if (!props.versionId) return
  loading.value = true
  try {
    const res = await queryEntries(props.versionId, { level: 3 })
    const entries = res.data || []
    const { biz, infra } = buildSections(entries)
    bizSection.value = biz
    infraSection.value = infra
  } finally {
    loading.value = false
  }
}

function onL1Click(l1Name) {
  emit('navigate-to-list', { categoryLabel: l1Name, domainLabel: '' })
}

function onL2Click(l1Name, l2Name) {
  emit('navigate-to-list', { categoryLabel: l1Name, domainLabel: l2Name })
}

function onL3Click(l3) {
  emit('openPreview', l3.id)
}

watch(() => props.versionId, loadData, { immediate: true })
</script>

<style scoped>
.panorama-tab {
  padding: 12px;
  overflow-y: auto;
  height: 100%;
  background: #F8FAFC;
}

.panorama-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #94A3B8;
  gap: 8px;
  font-size: 14px;
}

.panorama-section {
  margin-bottom: 16px;
}

.section-title {
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  border-radius: 6px 6px 0 0;
  letter-spacing: 2px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-count {
  font-size: 12px;
  font-weight: 400;
  letter-spacing: 0;
  opacity: 0.85;
}

.biz-title {
  background: #2563EB;
}

.infra-title {
  background: #0F172A;
}

.section-body {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.l1-row {
  display: flex;
  gap: 10px;
}

.l1-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  overflow: hidden;
  flex: 1;
  min-width: 0;
}

.l1-wide {
  width: 100%;
}

.l1-wide .l2-horizontal {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 10px;
}

.l1-wide .l2-group {
  flex: 1 1 200px;
  min-width: 160px;
}

.l1-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 2px solid #2563EB;
  cursor: pointer;
  transition: background 0.2s;
}

.l1-header:hover {
  background: #F8FAFC;
}

.l1-title {
  font-size: 13px;
  font-weight: 600;
  color: #0F172A;
}

.l1-count {
  font-size: 11px;
  color: #94A3B8;
}

.l2-groups {
  padding: 8px 10px;
}

.l2-group {
  margin-bottom: 8px;
  padding: 8px 10px;
  background: #F8FAFC;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
}

.l2-group:last-child {
  margin-bottom: 0;
}

.l2-tag {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563EB;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.l2-tag:hover {
  background: rgba(37, 99, 235, 0.16);
}

.l3-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.l3-list.l3-vertical {
  flex-direction: column;
}

.l3-card {
  padding: 4px 10px;
  background: #FFFFFF;
  border-radius: 4px;
  font-size: 12px;
  color: #334155;
  cursor: pointer;
  border: 1px solid #E2E8F0;
  transition: all 0.15s;
}

.l3-card:hover {
  background: #F1F5F9;
  border-color: #2563EB;
  color: #2563EB;
}
</style>
