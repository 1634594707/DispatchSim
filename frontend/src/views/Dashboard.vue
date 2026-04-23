<template>
  <div class="min-h-[calc(100vh-6rem)] lg:h-[calc(100vh-6rem)] px-2 sm:px-3 lg:px-4">
    <!-- Performance Monitor -->
    <PerformanceMonitor
      :entity-counts="entityCounts"
      :render-stats="renderStats"
      :render-metrics="renderMetrics"
      @debug-toggle="handleDebugToggle"
      @load-performance-scenario="loadPerformanceScenario"
      @restore-default-scenario="restoreDefaultScenario"
    />
    
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

    <div class="h-full grid grid-cols-1 lg:grid-cols-[minmax(0,1.9fr)_22rem] xl:grid-cols-[minmax(0,2.15fr)_24rem] gap-2 sm:gap-3">
      <!-- Map Visualization (Left - Main Area) -->
      <div class="min-h-[68vh] lg:min-h-0 h-full" :class="{ 'hidden': isMobile && isDrawerOpen }">
        <MapVisualization ref="mapRef" />
        <!-- Road Network Editor (overlaid on map) -->
        <RoadNetworkEditor />
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
                  :max="MAP_WIDTH"
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
                  :max="MAP_HEIGHT"
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
                  :max="MAP_WIDTH"
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
                  :max="MAP_HEIGHT"
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
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import MapVisualization from '@/components/MapVisualization.vue'
import MapImporter from '@/components/MapImporter.vue'
import SimulationControl from '@/components/SimulationControl.vue'
import OrderPanel from '@/components/OrderPanel.vue'
import VehiclePanel from '@/components/VehiclePanel.vue'
import PerformanceMonitor from '@/components/PerformanceMonitor.vue'
import RoadNetworkEditor from '@/components/RoadNetworkEditor.vue'
import { MAP_WIDTH, MAP_HEIGHT } from '@/constants/map'
import { useVehicleStore } from '@/stores/vehicle'
import { useOrderStore } from '@/stores/order'
import { useDepotStore } from '@/stores/depot'
import { useSimulationStore } from '@/stores/simulation'
import { useStatisticsStore } from '@/stores/statistics'
import { useRoadNetworkStore } from '@/stores/roadNetwork'
import { useKeyboard } from '@/composables/useKeyboard'
import { createPerformanceScenario } from '@/utils/performanceScenario'
import type { Obstacle } from '@/types'

const vehicleStore = useVehicleStore()
const orderStore = useOrderStore()
const depotStore = useDepotStore()
const simulationStore = useSimulationStore()
const statisticsStore = useStatisticsStore()
const roadNetworkStore = useRoadNetworkStore()

// Map ref
const mapRef = ref<InstanceType<typeof MapVisualization> | null>(null)

// Active tab
const activeTab = ref<'orders' | 'vehicles'>('orders')

// Mobile drawer state
const isDrawerOpen = ref(false)
const isMobile = ref(false)

// Performance monitor state
const debugMode = ref(false)
const entityCounts = ref({
  vehicles: 0,
  orders: 0,
  depots: 0,
  nodes: 0,
  edges: 0,
})
const renderStats = ref({
  rendered: 0,
  culled: 0,
})
const renderMetrics = ref({
  lastRenderMs: 0,
  averageRenderMs: 0,
  lodLevel: 2,
  visibleBounds: {
    minX: 0,
    minY: 0,
    maxX: MAP_WIDTH,
    maxY: MAP_HEIGHT,
  },
})

// Update entity counts and render stats
const updateEntityCounts = () => {
  entityCounts.value = {
    vehicles: vehicleStore.vehicleCount,
    orders: orderStore.orderCount,
    depots: depotStore.depotCount,
    nodes: roadNetworkStore.nodeCount,
    edges: roadNetworkStore.edgeCount,
  }
  if (mapRef.value) {
    renderStats.value = mapRef.value.renderStats
    renderMetrics.value = mapRef.value.renderMetrics
  }
}

const handleDebugToggle = (enabled: boolean) => {
  debugMode.value = enabled
  console.log(`Debug mode ${enabled ? 'enabled' : 'disabled'}`)
}

const loadPerformanceScenario = () => {
  const scenario = createPerformanceScenario({
    width: MAP_WIDTH,
    height: MAP_HEIGHT,
    nodeCount: 1000,
    vehicleCount: 100,
    orderCount: 500,
  })

  roadNetworkStore.replaceNetwork(scenario.network)
  vehicleStore.setVehicles(scenario.vehicles)
  orderStore.setOrders(scenario.orders)
  updateEntityCounts()
  void nextTick(() => {
    mapRef.value?.applyBenchmarkViewport?.()
    updateEntityCounts()
  })
}

const restoreDefaultScenario = async () => {
  roadNetworkStore.resetToDefaultNetwork()

  await Promise.allSettled([
    vehicleStore.fetchVehicles(),
    orderStore.fetchOrders(),
    orderStore.fetchArchivedOrders(),
    depotStore.fetchDepots(),
    statisticsStore.fetchStatistics(),
  ])

  updateEntityCounts()
}

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
  pickupX: 240,
  pickupY: 560,
  deliveryX: 1040,
  deliveryY: 240,
  priority: 5,
})

const resetOrderForm = () => {
  newOrder.value = {
    pickupX: 240,
    pickupY: 560,
    deliveryX: 1040,
    deliveryY: 240,
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
  },
  {
    key: 'z',
    ctrl: true,
    handler: (e) => {
      e.preventDefault()
      roadNetworkStore.undo()
    },
    description: 'Undo road network edit (Ctrl+Z)'
  },
  {
    key: 'y',
    ctrl: true,
    handler: (e) => {
      e.preventDefault()
      roadNetworkStore.redo()
    },
    description: 'Redo road network edit (Ctrl+Y)'
  },
  {
    key: 'Delete',
    handler: () => {
      if (roadNetworkStore.selectedCount > 0) {
        roadNetworkStore.deleteSelected()
      }
    },
    description: 'Delete selected road network elements (Delete)'
  },
  {
    key: 'Escape',
    handler: () => {
      roadNetworkStore.setEditorMode('view')
      roadNetworkStore.clearSelection()
    },
    description: 'Return to view mode (Escape)'
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
    orderStore.fetchArchivedOrders(),
    depotStore.fetchDepots(),
    simulationStore.fetchStatus(),
    statisticsStore.fetchStatistics(),
  ])

  statisticsStore.startAutoRefresh()
  
  // Update entity counts periodically
  const entityCountInterval = setInterval(updateEntityCounts, 1000)
  
  onUnmounted(() => {
    clearInterval(entityCountInterval)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  statisticsStore.stopAutoRefresh()
})
</script>
