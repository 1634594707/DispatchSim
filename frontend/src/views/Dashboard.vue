<template>
  <div class="min-h-[calc(100vh-7rem)] lg:h-[calc(100vh-7rem)] px-2 sm:px-4">
    <!-- Mobile: Drawer Toggle Button -->
    <button
      v-if="isMobile"
      @click="toggleDrawer"
      class="fixed bottom-4 right-4 z-50 bg-primary hover:bg-primary/90 text-white p-4 rounded-full shadow-lg transition-smooth cursor-pointer"
      aria-label="Toggle control panel"
    >
      <svg v-if="!isDrawerOpen" class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
      </svg>
      <svg v-else class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
      </svg>
    </button>

    <!-- Mobile: Drawer Overlay -->
    <div
      v-if="isMobile && isDrawerOpen"
      @click="closeDrawer"
      class="fixed inset-0 bg-black/50 z-40 transition-opacity"
    ></div>

    <div class="h-full grid grid-cols-1 lg:grid-cols-4 gap-2 sm:gap-4">
      <!-- Map Visualization (Left - Main Area) -->
      <div class="lg:col-span-3 h-full" :class="{ 'hidden': isMobile && isDrawerOpen }">
        <MapVisualization ref="mapRef" />
      </div>

      <!-- Control Panels (Right Side / Mobile Drawer) -->
      <div
        class="flex flex-col gap-2 sm:gap-4 h-full overflow-y-auto pb-6 pr-1"
        :class="{
          'fixed inset-y-0 right-0 w-full sm:w-96 bg-gray-50 z-40 transform transition-transform': isMobile,
          'translate-x-0': isMobile && isDrawerOpen,
          'translate-x-full': isMobile && !isDrawerOpen,
          'pt-4 px-4': isMobile
        }"
      >
        <!-- Map Import -->
        <div class="bg-white rounded-2xl shadow-lg p-3 sm:p-4 border border-gray-200 shrink-0">
          <h3 class="text-sm font-heading font-semibold text-text mb-3">
            地图管理
          </h3>
          <MapImporter @import="handleMapImport" />
        </div>

        <!-- Simulation Control -->
        <SimulationControl />

        <!-- Quick Stats -->
        <div class="bg-white rounded-2xl shadow-lg p-3 sm:p-4 border border-gray-200 shrink-0">
          <h3 class="text-sm font-heading font-semibold text-text mb-2 sm:mb-3">
            实时统计
          </h3>
          <div class="grid grid-cols-2 gap-2 sm:gap-3">
            <div class="text-center p-2 bg-gray-50 rounded-lg">
              <div class="text-xl sm:text-2xl font-bold text-primary">{{ vehicleStore.vehicleCount }}</div>
              <div class="text-xs text-gray-700">车辆总数</div>
            </div>
            <div class="text-center p-2 bg-gray-50 rounded-lg">
              <div class="text-xl sm:text-2xl font-bold text-secondary">{{ orderStore.orderCount }}</div>
              <div class="text-xs text-gray-700">订单总数</div>
            </div>
            <div class="text-center p-2 bg-gray-50 rounded-lg">
              <div class="text-xl sm:text-2xl font-bold text-green-500">{{ orderStore.completedOrders.length }}</div>
              <div class="text-xs text-gray-700">已完成</div>
            </div>
            <div class="text-center p-2 bg-gray-50 rounded-lg">
              <div class="text-xl sm:text-2xl font-bold text-yellow-500">{{ orderStore.pendingOrders.length }}</div>
              <div class="text-xs text-gray-700">待分配</div>
            </div>
          </div>
        </div>

        <!-- Create Order Form -->
        <div class="bg-white rounded-2xl shadow-lg p-3 sm:p-4 border border-gray-200 shrink-0">
          <h3 class="text-sm font-heading font-semibold text-text mb-2 sm:mb-3">
            创建订单
          </h3>
          <form @submit.prevent="handleCreateOrder" class="space-y-2.5">
            <div class="grid grid-cols-2 gap-2">
              <div>
                <label class="block text-xs font-medium text-text mb-1">
                  取货 X
                </label>
                <input
                  v-model.number="newOrder.pickupX"
                  type="number"
                  min="0"
                  max="140"
                  step="0.1"
                  class="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-lg bg-white text-text focus:ring-2 focus:ring-primary focus:border-transparent"
                  required
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-text mb-1">
                  取货 Y
                </label>
                <input
                  v-model.number="newOrder.pickupY"
                  type="number"
                  min="0"
                  max="100"
                  step="0.1"
                  class="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-lg bg-white text-text focus:ring-2 focus:ring-primary focus:border-transparent"
                  required
                />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-2">
              <div>
                <label class="block text-xs font-medium text-text mb-1">
                  送货 X
                </label>
                <input
                  v-model.number="newOrder.deliveryX"
                  type="number"
                  min="0"
                  max="140"
                  step="0.1"
                  class="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-lg bg-white text-text focus:ring-2 focus:ring-primary focus:border-transparent"
                  required
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-text mb-1">
                  送货 Y
                </label>
                <input
                  v-model.number="newOrder.deliveryY"
                  type="number"
                  min="0"
                  max="100"
                  step="0.1"
                  class="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-lg bg-white text-text focus:ring-2 focus:ring-primary focus:border-transparent"
                  required
                />
              </div>
            </div>
            <div>
              <label class="block text-xs font-medium text-text mb-1">
                优先级 (1-10)
              </label>
              <input
                v-model.number="newOrder.priority"
                type="number"
                min="1"
                max="10"
                class="w-full px-2 py-1.5 text-sm border border-gray-300 rounded-lg bg-white text-text focus:ring-2 focus:ring-primary focus:border-transparent"
                required
              />
            </div>
            <button
              type="submit"
              class="w-full bg-teal-700 hover:bg-teal-800 text-white font-semibold py-2 px-4 rounded-lg transition-smooth cursor-pointer text-sm shadow-sm"
            >
              创建订单
            </button>
          </form>
        </div>

        <!-- Tabs for Order/Vehicle Panels -->
        <div
          class="bg-white rounded-2xl shadow-lg border border-gray-200 flex flex-col min-h-[22rem]"
          :class="{ 'flex-1': !isMobile, 'shrink-0': isMobile }"
          :style="{ height: isMobile ? 'auto' : 'auto' }"
        >
          <!-- Tab Headers -->
          <div class="flex border-b border-gray-200">
            <button
              @click="activeTab = 'orders'"
              class="flex-1 px-3 sm:px-4 py-2 sm:py-3 text-xs sm:text-sm font-medium transition-smooth cursor-pointer"
              :class="activeTab === 'orders'
                ? 'text-primary border-b-2 border-primary bg-gray-100'
                : 'text-gray-700 hover:text-text'"
            >
              订单 ({{ orderStore.orderCount }})
            </button>
            <button
              @click="activeTab = 'vehicles'"
              class="flex-1 px-3 sm:px-4 py-2 sm:py-3 text-xs sm:text-sm font-medium transition-smooth cursor-pointer"
              :class="activeTab === 'vehicles'
                ? 'text-primary border-b-2 border-primary bg-gray-100'
                : 'text-gray-700 hover:text-text'"
            >
              车辆 ({{ vehicleStore.vehicleCount }})
            </button>
          </div>

          <!-- Tab Content -->
          <div class="flex-1 overflow-hidden" :style="{ maxHeight: isMobile ? '400px' : 'none' }">
            <OrderPanel v-if="activeTab === 'orders'" />
            <VehiclePanel v-if="activeTab === 'vehicles'" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import MapVisualization from '@/components/MapVisualization.vue'
import MapImporter from '@/components/MapImporter.vue'
import SimulationControl from '@/components/SimulationControl.vue'
import OrderPanel from '@/components/OrderPanel.vue'
import VehiclePanel from '@/components/VehiclePanel.vue'
import { useVehicleStore } from '@/stores/vehicle'
import { useOrderStore } from '@/stores/order'
import { useSimulationStore } from '@/stores/simulation'
import { useStatisticsStore } from '@/stores/statistics'
import { useKeyboard } from '@/composables/useKeyboard'
import type { Obstacle } from '@/types'

const vehicleStore = useVehicleStore()
const orderStore = useOrderStore()
const simulationStore = useSimulationStore()
const statisticsStore = useStatisticsStore()

// Map ref
const mapRef = ref<InstanceType<typeof MapVisualization> | null>(null)

// Active tab
const activeTab = ref<'orders' | 'vehicles'>('orders')

// Mobile drawer state
const isDrawerOpen = ref(false)
const isMobile = ref(false)

// Check if mobile
const checkMobile = () => {
  isMobile.value = window.innerWidth < 1024 // lg breakpoint
}

const toggleDrawer = () => {
  isDrawerOpen.value = !isDrawerOpen.value
}

const closeDrawer = () => {
  isDrawerOpen.value = false
}

// Handle map import
const handleMapImport = (obstacles: Obstacle[]) => {
  if (mapRef.value) {
    mapRef.value.updateObstacles(obstacles)
  }
}

// New order form
const newOrder = ref({
  pickupX: 20,
  pickupY: 20,
  deliveryX: 100,
  deliveryY: 80,
  priority: 5,
})

const resetOrderForm = () => {
  newOrder.value = {
    pickupX: 20,
    pickupY: 20,
    deliveryX: 100,
    deliveryY: 80,
    priority: 5,
  }
}

const handleCreateOrder = async () => {
  try {
    await orderStore.createOrder(
      { x: newOrder.value.pickupX, y: newOrder.value.pickupY },
      { x: newOrder.value.deliveryX, y: newOrder.value.deliveryY },
      newOrder.value.priority,
    )
    await statisticsStore.fetchStatistics()
    resetOrderForm()

    if (isMobile.value) {
      closeDrawer()
    }
  } catch {
  }
}

// Keyboard shortcuts
useKeyboard([
  {
    key: ' ',
    handler: () => {
      if (simulationStore.isRunning) {
        simulationStore.pause()
      } else if (simulationStore.isPaused) {
        simulationStore.resume()
      } else {
        simulationStore.start()
      }
    },
    description: 'Toggle simulation (Space)'
  },
  {
    key: 's',
    ctrl: true,
    handler: () => {
      simulationStore.stop()
    },
    description: 'Stop simulation (Ctrl+S)'
  },
  {
    key: 'n',
    ctrl: true,
    handler: (e) => {
      e.preventDefault()
      handleCreateOrder()
    },
    description: 'Create new order (Ctrl+N)'
  },
  {
    key: '1',
    handler: () => {
      activeTab.value = 'orders'
    },
    description: 'Switch to orders tab (1)'
  },
  {
    key: '2',
    handler: () => {
      activeTab.value = 'vehicles'
    },
    description: 'Switch to vehicles tab (2)'
  }
])

// Handle window resize
const handleResize = () => {
  checkMobile()
  // Close drawer when switching to desktop
  if (!isMobile.value) {
    isDrawerOpen.value = false
  }
}

// Initialize mock data
onMounted(() => {
  checkMobile()
  window.addEventListener('resize', handleResize)

  void Promise.allSettled([
    vehicleStore.fetchVehicles(),
    orderStore.fetchOrders(),
    simulationStore.fetchStatus(),
    statisticsStore.fetchStatistics(),
  ])

  statisticsStore.startAutoRefresh()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  statisticsStore.stopAutoRefresh()
})
</script>
