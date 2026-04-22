<template>
  <div class="bg-white rounded-2xl shadow-lg border border-gray-200 p-4">
    <h3 class="text-lg font-heading font-semibold text-text mb-4">
      仿真控制
    </h3>

    <!-- Status Indicator -->
    <div 
      class="flex items-center justify-between mb-4 p-3 bg-gray-50 rounded-lg"
      role="status"
      aria-live="polite"
      :aria-label="`仿真状态: ${statusText}`"
    >
      <div class="flex items-center gap-2">
        <div 
          class="w-3 h-3 rounded-full"
          :class="statusColor"
          role="img"
          :aria-label="`状态: ${statusText}`"
        ></div>
        <span class="text-sm font-medium text-text">{{ statusText }}</span>
      </div>
      <div 
        class="text-lg font-mono font-bold text-primary"
        role="timer"
        aria-label="仿真时间"
      >
        {{ formattedTime }}
      </div>
    </div>

    <!-- Control Buttons -->
    <div class="grid grid-cols-2 gap-2 mb-4" role="group" aria-label="仿真控制按钮">
      <!-- Start/Pause Button -->
      <button
        v-if="!simulationStore.isRunning"
        @click="handleStart"
        :disabled="simulationStore.loading"
        class="flex items-center justify-center gap-2 px-4 py-2 bg-green-500 hover:bg-green-600 text-white font-medium rounded-lg transition-smooth cursor-pointer"
        aria-label="启动仿真"
        aria-keyshortcuts="Space"
      >
        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd" />
        </svg>
        启动
      </button>

      <button
        v-else-if="!simulationStore.isPaused"
        @click="handlePause"
        :disabled="simulationStore.loading"
        class="flex items-center justify-center gap-2 px-4 py-2 bg-yellow-500 hover:bg-yellow-600 text-white font-medium rounded-lg transition-smooth cursor-pointer"
        aria-label="暂停仿真"
        aria-keyshortcuts="Space"
      >
        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
          <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zM7 8a1 1 0 012 0v4a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v4a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
        </svg>
        暂停
      </button>

      <button
        v-else
        @click="handleResume"
        :disabled="simulationStore.loading"
        class="flex items-center justify-center gap-2 px-4 py-2 bg-green-500 hover:bg-green-600 text-white font-medium rounded-lg transition-smooth cursor-pointer"
        aria-label="恢复仿真"
        aria-keyshortcuts="Space"
      >
        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd" />
        </svg>
        恢复
      </button>

      <!-- Stop Button -->
      <button
        @click="handleStop"
        :disabled="simulationStore.loading || (!simulationStore.isRunning && !simulationStore.isPaused)"
        class="flex items-center justify-center gap-2 px-4 py-2 bg-red-500 hover:bg-red-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-smooth cursor-pointer"
        aria-label="停止仿真"
        :aria-disabled="!simulationStore.isRunning && !simulationStore.isPaused"
        aria-keyshortcuts="Control+S"
      >
        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8 7a1 1 0 00-1 1v4a1 1 0 001 1h4a1 1 0 001-1V8a1 1 0 00-1-1H8z" clip-rule="evenodd" />
        </svg>
        停止
      </button>

      <!-- Step Button -->
      <button
        @click="handleStep"
        :disabled="simulationStore.loading || (simulationStore.isRunning && !simulationStore.isPaused)"
        class="flex items-center justify-center gap-2 px-4 py-2 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-smooth cursor-pointer"
        aria-label="单步执行"
        :aria-disabled="simulationStore.isRunning && !simulationStore.isPaused"
      >
        <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm.707-10.293a1 1 0 00-1.414-1.414l-3 3a1 1 0 000 1.414l3 3a1 1 0 001.414-1.414L9.414 11H13a1 1 0 100-2H9.414l1.293-1.293z" clip-rule="evenodd" />
        </svg>
        单步
      </button>
    </div>

    <!-- Speed Control -->
    <div class="mb-4">
      <label for="speed-slider" class="block text-sm font-medium text-text mb-2">
        仿真速度: {{ simulationStore.speed }}x
      </label>
      <input
        id="speed-slider"
        type="range"
        min="0.5"
        max="5"
        step="0.5"
        v-model.number="simulationStore.speed"
        class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-primary"
        aria-label="仿真速度"
        :aria-valuenow="simulationStore.speed"
        aria-valuemin="0.5"
        aria-valuemax="5"
        :aria-valuetext="`${simulationStore.speed}倍速`"
      />
      <div class="flex justify-between text-xs text-gray-600 mt-1">
        <span>0.5x</span>
        <span>1x</span>
        <span>2x</span>
        <span>3x</span>
        <span>4x</span>
        <span>5x</span>
      </div>
    </div>

    <div v-if="simulationStore.error" class="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-700">
      {{ simulationStore.error }}
    </div>

    <!-- Strategy Selector -->
    <div>
      <label for="strategy-select" class="block text-sm font-medium text-text mb-2">
        调度策略
      </label>
      <select
        id="strategy-select"
        v-model="selectedStrategy"
        @change="handleStrategyChange"
        class="w-full px-3 py-2 border border-gray-300 rounded-lg bg-white text-text focus:ring-2 focus:ring-primary focus:border-transparent cursor-pointer"
        aria-label="选择调度策略"
      >
        <option value="NEAREST_FIRST">最近优先</option>
        <option value="LOAD_BALANCE">负载均衡</option>
        <option value="COMPOSITE_SCORE">综合评分</option>
        <option value="FASTEST_ARRIVAL">最快到达</option>
      </select>
      <p class="text-xs text-gray-500 mt-2" role="note">
        {{ strategyDescription }}
      </p>
    </div>

    <div class="mt-5 border-t border-gray-200 pt-4">
      <div class="flex items-center justify-between mb-3">
        <h4 class="text-sm font-semibold text-text">批量订单</h4>
        <span class="text-[11px] text-gray-500">阶段 18</span>
      </div>

      <form class="space-y-3" @submit.prevent="handleBatchCreate">
        <div class="grid grid-cols-3 gap-2">
          <div>
            <label class="block text-xs font-medium text-text mb-1">总数</label>
            <input
              v-model.number="batchForm.totalOrders"
              type="number"
              min="1"
              max="1000"
              class="w-full rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-text mb-1">每批</label>
            <input
              v-model.number="batchForm.batchSize"
              type="number"
              min="1"
              max="200"
              class="w-full rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text"
            />
          </div>
          <div>
            <label class="block text-xs font-medium text-text mb-1">间隔 ms</label>
            <input
              v-model.number="batchForm.batchIntervalMs"
              type="number"
              min="0"
              max="60000"
              class="w-full rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text"
            />
          </div>
        </div>

        <div>
          <label class="block text-xs font-medium text-text mb-1">生成策略</label>
          <select
            v-model="batchForm.strategy"
            class="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-text"
          >
            <option value="UNIFORM">均匀分布</option>
            <option value="PEAK">高峰聚集</option>
            <option value="RANDOM">随机分布</option>
          </select>
          <p class="mt-2 text-xs text-gray-500">
            {{ batchStrategyDescription }}
          </p>
        </div>

        <div>
          <label class="block text-xs font-medium text-text mb-1">优先级</label>
          <input
            v-model.number="batchForm.priority"
            type="number"
            min="1"
            max="10"
            class="w-full rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text"
          />
        </div>

        <div class="grid grid-cols-2 gap-3">
          <div class="rounded-xl bg-gray-50 p-3">
            <p class="mb-2 text-xs font-semibold text-text">取货范围</p>
            <div class="grid grid-cols-2 gap-2">
              <input v-model.number="batchForm.pickupRange.minX" type="number" min="0" max="140" step="0.1" class="rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text" placeholder="minX" />
              <input v-model.number="batchForm.pickupRange.maxX" type="number" min="0" max="140" step="0.1" class="rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text" placeholder="maxX" />
              <input v-model.number="batchForm.pickupRange.minY" type="number" min="0" max="100" step="0.1" class="rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text" placeholder="minY" />
              <input v-model.number="batchForm.pickupRange.maxY" type="number" min="0" max="100" step="0.1" class="rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text" placeholder="maxY" />
            </div>
          </div>

          <div class="rounded-xl bg-gray-50 p-3">
            <p class="mb-2 text-xs font-semibold text-text">送货范围</p>
            <div class="grid grid-cols-2 gap-2">
              <input v-model.number="batchForm.deliveryRange.minX" type="number" min="0" max="140" step="0.1" class="rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text" placeholder="minX" />
              <input v-model.number="batchForm.deliveryRange.maxX" type="number" min="0" max="140" step="0.1" class="rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text" placeholder="maxX" />
              <input v-model.number="batchForm.deliveryRange.minY" type="number" min="0" max="100" step="0.1" class="rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text" placeholder="minY" />
              <input v-model.number="batchForm.deliveryRange.maxY" type="number" min="0" max="100" step="0.1" class="rounded-lg border border-gray-300 bg-white px-2 py-1.5 text-sm text-text" placeholder="maxY" />
            </div>
          </div>
        </div>

        <button
          type="submit"
          :disabled="simulationStore.loading"
          class="w-full rounded-lg bg-slate-800 px-4 py-2 text-sm font-medium text-white transition-smooth hover:bg-slate-900 disabled:cursor-not-allowed disabled:bg-gray-300"
        >
          生成批量订单
        </button>
      </form>
    </div>

    <div class="mt-5 border-t border-gray-200 pt-4">
      <div class="mb-3">
        <h4 class="text-sm font-semibold text-text">历史回放</h4>
      </div>

      <div class="space-y-3">
        <div>
          <label class="block text-xs font-medium text-text mb-1">回放会话</label>
          <select
            v-model="selectedReplaySession"
            @change="handleReplaySessionChange"
            class="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-text"
          >
            <option value="" disabled>请选择会话</option>
            <option
              v-for="session in simulationStore.replaySessions"
              :key="session.sessionId"
              :value="session.sessionId"
            >
              {{ formatReplaySession(session) }}
            </option>
          </select>
        </div>

        <div class="rounded-xl bg-gray-50 p-3">
          <div class="mb-2 flex items-center justify-between text-xs text-gray-700">
            <span>进度</span>
            <span>{{ replayPercent }}</span>
          </div>
          <input
            v-model.number="replayProgress"
            type="range"
            min="0"
            max="1"
            step="0.01"
            class="w-full accent-primary"
            @change="handleReplaySeek"
          />
          <div class="mt-2 flex items-center justify-between text-[11px] text-gray-500">
            <span>事件 {{ simulationStore.replayFrame?.appliedEvents ?? 0 }}/{{ simulationStore.replayFrame?.totalEvents ?? 0 }}</span>
            <span>{{ replayCursorText }}</span>
          </div>
        </div>

        <div class="grid grid-cols-[1fr_auto] gap-2">
          <select
            v-model.number="replaySpeed"
            @change="handleReplaySpeedChange"
            class="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-text"
          >
            <option :value="0.5">0.5x</option>
            <option :value="1">1x</option>
            <option :value="2">2x</option>
            <option :value="4">4x</option>
          </select>
          <div class="flex gap-2">
            <button
              type="button"
              class="rounded-lg bg-slate-700 px-3 py-2 text-sm font-medium text-white hover:bg-slate-800"
              :disabled="simulationStore.loading || !selectedReplaySession"
              @click="handleReplayToggle"
            >
              {{ simulationStore.replayPlaying ? '暂停' : '播放' }}
            </button>
            <button
              type="button"
              class="rounded-lg bg-white px-3 py-2 text-sm font-medium text-slate-900 ring-1 ring-gray-300 hover:bg-gray-50"
              :disabled="simulationStore.loading || !selectedReplaySession"
              @click="handleReplayStep"
            >
              单步
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import type { DispatchStrategy } from '@/types'
import { useSimulationStore } from '@/stores/simulation'
import type { BatchOrderGenerationStrategyDto, BatchOrderRequestDto, ReplaySessionDto } from '@/api/types'

const simulationStore = useSimulationStore()
const selectedStrategy = ref<DispatchStrategy>(simulationStore.strategy)
const selectedReplaySession = ref('')
const replayProgress = ref(1)
const replaySpeed = ref(1)
let replayTimer: ReturnType<typeof window.setInterval> | null = null
const batchForm = ref<BatchOrderRequestDto>({
  totalOrders: 20,
  batchSize: 5,
  batchIntervalMs: 1000,
  strategy: 'UNIFORM',
  priority: 5,
  pickupRange: {
    minX: 5,
    maxX: 40,
    minY: 5,
    maxY: 35,
  },
  deliveryRange: {
    minX: 60,
    maxX: 120,
    minY: 40,
    maxY: 90,
  },
})

// Status text
const statusText = computed(() => {
  if (!simulationStore.isRunning && !simulationStore.isPaused) return '已停止'
  if (simulationStore.isPaused) return '已暂停'
  return '运行中'
})

// Status color
const statusColor = computed(() => {
  if (!simulationStore.isRunning && !simulationStore.isPaused) return 'bg-gray-400'
  if (simulationStore.isPaused) return 'bg-yellow-400'
  return 'bg-green-400 animate-pulse'
})

// Formatted time
const formattedTime = computed(() => {
  const seconds = simulationStore.elapsedTime
  const h = Math.floor(seconds / 3600).toString().padStart(2, '0')
  const m = Math.floor((seconds % 3600) / 60).toString().padStart(2, '0')
  const s = (seconds % 60).toString().padStart(2, '0')
  return `${h}:${m}:${s}`
})

// Strategy description
const strategyDescription = computed(() => {
  const descriptions = {
    NEAREST_FIRST: '选择距离取货点最近的空闲车辆',
    LOAD_BALANCE: '选择当前任务数最少的车辆，避免单点过载',
    COMPOSITE_SCORE: '综合考虑距离(60%)、负载(30%)、电量(10%)',
    FASTEST_ARRIVAL: '考虑当前位置和速度，估算最快到达时间'
  }
  return descriptions[simulationStore.strategy as keyof typeof descriptions] || ''
})

const batchStrategyDescription = computed(() => {
  const descriptions: Record<BatchOrderGenerationStrategyDto, string> = {
    UNIFORM: '在指定范围内均匀铺开订单点位',
    PEAK: '在范围中心附近形成高峰聚集',
    RANDOM: '在范围内完全随机分布',
  }
  return descriptions[batchForm.value.strategy]
})

watch(
  () => simulationStore.strategy,
  (value) => {
    selectedStrategy.value = value
  },
)

watch(
  () => simulationStore.replaySessionId,
  (value) => {
    selectedReplaySession.value = value ?? ''
  },
  { immediate: true },
)

watch(
  () => simulationStore.replayProgress,
  (value) => {
    replayProgress.value = value
  },
  { immediate: true },
)

watch(
  () => simulationStore.speed,
  (value) => {
    replaySpeed.value = value
  },
  { immediate: true },
)

const replayPercent = computed(() => `${Math.round(replayProgress.value * 100)}%`)

const replayCursorText = computed(() => {
  return simulationStore.replayFrame?.cursorTime
    ? new Date(simulationStore.replayFrame.cursorTime).toLocaleTimeString()
    : '无数据'
})

const formatReplaySession = (session: ReplaySessionDto) => {
  const endedAt = session.endedAt ? new Date(session.endedAt).toLocaleTimeString() : '进行中'
  return `${endedAt} · ${session.eventCount} 条事件`
}

// Handlers
const handleStart = async () => {
  try {
    await simulationStore.start()
  } catch {
  }
}

const handlePause = async () => {
  try {
    await simulationStore.pause()
  } catch {
  }
}

const handleResume = async () => {
  try {
    await simulationStore.resume()
  } catch {
  }
}

const handleStop = async () => {
  try {
    await simulationStore.stop()
  } catch {
  }
}

const handleStep = async () => {
  try {
    await simulationStore.step()
  } catch {
  }
}

const handleStrategyChange = async () => {
  try {
    await simulationStore.setStrategy(selectedStrategy.value)
  } catch {
    selectedStrategy.value = simulationStore.strategy
  }
}

const handleBatchCreate = async () => {
  try {
    await simulationStore.createBatchOrders(batchForm.value)
  } catch {
  }
}

const handleReplaySessionChange = async () => {
  if (!selectedReplaySession.value) {
    return
  }
  try {
    await simulationStore.selectReplaySession(selectedReplaySession.value)
  } catch {
  }
}

const handleReplaySeek = async () => {
  try {
    await simulationStore.setReplayProgress(replayProgress.value)
  } catch {
  }
}

const handleReplaySpeedChange = async () => {
  simulationStore.setSpeed(replaySpeed.value)
  try {
    await simulationStore.controlReplay('SPEED', { speed: replaySpeed.value })
  } catch {
  }
}

const handleReplayToggle = async () => {
  try {
    await simulationStore.toggleReplay()
  } catch {
  }
}

const handleReplayStep = async () => {
  try {
    await simulationStore.stepReplay()
  } catch {
  }
}

onMounted(async () => {
  try {
    await simulationStore.fetchReplaySessions()
    if (simulationStore.replaySessionId) {
      await simulationStore.selectReplaySession(simulationStore.replaySessionId)
    }
  } catch {
  }
})

watch(
  () => [simulationStore.replayPlaying, replaySpeed.value, simulationStore.replayFrame?.totalEvents] as const,
  ([isPlaying, currentSpeed, totalEvents]) => {
    if (replayTimer) {
      window.clearInterval(replayTimer)
      replayTimer = null
    }

    if (!isPlaying || !selectedReplaySession.value || !totalEvents) {
      return
    }

    replayTimer = window.setInterval(async () => {
      const increment = Math.max(1 / totalEvents, currentSpeed / totalEvents)
      const nextProgress = Math.min(1, replayProgress.value + increment)

      try {
        await simulationStore.setReplayProgress(nextProgress)
        if (nextProgress >= 1) {
          simulationStore.stopReplayPlayback()
          if (replayTimer) {
            window.clearInterval(replayTimer)
            replayTimer = null
          }
        }
      } catch {
        simulationStore.stopReplayPlayback()
      }
    }, 500)
  },
  { immediate: true },
)

onUnmounted(() => {
  if (replayTimer) {
    window.clearInterval(replayTimer)
    replayTimer = null
  }
})
</script>
