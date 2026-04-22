<template>
  <div class="h-full overflow-y-auto p-4">
    <!-- Filter Tabs -->
    <div class="flex gap-2 mb-4 overflow-x-auto">
      <button
        v-for="filter in filters"
        :key="filter.value"
        @click="activeFilter = filter.value"
        class="px-3 py-1.5 text-sm font-medium rounded-lg whitespace-nowrap transition-smooth cursor-pointer"
        :class="activeFilter === filter.value
          ? 'bg-primary text-white'
          : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
      >
        {{ filter.label }} ({{ getFilteredCount(filter.value) }})
      </button>
    </div>

    <Loading v-if="vehicleStore.loading && vehicleStore.vehicles.length === 0" label="正在加载车辆..." centered />

    <div
      v-else-if="vehicleStore.error"
      class="mb-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"
    >
      {{ vehicleStore.error }}
    </div>

    <!-- Vehicle List -->
    <div v-else-if="filteredVehicles.length > 0" class="space-y-3">
      <div
        v-for="vehicle in filteredVehicles"
        :key="vehicle.id"
        class="bg-white border border-gray-200 rounded-lg p-3 hover:shadow-md transition-smooth cursor-pointer"
        @click="toggleVehicleDetails(vehicle.id)"
      >
        <!-- Vehicle Header -->
        <div class="flex items-start justify-between mb-2">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-sm font-semibold text-text">车辆 #{{ vehicle.id }}</span>
              <span
                class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium"
                :class="getStatusBadgeClass(vehicle.status)"
              >
                <span class="w-1.5 h-1.5 rounded-full" :class="getStatusDotClass(vehicle.status)"></span>
                {{ getStatusText(vehicle.status) }}
              </span>
            </div>
            <div class="flex items-center gap-3 text-xs text-gray-600">
              <span>电量: {{ vehicle.battery }}%</span>
              <span>任务: {{ vehicle.totalTasks }}</span>
            </div>
          </div>
          <svg
            class="w-5 h-5 text-gray-400 transition-transform"
            :class="{ 'rotate-180': expandedVehicles.has(vehicle.id) }"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </div>

        <!-- Vehicle Details (Expandable) -->
        <div v-if="expandedVehicles.has(vehicle.id)" class="mt-3 pt-3 border-t border-gray-200 space-y-2">
          <div class="grid grid-cols-2 gap-2 text-xs">
            <div>
              <span class="text-gray-600">当前位置:</span>
              <span class="ml-1 font-mono text-text">({{ vehicle.currentPosition.x.toFixed(1) }}, {{ vehicle.currentPosition.y.toFixed(1) }})</span>
            </div>
            <div>
              <span class="text-gray-600">速度:</span>
              <span class="ml-1 font-mono text-text">{{ vehicle.speed.toFixed(1) }} m/s</span>
            </div>
            <div>
              <span class="text-gray-600">载重:</span>
              <span class="ml-1 font-mono text-text">{{ vehicle.currentLoad }} / {{ vehicle.capacity }} kg</span>
            </div>
            <div>
              <span class="text-gray-600">行驶距离:</span>
              <span class="ml-1 font-mono text-text">{{ vehicle.totalDistance.toFixed(0) }} m</span>
            </div>
          </div>

          <!-- Battery Bar -->
          <div class="mt-2">
            <div class="flex items-center justify-between text-xs text-gray-600 mb-1">
              <span>电量</span>
              <span>{{ vehicle.battery }}%</span>
            </div>
            <div class="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
              <div
                class="h-full rounded-full transition-all"
                :class="getBatteryColor(vehicle.battery)"
                :style="{ width: `${vehicle.battery}%` }"
              ></div>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="flex gap-2 mt-3">
            <button
              v-if="vehicle.status !== 'FAULTY'"
              @click.stop="handleTriggerFault(vehicle.id)"
              class="flex-1 px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-600 text-xs font-medium rounded-lg transition-smooth cursor-pointer"
            >
              触发故障
            </button>
            <button
              v-else
              @click.stop="handleRecoverVehicle(vehicle.id)"
              class="flex-1 px-3 py-1.5 bg-green-50 hover:bg-green-100 text-green-600 text-xs font-medium rounded-lg transition-smooth cursor-pointer"
            >
              恢复车辆
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="flex flex-col items-center justify-center py-12 text-center">
      <svg class="w-16 h-16 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17a2 2 0 11-4 0 2 2 0 014 0zM19 17a2 2 0 11-4 0 2 2 0 014 0z" />
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a1 1 0 001 1h1M5 17a2 2 0 104 0m-4 0a2 2 0 114 0m6 0a2 2 0 104 0m-4 0a2 2 0 114 0" />
      </svg>
      <p class="text-gray-500 text-sm">暂无车辆</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Loading from '@/components/Loading.vue'
import { useVehicleStore } from '@/stores/vehicle'
import type { VehicleStatus } from '@/types'

const vehicleStore = useVehicleStore()

// Filters
const filters = [
  { label: '全部', value: 'ALL' },
  { label: '空闲', value: 'IDLE' },
  { label: '配送中', value: 'DELIVERING' },
  { label: '故障', value: 'FAULTY' }
]

const activeFilter = ref('ALL')
const expandedVehicles = ref(new Set<number>())

// Filtered vehicles
const filteredVehicles = computed(() => {
  if (activeFilter.value === 'ALL') {
    return vehicleStore.vehicles
  }
  return vehicleStore.vehicles.filter(vehicle => vehicle.status === activeFilter.value)
})

// Get filtered count
const getFilteredCount = (filter: string) => {
  if (filter === 'ALL') return vehicleStore.vehicles.length
  return vehicleStore.vehicles.filter(vehicle => vehicle.status === filter).length
}

// Toggle vehicle details
const toggleVehicleDetails = (vehicleId: number) => {
  if (expandedVehicles.value.has(vehicleId)) {
    expandedVehicles.value.delete(vehicleId)
  } else {
    expandedVehicles.value.add(vehicleId)
  }
}

// Status badge class
const getStatusBadgeClass = (status: VehicleStatus) => {
  const classes = {
    IDLE: 'bg-green-100 text-green-700',
    DELIVERING: 'bg-blue-100 text-blue-700',
    FAULTY: 'bg-red-100 text-red-700',
    OFFLINE: 'bg-gray-100 text-gray-500'
  }
  return classes[status] || classes.IDLE
}

// Status dot class
const getStatusDotClass = (status: VehicleStatus) => {
  const classes = {
    IDLE: 'bg-green-500',
    DELIVERING: 'bg-blue-500 animate-pulse',
    FAULTY: 'bg-red-500',
    OFFLINE: 'bg-gray-400'
  }
  return classes[status] || classes.IDLE
}

// Status text
const getStatusText = (status: VehicleStatus) => {
  const texts = {
    IDLE: '空闲',
    DELIVERING: '配送中',
    FAULTY: '故障',
    OFFLINE: '离线'
  }
  return texts[status] || status
}

// Battery color
const getBatteryColor = (battery: number) => {
  if (battery > 60) return 'bg-green-500'
  if (battery > 30) return 'bg-yellow-500'
  return 'bg-red-500'
}

// Handle trigger fault
const handleTriggerFault = async (vehicleId: number) => {
  if (confirm('确定要触发车辆故障吗？')) {
    try {
      await vehicleStore.triggerFault(vehicleId)
    } catch {
    }
  }
}

// Handle recover vehicle
const handleRecoverVehicle = async (vehicleId: number) => {
  try {
    await vehicleStore.recoverVehicle(vehicleId)
  } catch {
  }
}
</script>
