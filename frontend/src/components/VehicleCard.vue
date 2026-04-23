<template>
  <div class="p-3 border border-gray-200 rounded-lg hover:border-primary/50 transition-colors">
    <div class="flex items-start justify-between mb-2">
      <div class="flex-1">
        <div class="flex items-center gap-2 mb-1">
          <div
            class="w-3 h-3 rounded-full"
            :class="statusColor"
          ></div>
          <h4 class="font-semibold text-text">车辆 #{{ vehicle.id }}</h4>
        </div>
        <div class="text-xs text-gray-600">
          {{ statusText }}
        </div>
      </div>
    </div>

    <!-- Battery Level -->
    <div class="mb-2">
      <div class="flex items-center justify-between text-xs mb-1">
        <span class="text-gray-600">电量</span>
        <span class="font-medium" :class="batteryColor">{{ vehicle.battery }}%</span>
      </div>
      <div class="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
        <div
          class="h-full transition-all duration-300"
          :class="batteryBarColor"
          :style="{ width: `${vehicle.battery}%` }"
        ></div>
      </div>
    </div>

    <!-- Loading Time Indicator -->
    <div v-if="isLoading" class="mb-2 flex items-center gap-2 text-xs text-gray-600">
      <svg class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
      </svg>
      <span>装卸中... {{ loadingTimeRemaining }}s</span>
    </div>

    <!-- Order Queue -->
    <div v-if="orderQueue.length > 0" class="mb-2">
      <div class="text-xs text-gray-600 mb-1">订单队列 ({{ orderQueue.length }})</div>
      <div class="flex flex-wrap gap-1">
        <span
          v-for="orderId in orderQueue.slice(0, 5)"
          :key="orderId"
          class="px-2 py-0.5 bg-blue-100 text-blue-700 rounded text-xs"
        >
          #{{ orderId }}
        </span>
        <span v-if="orderQueue.length > 5" class="px-2 py-0.5 bg-gray-100 text-gray-600 rounded text-xs">
          +{{ orderQueue.length - 5 }}
        </span>
      </div>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-2 gap-2 text-xs">
      <div class="flex justify-between">
        <span class="text-gray-600">任务数:</span>
        <span class="font-medium">{{ vehicle.totalTasks }}</span>
      </div>
      <div class="flex justify-between">
        <span class="text-gray-600">距离:</span>
        <span class="font-medium">{{ vehicle.totalDistance.toFixed(1) }}m</span>
      </div>
      <div class="flex justify-between">
        <span class="text-gray-600">载重:</span>
        <span class="font-medium">{{ vehicle.currentLoad }}/{{ vehicle.capacity }}</span>
      </div>
      <div class="flex justify-between">
        <span class="text-gray-600">速度:</span>
        <span class="font-medium">{{ vehicle.speed.toFixed(1) }}m/s</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Vehicle } from '@/types'

interface Props {
  vehicle: Vehicle
  orderQueue?: number[]
  loadingTimeRemaining?: number
}

const props = withDefaults(defineProps<Props>(), {
  orderQueue: () => [],
  loadingTimeRemaining: 0,
})

// Computed
const statusColor = computed(() => {
  const colors = {
    IDLE: 'bg-green-500',
    DELIVERING: 'bg-blue-500',
    FAULTY: 'bg-red-500',
    OFFLINE: 'bg-gray-500',
  }
  return colors[props.vehicle.status]
})

const statusText = computed(() => {
  const texts = {
    IDLE: '空闲',
    DELIVERING: '配送中',
    FAULTY: '故障',
    OFFLINE: '离线',
  }
  return texts[props.vehicle.status]
})

const batteryColor = computed(() => {
  if (props.vehicle.battery > 50) return 'text-green-600'
  if (props.vehicle.battery > 20) return 'text-yellow-600'
  return 'text-red-600'
})

const batteryBarColor = computed(() => {
  if (props.vehicle.battery > 50) return 'bg-green-500'
  if (props.vehicle.battery > 20) return 'bg-yellow-500'
  return 'bg-red-500'
})

const isLoading = computed(() => props.loadingTimeRemaining > 0)
</script>
