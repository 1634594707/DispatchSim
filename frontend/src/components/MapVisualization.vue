<template>
  <div class="relative w-full h-full bg-white rounded-2xl shadow-lg border border-gray-200 overflow-hidden">
    <div v-if="isLoading" class="absolute inset-0 z-20 bg-white/90 p-6">
      <Loading label="正在加载地图与实时数据..." centered />
    </div>

    <!-- Canvas -->
    <canvas
      ref="canvasRef"
      class="w-full h-full"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
    ></canvas>

    <!-- Legend -->
    <div class="absolute top-4 right-4 bg-white/90 backdrop-blur-sm rounded-lg p-3 shadow-md border border-gray-200 max-h-[calc(100%-2rem)] overflow-y-auto">
      <h4 class="text-xs font-semibold text-text mb-2">图例</h4>
      <div class="space-y-1.5 text-xs">
        <!-- Vehicle Legend -->
        <div class="flex items-center gap-2">
          <div class="w-3 h-3 rounded-full bg-green-500"></div>
          <span class="text-gray-700">空闲车辆</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-3 h-3 rounded-full bg-blue-500"></div>
          <span class="text-gray-700">配送中</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-3 h-3 rounded-full bg-red-500"></div>
          <span class="text-gray-700">故障车辆</span>
        </div>
        <!-- Order Legend -->
        <div class="flex items-center gap-2 mt-2 pt-2 border-t border-gray-200">
          <div class="w-3 h-3 bg-yellow-500"></div>
          <span class="text-gray-700">待分配订单</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-3 h-3 bg-purple-500"></div>
          <span class="text-gray-700">已分配订单</span>
        </div>
        <div class="flex items-center gap-2">
          <div class="w-3 h-3 bg-blue-500"></div>
          <span class="text-gray-700">配送中订单</span>
        </div>
        <!-- Obstacle Legend -->
        <div v-if="obstacles.length > 0" class="mt-2 pt-2 border-t border-gray-200">
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 bg-gray-400"></div>
            <span class="text-gray-700">建筑物</span>
          </div>
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 bg-gray-600"></div>
            <span class="text-gray-700">道路</span>
          </div>
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 bg-blue-300"></div>
            <span class="text-gray-700">停车场</span>
          </div>
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 bg-green-300"></div>
            <span class="text-gray-700">绿地</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Tooltip -->
    <div
      v-if="tooltip.visible"
      class="absolute bg-white/95 backdrop-blur-sm rounded-lg p-3 shadow-lg border border-gray-200 pointer-events-none"
      :style="{ left: `${tooltip.x}px`, top: `${tooltip.y}px` }"
    >
      <div class="text-xs space-y-1">
        <div class="font-semibold text-text">{{ tooltip.title }}</div>
        <div v-for="(line, index) in tooltip.lines" :key="index" class="text-gray-600">
          {{ line }}
        </div>
      </div>
    </div>

    <!-- Coordinates Display -->
    <div class="absolute bottom-4 left-4 bg-white/90 backdrop-blur-sm rounded-lg px-3 py-2 shadow-md border border-gray-200">
      <div class="text-xs font-mono text-gray-700">
        地图: 140m × 100m | 网格: 20m × 20m
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import Loading from '@/components/Loading.vue'
import { useVehicleStore } from '@/stores/vehicle'
import { useOrderStore } from '@/stores/order'
import type { Vehicle, Order, Obstacle } from '@/types'

const vehicleStore = useVehicleStore()
const orderStore = useOrderStore()
const isLoading = computed(() => {
  return (vehicleStore.loading || orderStore.loading) && vehicleStore.vehicles.length === 0 && orderStore.orders.length === 0
})

// Props
const props = defineProps<{
  obstacles?: Obstacle[]
}>()

const obstacles = ref<Obstacle[]>(props.obstacles || [])

const canvasRef = ref<HTMLCanvasElement | null>(null)
let ctx: CanvasRenderingContext2D | null = null
let staticLayerCanvas: HTMLCanvasElement | null = null
let staticLayerCtx: CanvasRenderingContext2D | null = null
let animationFrameId: number | null = null
let renderQueued = false

// Tooltip state
const tooltip = ref({
  visible: false,
  x: 0,
  y: 0,
  title: '',
  lines: [] as string[]
})

// Map constants
const MAP_WIDTH = 140 // meters
const MAP_HEIGHT = 100 // meters
const GRID_SIZE = 20 // meters
const PADDING = 40 // pixels

// Initialize canvas
const initCanvas = () => {
  if (!canvasRef.value) return

  const canvas = canvasRef.value
  const container = canvas.parentElement
  if (!container) return
  const devicePixelRatio = window.devicePixelRatio || 1

  // Set canvas size
  canvas.width = Math.floor(container.clientWidth * devicePixelRatio)
  canvas.height = Math.floor(container.clientHeight * devicePixelRatio)
  canvas.style.width = `${container.clientWidth}px`
  canvas.style.height = `${container.clientHeight}px`

  ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.setTransform(1, 0, 0, 1, 0, 0)
  ctx.scale(devicePixelRatio, devicePixelRatio)

  buildStaticLayer(container.clientWidth, container.clientHeight, devicePixelRatio)
  scheduleRender()
}

// Convert map coordinates to canvas coordinates
const mapToCanvas = (x: number, y: number): { x: number, y: number } => {
  if (!canvasRef.value) return { x: 0, y: 0 }

  const canvas = canvasRef.value
  const width = canvas.clientWidth
  const height = canvas.clientHeight
  const scaleX = (width - 2 * PADDING) / MAP_WIDTH
  const scaleY = (height - 2 * PADDING) / MAP_HEIGHT

  return {
    x: PADDING + x * scaleX,
    y: height - PADDING - y * scaleY // Flip Y axis
  }
}

const buildStaticLayer = (width: number, height: number, devicePixelRatio: number) => {
  staticLayerCanvas = document.createElement('canvas')
  staticLayerCanvas.width = Math.floor(width * devicePixelRatio)
  staticLayerCanvas.height = Math.floor(height * devicePixelRatio)
  staticLayerCtx = staticLayerCanvas.getContext('2d')
  if (!staticLayerCtx) {
    return
  }

  staticLayerCtx.setTransform(1, 0, 0, 1, 0, 0)
  staticLayerCtx.scale(devicePixelRatio, devicePixelRatio)
  staticLayerCtx.clearRect(0, 0, width, height)

  drawGrid(staticLayerCtx)
  drawAxes(staticLayerCtx)
  obstacles.value.forEach(obstacle => drawObstacle(staticLayerCtx!, obstacle))
}

const scheduleRender = () => {
  if (renderQueued) {
    return
  }
  renderQueued = true
  animationFrameId = requestAnimationFrame(() => {
    renderQueued = false
    render()
  })
}

// Render function
const render = () => {
  if (!ctx || !canvasRef.value) return

  const canvas = canvasRef.value
  ctx.clearRect(0, 0, canvas.clientWidth, canvas.clientHeight)

  if (staticLayerCanvas) {
    ctx.drawImage(staticLayerCanvas, 0, 0, canvas.clientWidth, canvas.clientHeight)
  }

  orderStore.orders.forEach(order => drawOrder(ctx!, order))
  vehicleStore.vehicles.forEach(vehicle => drawVehicle(ctx!, vehicle))
}

// Draw grid
const drawGrid = (targetCtx: CanvasRenderingContext2D) => {
  if (!canvasRef.value) return

  targetCtx.strokeStyle = '#E5E7EB'
  targetCtx.lineWidth = 1

  // Vertical lines
  for (let x = 0; x <= MAP_WIDTH; x += GRID_SIZE) {
    const pos = mapToCanvas(x, 0)
    const posEnd = mapToCanvas(x, MAP_HEIGHT)
    targetCtx.beginPath()
    targetCtx.moveTo(pos.x, pos.y)
    targetCtx.lineTo(posEnd.x, posEnd.y)
    targetCtx.stroke()
  }

  // Horizontal lines
  for (let y = 0; y <= MAP_HEIGHT; y += GRID_SIZE) {
    const pos = mapToCanvas(0, y)
    const posEnd = mapToCanvas(MAP_WIDTH, y)
    targetCtx.beginPath()
    targetCtx.moveTo(pos.x, pos.y)
    targetCtx.lineTo(posEnd.x, posEnd.y)
    targetCtx.stroke()
  }
}

// Draw axes
const drawAxes = (targetCtx: CanvasRenderingContext2D) => {
  if (!canvasRef.value) return

  targetCtx.strokeStyle = '#6B7280'
  targetCtx.lineWidth = 2
  targetCtx.font = '11px monospace'
  targetCtx.fillStyle = '#6B7280'

  // X axis
  const xStart = mapToCanvas(0, 0)
  const xEnd = mapToCanvas(MAP_WIDTH, 0)
  targetCtx.beginPath()
  targetCtx.moveTo(xStart.x, xStart.y)
  targetCtx.lineTo(xEnd.x, xEnd.y)
  targetCtx.stroke()

  // Y axis
  const yStart = mapToCanvas(0, 0)
  const yEnd = mapToCanvas(0, MAP_HEIGHT)
  targetCtx.beginPath()
  targetCtx.moveTo(yStart.x, yStart.y)
  targetCtx.lineTo(yEnd.x, yEnd.y)
  targetCtx.stroke()

  // X axis labels
  for (let x = 0; x <= MAP_WIDTH; x += 20) {
    const pos = mapToCanvas(x, 0)
    targetCtx.fillText(x.toString(), pos.x - 8, pos.y + 20)
  }

  // Y axis labels
  for (let y = 0; y <= MAP_HEIGHT; y += 20) {
    const pos = mapToCanvas(0, y)
    targetCtx.fillText(y.toString(), pos.x - 25, pos.y + 4)
  }
}

// Draw obstacle
const drawObstacle = (targetCtx: CanvasRenderingContext2D, obstacle: Obstacle) => {
  if (!canvasRef.value) return

  const topLeft = mapToCanvas(obstacle.x, obstacle.y + obstacle.height)
  const width = (obstacle.width / MAP_WIDTH) * (canvasRef.value.clientWidth - 2 * PADDING)
  const height = (obstacle.height / MAP_HEIGHT) * (canvasRef.value.clientHeight - 2 * PADDING)

  // Obstacle colors
  const colors = {
    BUILDING: '#9CA3AF',
    ROAD: '#6B7280',
    PARKING: '#93C5FD',
    GREEN_SPACE: '#86EFAC'
  }

  const color = colors[obstacle.type]

  // Draw obstacle
  targetCtx.fillStyle = color + 'CC'
  targetCtx.fillRect(topLeft.x, topLeft.y, width, height)

  // Draw border
  targetCtx.strokeStyle = color
  targetCtx.lineWidth = 2
  targetCtx.strokeRect(topLeft.x, topLeft.y, width, height)

  // Draw label
  if (obstacle.label) {
    targetCtx.fillStyle = '#1F2937'
    targetCtx.font = 'bold 10px sans-serif'
    targetCtx.textAlign = 'center'
    targetCtx.fillText(obstacle.label, topLeft.x + width / 2, topLeft.y + height / 2 + 4)
  }
}

// Draw vehicle
const drawVehicle = (targetCtx: CanvasRenderingContext2D, vehicle: Vehicle) => {

  const pos = mapToCanvas(vehicle.currentPosition.x, vehicle.currentPosition.y)

  // Status colors
  const colors = {
    IDLE: '#10B981',
    DELIVERING: '#3B82F6',
    FAULTY: '#EF4444',
    OFFLINE: '#6B7280'
  }

  const color = colors[vehicle.status]

  // Draw outer glow
  targetCtx.strokeStyle = color + '40'
  targetCtx.lineWidth = 2
  targetCtx.beginPath()
  targetCtx.arc(pos.x, pos.y, 12, 0, 2 * Math.PI)
  targetCtx.stroke()

  // Draw vehicle circle
  targetCtx.fillStyle = color
  targetCtx.beginPath()
  targetCtx.arc(pos.x, pos.y, 8, 0, 2 * Math.PI)
  targetCtx.fill()

  // Draw direction arrow for delivering vehicles
  if (vehicle.status === 'DELIVERING' && vehicle.heading !== undefined) {
    targetCtx.save()
    targetCtx.translate(pos.x, pos.y)
    targetCtx.rotate((vehicle.heading * Math.PI) / 180)
    targetCtx.fillStyle = '#FFFFFF'
    targetCtx.beginPath()
    targetCtx.moveTo(0, -10)
    targetCtx.lineTo(-4, -6)
    targetCtx.lineTo(4, -6)
    targetCtx.closePath()
    targetCtx.fill()
    targetCtx.restore()
  }

  // Draw vehicle ID
  targetCtx.fillStyle = '#1F2937'
  targetCtx.font = 'bold 10px sans-serif'
  targetCtx.textAlign = 'center'
  targetCtx.fillText(`#${vehicle.id}`, pos.x, pos.y + 22)
}

// Draw order
const drawOrder = (targetCtx: CanvasRenderingContext2D, order: Order) => {

  const pickupPos = mapToCanvas(order.pickup.x, order.pickup.y)
  const deliveryPos = mapToCanvas(order.delivery.x, order.delivery.y)

  // Status colors
  const colors = {
    CREATED: '#9CA3AF',
    PENDING: '#F59E0B',
    ASSIGNED: '#8B5CF6',
    DELIVERING: '#3B82F6',
    COMPLETED: '#10B981',
    CANCELLED: '#6B7280'
  }

  const color = colors[order.status] || colors.PENDING

  // Draw connection line
  if (order.status !== 'COMPLETED' && order.status !== 'CANCELLED') {
    targetCtx.strokeStyle = color + '60'
    targetCtx.lineWidth = 2
    targetCtx.setLineDash([5, 5])
    targetCtx.beginPath()
    targetCtx.moveTo(pickupPos.x, pickupPos.y)
    targetCtx.lineTo(deliveryPos.x, deliveryPos.y)
    targetCtx.stroke()
    targetCtx.setLineDash([])
  }

  // Draw pickup point (filled square)
  targetCtx.fillStyle = color
  targetCtx.fillRect(pickupPos.x - 5, pickupPos.y - 5, 10, 10)

  // Draw delivery point (hollow square)
  targetCtx.strokeStyle = color
  targetCtx.lineWidth = 2
  targetCtx.strokeRect(deliveryPos.x - 5, deliveryPos.y - 5, 10, 10)
}

// Handle mouse move
const handleMouseMove = (event: MouseEvent) => {
  if (!canvasRef.value) return

  const rect = canvasRef.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  // Check obstacles
  for (const obstacle of obstacles.value) {
    const topLeft = mapToCanvas(obstacle.x, obstacle.y + obstacle.height)
    const width = (obstacle.width / MAP_WIDTH) * (canvasRef.value.clientWidth - 2 * PADDING)
    const height = (obstacle.height / MAP_HEIGHT) * (canvasRef.value.clientHeight - 2 * PADDING)

    if (x >= topLeft.x && x <= topLeft.x + width && y >= topLeft.y && y <= topLeft.y + height) {
      tooltip.value = {
        visible: true,
        x: event.clientX - rect.left + 15,
        y: event.clientY - rect.top + 15,
        title: obstacle.label || `障碍物 ${obstacle.id}`,
        lines: [
          `类型: ${getObstacleTypeText(obstacle.type)}`,
          `位置: (${obstacle.x}, ${obstacle.y})`,
          `尺寸: ${obstacle.width}m × ${obstacle.height}m`
        ]
      }
      return
    }
  }

  // Check vehicles
  for (const vehicle of vehicleStore.vehicles) {
    const pos = mapToCanvas(vehicle.currentPosition.x, vehicle.currentPosition.y)
    const distance = Math.sqrt((x - pos.x) ** 2 + (y - pos.y) ** 2)

    if (distance < 12) {
      tooltip.value = {
        visible: true,
        x: event.clientX - rect.left + 15,
        y: event.clientY - rect.top + 15,
        title: `车辆 #${vehicle.id}`,
        lines: [
          `状态: ${getVehicleStatusText(vehicle.status)}`,
          `位置: (${vehicle.currentPosition.x.toFixed(1)}, ${vehicle.currentPosition.y.toFixed(1)})`,
          `电量: ${vehicle.battery}%`,
          `任务数: ${vehicle.totalTasks}`
        ]
      }
      return
    }
  }

  // Check orders
  for (const order of orderStore.orders) {
    const pickupPos = mapToCanvas(order.pickup.x, order.pickup.y)
    const deliveryPos = mapToCanvas(order.delivery.x, order.delivery.y)

    const distancePickup = Math.sqrt((x - pickupPos.x) ** 2 + (y - pickupPos.y) ** 2)
    const distanceDelivery = Math.sqrt((x - deliveryPos.x) ** 2 + (y - deliveryPos.y) ** 2)

    if (distancePickup < 10 || distanceDelivery < 10) {
      tooltip.value = {
        visible: true,
        x: event.clientX - rect.left + 15,
        y: event.clientY - rect.top + 15,
        title: `订单 #${order.id}`,
        lines: [
          `状态: ${getOrderStatusText(order.status)}`,
          `优先级: ${order.priority}/10`,
          `取货: (${order.pickup.x.toFixed(1)}, ${order.pickup.y.toFixed(1)})`,
          `送货: (${order.delivery.x.toFixed(1)}, ${order.delivery.y.toFixed(1)})`
        ]
      }
      return
    }
  }

  tooltip.value.visible = false
}

// Handle mouse leave
const handleMouseLeave = () => {
  tooltip.value.visible = false
}

// Get vehicle status text
const getVehicleStatusText = (status: string) => {
  const texts: Record<string, string> = {
    IDLE: '空闲',
    DELIVERING: '配送中',
    FAULTY: '故障',
    OFFLINE: '离线'
  }
  return texts[status] || status
}

// Get order status text
const getOrderStatusText = (status: string) => {
  const texts: Record<string, string> = {
    CREATED: '已创建',
    PENDING: '待分配',
    ASSIGNED: '已分配',
    DELIVERING: '配送中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return texts[status] || status
}

// Get obstacle type text
const getObstacleTypeText = (type: string) => {
  const texts: Record<string, string> = {
    BUILDING: '建筑物',
    ROAD: '道路',
    PARKING: '停车场',
    GREEN_SPACE: '绿地'
  }
  return texts[type] || type
}

// Update obstacles
const updateObstacles = (newObstacles: Obstacle[]) => {
  obstacles.value = newObstacles
  if (canvasRef.value?.parentElement) {
    buildStaticLayer(
      canvasRef.value.parentElement.clientWidth,
      canvasRef.value.parentElement.clientHeight,
      window.devicePixelRatio || 1
    )
  }
  scheduleRender()
}

// Expose method to parent
defineExpose({
  updateObstacles
})

// Handle window resize
const handleResize = () => {
  initCanvas()
}

// Lifecycle
onMounted(() => {
  initCanvas()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
  window.removeEventListener('resize', handleResize)
})

// Watch for data changes
watch([() => vehicleStore.vehicles, () => orderStore.orders], () => {
  scheduleRender()
}, { deep: true })

watch(obstacles, () => {
  if (canvasRef.value?.parentElement) {
    buildStaticLayer(
      canvasRef.value.parentElement.clientWidth,
      canvasRef.value.parentElement.clientHeight,
      window.devicePixelRatio || 1
    )
  }
  scheduleRender()
}, { deep: true })

watch(() => props.obstacles, (newObstacles) => {
  obstacles.value = newObstacles || []
}, { deep: true })
</script>
