<template>
  <div
    class="fixed top-4 left-4 bg-white/90 backdrop-blur-sm rounded-lg shadow-md border border-gray-200 z-40 transition-all"
    :class="{ 'w-64': !collapsed, 'w-auto': collapsed }"
  >
    <!-- Header -->
    <div class="flex items-center justify-between p-2 border-b border-gray-200">
      <h4 class="text-xs font-semibold text-text">性能监控</h4>
      <button
        class="p-1 hover:bg-gray-100 rounded transition-colors"
        @click="collapsed = !collapsed"
      >
        <svg
          class="w-4 h-4 text-gray-600 transition-transform"
          :class="{ 'rotate-180': collapsed }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
    </div>

    <!-- Content -->
    <div v-if="!collapsed" class="p-3 space-y-3">
      <!-- FPS -->
      <div>
        <div class="flex items-center justify-between mb-1">
          <span class="text-xs text-gray-600">FPS</span>
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-500 font-mono">{{ frameTime }}ms</span>
            <span class="text-sm font-mono font-semibold" :class="fpsColor">{{ fps }}</span>
          </div>
        </div>
        <div class="h-8 flex items-end gap-0.5">
          <div
            v-for="(value, index) in fpsHistory"
            :key="index"
            class="flex-1 rounded-t transition-all"
            :style="{ height: `${(value / 60) * 100}%` }"
            :class="getFpsBarColor(value)"
          ></div>
        </div>
      </div>

      <!-- Entity Counts -->
      <div class="space-y-1">
        <div class="text-xs font-medium text-gray-700 mb-1">实体数量</div>
        <div class="grid grid-cols-2 gap-1 text-xs">
          <div class="flex justify-between">
            <span class="text-gray-600">车辆:</span>
            <span class="font-mono">{{ entityCounts.vehicles }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">订单:</span>
            <span class="font-mono">{{ entityCounts.orders }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">出货点:</span>
            <span class="font-mono">{{ entityCounts.depots }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">节点:</span>
            <span class="font-mono">{{ entityCounts.nodes }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">边:</span>
            <span class="font-mono">{{ entityCounts.edges || 0 }}</span>
          </div>
        </div>
      </div>

      <!-- Render Stats -->
      <div class="space-y-1">
        <div class="text-xs font-medium text-gray-700 mb-1">渲染统计</div>
        <div class="grid grid-cols-2 gap-1 text-xs">
          <div class="flex justify-between">
            <span class="text-gray-600">已渲染:</span>
            <span class="font-mono">{{ renderStats.rendered }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">已剔除:</span>
            <span class="font-mono">{{ renderStats.culled }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">单帧渲染:</span>
            <span class="font-mono">{{ renderMetrics.lastRenderMs.toFixed(2) }}ms</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">平均渲染:</span>
            <span class="font-mono">{{ renderMetrics.averageRenderMs.toFixed(2) }}ms</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">LOD:</span>
            <span class="font-mono">L{{ renderMetrics.lodLevel }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">剔除率:</span>
            <span class="font-mono">{{ cullingRate }}%</span>
          </div>
        </div>
      </div>

      <div class="space-y-1">
        <div class="text-xs font-medium text-gray-700 mb-1">视口范围</div>
        <div class="grid grid-cols-2 gap-1 text-xs">
          <div class="flex justify-between">
            <span class="text-gray-600">Min X:</span>
            <span class="font-mono">{{ renderMetrics.visibleBounds.minX.toFixed(0) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">Max X:</span>
            <span class="font-mono">{{ renderMetrics.visibleBounds.maxX.toFixed(0) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">Min Y:</span>
            <span class="font-mono">{{ renderMetrics.visibleBounds.minY.toFixed(0) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-600">Max Y:</span>
            <span class="font-mono">{{ renderMetrics.visibleBounds.maxY.toFixed(0) }}</span>
          </div>
        </div>
      </div>

      <!-- Memory Usage -->
      <div v-if="memoryAvailable" class="space-y-1">
        <div class="text-xs font-medium text-gray-700 mb-1">内存使用</div>
        <div class="text-xs">
          <div class="flex justify-between mb-1">
            <span class="text-gray-600">已用:</span>
            <span class="font-mono">{{ memoryUsed }} MB</span>
          </div>
          <div class="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
            <div
              class="h-full bg-blue-500 transition-all"
              :style="{ width: `${memoryPercent}%` }"
            ></div>
          </div>
        </div>
      </div>

      <!-- Debug Mode -->
      <div class="pt-2 border-t border-gray-200">
        <label class="flex items-center gap-2 cursor-pointer">
          <input
            v-model="debugMode"
            type="checkbox"
            class="w-4 h-4 text-primary border-gray-300 rounded focus:ring-primary"
            @change="handleDebugToggle"
          />
          <span class="text-xs text-gray-700">调试模式</span>
        </label>
      </div>

      <div class="pt-2 border-t border-gray-200 space-y-2">
        <div class="text-xs font-medium text-gray-700">性能场景</div>
        <div class="grid grid-cols-2 gap-2">
          <button
            type="button"
            class="rounded-md border border-slate-300 px-2 py-1.5 text-[11px] font-medium text-slate-700 transition-colors hover:bg-slate-100"
            @click="emit('load-performance-scenario')"
          >
            加载压力场景
          </button>
          <button
            type="button"
            class="rounded-md border border-slate-300 px-2 py-1.5 text-[11px] font-medium text-slate-700 transition-colors hover:bg-slate-100"
            @click="emit('restore-default-scenario')"
          >
            恢复默认场景
          </button>
        </div>
        <div class="text-[10px] leading-4 text-gray-500">
          载入 100 车 / 500 单 / 1000 节点，用于观察 FPS、渲染耗时、剔除率与 LOD。
        </div>
      </div>

      <div class="pt-2 border-t border-gray-200 space-y-2">
        <div class="flex items-center justify-between">
          <div class="text-xs font-medium text-gray-700">前端基准</div>
          <span
            class="rounded px-2 py-0.5 text-[10px] font-medium"
            :class="benchmarkState.running ? 'bg-amber-100 text-amber-700' : 'bg-slate-100 text-slate-600'"
          >
            {{ benchmarkState.running ? '采样中' : '空闲' }}
          </span>
        </div>
        <button
          type="button"
          class="w-full rounded-md border border-slate-300 px-2 py-1.5 text-[11px] font-medium text-slate-700 transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="benchmarkState.running"
          @click="startBenchmark"
        >
          {{ benchmarkState.running ? `采样中 ${benchmarkState.remainingSeconds}s` : '运行 10 秒基准采样' }}
        </button>
        <div v-if="benchmarkSummary" class="space-y-1 rounded-md border border-slate-200 bg-slate-50 p-2 text-[10px] text-slate-700">
          <div class="flex items-center justify-between gap-2">
            <div class="font-medium text-slate-800">最近一次基准结果</div>
            <button
              type="button"
              class="rounded border border-slate-300 px-2 py-0.5 text-[10px] font-medium text-slate-600 transition-colors hover:bg-white"
              @click="copyBenchmarkSummary"
            >
              复制结果
            </button>
          </div>
          <div class="text-[10px] text-slate-500">{{ benchmarkSummary.timestamp }}</div>
          <div class="grid grid-cols-2 gap-x-2 gap-y-1">
            <div class="flex justify-between">
              <span>平均 FPS:</span>
              <span class="font-mono">{{ benchmarkSummary.avgFps.toFixed(1) }}</span>
            </div>
            <div class="flex justify-between">
              <span>最低 FPS:</span>
              <span class="font-mono">{{ benchmarkSummary.minFps }}</span>
            </div>
            <div class="flex justify-between">
              <span>平均渲染:</span>
              <span class="font-mono">{{ benchmarkSummary.avgRenderMs.toFixed(2) }}ms</span>
            </div>
            <div class="flex justify-between">
              <span>峰值渲染:</span>
              <span class="font-mono">{{ benchmarkSummary.maxRenderMs.toFixed(2) }}ms</span>
            </div>
            <div class="flex justify-between">
              <span>平均剔除率:</span>
              <span class="font-mono">{{ benchmarkSummary.avgCullingRate.toFixed(1) }}%</span>
            </div>
            <div class="flex justify-between">
              <span>主 LOD:</span>
              <span class="font-mono">L{{ benchmarkSummary.dominantLod }}</span>
            </div>
            <div class="flex justify-between">
              <span>最大已渲染:</span>
              <span class="font-mono">{{ benchmarkSummary.maxRendered }}</span>
            </div>
            <div class="flex justify-between">
              <span>样本数:</span>
              <span class="font-mono">{{ benchmarkSummary.samples }}</span>
            </div>
          </div>
        </div>
        <div class="text-[10px] leading-4 text-gray-500">
          建议先加载压力场景，再运行基准采样。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

interface Props {
  entityCounts?: {
    vehicles: number
    orders: number
    depots: number
    nodes: number
    edges?: number
  }
  renderStats?: {
    rendered: number
    culled: number
  }
  renderMetrics?: {
    lastRenderMs: number
    averageRenderMs: number
    lodLevel: number
    visibleBounds: {
      minX: number
      minY: number
      maxX: number
      maxY: number
    }
  }
}

const props = withDefaults(defineProps<Props>(), {
  entityCounts: () => ({ vehicles: 0, orders: 0, depots: 0, nodes: 0, edges: 0 }),
  renderStats: () => ({ rendered: 0, culled: 0 }),
  renderMetrics: () => ({
    lastRenderMs: 0,
    averageRenderMs: 0,
    lodLevel: 2,
    visibleBounds: {
      minX: 0,
      minY: 0,
      maxX: 0,
      maxY: 0,
    },
  }),
})

const emit = defineEmits<{
  'debug-toggle': [enabled: boolean]
  'load-performance-scenario': []
  'restore-default-scenario': []
}>()

// State
const collapsed = ref(false)
const debugMode = ref(false)
const fps = ref(60)
const fpsHistory = ref<number[]>(new Array(60).fill(60))
const benchmarkState = ref({
  running: false,
  remainingSeconds: 10,
})
const benchmarkSummary = ref<null | {
  avgFps: number
  minFps: number
  avgRenderMs: number
  maxRenderMs: number
  avgCullingRate: number
  dominantLod: number
  maxRendered: number
  samples: number
  timestamp: string
}>(null)
let benchmarkTimeoutId: number | null = null
let benchmarkTickId: number | null = null
let benchmarkSamples: Array<{
  fps: number
  renderMs: number
  cullingRate: number
  lodLevel: number
  rendered: number
}> = []

// FPS tracking
let lastFrameTime = performance.now()
let frameCount = 0
let fpsUpdateTimer: number | null = null
const frameTimes: number[] = []

const updateFPS = () => {
  const now = performance.now()
  const delta = now - lastFrameTime
  
  // Track frame times for accurate FPS calculation
  frameTimes.push(delta)
  if (frameTimes.length > 60) {
    frameTimes.shift()
  }
  
  frameCount++
  lastFrameTime = now

  // Update FPS every second
  if (frameCount >= 60 || delta >= 1000) {
    // Calculate average FPS from frame times
    const avgFrameTime = frameTimes.reduce((sum, time) => sum + time, 0) / frameTimes.length
    const calculatedFPS = avgFrameTime > 0 ? Math.round(1000 / avgFrameTime) : 60
    
    fps.value = Math.min(60, calculatedFPS) // Cap at 60 FPS
    
    // Update history (last 60 frames)
    fpsHistory.value.push(fps.value)
    if (fpsHistory.value.length > 60) {
      fpsHistory.value.shift()
    }
    
    frameCount = 0
  }

  fpsUpdateTimer = requestAnimationFrame(updateFPS)
}

// Memory tracking
const memoryAvailable = computed(() => {
  return 'memory' in performance && (performance as any).memory
})

const memoryUsed = computed(() => {
  if (!memoryAvailable.value) return 0
  return Math.round(((performance as any).memory).usedJSHeapSize / 1048576)
})

const memoryTotal = computed(() => {
  if (!memoryAvailable.value) return 0
  return Math.round(((performance as any).memory).jsHeapSizeLimit / 1048576)
})

const memoryPercent = computed(() => {
  if (!memoryAvailable.value || memoryTotal.value === 0) return 0
  return Math.min(100, (memoryUsed.value / memoryTotal.value) * 100)
})

// FPS color coding
const fpsColor = computed(() => {
  if (fps.value >= 50) return 'text-green-600'
  if (fps.value >= 30) return 'text-yellow-600'
  return 'text-red-600'
})

// Frame time (average)
const frameTime = computed(() => {
  if (frameTimes.length === 0) return '0.0'
  const avgFrameTime = frameTimes.reduce((sum, time) => sum + time, 0) / frameTimes.length
  return avgFrameTime.toFixed(1)
})

const cullingRate = computed(() => {
  const total = props.renderStats.rendered + props.renderStats.culled
  if (total <= 0) return '0.0'
  return ((props.renderStats.culled / total) * 100).toFixed(1)
})

const collectBenchmarkSample = () => {
  const total = props.renderStats.rendered + props.renderStats.culled
  benchmarkSamples.push({
    fps: fps.value,
    renderMs: props.renderMetrics.averageRenderMs,
    cullingRate: total > 0 ? (props.renderStats.culled / total) * 100 : 0,
    lodLevel: props.renderMetrics.lodLevel,
    rendered: props.renderStats.rendered,
  })
}

const finishBenchmark = () => {
  benchmarkState.value.running = false
  benchmarkState.value.remainingSeconds = 10

  if (benchmarkTickId !== null) {
    clearInterval(benchmarkTickId)
    benchmarkTickId = null
  }

  if (benchmarkTimeoutId !== null) {
    clearTimeout(benchmarkTimeoutId)
    benchmarkTimeoutId = null
  }

  if (benchmarkSamples.length === 0) {
    return
  }

  const totalSamples = benchmarkSamples.length
  const fpsValues = benchmarkSamples.map((sample) => sample.fps)
  const renderValues = benchmarkSamples.map((sample) => sample.renderMs)
  const cullingValues = benchmarkSamples.map((sample) => sample.cullingRate)
  const lodCounter = new Map<number, number>()
  let maxRendered = 0

  for (const sample of benchmarkSamples) {
    lodCounter.set(sample.lodLevel, (lodCounter.get(sample.lodLevel) ?? 0) + 1)
    maxRendered = Math.max(maxRendered, sample.rendered)
  }

  const dominantLod =
    [...lodCounter.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ?? props.renderMetrics.lodLevel

  benchmarkSummary.value = {
    avgFps: fpsValues.reduce((sum, value) => sum + value, 0) / totalSamples,
    minFps: Math.min(...fpsValues),
    avgRenderMs: renderValues.reduce((sum, value) => sum + value, 0) / totalSamples,
    maxRenderMs: Math.max(...renderValues),
    avgCullingRate: cullingValues.reduce((sum, value) => sum + value, 0) / totalSamples,
    dominantLod,
    maxRendered,
    samples: totalSamples,
    timestamp: new Date().toLocaleString('zh-CN'),
  }

  benchmarkSamples = []
}

const startBenchmark = () => {
  if (benchmarkState.value.running) {
    return
  }

  benchmarkSamples = []
  benchmarkSummary.value = null
  benchmarkState.value.running = true
  benchmarkState.value.remainingSeconds = 10

  collectBenchmarkSample()

  benchmarkTickId = window.setInterval(() => {
    collectBenchmarkSample()
    benchmarkState.value.remainingSeconds = Math.max(0, benchmarkState.value.remainingSeconds - 1)
  }, 1000)

  benchmarkTimeoutId = window.setTimeout(() => {
    finishBenchmark()
  }, 10_000)
}

const copyBenchmarkSummary = async () => {
  if (!benchmarkSummary.value || !('clipboard' in navigator)) {
    return
  }

  const summary = benchmarkSummary.value
  const text = [
    `DispatchSim 前端基准结果 (${summary.timestamp})`,
    `平均 FPS: ${summary.avgFps.toFixed(1)}`,
    `最低 FPS: ${summary.minFps}`,
    `平均渲染: ${summary.avgRenderMs.toFixed(2)}ms`,
    `峰值渲染: ${summary.maxRenderMs.toFixed(2)}ms`,
    `平均剔除率: ${summary.avgCullingRate.toFixed(1)}%`,
    `主 LOD: L${summary.dominantLod}`,
    `最大已渲染实体: ${summary.maxRendered}`,
    `样本数: ${summary.samples}`,
  ].join('\n')

  try {
    await navigator.clipboard.writeText(text)
  } catch {
  }
}

// Helper function for FPS bar colors
const getFpsBarColor = (value: number) => {
  if (value >= 50) return 'bg-green-500'
  if (value >= 30) return 'bg-yellow-500'
  return 'bg-red-500'
}

// Methods
const handleDebugToggle = () => {
  emit('debug-toggle', debugMode.value)
  if (debugMode.value) {
    console.log('Debug mode enabled')
  } else {
    console.log('Debug mode disabled')
  }
}

// Lifecycle
onMounted(() => {
  updateFPS()
})

onUnmounted(() => {
  if (fpsUpdateTimer !== null) {
    cancelAnimationFrame(fpsUpdateTimer)
  }
  if (benchmarkTickId !== null) {
    clearInterval(benchmarkTickId)
  }
  if (benchmarkTimeoutId !== null) {
    clearTimeout(benchmarkTimeoutId)
  }
})
</script>
