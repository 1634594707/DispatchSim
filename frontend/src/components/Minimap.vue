<template>
  <div class="absolute bottom-4 right-4">
    <!-- Toggle Button -->
    <button
      @click="toggleMinimap"
      class="absolute -top-10 right-0 bg-white/90 backdrop-blur-sm rounded-lg shadow-lg border border-gray-200 p-2 hover:bg-white transition-colors duration-200 cursor-pointer"
      :title="isVisible ? '隐藏小地图' : '显示小地图'"
    >
      <svg
        v-if="isVisible"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        stroke-width="1.5"
        stroke="currentColor"
        class="w-5 h-5 text-gray-700"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
      </svg>
      <svg
        v-else
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        stroke-width="1.5"
        stroke="currentColor"
        class="w-5 h-5 text-gray-700"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
        <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
    </button>

    <!-- Minimap Container -->
    <div
      v-show="isVisible"
      class="bg-white/90 backdrop-blur-sm rounded-lg shadow-lg border border-gray-200 overflow-hidden transition-opacity duration-200"
      :class="{ 'opacity-0': !isVisible, 'opacity-100': isVisible }"
    >
      <canvas
        ref="minimapCanvasRef"
        :width="MINIMAP_SIZE"
        :height="MINIMAP_SIZE"
        class="block cursor-pointer"
        @click="handleMinimapClick"
      ></canvas>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import type { ZoomPanController } from '@/composables/useZoomPan'
import type { Vehicle, Order, Obstacle } from '@/types'

// Props
const props = defineProps<{
  zoomPanController: ZoomPanController
  vehicles: Vehicle[]
  orders: Order[]
  obstacles?: Obstacle[]
  mapWidth: number
  mapHeight: number
  canvasWidth: number
  canvasHeight: number
  padding: number
}>()

// Constants
const MINIMAP_SIZE = 200 // 200x200px minimap
const MINIMAP_VISIBILITY_KEY = 'dispatchsim_minimap_visible'

// Refs
const minimapCanvasRef = ref<HTMLCanvasElement | null>(null)
const isVisible = ref<boolean>(true)
let ctx: CanvasRenderingContext2D | null = null
let animationFrameId: number | null = null
let renderQueued = false

/**
 * Toggle minimap visibility
 */
const toggleMinimap = () => {
  isVisible.value = !isVisible.value
  localStorage.setItem(MINIMAP_VISIBILITY_KEY, String(isVisible.value))
}

/**
 * Load minimap visibility from localStorage
 */
const loadVisibilityState = () => {
  const saved = localStorage.getItem(MINIMAP_VISIBILITY_KEY)
  if (saved !== null) {
    isVisible.value = saved === 'true'
  }
}

/**
 * Convert map coordinates to minimap coordinates
 */
const mapToMinimap = (x: number, y: number): { x: number; y: number } => {
  const scaleX = MINIMAP_SIZE / props.mapWidth
  const scaleY = MINIMAP_SIZE / props.mapHeight

  return {
    x: x * scaleX,
    y: MINIMAP_SIZE - y * scaleY // Flip Y axis
  }
}

/**
 * Convert minimap coordinates to map coordinates
 */
const minimapToMap = (x: number, y: number): { x: number; y: number } => {
  const scaleX = props.mapWidth / MINIMAP_SIZE
  const scaleY = props.mapHeight / MINIMAP_SIZE

  return {
    x: x * scaleX,
    y: (MINIMAP_SIZE - y) * scaleY // Flip Y axis back
  }
}

/**
 * Handle click on minimap to pan main map viewport
 */
const handleMinimapClick = (event: MouseEvent) => {
  if (!minimapCanvasRef.value) return

  // Get click position relative to canvas
  const rect = minimapCanvasRef.value.getBoundingClientRect()
  const minimapX = event.clientX - rect.left
  const minimapY = event.clientY - rect.top

  // Convert minimap coordinates to map coordinates
  const mapPos = minimapToMap(minimapX, minimapY)

  const scaleX = (props.canvasWidth - 2 * props.padding) / props.mapWidth
  const scaleY = (props.canvasHeight - 2 * props.padding) / props.mapHeight
  const zoom = props.zoomPanController.zoom.value

  const baseCanvasX = props.padding + mapPos.x * scaleX
  const baseCanvasY = props.canvasHeight - props.padding - mapPos.y * scaleY

  const targetOffsetX = props.canvasWidth / 2 - baseCanvasX * zoom
  const targetOffsetY = props.canvasHeight / 2 - baseCanvasY * zoom

  props.zoomPanController.setPan(targetOffsetX, targetOffsetY)
}

/**
 * Schedule a render on the next animation frame
 */
const scheduleRender = () => {
  if (renderQueued) return
  renderQueued = true
  animationFrameId = requestAnimationFrame(() => {
    renderQueued = false
    render()
  })
}

/**
 * Render the minimap
 */
const render = () => {
  if (!ctx || !minimapCanvasRef.value) return

  // Clear canvas
  ctx.clearRect(0, 0, MINIMAP_SIZE, MINIMAP_SIZE)

  // Draw background
  ctx.fillStyle = '#F9FAFB'
  ctx.fillRect(0, 0, MINIMAP_SIZE, MINIMAP_SIZE)

  // Draw grid (simplified)
  drawGrid()

  // Draw obstacles (simplified)
  if (props.obstacles) {
    props.obstacles.forEach(obstacle => drawObstacle(obstacle))
  }

  // Draw orders (simplified)
  props.orders.forEach(order => drawOrder(order))

  // Draw vehicles (simplified)
  props.vehicles.forEach(vehicle => drawVehicle(vehicle))

  // Draw viewport rectangle
  drawViewport()
}

/**
 * Draw simplified grid
 */
const drawGrid = () => {
  if (!ctx) return

  ctx.strokeStyle = '#E5E7EB'
  ctx.lineWidth = 0.5

  const gridSize = 20 // meters

  // Vertical lines
  for (let x = 0; x <= props.mapWidth; x += gridSize) {
    const pos = mapToMinimap(x, 0)
    const posEnd = mapToMinimap(x, props.mapHeight)
    ctx.beginPath()
    ctx.moveTo(pos.x, pos.y)
    ctx.lineTo(posEnd.x, posEnd.y)
    ctx.stroke()
  }

  // Horizontal lines
  for (let y = 0; y <= props.mapHeight; y += gridSize) {
    const pos = mapToMinimap(0, y)
    const posEnd = mapToMinimap(props.mapWidth, y)
    ctx.beginPath()
    ctx.moveTo(pos.x, pos.y)
    ctx.lineTo(posEnd.x, posEnd.y)
    ctx.stroke()
  }
}

/**
 * Draw simplified obstacle
 */
const drawObstacle = (obstacle: Obstacle) => {
  if (!ctx) return

  const topLeft = mapToMinimap(obstacle.x, obstacle.y + obstacle.height)
  const width = (obstacle.width / props.mapWidth) * MINIMAP_SIZE
  const height = (obstacle.height / props.mapHeight) * MINIMAP_SIZE

  // Obstacle colors (simplified)
  const colors = {
    BUILDING: '#D1D5DB',
    ROAD: '#9CA3AF',
    PARKING: '#BFDBFE',
    GREEN_SPACE: '#BBF7D0'
  }

  const color = colors[obstacle.type] || '#D1D5DB'

  ctx.fillStyle = color
  ctx.fillRect(topLeft.x, topLeft.y, width, height)
}

/**
 * Draw simplified order
 */
const drawOrder = (order: Order) => {
  if (!ctx) return

  // Skip completed and cancelled orders
  if (order.status === 'COMPLETED' || order.status === 'CANCELLED') return

  const pickupPos = mapToMinimap(order.pickup.x, order.pickup.y)
  const deliveryPos = mapToMinimap(order.delivery.x, order.delivery.y)

  // Status colors
  const colors = {
    PENDING: '#F59E0B',
    ASSIGNED: '#8B5CF6',
    DELIVERING: '#3B82F6'
  }

  const color = colors[order.status as keyof typeof colors] || colors.PENDING

  // Draw pickup point (small filled square)
  ctx.fillStyle = color
  ctx.fillRect(pickupPos.x - 1.5, pickupPos.y - 1.5, 3, 3)

  // Draw delivery point (small hollow square)
  ctx.strokeStyle = color
  ctx.lineWidth = 1
  ctx.strokeRect(deliveryPos.x - 1.5, deliveryPos.y - 1.5, 3, 3)
}

/**
 * Draw simplified vehicle
 */
const drawVehicle = (vehicle: Vehicle) => {
  if (!ctx) return

  const pos = mapToMinimap(vehicle.currentPosition.x, vehicle.currentPosition.y)

  // Status colors
  const colors = {
    IDLE: '#10B981',
    DELIVERING: '#3B82F6',
    FAULTY: '#EF4444',
    OFFLINE: '#6B7280'
  }

  const color = colors[vehicle.status] || colors.IDLE

  // Draw vehicle as small circle
  ctx.fillStyle = color
  ctx.beginPath()
  ctx.arc(pos.x, pos.y, 2, 0, 2 * Math.PI)
  ctx.fill()
}

/**
 * Draw viewport rectangle showing current view
 */
const drawViewport = () => {
  if (!ctx) return

  // Calculate the visible area in canvas coordinates
  const scaleX = (props.canvasWidth - 2 * props.padding) / props.mapWidth
  const scaleY = (props.canvasHeight - 2 * props.padding) / props.mapHeight

  // Calculate viewport corners in map coordinates
  // Top-left corner of viewport in canvas coordinates
  const canvasTopLeftX = props.padding
  const canvasTopLeftY = props.padding

  // Bottom-right corner of viewport in canvas coordinates
  const canvasBottomRightX = props.canvasWidth - props.padding
  const canvasBottomRightY = props.canvasHeight - props.padding

  // Convert canvas coordinates to map coordinates using screenToMap
  const topLeft = props.zoomPanController.screenToMap(canvasTopLeftX, canvasTopLeftY)
  const bottomRight = props.zoomPanController.screenToMap(canvasBottomRightX, canvasBottomRightY)

  // Convert back to base map coordinates (before zoom/pan transformation)
  const mapTopLeftX = (topLeft.x - props.padding) / scaleX
  const mapTopLeftY = props.mapHeight - (topLeft.y - props.padding) / scaleY

  const mapBottomRightX = (bottomRight.x - props.padding) / scaleX
  const mapBottomRightY = props.mapHeight - (bottomRight.y - props.padding) / scaleY

  // Convert to minimap coordinates
  const minimapTopLeft = mapToMinimap(mapTopLeftX, mapTopLeftY)
  const minimapBottomRight = mapToMinimap(mapBottomRightX, mapBottomRightY)

  const left = Math.min(minimapTopLeft.x, minimapBottomRight.x)
  const right = Math.max(minimapTopLeft.x, minimapBottomRight.x)
  const top = Math.min(minimapTopLeft.y, minimapBottomRight.y)
  const bottom = Math.max(minimapTopLeft.y, minimapBottomRight.y)
  const viewportWidth = right - left
  const viewportHeight = bottom - top

  // Draw viewport rectangle with semi-transparent fill
  ctx.fillStyle = 'rgba(59, 130, 246, 0.1)' // Blue with 10% opacity
  ctx.fillRect(left, top, viewportWidth, viewportHeight)

  // Draw viewport border
  ctx.strokeStyle = '#3B82F6' // Blue
  ctx.lineWidth = 2
  ctx.strokeRect(left, top, viewportWidth, viewportHeight)
}

/**
 * Initialize canvas
 */
const initCanvas = () => {
  if (!minimapCanvasRef.value) return

  ctx = minimapCanvasRef.value.getContext('2d')
  if (!ctx) return

  scheduleRender()
}

// Lifecycle
onMounted(() => {
  loadVisibilityState()
  initCanvas()
})

onUnmounted(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
})

// Watch for changes
watch(
  () => [
    props.vehicles,
    props.orders,
    props.obstacles,
    props.zoomPanController.state.value
  ],
  () => {
    scheduleRender()
  },
  { deep: true }
)
</script>
