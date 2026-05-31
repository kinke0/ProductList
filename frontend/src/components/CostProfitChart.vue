<template>
  <div class="cost-profit-chart">
    <div class="chart-header">
      <div class="chart-header-left">
        <el-icon class="toggle-btn" @click="toggleExpand">
          <ArrowDown v-if="expanded" />
          <ArrowRight v-else />
        </el-icon>
        <span class="summary-item">
          <span class="summary-label">总成本</span>
          <span class="summary-value cost-value">{{ formatMoney(totalCost) }}</span>
        </span>
        <span class="summary-item">
          <span class="summary-label">毛利润</span>
          <span class="summary-value" :class="totalProfit >= 0 ? 'profit-positive' : 'profit-negative'">{{ formatMoney(totalProfit) }}</span>
        </span>
      </div>
      <div class="mock-hint">下方成本利润分布为模拟数据，后续打通两单一务后可出精准数据</div>
      <div class="solution-selector">
        <span class="selector-label">方案选择</span>
        <el-select v-model="selectedSolution" placeholder="选择方案" size="small" style="width: 180px;" @change="onSolutionChange">
          <el-option label="全部" value="all" />
          <el-option v-for="s in solutions" :key="s.id" :label="s.value" :value="s.id" />
        </el-select>
      </div>
    </div>
    <div v-show="expanded" ref="chartRef" class="chart-body"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { ArrowDown, ArrowRight } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getOptions } from '../api/option'

const props = defineProps({
  versionId: [Number, String],
  l2Names: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chartInstance = null

const expanded = ref(true)
const solutions = ref([])
const selectedSolution = ref('all')
const totalCost = ref(0)
const totalProfit = ref(0)

function formatMoney(val) {
  if (val === 0) return '¥0'
  const abs = Math.abs(val)
  if (abs >= 100000000) {
    return (val / 100000000).toFixed(2) + '亿'
  }
  if (abs >= 10000) {
    return (val / 10000).toFixed(1) + '万'
  }
  return '¥' + val.toFixed(0)
}

function stripPrefix(name) {
  if (!name) return ''
  return name.replace(/^[\d.]+\s*/, '')
}

function generateMockData() {
  const l2List = (props.l2Names && props.l2Names.length > 0) ? props.l2Names : ['未分类']
  const data = []
  let costSum = 0
  let profitSum = 0
  const count = l2List.length
  const avgCost = 10000000 / Math.max(count, 1)

  for (const l2 of l2List) {
    const cost = Math.round(avgCost * (0.5 + Math.random()))
    const isPositive = Math.random() > 0.3
    const profit = isPositive
      ? Math.round(cost * (0.05 + Math.random() * 0.35))
      : -Math.round(cost * (0.02 + Math.random() * 0.15))
    costSum += cost
    profitSum += profit
    data.push({ l2: stripPrefix(l2), cost, profit })
  }
  totalCost.value = costSum
  totalProfit.value = profitSum
  return data
}

function renderChart(data) {
  if (!chartRef.value || !expanded.value) return
  if (chartRef.value.clientWidth === 0 || chartRef.value.clientHeight === 0) {
    nextTick(() => renderChart(data))
    return
  }
  if (!chartInstance) chartInstance = echarts.init(chartRef.value)

  const l2Labels = data.map(d => d.l2)
  const costData = data.map(d => d.cost)
  const positiveProfitData = data.map(d => d.profit > 0 ? d.profit : 0)
  const negativeProfitData = data.map(d => d.profit < 0 ? Math.abs(d.profit) : 0)

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        let tip = `<b>${params[0].axisValue}</b><br/>`
        let cost = 0
        let profit = 0
        for (const p of params) {
          if (p.value === 0) continue
          const displayVal = p.seriesName === '利润(负)' ? -p.value : p.value
          tip += `${p.marker} ${p.seriesName}: ${formatMoney(displayVal)}<br/>`
          if (p.seriesName === '成本') cost = p.value
          if (p.seriesName === '利润(正)') profit = p.value
          if (p.seriesName === '利润(负)') profit = -p.value
        }
        tip += `<b>利润: ${formatMoney(profit)}</b>`
        return tip
      }
    },
    legend: {
      data: ['成本', '利润', '利润(负)'],
      top: 0,
      right: 0,
      textStyle: { fontSize: 11, color: '#64748B' },
      itemWidth: 14,
      itemHeight: 10
    },
    grid: {
      left: 10,
      right: 10,
      top: 30,
      bottom: 14,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: l2Labels,
      axisLabel: { fontSize: 10, color: '#64748B', rotate: 30, interval: 0 },
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        fontSize: 10,
        color: '#94A3B8',
        formatter(v) {
          if (v >= 10000000) return (v / 10000).toFixed(0) + '万'
          if (v >= 10000) return (v / 10000).toFixed(0) + '万'
          return v
        }
      },
      splitLine: { lineStyle: { color: '#F1F5F9', type: 'dashed' } },
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        name: '成本',
        type: 'bar',
        stack: 'total',
        data: costData,
        itemStyle: { color: '#6366F1' },
        barWidth: 28,
        emphasis: { focus: 'series' }
      },
      {
        name: '利润',
        type: 'bar',
        stack: 'total',
        data: positiveProfitData,
        itemStyle: { color: '#67C23A', borderRadius: [3, 3, 0, 0] },
        emphasis: { focus: 'series' }
      },
      {
        name: '利润(负)',
        type: 'bar',
        stack: 'total',
        data: negativeProfitData,
        itemStyle: { color: '#F56C6C', borderRadius: [3, 3, 0, 0] },
        emphasis: { focus: 'series' }
      }
    ]
  }, true)
}

function toggleExpand() {
  expanded.value = !expanded.value
  if (expanded.value) {
    nextTick(() => {
      const data = generateMockData()
      renderChart(data)
    })
  } else {
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
  }
}

async function loadSolutions() {
  if (!props.versionId) return
  try {
    const res = await getOptions(props.versionId, 'solution')
    solutions.value = res.data || []
  } catch {
    solutions.value = []
  }
}

function onSolutionChange() {
  if (!expanded.value) return
  const data = generateMockData()
  renderChart(data)
}

function refreshChart() {
  const data = generateMockData()
  if (expanded.value) {
    nextTick(() => renderChart(data))
  }
}

function handleResize() {
  if (chartInstance) chartInstance.resize()
}

watch(() => props.versionId, () => {
  loadSolutions()
  refreshChart()
})

watch(() => props.l2Names, () => {
  refreshChart()
}, { deep: true })

onMounted(() => {
  loadSolutions()
  nextTick(() => refreshChart())
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.cost-profit-chart {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  padding: 8px 14px 6px;
  flex-shrink: 0;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toggle-btn {
  cursor: pointer;
  font-size: 14px;
  color: #64748B;
  transition: color 0.2s;
  flex-shrink: 0;
}

.toggle-btn:hover {
  color: #2563EB;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.summary-label {
  font-size: 11px;
  color: #94A3B8;
  font-weight: 500;
}

.summary-value {
  font-size: 15px;
  font-weight: 700;
  font-family: 'DIN Alternate', 'Helvetica Neue', monospace;
}

.cost-value {
  color: #6366F1;
}

.profit-positive {
  color: #67C23A;
}

.profit-negative {
  color: #F56C6C;
}

.chart-body {
  height: 220px;
  width: 100%;
  margin-top: 4px;
}

.solution-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.selector-label {
  font-size: 12px;
  color: #64748B;
  white-space: nowrap;
}

.mock-hint {
  font-size: 11px;
  color: #F56C6C;
  margin-top: 2px;
}
</style>
