<template>
  <div class="mx-auto max-w-7xl px-4 py-6">
    <div class="space-y-6">
      <!-- Hero Header -->
      <section class="rounded-2xl border border-slate-200/60 bg-white p-6 shadow-sm">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div class="flex items-center gap-4">
            <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-100">
              <svg class="h-5 w-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h2 class="text-xl font-bold text-slate-900" style="font-family:'Fira Sans',sans-serif">观测与统计中心</h2>
                <span class="relative flex h-2.5 w-2.5">
                  <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75"></span>
                  <span class="relative inline-flex h-2.5 w-2.5 rounded-full bg-emerald-500"></span>
                </span>
              </div>
              <p class="mt-1 text-sm text-slate-500">实时指标 · 车辆状态 · 策略表现 · 性能监控</p>
            </div>
          </div>

          <div class="flex items-center gap-3">
            <div class="rounded-lg bg-slate-100 px-3 py-1.5 text-xs font-medium text-slate-700" style="font-family:'Fira Code',monospace">
              {{ statisticsStore.history.length }} 采样点
            </div>
            <div class="rounded-xl border border-slate-200 bg-slate-100 p-1">
              <div class="flex gap-1">
                <button
                  v-for="mode in chartModes"
                  :key="mode.key"
                  type="button"
                  class="cursor-pointer rounded-lg px-4 py-2 text-xs font-semibold transition-all duration-200"
                  :class="chartMode === mode.key
                    ? 'bg-white text-blue-600 shadow-sm'
                    : 'text-slate-500 hover:bg-white/80 hover:text-slate-700'"
                  @click="chartMode = mode.key"
                >
                  {{ mode.label }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <Loading v-if="statisticsStore.loading && statisticsStore.totalOrders === 0" label="正在加载统计数据..." centered />

      <div
        v-else-if="statisticsStore.error"
        class="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"
      >
        {{ statisticsStore.error }}
      </div>

      <div
        v-else-if="statisticsStore.totalOrders === 0 && statisticsStore.activeVehicles === 0"
        class="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center shadow-sm"
      >
        <div class="text-lg font-semibold text-slate-800">暂无统计数据</div>
        <div class="mt-2 text-sm text-slate-500">等待订单、车辆或实时统计推送后再查看此页面。</div>
      </div>

      <template v-else>
        <!-- KPI Cards Row -->
        <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-6">
          <StatCard
            title="总订单"
            :value="statisticsStore.totalOrders"
            icon="orders"
            color="blue"
            :trend="orderTrend"
            :sparkline="statisticsStore.history.map(h => h.totalOrders)"
          />
          <StatCard
            title="完成率"
            :value="`${statisticsStore.completionRate.toFixed(1)}%`"
            icon="check"
            color="green"
            :trend="completionTrend"
            :sparkline="statisticsStore.history.map(h => h.completedOrders)"
          />
          <StatCard
            title="平均配送"
            :value="`${statisticsStore.avgDeliveryTime.toFixed(1)}s`"
            icon="clock"
            color="purple"
            :sparkline="statisticsStore.history.map(h => h.utilization)"
          />
          <StatCard
            title="车辆利用率"
            :value="`${statisticsStore.avgUtilization.toFixed(1)}%`"
            icon="truck"
            color="orange"
            :trend="utilizationTrend"
            :sparkline="statisticsStore.history.map(h => h.utilization)"
          />
          <StatCard
            title="等待时间"
            :value="`${statisticsStore.avgWaitingTime.toFixed(1)}s`"
            icon="gauge"
            color="red"
          />
          <StatCard
            title="重调度"
            :value="statisticsStore.reDispatchCount"
            icon="alert"
            color="red"
          />
        </div>

        <!-- Main Charts Row: Order Trend + Vehicle Heatmap -->
        <div class="grid grid-cols-1 gap-6 xl:grid-cols-[1.6fr_1fr]">
          <section class="rounded-2xl border border-slate-200/60 bg-white p-5 shadow-sm">
            <div class="mb-3 flex items-center justify-between">
              <div>
                <h3 class="text-sm font-bold text-slate-900">{{ mainChartTitle }}</h3>
                <p class="mt-0.5 text-xs text-slate-500">{{ mainChartDesc }}</p>
              </div>
              <span class="rounded-md bg-blue-50 px-2 py-1 text-[10px] font-semibold uppercase tracking-wider text-blue-600">Streaming</span>
            </div>
            <EChartPanel :option="orderChartOption" />
          </section>

          <section class="rounded-2xl border border-slate-200/60 bg-white p-5 shadow-sm">
            <div class="mb-3 flex items-center justify-between">
              <div>
                <h3 class="text-sm font-bold text-slate-900">{{ sideChartTitle }}</h3>
                <p class="mt-0.5 text-xs text-slate-500">{{ sideChartDesc }}</p>
              </div>
              <span class="rounded-md bg-teal-50 px-2 py-1 text-[10px] font-semibold uppercase tracking-wider text-teal-600">Heatmap</span>
            </div>
            <EChartPanel :option="vehicleChartOption" />
          </section>
        </div>

        <!-- Strategy Radar + Performance Gauges -->
        <div class="grid grid-cols-1 gap-6 xl:grid-cols-[1fr_1.6fr]">
          <section class="rounded-2xl border border-slate-200/60 bg-white p-5 shadow-sm">
            <div class="mb-3 flex items-center justify-between">
              <div>
                <h3 class="text-sm font-bold text-slate-900">策略对比雷达图</h3>
                <p class="mt-0.5 text-xs text-slate-500">各调度策略使用占比</p>
              </div>
              <span class="rounded-md bg-cyan-50 px-2 py-1 text-[10px] font-semibold uppercase tracking-wider text-cyan-600">Radar</span>
            </div>
            <EChartPanel :option="strategyRadarOption" />
          </section>

          <section class="rounded-2xl border border-slate-200/60 bg-white p-5 shadow-sm">
            <div class="mb-3 flex items-center justify-between">
              <div>
                <h3 class="text-sm font-bold text-slate-900">性能监控仪表盘</h3>
                <p class="mt-0.5 text-xs text-slate-500">调度耗时 · 推送吞吐 · 慢查询</p>
              </div>
              <span class="rounded-md bg-amber-50 px-2 py-1 text-[10px] font-semibold uppercase tracking-wider text-amber-600">Gauges</span>
            </div>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <EChartPanel :option="dispatchGaugeOption" />
              <EChartPanel :option="websocketGaugeOption" />
              <EChartPanel :option="slowQueryGaugeOption" />
            </div>
          </section>
        </div>

        <!-- Donut Charts: Order + Vehicle Status -->
        <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <section class="rounded-2xl border border-slate-200/60 bg-white p-5 shadow-sm">
            <div class="mb-3 flex items-center justify-between">
              <h3 class="text-sm font-bold text-slate-900">订单状态分布</h3>
              <span class="rounded-md bg-amber-50 px-2 py-1 text-[10px] font-semibold uppercase tracking-wider text-amber-600">Donut</span>
            </div>
            <EChartPanel :option="orderDonutOption" />
          </section>

          <section class="rounded-2xl border border-slate-200/60 bg-white p-5 shadow-sm">
            <div class="mb-3 flex items-center justify-between">
              <h3 class="text-sm font-bold text-slate-900">车辆状态分布</h3>
              <span class="rounded-md bg-teal-50 px-2 py-1 text-[10px] font-semibold uppercase tracking-wider text-teal-600">Donut</span>
            </div>
            <EChartPanel :option="vehicleDonutOption" />
          </section>
        </div>

        <!-- Strategy Performance Bars + System Metrics -->
        <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <section class="rounded-2xl border border-slate-200/60 bg-white p-5 shadow-sm">
            <div class="mb-4">
              <h3 class="text-sm font-bold text-slate-900">调度策略性能</h3>
            </div>
            <div class="space-y-4">
              <div v-for="strategy in strategies" :key="strategy.key" class="space-y-1.5">
                <div class="flex items-center justify-between text-xs">
                  <span class="font-medium text-slate-600">{{ strategy.label }}</span>
                  <span class="font-bold text-slate-900" style="font-family:'Fira Code',monospace">{{ strategy.count }} 次 · {{ strategy.percentage.toFixed(1) }}%</span>
                </div>
                <div class="h-2 w-full overflow-hidden rounded-full bg-slate-100">
                  <div
                    class="h-full rounded-full transition-all duration-500"
                    :class="strategy.color"
                    :style="{ width: `${strategy.percentage}%` }"
                  ></div>
                </div>
              </div>
            </div>
          </section>

          <section class="rounded-2xl border border-slate-200/60 bg-white p-5 shadow-sm">
            <div class="mb-4">
              <h3 class="text-sm font-bold text-slate-900">系统指标</h3>
            </div>
            <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div class="rounded-xl border border-slate-100 bg-slate-50 p-4">
                <div class="text-[10px] uppercase tracking-widest text-slate-500">平均等待</div>
                <div class="mt-1.5 text-xl font-bold text-slate-900" style="font-family:'Fira Code',monospace">{{ statisticsStore.avgWaitingTime.toFixed(1) }}s</div>
              </div>
              <div class="rounded-xl border border-slate-100 bg-slate-50 p-4">
                <div class="text-[10px] uppercase tracking-widest text-slate-500">平均分配</div>
                <div class="mt-1.5 text-xl font-bold text-slate-900" style="font-family:'Fira Code',monospace">{{ statisticsStore.avgDispatchTime.toFixed(1) }}ms</div>
              </div>
              <div class="rounded-xl border border-slate-100 bg-slate-50 p-4">
                <div class="text-[10px] uppercase tracking-widest text-slate-500">重调度</div>
                <div class="mt-1.5 text-xl font-bold text-slate-900" style="font-family:'Fira Code',monospace">{{ statisticsStore.reDispatchCount }}</div>
              </div>
            </div>
            <!-- Slow Query Alert -->
            <div v-if="statisticsStore.performanceMetrics.slowQueryCount > 0" class="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-4">
              <div class="flex items-center gap-2">
                <svg class="h-4 w-4 text-rose-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <span class="text-xs font-semibold text-rose-700">慢查询告警</span>
              </div>
              <div class="mt-2 text-2xl font-bold text-rose-700" style="font-family:'Fira Code',monospace">{{ statisticsStore.performanceMetrics.slowQueryCount }}</div>
              <div v-if="statisticsStore.performanceMetrics.slowQueries?.length" class="mt-2 space-y-1">
                <div v-for="sq in statisticsStore.performanceMetrics.slowQueries.slice(0, 3)" :key="sq.name" class="flex items-center justify-between text-xs">
                  <span class="text-rose-600 truncate">{{ sq.label || sq.name }}</span>
                  <span class="font-mono font-bold text-rose-700">{{ sq.value }}ms</span>
                </div>
              </div>
            </div>
          </section>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import EChartPanel from '@/components/EChartPanel.vue'
import Loading from '@/components/Loading.vue'
import StatCard from '@/components/StatCard.vue'
import type { EChartsOption } from '@/lib/echarts'
import { useStatisticsStore } from '@/stores/statistics'

const statisticsStore = useStatisticsStore()
const chartMode = ref<'overview' | 'trend'>('overview')

const chartModes = [
  { key: 'overview' as const, label: '概览' },
  { key: 'trend' as const, label: '趋势' },
]

const P = {
  blue: '#2563eb',
  cyan: '#0891b2',
  green: '#16a34a',
  amber: '#d97706',
  rose: '#e11d48',
  slate: '#475569',
}

type HistoryPoint = (typeof statisticsStore.history)[number]

const hasTrendData = computed(() => statisticsStore.history.length >= 2)
const currentHistoryPoint = computed<HistoryPoint | null>(() => statisticsStore.history.at(-1) ?? null)

// Trend calculations for sparkline cards
const orderTrend = computed(() => {
  const h = statisticsStore.history
  if (h.length < 2) return undefined
  const prev = h[h.length - 2].totalOrders
  const curr = h[h.length - 1].totalOrders
  if (prev === 0) return undefined
  return Math.round(((curr - prev) / prev) * 100)
})
const completionTrend = computed(() => {
  const h = statisticsStore.history
  if (h.length < 2) return undefined
  const prev = h[h.length - 2].completedOrders
  const curr = h[h.length - 1].completedOrders
  if (prev === 0) return undefined
  return Math.round(((curr - prev) / prev) * 100)
})
const utilizationTrend = computed(() => {
  const h = statisticsStore.history
  if (h.length < 2) return undefined
  const prev = h[h.length - 2].utilization
  const curr = h[h.length - 1].utilization
  return Math.round(curr - prev)
})

const mainChartTitle = computed(() =>
  chartMode.value === 'overview' ? '订单当前概览' : '订单完成趋势'
)
const mainChartDesc = computed(() =>
  chartMode.value === 'overview'
    ? '当前订单规模与完成情况对比'
    : '订单总量、完成量与利用率时间序列'
)
const sideChartTitle = computed(() =>
  chartMode.value === 'overview' ? '车辆状态概览' : '车辆状态热力图'
)
const sideChartDesc = computed(() =>
  chartMode.value === 'overview'
    ? '各类车辆状态即时分布'
    : '按时间观察车队状态变化密度'
)

const strategies = computed(() => {
  const fallback = [
    { key: 'NEAREST_FIRST', label: '最近优先', count: 0, percentage: 0, color: 'bg-blue-500' },
    { key: 'LOAD_BALANCE', label: '负载均衡', count: 0, percentage: 0, color: 'bg-violet-500' },
    { key: 'COMPOSITE_SCORE', label: '综合评分', count: 0, percentage: 0, color: 'bg-emerald-500' },
    { key: 'FASTEST_ARRIVAL', label: '最快到达', count: 0, percentage: 0, color: 'bg-amber-500' },
  ]
  if (statisticsStore.strategyStats.length === 0) return fallback
  return fallback.map((item) => {
    const matched = statisticsStore.strategyStats.find((stat) => stat.strategy === item.key)
    return { ...item, count: matched?.count ?? 0, percentage: matched?.percentage ?? 0 }
  })
})

// ── Tooltip style ──
const tooltipStyle = {
  backgroundColor: 'rgba(15, 23, 42, 0.92)',
  borderWidth: 0,
  textStyle: { color: '#f8fafc', fontFamily: 'Fira Code, monospace', fontSize: 12 },
}

// ── Overview: Order bar chart ──
const overviewOrderChartOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item', ...tooltipStyle },
  grid: { left: 56, right: 24, top: 24, bottom: 62 },
  xAxis: {
    type: 'value',
    max: Math.max(currentHistoryPoint.value?.totalOrders ?? 0, 1),
    axisLabel: { color: '#64748b' },
    splitLine: { lineStyle: { color: '#f1f5f9' } },
  },
  yAxis: {
    type: 'category',
    data: ['已完成', '总订单'],
    axisLabel: { color: '#475569', fontWeight: 600 },
    axisTick: { show: false },
  },
  series: [{
    type: 'bar',
    barWidth: 18,
    data: [
      { value: currentHistoryPoint.value?.completedOrders ?? 0, itemStyle: { color: P.green, borderRadius: 8 } },
      { value: currentHistoryPoint.value?.totalOrders ?? 0, itemStyle: { color: P.blue, borderRadius: 8 } },
    ],
    label: { show: true, position: 'right', color: '#0f172a', fontWeight: 700, fontFamily: 'Fira Code' },
  }],
  graphic: [{
    type: 'group',
    left: 'center',
    bottom: 8,
    children: [
      { type: 'rect', shape: { x: 0, y: 0, width: 176, height: 30, r: 15 }, style: { fill: 'rgba(217,119,6,0.1)' } },
      { type: 'text', left: 16, top: 8, style: { text: `利用率 ${(currentHistoryPoint.value?.utilization ?? 0).toFixed(1)}%`, fill: '#b45309', fontSize: 12, fontWeight: 700, fontFamily: 'Fira Code' } },
    ],
  }],
}))

// ── Trend: Streaming area chart ──
const trendOrderChartOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis', ...tooltipStyle },
  legend: { top: 0, icon: 'roundRect', itemWidth: 12, textStyle: { color: '#475569' }, data: ['总订单', '已完成', '利用率'] },
  grid: { left: 54, right: 44, top: 48, bottom: 36 },
  xAxis: {
    type: 'category',
    data: statisticsStore.history.map((i) => i.time),
    boundaryGap: false,
    axisLine: { lineStyle: { color: '#e2e8f0' } },
    axisLabel: { color: '#64748b', fontSize: 10 },
  },
  yAxis: [
    { type: 'value', name: '订单', minInterval: 1, splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#64748b' } },
    { type: 'value', name: '%', min: 0, max: 100, splitLine: { show: false }, axisLabel: { color: '#64748b', formatter: '{value}%' } },
  ],
  series: [
    {
      name: '总订单', type: 'line', smooth: true, showSymbol: false,
      lineStyle: { width: 2.5, color: P.blue }, itemStyle: { color: P.blue },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(37,99,235,0.2)' }, { offset: 1, color: 'rgba(37,99,235,0.01)' }] } },
      data: statisticsStore.history.map((i) => i.totalOrders),
    },
    {
      name: '已完成', type: 'line', smooth: true, showSymbol: false,
      lineStyle: { width: 2.5, color: P.green }, itemStyle: { color: P.green },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(22,163,74,0.15)' }, { offset: 1, color: 'rgba(22,163,74,0.01)' }] } },
      data: statisticsStore.history.map((i) => i.completedOrders),
    },
    {
      name: '利用率', type: 'line', yAxisIndex: 1, smooth: true, showSymbol: false,
      lineStyle: { width: 1.5, type: 'dashed', color: P.amber }, itemStyle: { color: P.amber },
      data: statisticsStore.history.map((i) => Number(i.utilization.toFixed(1))),
    },
  ],
}))

// ── Placeholder ──
const placeholderChartOption = computed<EChartsOption>(() => ({
  animation: false,
  xAxis: { type: 'value', show: false, min: 0, max: 1 },
  yAxis: { type: 'value', show: false, min: 0, max: 1 },
  series: [],
  graphic: [{ type: 'group', left: 'center', top: 'middle', children: [
    { type: 'rect', shape: { x: -120, y: -36, width: 240, height: 72, r: 16 }, style: { fill: '#f8fafc', stroke: '#e2e8f0', lineWidth: 1 } },
    { type: 'text', style: { text: '等待采样数据...', fill: '#94a3b8', fontSize: 14, fontWeight: 600 } },
  ] }],
}))

const orderChartOption = computed(() => {
  if (chartMode.value === 'overview') return overviewOrderChartOption.value
  return hasTrendData.value ? trendOrderChartOption.value : placeholderChartOption.value
})

// ── Vehicle charts ──
const overviewVehicleChartOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...tooltipStyle },
  grid: { left: 64, right: 24, top: 16, bottom: 24 },
  xAxis: { type: 'value', max: Math.max(statisticsStore.vehicleStats.totalVehicles, 1), axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
  yAxis: { type: 'category', data: ['空闲', '配送中', '故障', '离线'], axisLabel: { color: '#475569', fontWeight: 600 }, axisTick: { show: false } },
  series: [{
    type: 'bar', barWidth: 16,
    data: [
      { value: statisticsStore.vehicleStats.idleVehicles, itemStyle: { color: '#22c55e', borderRadius: 8 } },
      { value: statisticsStore.vehicleStats.deliveringVehicles, itemStyle: { color: '#1e40af', borderRadius: 8 } },
      { value: statisticsStore.vehicleStats.faultyVehicles, itemStyle: { color: '#e11d48', borderRadius: 8 } },
      { value: statisticsStore.vehicleStats.offlineVehicles, itemStyle: { color: '#94a3b8', borderRadius: 8 } },
    ],
    label: { show: true, position: 'right', color: '#0f172a', fontWeight: 700, fontFamily: 'Fira Code' },
  }],
}))

const vehicleHeatmapXAxis = computed(() => statisticsStore.history.map((i) => i.time))
const vehicleHeatmapSeriesData = computed(() => {
  const rows = [
    { index: 0, value: (i: HistoryPoint) => i.idleVehicles },
    { index: 1, value: (i: HistoryPoint) => i.deliveringVehicles },
    { index: 2, value: (i: HistoryPoint) => i.faultyVehicles },
    { index: 3, value: (i: HistoryPoint) => i.offlineVehicles },
  ]
  return statisticsStore.history.flatMap((item, col) => rows.map((r) => [col, r.index, r.value(item)]))
})

const trendVehicleChartOption = computed<EChartsOption>(() => ({
  tooltip: { position: 'top', ...tooltipStyle, formatter: ((params: { data?: [number, number, number] }) => {
    const [ti, si, v] = params.data ?? [0, 0, 0]
    const labels = ['空闲', '配送中', '故障', '离线']
    return `${vehicleHeatmapXAxis.value[ti]}<br/>${labels[si]}: ${v} 辆`
  }) as never },
  grid: { top: 16, bottom: 72, left: 64, right: 16 },
  xAxis: { type: 'category', data: vehicleHeatmapXAxis.value, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b', fontSize: 10 } },
  yAxis: { type: 'category', data: ['空闲', '配送中', '故障', '离线'], axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#475569', fontWeight: 600 } },
  visualMap: {
    min: 0,
    max: Math.max(statisticsStore.vehicleStats.totalVehicles || 1, ...[statisticsStore.vehicleStats.idleVehicles, statisticsStore.vehicleStats.deliveringVehicles, statisticsStore.vehicleStats.faultyVehicles, statisticsStore.vehicleStats.offlineVehicles]),
    orient: 'horizontal', left: 'center', bottom: 8, textStyle: { color: '#64748b' },
    inRange: { color: ['#eff6ff', '#bfdbfe', '#7dd3fc', '#38bdf8', '#2563eb'] },
  },
  series: [{ type: 'heatmap', label: { show: true, color: '#1e293b', fontWeight: 600, fontSize: 11 }, itemStyle: { borderRadius: 8, borderColor: '#ffffff', borderWidth: 1.5 }, data: vehicleHeatmapSeriesData.value }],
}))

const vehicleChartOption = computed(() => {
  if (chartMode.value === 'overview') return overviewVehicleChartOption.value
  return hasTrendData.value ? trendVehicleChartOption.value : placeholderChartOption.value
})

// ── Strategy Radar ──
const strategyRadarOption = computed<EChartsOption>(() => ({
  tooltip: { ...tooltipStyle },
  legend: { top: 0, textStyle: { color: '#475569' }, data: ['策略占比'] },
  radar: {
    center: ['50%', '56%'], radius: '62%',
    indicator: strategies.value.map((i) => ({ name: i.label, max: 100 })),
    splitArea: { areaStyle: { color: ['rgba(8,145,178,0.02)', 'rgba(8,145,178,0.06)'] } },
    splitLine: { lineStyle: { color: '#e2e8f0' } },
    axisLine: { lineStyle: { color: '#bfdbfe' } },
  },
  series: [{
    name: '策略对比', type: 'radar',
    data: [{
      value: strategies.value.map((i) => i.percentage),
      name: '策略占比',
      lineStyle: { width: 2.5, color: P.cyan },
      itemStyle: { color: P.cyan },
      areaStyle: { color: 'rgba(8,145,178,0.2)' },
    }],
  }],
}))

// ── Performance Gauges ──
const dispatchGaugeOption = computed<EChartsOption>(() => {
  const val = statisticsStore.performanceMetrics.latestDispatchDurationMs
  const maxVal = Math.max(val * 2, 100)
  return {
    series: [{
      type: 'gauge',
      startAngle: 210, endAngle: -30,
      radius: '90%',
      min: 0, max: maxVal,
      progress: { show: true, width: 10, roundCap: true },
      pointer: { show: false },
      axisLine: { lineStyle: { width: 10, color: [[1, '#e2e8f0']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      title: { offsetCenter: [0, '70%'], fontSize: 11, color: '#64748b' },
      detail: { offsetCenter: [0, '30%'], fontSize: 18, fontWeight: 700, color: '#0f172a', fontFamily: 'Fira Code', formatter: '{value}ms' },
      data: [{ value: Number(val.toFixed(1)), name: '最近调度' }],
    }],
  }
})

const websocketGaugeOption = computed<EChartsOption>(() => {
  const val = statisticsStore.performanceMetrics.websocketMessagesLastMinute
  const maxVal = Math.max(val * 1.5, 100)
  return {
    series: [{
      type: 'gauge',
      startAngle: 210, endAngle: -30,
      radius: '90%',
      min: 0, max: maxVal,
      progress: { show: true, width: 10, roundCap: true, itemStyle: { color: P.cyan } },
      pointer: { show: false },
      axisLine: { lineStyle: { width: 10, color: [[1, '#e2e8f0']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      title: { offsetCenter: [0, '70%'], fontSize: 11, color: '#64748b' },
      detail: { offsetCenter: [0, '30%'], fontSize: 18, fontWeight: 700, color: '#0f172a', fontFamily: 'Fira Code', formatter: '{value}' },
      data: [{ value: val, name: '推送/分钟' }],
    }],
  }
})

const slowQueryGaugeOption = computed<EChartsOption>(() => {
  const val = statisticsStore.performanceMetrics.slowQueryCount
  const maxVal = Math.max(val * 2, 10)
  const color = val > 0 ? P.rose : P.green
  return {
    series: [{
      type: 'gauge',
      startAngle: 210, endAngle: -30,
      radius: '90%',
      min: 0, max: maxVal,
      progress: { show: true, width: 10, roundCap: true, itemStyle: { color } },
      pointer: { show: false },
      axisLine: { lineStyle: { width: 10, color: [[1, '#e2e8f0']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      title: { offsetCenter: [0, '70%'], fontSize: 11, color: '#64748b' },
      detail: { offsetCenter: [0, '30%'], fontSize: 18, fontWeight: 700, color: val > 0 ? '#be123c' : '#0f172a', fontFamily: 'Fira Code', formatter: '{value}' },
      data: [{ value: val, name: '慢查询' }],
    }],
  }
})

// ── Donut Charts ──
const orderDonutOption = computed<EChartsOption>(() => {
  const data = [
    { value: statisticsStore.orderStats.pendingOrders, name: '待分配', itemStyle: { color: '#f59e0b' } },
    { value: statisticsStore.orderStats.assignedOrders, name: '已分配', itemStyle: { color: '#8b5cf6' } },
    { value: statisticsStore.orderStats.deliveringOrders, name: '配送中', itemStyle: { color: '#2563eb' } },
    { value: statisticsStore.orderStats.completedOrders, name: '已完成', itemStyle: { color: '#16a34a' } },
    { value: statisticsStore.orderStats.cancelledOrders, name: '已取消', itemStyle: { color: '#94a3b8' } },
  ].filter((d) => d.value > 0)

  return {
    tooltip: { trigger: 'item', ...tooltipStyle, formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, textStyle: { color: '#475569', fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11, color: '#475569' },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: 700 } },
      data: data.length > 0 ? data : [{ value: 1, name: '暂无', itemStyle: { color: '#e2e8f0' } }],
    }],
  }
})

const vehicleDonutOption = computed<EChartsOption>(() => {
  const data = [
    { value: statisticsStore.vehicleStats.idleVehicles, name: '空闲', itemStyle: { color: '#16a34a' } },
    { value: statisticsStore.vehicleStats.deliveringVehicles, name: '配送中', itemStyle: { color: '#1e40af' } },
    { value: statisticsStore.vehicleStats.faultyVehicles, name: '故障', itemStyle: { color: '#e11d48' } },
    { value: statisticsStore.vehicleStats.offlineVehicles, name: '离线', itemStyle: { color: '#94a3b8' } },
  ].filter((d) => d.value > 0)

  return {
    tooltip: { trigger: 'item', ...tooltipStyle, formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, textStyle: { color: '#475569', fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11, color: '#475569' },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: 700 } },
      data: data.length > 0 ? data : [{ value: 1, name: '暂无', itemStyle: { color: '#e2e8f0' } }],
    }],
  }
})

onMounted(() => {
  void statisticsStore.fetchStatistics().catch(() => {})
  statisticsStore.startAutoRefresh()
})

onUnmounted(() => {
  statisticsStore.stopAutoRefresh()
})
</script>
