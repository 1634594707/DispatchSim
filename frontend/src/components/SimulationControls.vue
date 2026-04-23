<template>
  <div class="bg-white rounded-2xl shadow-lg border border-gray-200 p-4">
    <h3 class="text-lg font-semibold text-text mb-4">模拟控制</h3>

    <!-- Speed Control -->
    <div class="mb-4">
      <div class="flex items-center justify-between mb-2">
        <label class="text-sm font-medium text-gray-700">速度</label>
        <span class="text-sm font-mono text-gray-600">{{ speed.toFixed(1) }}x</span>
      </div>
      
      <input
        v-model.number="speed"
        type="range"
        min="0.1"
        max="10"
        step="0.1"
        class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-primary"
        @change="handleSpeedChange"
      />
      
      <div class="flex gap-1 mt-2">
        <button
          v-for="preset in speedPresets"
          :key="preset"
          class="flex-1 px-2 py-1 text-xs border border-gray-300 rounded hover:bg-gray-50 transition-colors"
          :class="{ 'bg-primary/10 border-primary': speed === preset }"
          @click="setSpeed(preset)"
        >
          {{ preset }}x
        </button>
      </div>
    </div>

    <!-- Control Buttons -->
    <div class="grid grid-cols-2 gap-2 mb-4">
      <button
        class="px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm font-medium flex items-center justify-center gap-1"
        :class="{ 'bg-yellow-50 border-yellow-500': isPaused }"
        @click="togglePause"
      >
        <svg v-if="!isPaused" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        {{ isPaused ? '继续' : '暂停' }}
      </button>

      <button
        class="px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm font-medium flex items-center justify-center gap-1"
        :disabled="!isPaused"
        @click="executeStep"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7" />
        </svg>
        单步执行
      </button>

      <button
        class="px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm font-medium flex items-center justify-center gap-1"
        @click="showResetConfirm = true"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        重置
      </button>

      <button
        class="px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm font-medium flex items-center justify-center gap-1"
        :class="{ 'bg-yellow-50 border-yellow-500': isEditMode }"
        @click="toggleEditMode"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
        </svg>
        {{ isEditMode ? '退出编辑' : '暂停编辑' }}
      </button>
    </div>

    <!-- Status Info -->
    <div class="text-xs text-gray-600 space-y-1 p-2 bg-gray-50 rounded-lg">
      <div class="flex justify-between">
        <span>状态:</span>
        <span class="font-medium">{{ statusText }}</span>
      </div>
      <div class="flex justify-between">
        <span>运行时间:</span>
        <span class="font-mono">{{ formatTime(elapsedTime) }}</span>
      </div>
    </div>

    <!-- Reset Confirmation -->
    <ConfirmModal
      v-if="showResetConfirm"
      title="确认重置"
      message="重置将清除所有订单、车辆和统计数据，但保留出货点和路网。确定要继续吗？"
      confirm-text="重置"
      cancel-text="取消"
      :danger="true"
      @confirm="handleReset"
      @cancel="showResetConfirm = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ConfirmModal from './ConfirmModal.vue'

// State
const speed = ref(1.0)
const isPaused = ref(false)
const isEditMode = ref(false)
const elapsedTime = ref(0)
const showResetConfirm = ref(false)

const speedPresets = [0.5, 1, 2, 5, 10]

// Computed
const statusText = computed(() => {
  if (isEditMode.value) return '编辑模式'
  if (isPaused.value) return '已暂停'
  return '运行中'
})

// Methods
const setSpeed = (value: number) => {
  speed.value = value
  handleSpeedChange()
}

const handleSpeedChange = () => {
  console.log('Speed changed to:', speed.value)
  // TODO: Call simulation API to update speed
}

const togglePause = () => {
  isPaused.value = !isPaused.value
  if (isEditMode.value && !isPaused.value) {
    isEditMode.value = false
  }
  console.log('Pause toggled:', isPaused.value)
  // TODO: Call simulation API to pause/resume
}

const executeStep = () => {
  console.log('Execute single step')
  // TODO: Call simulation API to execute one step
}

const handleReset = () => {
  console.log('Reset simulation')
  elapsedTime.value = 0
  isPaused.value = false
  isEditMode.value = false
  showResetConfirm.value = false
  // TODO: Call simulation API to reset
}

const toggleEditMode = () => {
  if (!isEditMode.value) {
    isPaused.value = true
  }
  isEditMode.value = !isEditMode.value
  console.log('Edit mode toggled:', isEditMode.value)
  // TODO: Emit event to enable/disable editing
}

const formatTime = (seconds: number) => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

// Simulate elapsed time (for demo)
setInterval(() => {
  if (!isPaused.value) {
    elapsedTime.value += 1
  }
}, 1000)
</script>
