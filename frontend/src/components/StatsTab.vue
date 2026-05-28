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
    <div class="stat-charts" style="margin-top:16px;">
      <div class="chart-container" style="flex: 1;">
        <h4 class="chart-title">可交付功能审批完成度</h4>
        <div ref="approvalPie" style="height: 280px;"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
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
const approvalPie = ref(null)
let pieInstance = null
let barInstance = null
let approvalPieInstance = null

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

  const deliverableEntries = entries.filter(e => e.colStatus === '可交付')
  const approvedCount = deliverableEntries.filter(e => e.approvalStatus === '审核通过').length
  const notApprovedCount = deliverableEntries.length - approvedCount
  renderApprovalPie(approvedCount, notApprovedCount)
}

function renderPie(data) {
  if (!pieChart.value) return
  if (!pieInstance) pieInstance = echarts.init(pieChart.value)
  if (pieChart.value.clientWidth === 0 || pieChart.value.clientHeight === 0) {
    nextTick(() => renderPie(data))
    return
  }
  pieInstance.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['30%', '60%'],
      data: data.length > 0 ? data : [{ name: '暂无数据', value: 1 }],
      label: { show: true, formatter: '{b}: {c}', color: '#94A3B8' },
      itemStyle: { borderColor: 'rgba(0,0,0,0.1)', borderWidth: 2, color: '#2563EB' }
    }]
  })
}

function renderBar(data) {
  if (!barChart.value) return
  if (!barInstance) barInstance = echarts.init(barChart.value)
  if (barChart.value.clientWidth === 0 || barChart.value.clientHeight === 0) {
    nextTick(() => renderBar(data))
    return
  }
  barInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.name) },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: data.map(d => d.value),
      itemStyle: { color: '#2563EB' }
    }]
  })
}

function renderApprovalPie(approvedCount, notApprovedCount) {
  if (!approvalPie.value) return
  if (!approvalPieInstance) approvalPieInstance = echarts.init(approvalPie.value)
  if (approvalPie.value.clientWidth === 0 || approvalPie.value.clientHeight === 0) {
    nextTick(() => renderApprovalPie(approvedCount, notApprovedCount))
    return
  }
  const total = approvedCount + notApprovedCount
  const data = total === 0
    ? [{ name: '暂无可交付功能', value: 1 }]
    : [
        { name: '审核通过', value: approvedCount },
        { name: '未通过', value: notApprovedCount }
      ]
  approvalPieInstance.setOption({
    tooltip: { trigger: 'item', formatter: total === 0 ? '暂无数据' : '{b}: {c} ({d}%)' },
    color: total === 0 ? ['#d0d0d0'] : ['#67C23A', '#E6A23C'],
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data,
      label: {
        show: total > 0,
        formatter: '{b}\n{c}项 ({d}%)',
        color: '#94A3B8',
        lineHeight: 18
      },
      itemStyle: { borderColor: 'rgba(0,0,0,0.1)', borderWidth: 2 },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } }
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
  margin-bottom: 20px;
}
.stat-card {
  flex: 1;
  background: var(--si-bg-card);
  border: 1px solid var(--si-border);
  border-radius: var(--si-radius-lg);
  padding: 20px;
  text-align: center;
  box-shadow: var(--si-shadow-sm);
}
.stat-label {
  color: var(--si-text-muted);
  font-size: 12px;
  margin-bottom: 8px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 1px;
}
.stat-value {
  color: var(--si-primary);
  font-size: 32px;
  font-weight: 700;
  font-family: var(--si-font);
}
.stat-charts {
  display: flex;
  gap: 16px;
}
.chart-container {
  flex: 1;
  background: var(--si-bg-card);
  border: 1px solid var(--si-border);
  border-radius: var(--si-radius-lg);
  padding: 16px;
  box-shadow: var(--si-shadow-sm);
}
.chart-title {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--si-text-secondary);
  font-weight: 500;
  font-family: var(--si-font);
}
</style>
