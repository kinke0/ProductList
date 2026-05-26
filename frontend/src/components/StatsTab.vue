<template>
  <div class="stats-tab">
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-label">产品总数</div>
        <div class="stat-value">{{ stats.productCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">模块总数</div>
        <div class="stat-value">{{ stats.moduleCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">功能总数</div>
        <div class="stat-value">{{ stats.featureCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">子功能总数</div>
        <div class="stat-value">{{ stats.subFeatureCount }}</div>
      </div>
    </div>
    <div class="stat-charts">
      <div class="chart-container" style="flex: 1;">
        <h4 class="chart-title">各业务域产品分布</h4>
        <div ref="pieChart" style="height: 280px;"></div>
      </div>
      <div class="chart-container" style="flex: 1;">
        <h4 class="chart-title">各状态产品分布</h4>
        <div ref="barChart" style="height: 280px;"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { queryEntries } from '../api/data'
import * as echarts from 'echarts'

const props = defineProps({ versionId: [Number, String] })

const stats = ref({
  productCount: 0,
  moduleCount: 0,
  featureCount: 0,
  subFeatureCount: 0
})

const pieChart = ref(null)
const barChart = ref(null)
let pieInstance = null
let barInstance = null

async function loadStats() {
  if (!props.versionId) return
  const res = await queryEntries(props.versionId, {})
  const entries = res.data || []

  stats.value.productCount = entries.filter(e => e.level === 3).length
  stats.value.moduleCount = entries.filter(e => e.level === 4).length
  stats.value.featureCount = entries.filter(e => e.level === 5).length
  stats.value.subFeatureCount = entries.filter(e => e.level === 6).length

  const domainMap = {}
  const statusMap = {}
  entries.filter(e => e.level === 3).forEach(e => {
    const domain = e.colBizDomain || '未分类'
    domainMap[domain] = (domainMap[domain] || 0) + 1
    const status = e.colStatus || '未知'
    statusMap[status] = (statusMap[status] || 0) + 1
  })

  renderPie(Object.entries(domainMap).map(([name, value]) => ({ name, value })))
  renderBar(Object.entries(statusMap).map(([name, value]) => ({ name, value })))
}

function renderPie(data) {
  if (!pieChart.value) return
  if (!pieInstance) pieInstance = echarts.init(pieChart.value)
  pieInstance.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['30%', '60%'],
      data: data.length > 0 ? data : [{ name: '暂无数据', value: 1 }],
      label: { show: true, formatter: '{b}: {c}' }
    }]
  })
}

function renderBar(data) {
  if (!barChart.value) return
  if (!barInstance) barInstance = echarts.init(barChart.value)
  barInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.name) },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: data.map(d => d.value),
      itemStyle: { color: '#409eff' }
    }]
  })
}

watch(() => props.versionId, loadStats)
onMounted(loadStats)
</script>

<style scoped>
.stats-tab {
  padding: 16px;
  overflow-y: auto;
  height: 100%;
}
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
.stat-card {
  flex: 1;
  background: linear-gradient(135deg, #1a2a3a, #2a4a6a);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}
.stat-label {
  color: rgba(255,255,255,0.6);
  font-size: 13px;
  margin-bottom: 8px;
}
.stat-value {
  color: #fff;
  font-size: 32px;
  font-weight: 700;
}
.stat-charts {
  display: flex;
  gap: 16px;
}
.chart-container {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;
}
.chart-title {
  margin: 0 0 8px;
  font-size: 14px;
  color: #666;
}
</style>
