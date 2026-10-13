<template>
  <div class="report-page">
    <div class="page-head">
      <h2 class="page-title">学习报告</h2>
      <el-radio-group v-model="days" @change="load">
        <el-radio-button :value="7">近 7 天</el-radio-button>
        <el-radio-button :value="30">近 30 天</el-radio-button>
        <el-radio-button :value="90">近 90 天</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="loading" class="list-empty"><el-skeleton :rows="8" animated /></div>

    <template v-else-if="report">
      <!-- 概览卡片 -->
      <div class="overview">
        <div class="ov-item">
          <div class="ov-label">练习场次</div>
          <div class="ov-val">{{ report.practiceCount }}</div>
        </div>
        <div class="ov-item">
          <div class="ov-label">累计答题</div>
          <div class="ov-val">{{ report.totalAnswered }}</div>
        </div>
        <div class="ov-item">
          <div class="ov-label">整体正确率</div>
          <div class="ov-val accent">{{ report.overallRate }}%</div>
        </div>
        <div class="ov-item suggestion">
          <div class="ov-label">训练建议</div>
          <div class="ov-text">{{ report.suggestion }}</div>
        </div>
      </div>

      <div class="chart-grid">
        <!-- 领域正确率 -->
        <el-card class="chart-card" shadow="never">
          <template #header><span class="card-title">各知识领域正确率</span></template>
          <div ref="domainRef" class="chart"></div>
        </el-card>
        <!-- 知识点掌握度 -->
        <el-card class="chart-card" shadow="never">
          <template #header><span class="card-title">知识点掌握度 TOP10</span></template>
          <div ref="pointRef" class="chart"></div>
        </el-card>
        <!-- 薄弱知识点 -->
        <el-card class="chart-card" shadow="never">
          <template #header><span class="card-title">薄弱知识点 Top{{ report.weakPoints.length || 5 }}</span></template>
          <div ref="weakRef" class="chart"></div>
        </el-card>
        <!-- 30 天趋势 -->
        <el-card class="chart-card wide" shadow="never">
          <template #header><span class="card-title">练习量与正确率趋势（近 {{ report.days }} 天）</span></template>
          <div ref="trendRef" class="chart"></div>
        </el-card>
      </div>
    </template>

    <el-empty v-else description="暂无练习数据，完成几组练习后这里会生成你的学习画像" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getReportApi, type StudyReportVO } from '@/api/stats'

const days = ref(30)
const loading = ref(false)
const report = ref<StudyReportVO | null>(null)

const domainRef = ref<HTMLElement>()
const pointRef = ref<HTMLElement>()
const weakRef = ref<HTMLElement>()
const trendRef = ref<HTMLElement>()

const instances: echarts.ECharts[] = []

async function load() {
  loading.value = true
  try {
    report.value = await getReportApi(days.value)
  } finally {
    loading.value = false
  }
  // 等 loading 置 false、内容分支挂载完成后再初始化 ECharts
  await nextTick()
  renderCharts()
}

function renderCharts() {
  const r = report.value
  if (!r) return
  dispose()
  renderDomain(r)
  renderPoint(r)
  renderWeak(r)
  renderTrend(r)
}

function renderDomain(r: StudyReportVO) {
  const el = domainRef.value
  if (!el || !r.domainStats.length) return
  const chart = echarts.init(el)
  instances.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis', renderMode: 'richText', confine: true },
    grid: { left: 50, right: 20, top: 30, bottom: 40, containLabel: true },
    xAxis: {
      type: 'category',
      data: r.domainStats.map((d) => d.name),
      axisLabel: { color: '#555', fontSize: 11 },
    },
    yAxis: { type: 'value', name: '正确率(%)', min: 0, max: 100, axisLabel: { color: '#555', fontSize: 11 } },
    series: [
      {
        type: 'bar',
        data: r.domainStats.map((d) => d.rate),
        itemStyle: { color: '#9BBBF4', borderRadius: [6, 6, 0, 0] },
        label: { show: true, position: 'top', fontSize: 10, color: '#555', formatter: '{c}%' },
      },
    ],
  })
}

function renderPoint(r: StudyReportVO) {
  const el = pointRef.value
  if (!el || !r.pointStats.length) return
  const chart = echarts.init(el)
  instances.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis', renderMode: 'richText', confine: true },
    grid: { left: 20, right: 50, top: 30, bottom: 40, containLabel: true },
    xAxis: { type: 'value', name: '掌握度', min: 0, max: 100, axisLabel: { color: '#555', fontSize: 11 } },
    yAxis: {
      type: 'category',
      data: r.pointStats.map((p) => p.name),
      axisLabel: { color: '#555', fontSize: 11 },
    },
    series: [
      {
        type: 'bar',
        data: r.pointStats.map((p) => p.mastery),
        itemStyle: { color: '#A2DDAA', borderRadius: [0, 6, 6, 0] },
        label: { show: true, position: 'right', fontSize: 10, color: '#555', formatter: '{c}' },
      },
    ],
  })
}

function renderWeak(r: StudyReportVO) {
  const el = weakRef.value
  if (!el || !r.weakPoints.length) return
  const chart = echarts.init(el)
  instances.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis', renderMode: 'richText', confine: true },
    grid: { left: 20, right: 50, top: 30, bottom: 40, containLabel: true },
    xAxis: { type: 'value', name: '错误率(%)', min: 0, max: 100, axisLabel: { color: '#555', fontSize: 11 } },
    yAxis: {
      type: 'category',
      data: r.weakPoints.map((w) => (w.domain ? `${w.domain}·${w.name}` : w.name)),
      axisLabel: { color: '#555', fontSize: 11 },
    },
    series: [
      {
        type: 'bar',
        data: r.weakPoints.map((w) => w.errorRate),
        itemStyle: { color: '#EA6668', borderRadius: [0, 6, 6, 0] },
        label: { show: true, position: 'right', fontSize: 10, color: '#555', formatter: '{c}%' },
      },
    ],
  })
}

function renderTrend(r: StudyReportVO) {
  const el = trendRef.value
  if (!el) return
  const chart = echarts.init(el)
  instances.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis', renderMode: 'richText', confine: true },
    legend: { top: 0, itemWidth: 14, itemHeight: 8, textStyle: { color: '#6B7280', fontSize: 11 } },
    grid: { left: 50, right: 50, top: 36, bottom: 40, containLabel: true },
    xAxis: { type: 'category', data: r.trend.map((t) => t.date), axisLabel: { color: '#555', fontSize: 10, hideOverlap: true } },
    yAxis: [
      { type: 'value', name: '场次', minInterval: 1, axisLabel: { color: '#555', fontSize: 11 } },
      { type: 'value', name: '正确率(%)', min: 0, max: 100, axisLabel: { color: '#555', fontSize: 11 } },
    ],
    series: [
      {
        name: '练习场次',
        type: 'bar',
        data: r.trend.map((t) => t.practiceCount),
        itemStyle: { color: '#9EACEA', borderRadius: [4, 4, 0, 0] },
      },
      {
        name: '正确率',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: r.trend.map((t) => t.correctRate),
        itemStyle: { color: '#52C41A' },
        lineStyle: { color: '#52C41A', width: 2 },
      },
    ],
  })
}

function dispose() {
  for (const c of instances) c.dispose()
  instances.length = 0
}

function onResize() {
  for (const c of instances) c.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  dispose()
})
</script>

<style scoped>
.report-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 20px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
}

.list-empty {
  padding: 20px 0;
}

.overview {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.ov-item {
  flex: 1 1 160px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px 18px;
}

.ov-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}

.ov-val {
  font-size: 26px;
  font-weight: 700;
  color: #1f2937;
}

.ov-val.accent {
  color: #2563eb;
}

.ov-item.suggestion {
  flex: 2 1 340px;
}

.ov-text {
  font-size: 13px;
  color: #374151;
  line-height: 1.7;
}

.chart-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.chart-card {
  flex: 1 1 46%;
  min-width: 360px;
}

.chart-card.wide {
  flex: 1 1 100%;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.chart {
  height: 300px;
  width: 100%;
}
</style>
