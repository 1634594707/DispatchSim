<template>
  <div class="relative h-full w-full overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-lg">
    <div v-if="isLoading" class="absolute inset-0 z-20 bg-white/90 p-6">
      <Loading label="正在加载地图..." centered />
    </div>

    <canvas
      ref="canvasRef"
      class="h-full w-full"
      :style="{ cursor: canvasCursor }"
      @mouseenter="handleMouseEnter"
      @mousemove="handleCanvasMouseMove"
      @mouseleave="handleMouseLeave"
      @mousedown="handleMouseDown"
      @mouseup="handleMouseUp"
      @wheel="handleWheel"
      @contextmenu="handleContextMenu"
    />

    <div
      v-if="showCursorOverlay"
      class="pointer-events-none absolute z-20"
      :style="cursorOverlayStyle"
    >
      <div class="absolute inset-0 rounded-full border border-slate-700/90"></div>
      <div class="absolute left-1/2 top-[2px] h-[4px] w-px -translate-x-1/2 bg-slate-700/90"></div>
      <div class="absolute bottom-[2px] left-1/2 h-[4px] w-px -translate-x-1/2 bg-slate-700/90"></div>
      <div class="absolute left-[2px] top-1/2 h-px w-[4px] -translate-y-1/2 bg-slate-700/90"></div>
      <div class="absolute right-[2px] top-1/2 h-px w-[4px] -translate-y-1/2 bg-slate-700/90"></div>
      <div
        class="absolute left-1/2 top-1/2 h-[3px] w-[3px] -translate-x-1/2 -translate-y-1/2 rounded-full"
        :style="{ backgroundColor: cursorAccentColor }"
      />
    </div>

    <div class="absolute bottom-4 left-4 z-10 max-h-[calc(45%-1rem)] overflow-y-auto rounded-lg border border-gray-200 bg-white/92 px-3 py-2 shadow-md backdrop-blur-sm">
      <h4 class="mb-2 text-[11px] font-semibold text-text">图例</h4>
      <div class="grid grid-cols-2 gap-x-4 gap-y-1 text-[10px]">
        <div class="flex items-center gap-1">
          <div class="h-2 w-2 rounded-full bg-blue-500"></div>
          <span class="text-gray-700">节点</span>
        </div>
        <div class="flex items-center gap-1">
          <div class="h-[2px] w-4 bg-slate-500"></div>
          <span class="text-gray-700">边</span>
        </div>
        <div class="flex items-center gap-1">
          <div class="h-2 w-2 rounded-full bg-teal-700"></div>
          <span class="text-gray-700">配送点</span>
        </div>
        <div class="flex items-center gap-1">
          <div class="h-2 w-2 rounded-full bg-green-500"></div>
          <span class="text-gray-700">空闲车辆</span>
        </div>
        <div class="flex items-center gap-1">
          <div class="h-2 w-2 rounded-full bg-blue-500"></div>
          <span class="text-gray-700">配送中</span>
        </div>
        <div class="flex items-center gap-1">
          <div class="h-2 w-2 rounded-full bg-red-500"></div>
          <span class="text-gray-700">故障</span>
        </div>
        <div class="flex items-center gap-1">
          <div class="h-2 w-2 bg-yellow-500"></div>
          <span class="text-gray-700">待分配订单</span>
        </div>
        <div class="flex items-center gap-1">
          <div class="h-2 w-2 bg-purple-500"></div>
          <span class="text-gray-700">已分配订单</span>
        </div>
      </div>
    </div>

    <div
      v-if="tooltip.visible"
      class="pointer-events-none absolute z-50 rounded-lg border border-gray-200 bg-white/95 p-3 shadow-lg backdrop-blur-sm"
      :style="{ left: `${tooltip.x}px`, top: `${tooltip.y}px` }"
    >
      <div class="space-y-1 text-xs">
        <div class="font-semibold text-text">{{ tooltip.title }}</div>
        <div v-for="(line, index) in tooltip.lines" :key="index" class="text-gray-600">
          {{ line }}
        </div>
      </div>
    </div>

    <div class="absolute bottom-4 right-20 z-10 rounded-lg border border-gray-200 bg-white/92 px-3 py-2 shadow-md backdrop-blur-sm">
      <div class="text-xs font-mono text-gray-700">
        地图 {{ MAP_WIDTH }} x {{ MAP_HEIGHT }} | 网格 {{ GRID_SIZE }} | 缩放 {{ (zoomPanController.zoom.value * 100).toFixed(0) }}%
      </div>
    </div>

    <Minimap
      :zoom-pan-controller="zoomPanController"
      :vehicles="vehicleStore.vehicles"
      :orders="orderStore.orders"
      :obstacles="obstacles"
      :map-width="MAP_WIDTH"
      :map-height="MAP_HEIGHT"
      :canvas-width="canvasRef?.clientWidth || 0"
      :canvas-height="canvasRef?.clientHeight || 0"
      :padding="PADDING"
    />

    <ContextMenu
      :visible="contextMenu.visible"
      :position="contextMenu.position"
      :items="contextMenu.items"
      @close="closeContextMenu"
    />

    <ZoomControls
      @zoom-in="zoomPanController.zoomIn"
      @zoom-out="zoomPanController.zoomOut"
      @reset="zoomPanController.reset"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import Loading from '@/components/Loading.vue'
import Minimap from '@/components/Minimap.vue'
import ContextMenu from '@/components/ContextMenu.vue'
import ZoomControls from '@/components/ZoomControls.vue'
import { MAP_HEIGHT, MAP_WIDTH, GRID_SIZE } from '@/constants/map'
import { useZoomPan } from '@/composables/useZoomPan'
import { useDepotStore } from '@/stores/depot'
import { useOrderStore } from '@/stores/order'
import { useRoadNetworkStore } from '@/stores/roadNetwork'
import { useVehicleStore } from '@/stores/vehicle'
import type { ContextMenuItem, Depot, Obstacle, Order, Position, Vehicle } from '@/types'

const props = defineProps<{
  obstacles?: Obstacle[]
}>()

const vehicleStore = useVehicleStore()
const orderStore = useOrderStore()
const depotStore = useDepotStore()
const roadNetworkStore = useRoadNetworkStore()
const zoomPanController = useZoomPan()

const isLoading = computed(() => {
  return (vehicleStore.loading || orderStore.loading) && vehicleStore.vehicles.length === 0 && orderStore.orders.length === 0
})

const obstacles = ref<Obstacle[]>(props.obstacles || [])
const canvasRef = ref<HTMLCanvasElement | null>(null)
const tooltip = ref({
  visible: false,
  x: 0,
  y: 0,
  title: '',
  lines: [] as string[],
})

const contextMenu = ref({
  visible: false,
  position: { x: 0, y: 0 },
  items: [] as ContextMenuItem[],
})

const renderStats = ref({ rendered: 0, culled: 0 })
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
const mouseCanvasPos = ref({ x: 0, y: 0 })
const isMouseOverCanvas = ref(false)

let ctx: CanvasRenderingContext2D | null = null
let animationFrameId: number | null = null
let renderQueued = false
let isDraggingNode = false
let draggedNodeId: number | null = null
let isDraggingVehicle = false
let draggedVehicleId: number | null = null
let boxSelectStart: { x: number; y: number } | null = null
let boxSelectEnd: { x: number; y: number } | null = null
const recentRenderDurations: number[] = []

const PADDING = 24
const AXIS_LABEL_INTERVAL = 100
const PATH_CACHE_TTL = 5 * 60 * 1000
const pathCache = new Map<string, { path: number[]; timestamp: number }>()

const canvasCursor = computed(() => {
  if (isMouseOverCanvas.value) return 'none'
  if (zoomPanController.isDragging.value) return 'none'

  const mode = roadNetworkStore.editorMode
  if (mode === 'add-node') return 'cell'
  if (mode === 'add-edge') return 'alias'
  if (mode === 'move' || mode === 'move-vehicle') return 'grab'
  if (mode === 'add-vehicle') return 'copy'
  if (mode === 'delete' || mode === 'delete-vehicle') return 'not-allowed'
  return 'grab'
})

const cursorAccentColor = computed(() => {
  const mode = roadNetworkStore.editorMode
  if (mode === 'add-node') return '#0f766e'
  if (mode === 'add-edge') return '#2563eb'
  if (mode === 'move') return '#7c3aed'
  if (mode === 'add-vehicle') return '#0f766e'
  if (mode === 'move-vehicle') return '#1d4ed8'
  if (mode === 'delete' || mode === 'delete-vehicle') return '#b91c1c'
  return '#334155'
})

const showCursorOverlay = computed(() => isMouseOverCanvas.value)
const cursorOverlayStyle = computed(() => ({
  left: `${mouseCanvasPos.value.x}px`,
  top: `${mouseCanvasPos.value.y}px`,
  width: '14px',
  height: '14px',
  transform: 'translate(-50%, -50%)',
}))

const lodLevel = computed(() => {
  const zoom = zoomPanController.zoom.value
  if (zoom < 0.5) return 0
  if (zoom < 2) return 1
  return 2
})

const getBaseScale = () => {
  if (!canvasRef.value) {
    return { width: 0, height: 0, scaleX: 1, scaleY: 1 }
  }

  const width = canvasRef.value.clientWidth
  const height = canvasRef.value.clientHeight

  return {
    width,
    height,
    scaleX: (width - 2 * PADDING) / MAP_WIDTH,
    scaleY: (height - 2 * PADDING) / MAP_HEIGHT,
  }
}

const mapToCanvasBase = (x: number, y: number) => {
  const { height, scaleX, scaleY } = getBaseScale()
  return {
    x: PADDING + x * scaleX,
    y: height - PADDING - y * scaleY,
  }
}

const mapToCanvas = (x: number, y: number) => {
  const base = mapToCanvasBase(x, y)
  return zoomPanController.mapToScreen(base.x, base.y)
}

const canvasToMap = (screenX: number, screenY: number): Position => {
  const { height, scaleX, scaleY } = getBaseScale()
  const base = zoomPanController.screenToMap(screenX, screenY)

  return {
    x: Math.max(0, Math.min(MAP_WIDTH, (base.x - PADDING) / scaleX)),
    y: Math.max(0, Math.min(MAP_HEIGHT, (height - PADDING - base.y) / scaleY)),
  }
}

const focusMapArea = (position: Position, zoomLevel: number) => {
  if (!canvasRef.value) {
    return
  }

  const constrainedZoom = Math.max(0.1, Math.min(10, zoomLevel))
  zoomPanController.setZoom(constrainedZoom)

  const base = mapToCanvasBase(position.x, position.y)
  const canvasWidth = canvasRef.value.clientWidth
  const canvasHeight = canvasRef.value.clientHeight
  zoomPanController.setPan(
    canvasWidth / 2 - base.x * constrainedZoom,
    canvasHeight / 2 - base.y * constrainedZoom,
  )
  scheduleRender()
}

const applyBenchmarkViewport = () => {
  focusMapArea({ x: MAP_WIDTH * 0.72, y: MAP_HEIGHT * 0.62 }, 2.6)
}

const getVisibleBounds = () => {
  if (!canvasRef.value) {
    return { minX: 0, minY: 0, maxX: MAP_WIDTH, maxY: MAP_HEIGHT }
  }

  const topLeft = canvasToMap(0, 0)
  const bottomRight = canvasToMap(canvasRef.value.clientWidth, canvasRef.value.clientHeight)

  return {
    minX: Math.min(topLeft.x, bottomRight.x),
    minY: Math.min(topLeft.y, bottomRight.y),
    maxX: Math.max(topLeft.x, bottomRight.x),
    maxY: Math.max(topLeft.y, bottomRight.y),
  }
}

const isVisible = (position: Position, padding = 20) => {
  const bounds = getVisibleBounds()
  return position.x >= bounds.minX - padding &&
    position.x <= bounds.maxX + padding &&
    position.y >= bounds.minY - padding &&
    position.y <= bounds.maxY + padding
}

const getPathCacheKey = (fromId: number, toId: number) => `${Math.min(fromId, toId)}-${Math.max(fromId, toId)}`

const getCachedPath = (fromId: number, toId: number) => {
  const cached = pathCache.get(getPathCacheKey(fromId, toId))
  if (!cached) {
    return null
  }

  if (Date.now() - cached.timestamp > PATH_CACHE_TTL) {
    pathCache.delete(getPathCacheKey(fromId, toId))
    return null
  }

  return cached.path
}

const setCachedPath = (fromId: number, toId: number, path: number[]) => {
  pathCache.set(getPathCacheKey(fromId, toId), { path, timestamp: Date.now() })
}

const clearPathCache = () => {
  pathCache.clear()
}

const initCanvas = () => {
  if (!canvasRef.value) return

  const canvas = canvasRef.value
  const container = canvas.parentElement
  if (!container) return

  const dpr = window.devicePixelRatio || 1
  canvas.width = Math.floor(container.clientWidth * dpr)
  canvas.height = Math.floor(container.clientHeight * dpr)
  canvas.style.width = `${container.clientWidth}px`
  canvas.style.height = `${container.clientHeight}px`

  ctx = canvas.getContext('2d')
  if (!ctx) return

  ctx.setTransform(1, 0, 0, 1, 0, 0)
  ctx.scale(dpr, dpr)

  zoomPanController.setMapBoundaries(
    container.clientWidth,
    container.clientHeight,
    MAP_WIDTH,
    MAP_HEIGHT,
    PADDING,
  )

  scheduleRender()
}

const scheduleRender = () => {
  if (renderQueued) return
  renderQueued = true
  animationFrameId = requestAnimationFrame(() => {
    renderQueued = false
    render()
  })
}

const render = () => {
  if (!ctx || !canvasRef.value) return
  const renderStart = performance.now()

  const canvas = canvasRef.value
  ctx.clearRect(0, 0, canvas.clientWidth, canvas.clientHeight)

  drawGrid(ctx)
  drawAxes(ctx)
  obstacles.value.forEach((obstacle) => drawObstacle(ctx!, obstacle))

  let rendered = 0
  let culled = 0
  const lod = lodLevel.value
  const visibleBounds = getVisibleBounds()

  const networkStats = drawRoadNetwork(ctx, lod)
  rendered += networkStats.rendered
  culled += networkStats.culled

  for (const depot of depotStore.depots) {
    if (isVisible(depot.position)) {
      drawDepot(ctx, depot, lod)
      rendered++
    } else {
      culled++
    }
  }

  for (const order of orderStore.orders) {
    if (isVisible(order.pickup) || isVisible(order.delivery)) {
      drawOrder(ctx, order, lod)
      rendered++
    } else {
      culled++
    }
  }

  for (const vehicle of vehicleStore.vehicles) {
    if (isVisible(vehicle.currentPosition)) {
      drawVehicle(ctx, vehicle, lod)
      rendered++
    } else {
      culled++
    }
  }

  renderStats.value = { rendered, culled }
  const renderDuration = performance.now() - renderStart
  recentRenderDurations.push(renderDuration)
  if (recentRenderDurations.length > 60) {
    recentRenderDurations.shift()
  }
  const averageRenderMs = recentRenderDurations.reduce((sum, value) => sum + value, 0) / recentRenderDurations.length
  renderMetrics.value = {
    lastRenderMs: renderDuration,
    averageRenderMs,
    lodLevel: lod,
    visibleBounds,
  }

  drawTempEdge(ctx)
  drawBoxSelection(ctx)
}

const drawGrid = (targetCtx: CanvasRenderingContext2D) => {
  targetCtx.strokeStyle = '#e5e7eb'
  targetCtx.lineWidth = 1

  for (let x = 0; x <= MAP_WIDTH; x += GRID_SIZE) {
    const start = mapToCanvas(x, 0)
    const end = mapToCanvas(x, MAP_HEIGHT)
    targetCtx.beginPath()
    targetCtx.moveTo(start.x, start.y)
    targetCtx.lineTo(end.x, end.y)
    targetCtx.stroke()
  }

  for (let y = 0; y <= MAP_HEIGHT; y += GRID_SIZE) {
    const start = mapToCanvas(0, y)
    const end = mapToCanvas(MAP_WIDTH, y)
    targetCtx.beginPath()
    targetCtx.moveTo(start.x, start.y)
    targetCtx.lineTo(end.x, end.y)
    targetCtx.stroke()
  }
}

const drawAxes = (targetCtx: CanvasRenderingContext2D) => {
  targetCtx.strokeStyle = '#64748b'
  targetCtx.fillStyle = '#64748b'
  targetCtx.lineWidth = 1.5
  targetCtx.font = '11px monospace'

  const xStart = mapToCanvas(0, 0)
  const xEnd = mapToCanvas(MAP_WIDTH, 0)
  targetCtx.beginPath()
  targetCtx.moveTo(xStart.x, xStart.y)
  targetCtx.lineTo(xEnd.x, xEnd.y)
  targetCtx.stroke()

  const yStart = mapToCanvas(0, 0)
  const yEnd = mapToCanvas(0, MAP_HEIGHT)
  targetCtx.beginPath()
  targetCtx.moveTo(yStart.x, yStart.y)
  targetCtx.lineTo(yEnd.x, yEnd.y)
  targetCtx.stroke()

  for (let x = 0; x <= MAP_WIDTH; x += AXIS_LABEL_INTERVAL) {
    const pos = mapToCanvas(x, 0)
    targetCtx.fillText(String(x), pos.x - 8, pos.y + 18)
  }

  for (let y = 0; y <= MAP_HEIGHT; y += AXIS_LABEL_INTERVAL) {
    const pos = mapToCanvas(0, y)
    targetCtx.fillText(String(y), pos.x - 24, pos.y + 4)
  }
}

const drawObstacle = (targetCtx: CanvasRenderingContext2D, obstacle: Obstacle) => {
  const topLeft = mapToCanvas(obstacle.x, obstacle.y + obstacle.height)
  const bottomRight = mapToCanvas(obstacle.x + obstacle.width, obstacle.y)
  const width = bottomRight.x - topLeft.x
  const height = bottomRight.y - topLeft.y

  const colors = {
    BUILDING: '#9ca3af',
    ROAD: '#6b7280',
    PARKING: '#93c5fd',
    GREEN_SPACE: '#86efac',
  }

  const color = colors[obstacle.type]
  targetCtx.fillStyle = `${color}cc`
  targetCtx.fillRect(topLeft.x, topLeft.y, width, height)
  targetCtx.strokeStyle = color
  targetCtx.lineWidth = 1.5
  targetCtx.strokeRect(topLeft.x, topLeft.y, width, height)
}

const drawRoadNetwork = (targetCtx: CanvasRenderingContext2D, lod: number) => {
  let rendered = 0
  let culled = 0
  const network = roadNetworkStore.network

  for (const edge of network.edges) {
    const fromNode = network.nodes.find((node) => node.id === edge.fromNodeId)
    const toNode = network.nodes.find((node) => node.id === edge.toNodeId)
    if (!fromNode || !toNode) continue

    if (!isVisible(fromNode.position, 40) && !isVisible(toNode.position, 40)) {
      culled++
      continue
    }

    const fromPos = mapToCanvas(fromNode.position.x, fromNode.position.y)
    const toPos = mapToCanvas(toNode.position.x, toNode.position.y)
    const isSelected = roadNetworkStore.selectedEdgeIds.has(edge.id)

    targetCtx.strokeStyle = isSelected ? '#f59e0b' : '#64748b'
    targetCtx.lineWidth = lod === 0 ? 1.5 : 2.5
    targetCtx.beginPath()
    targetCtx.moveTo(fromPos.x, fromPos.y)
    targetCtx.lineTo(toPos.x, toPos.y)
    targetCtx.stroke()
    rendered++
  }

  for (const node of network.nodes) {
    if (!isVisible(node.position, 20)) {
      culled++
      continue
    }

    const pos = mapToCanvas(node.position.x, node.position.y)
    const isSelected = roadNetworkStore.selectedNodeIds.has(node.id)
    const isTempStart = roadNetworkStore.tempEdgeStart === node.id

    const colors = {
      intersection: '#3b82f6',
      depot: '#0f766e',
      poi: '#8b5cf6',
    }

    targetCtx.fillStyle = colors[node.type]
    targetCtx.strokeStyle = isTempStart ? '#10b981' : isSelected ? '#f59e0b' : '#ffffff'
    targetCtx.lineWidth = isSelected || isTempStart ? 3 : 1.5
    targetCtx.beginPath()
    targetCtx.arc(pos.x, pos.y, lod === 0 ? 4 : 6, 0, Math.PI * 2)
    targetCtx.fill()
    targetCtx.stroke()

    if (lod >= 1 && node.metadata?.label) {
      targetCtx.fillStyle = '#111827'
      targetCtx.font = '10px sans-serif'
      targetCtx.textAlign = 'center'
      targetCtx.fillText(String(node.metadata.label), pos.x, pos.y - 10)
    }

    rendered++
  }

  return { rendered, culled }
}

const drawDepot = (targetCtx: CanvasRenderingContext2D, depot: Depot, lod: number) => {
  const pos = mapToCanvas(depot.position.x, depot.position.y)
  const radius = lod === 0 ? 6 : 10

  targetCtx.fillStyle = '#0f766e'
  targetCtx.beginPath()
  targetCtx.arc(pos.x, pos.y, radius, 0, Math.PI * 2)
  targetCtx.fill()

  targetCtx.fillStyle = '#ffffff'
  targetCtx.font = 'bold 10px sans-serif'
  targetCtx.textAlign = 'center'
  targetCtx.textBaseline = 'middle'
  targetCtx.fillText((depot.icon || 'DP').slice(0, 2), pos.x, pos.y)

  if (lod >= 1) {
    targetCtx.fillStyle = '#111827'
    targetCtx.textBaseline = 'top'
    targetCtx.fillText(depot.name, pos.x, pos.y + 12)
  }
}

const drawVehicle = (targetCtx: CanvasRenderingContext2D, vehicle: Vehicle, lod: number) => {
  const pos = mapToCanvas(vehicle.currentPosition.x, vehicle.currentPosition.y)

  const colors = {
    IDLE: '#10b981',
    DELIVERING: '#3b82f6',
    FAULTY: '#ef4444',
    OFFLINE: '#6b7280',
  }

  const color = colors[vehicle.status]
  targetCtx.strokeStyle = `${color}40`
  targetCtx.lineWidth = 2
  targetCtx.beginPath()
  targetCtx.arc(pos.x, pos.y, 11, 0, Math.PI * 2)
  targetCtx.stroke()

  targetCtx.fillStyle = color
  targetCtx.beginPath()
  targetCtx.arc(pos.x, pos.y, 7, 0, Math.PI * 2)
  targetCtx.fill()

  if (lod >= 1) {
    targetCtx.fillStyle = '#111827'
    targetCtx.font = 'bold 10px sans-serif'
    targetCtx.textAlign = 'center'
    targetCtx.textBaseline = 'top'
    targetCtx.fillText(`#${vehicle.id}`, pos.x, pos.y + 12)
  }
}

const drawOrder = (targetCtx: CanvasRenderingContext2D, order: Order, lod: number) => {
  const pickupPos = mapToCanvas(order.pickup.x, order.pickup.y)
  const deliveryPos = mapToCanvas(order.delivery.x, order.delivery.y)

  const colors = {
    CREATED: '#9ca3af',
    PENDING: '#f59e0b',
    ASSIGNED: '#8b5cf6',
    DELIVERING: '#3b82f6',
    COMPLETED: '#10b981',
    CANCELLED: '#6b7280',
  }

  const color = colors[order.status] || colors.PENDING

  if (lod >= 1 && order.status !== 'COMPLETED' && order.status !== 'CANCELLED') {
    targetCtx.strokeStyle = `${color}66`
    targetCtx.lineWidth = 2
    targetCtx.setLineDash([5, 5])
    targetCtx.beginPath()
    targetCtx.moveTo(pickupPos.x, pickupPos.y)
    targetCtx.lineTo(deliveryPos.x, deliveryPos.y)
    targetCtx.stroke()
    targetCtx.setLineDash([])
  }

  targetCtx.fillStyle = color
  targetCtx.fillRect(pickupPos.x - 4, pickupPos.y - 4, 8, 8)
  targetCtx.strokeStyle = color
  targetCtx.lineWidth = 2
  targetCtx.strokeRect(deliveryPos.x - 4, deliveryPos.y - 4, 8, 8)
}

const drawTempEdge = (targetCtx: CanvasRenderingContext2D) => {
  if (roadNetworkStore.tempEdgeStart === null || roadNetworkStore.editorMode !== 'add-edge') return

  const startNode = roadNetworkStore.network.nodes.find((node) => node.id === roadNetworkStore.tempEdgeStart)
  if (!startNode) return

  const startPos = mapToCanvas(startNode.position.x, startNode.position.y)
  targetCtx.strokeStyle = '#10b98180'
  targetCtx.lineWidth = 2
  targetCtx.setLineDash([6, 4])
  targetCtx.beginPath()
  targetCtx.moveTo(startPos.x, startPos.y)
  targetCtx.lineTo(mouseCanvasPos.value.x, mouseCanvasPos.value.y)
  targetCtx.stroke()
  targetCtx.setLineDash([])
}

const drawBoxSelection = (targetCtx: CanvasRenderingContext2D) => {
  if (!boxSelectStart || !boxSelectEnd || roadNetworkStore.editorMode !== 'select') return

  const x = Math.min(boxSelectStart.x, boxSelectEnd.x)
  const y = Math.min(boxSelectStart.y, boxSelectEnd.y)
  const width = Math.abs(boxSelectEnd.x - boxSelectStart.x)
  const height = Math.abs(boxSelectEnd.y - boxSelectStart.y)

  targetCtx.strokeStyle = '#3b82f6'
  targetCtx.fillStyle = '#3b82f620'
  targetCtx.lineWidth = 1
  targetCtx.setLineDash([4, 4])
  targetCtx.strokeRect(x, y, width, height)
  targetCtx.fillRect(x, y, width, height)
  targetCtx.setLineDash([])
}

const findNodeNear = (canvasX: number, canvasY: number, radius = 12) => {
  for (const node of roadNetworkStore.network.nodes) {
    const pos = mapToCanvas(node.position.x, node.position.y)
    const distance = Math.hypot(canvasX - pos.x, canvasY - pos.y)
    if (distance < radius) {
      return node.id
    }
  }
  return null
}

const findEdgeNear = (canvasX: number, canvasY: number, threshold = 8) => {
  const network = roadNetworkStore.network

  for (const edge of network.edges) {
    const fromNode = network.nodes.find((node) => node.id === edge.fromNodeId)
    const toNode = network.nodes.find((node) => node.id === edge.toNodeId)
    if (!fromNode || !toNode) continue

    const fromPos = mapToCanvas(fromNode.position.x, fromNode.position.y)
    const toPos = mapToCanvas(toNode.position.x, toNode.position.y)
    const dx = toPos.x - fromPos.x
    const dy = toPos.y - fromPos.y
    const lenSq = dx * dx + dy * dy
    if (lenSq === 0) continue

    let t = ((canvasX - fromPos.x) * dx + (canvasY - fromPos.y) * dy) / lenSq
    t = Math.max(0, Math.min(1, t))
    const projX = fromPos.x + t * dx
    const projY = fromPos.y + t * dy
    const distance = Math.hypot(canvasX - projX, canvasY - projY)

    if (distance < threshold) {
      return edge.id
    }
  }

  return null
}

const findVehicleNear = (canvasX: number, canvasY: number, radius = 14) => {
  for (const vehicle of vehicleStore.vehicles) {
    const pos = mapToCanvas(vehicle.currentPosition.x, vehicle.currentPosition.y)
    const distance = Math.hypot(canvasX - pos.x, canvasY - pos.y)
    if (distance < radius) {
      return vehicle.id
    }
  }
  return null
}

const snapPositionToNearestNode = (position: Position) => {
  let nearest: { position: Position; distance: number } | null = null

  for (const node of roadNetworkStore.network.nodes) {
    const distance = Math.hypot(node.position.x - position.x, node.position.y - position.y)
    if (!nearest || distance < nearest.distance) {
      nearest = { position: node.position, distance }
    }
  }

  return nearest ? { ...nearest.position } : null
}

const updateTooltip = (x: number, y: number) => {
  if (roadNetworkStore.editorMode !== 'view') {
    tooltip.value.visible = false
    return
  }

  const nodeId = findNodeNear(x, y)
  if (nodeId !== null) {
    const node = roadNetworkStore.network.nodes.find((item) => item.id === nodeId)
    if (node) {
      tooltip.value = {
        visible: true,
        x: x + 15,
        y: y + 15,
        title: `节点 #${node.id}`,
        lines: [
          `类型: ${node.type}`,
          `位置: (${node.position.x.toFixed(1)}, ${node.position.y.toFixed(1)})`,
        ],
      }
      return
    }
  }

  const edgeId = findEdgeNear(x, y)
  if (edgeId !== null) {
    const edge = roadNetworkStore.network.edges.find((item) => item.id === edgeId)
    if (edge) {
      tooltip.value = {
        visible: true,
        x: x + 15,
        y: y + 15,
        title: `边 #${edge.id}`,
        lines: [
          `起点: #${edge.fromNodeId} -> 终点: #${edge.toNodeId}`,
          `权重: ${edge.weight.toFixed(1)}`,
          `双向: ${edge.bidirectional ? '是' : '否'}`,
        ],
      }
      return
    }
  }

  for (const depot of depotStore.depots) {
    const pos = mapToCanvas(depot.position.x, depot.position.y)
    if (Math.hypot(x - pos.x, y - pos.y) < 12) {
      tooltip.value = {
        visible: true,
        x: x + 15,
        y: y + 15,
        title: `${depot.icon || '站点'} ${depot.name}`,
        lines: [
          `位置: (${depot.position.x.toFixed(1)}, ${depot.position.y.toFixed(1)})`,
          `待处理订单: ${depot.pendingOrderCount || 0}`,
        ],
      }
      return
    }
  }

  for (const vehicle of vehicleStore.vehicles) {
    const pos = mapToCanvas(vehicle.currentPosition.x, vehicle.currentPosition.y)
    if (Math.hypot(x - pos.x, y - pos.y) < 12) {
      tooltip.value = {
        visible: true,
        x: x + 15,
        y: y + 15,
        title: `车辆 #${vehicle.id}`,
        lines: [
          `状态: ${vehicle.status}`,
          `位置: (${vehicle.currentPosition.x.toFixed(1)}, ${vehicle.currentPosition.y.toFixed(1)})`,
          `电量: ${vehicle.battery}%`,
        ],
      }
      return
    }
  }

  for (const order of orderStore.orders) {
    const pickupPos = mapToCanvas(order.pickup.x, order.pickup.y)
    const deliveryPos = mapToCanvas(order.delivery.x, order.delivery.y)
    const pickupDistance = Math.hypot(x - pickupPos.x, y - pickupPos.y)
    const deliveryDistance = Math.hypot(x - deliveryPos.x, y - deliveryPos.y)

    if (pickupDistance < 10 || deliveryDistance < 10) {
      tooltip.value = {
        visible: true,
        x: x + 15,
        y: y + 15,
        title: `订单 #${order.id}`,
        lines: [
          `状态: ${order.status}`,
          `优先级: ${order.priority}`,
          `取货点: (${order.pickup.x.toFixed(1)}, ${order.pickup.y.toFixed(1)})`,
          `送货点: (${order.delivery.x.toFixed(1)}, ${order.delivery.y.toFixed(1)})`,
        ],
      }
      return
    }
  }

  tooltip.value.visible = false
}

const handleMouseEnter = () => {
  isMouseOverCanvas.value = true
}

const handleCanvasMouseMove = (event: MouseEvent) => {
  if (!canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  handlePointerMove(event.clientX - rect.left, event.clientY - rect.top)
}

const handlePointerMove = (x: number, y: number) => {
  mouseCanvasPos.value = { x, y }

  if (isDraggingNode && draggedNodeId !== null) {
    roadNetworkStore.moveNode(draggedNodeId, canvasToMap(x, y))
    scheduleRender()
    return
  }

  if (isDraggingVehicle && draggedVehicleId !== null) {
    const snapped = snapPositionToNearestNode(canvasToMap(x, y))
    if (snapped) {
      vehicleStore.updateVehiclePosition(draggedVehicleId, snapped.x, snapped.y)
      scheduleRender()
    }
    return
  }

  if (zoomPanController.isDragging.value) {
    zoomPanController.updateDrag(x, y)
    scheduleRender()
    return
  }

  if (boxSelectStart && roadNetworkStore.editorMode === 'select') {
    boxSelectEnd = { x, y }
    scheduleRender()
    return
  }

  if (roadNetworkStore.editorMode === 'add-edge' && roadNetworkStore.tempEdgeStart !== null) {
    scheduleRender()
  }

  updateTooltip(x, y)
}

const handleMouseDown = (event: MouseEvent) => {
  if (!canvasRef.value || event.button !== 0) return

  const rect = canvasRef.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  const mode = roadNetworkStore.editorMode

  if (mode === 'add-node') {
    const mapPos = canvasToMap(x, y)
    const tooClose = roadNetworkStore.network.nodes.some((node) => {
      return Math.hypot(node.position.x - mapPos.x, node.position.y - mapPos.y) < 3
    })
    if (!tooClose) {
      roadNetworkStore.addNode(mapPos)
    }
    scheduleRender()
    return
  }

  if (mode === 'add-edge') {
    const nodeId = findNodeNear(x, y)
    if (nodeId === null) return

    if (roadNetworkStore.tempEdgeStart === null) {
      roadNetworkStore.setTempEdgeStart(nodeId)
    } else {
      if (nodeId !== roadNetworkStore.tempEdgeStart) {
        roadNetworkStore.addEdge(roadNetworkStore.tempEdgeStart, nodeId)
      }
      roadNetworkStore.setTempEdgeStart(null)
    }
    scheduleRender()
    return
  }

  if (mode === 'select') {
    const nodeId = findNodeNear(x, y)
    const edgeId = nodeId === null ? findEdgeNear(x, y) : null
    const addToSelection = event.ctrlKey || event.metaKey

    if (nodeId !== null) {
      addToSelection ? roadNetworkStore.toggleNodeSelection(nodeId) : roadNetworkStore.selectNode(nodeId, false)
    } else if (edgeId !== null) {
      addToSelection ? roadNetworkStore.toggleEdgeSelection(edgeId) : roadNetworkStore.selectEdge(edgeId, false)
    } else {
      if (!addToSelection) {
        roadNetworkStore.clearSelection()
      }
      boxSelectStart = { x, y }
      boxSelectEnd = null
    }
    scheduleRender()
    return
  }

  if (mode === 'move') {
    const nodeId = findNodeNear(x, y)
    if (nodeId !== null) {
      isDraggingNode = true
      draggedNodeId = nodeId
      roadNetworkStore.selectNode(nodeId, false)
      scheduleRender()
      return
    }
  }

  if (mode === 'add-vehicle') {
    const snapped = snapPositionToNearestNode(canvasToMap(x, y))
    if (snapped) {
      vehicleStore.createVehicleAtPosition(snapped)
    }
    scheduleRender()
    return
  }

  if (mode === 'move-vehicle') {
    const vehicleId = findVehicleNear(x, y)
    if (vehicleId !== null) {
      isDraggingVehicle = true
      draggedVehicleId = vehicleId
      scheduleRender()
      return
    }
  }

  if (mode === 'delete-vehicle') {
    const vehicleId = findVehicleNear(x, y)
    if (vehicleId !== null) {
      vehicleStore.removeVehicle(vehicleId)
      scheduleRender()
    }
    return
  }

  if (mode === 'delete') {
    const nodeId = findNodeNear(x, y)
    const edgeId = nodeId === null ? findEdgeNear(x, y) : null

    if (nodeId !== null) {
      roadNetworkStore.selectNode(nodeId, false)
      roadNetworkStore.deleteSelected()
    } else if (edgeId !== null) {
      roadNetworkStore.selectEdge(edgeId, false)
      roadNetworkStore.deleteSelected()
    }
    scheduleRender()
    return
  }

  zoomPanController.startDrag(x, y)
  scheduleRender()
}

const handleMouseUp = (event: MouseEvent) => {
  if (isDraggingNode) {
    isDraggingNode = false
    draggedNodeId = null
    scheduleRender()
    return
  }

  if (isDraggingVehicle) {
    isDraggingVehicle = false
    draggedVehicleId = null
    scheduleRender()
    return
  }

  if (boxSelectStart && boxSelectEnd && roadNetworkStore.editorMode === 'select') {
    const x1 = Math.min(boxSelectStart.x, boxSelectEnd.x)
    const y1 = Math.min(boxSelectStart.y, boxSelectEnd.y)
    const x2 = Math.max(boxSelectStart.x, boxSelectEnd.x)
    const y2 = Math.max(boxSelectStart.y, boxSelectEnd.y)

    if (Math.abs(x2 - x1) > 5 || Math.abs(y2 - y1) > 5) {
      const addToSelection = event.ctrlKey || event.metaKey
      const selectedNodeIds: number[] = []
      const selectedEdgeIds: number[] = []

      for (const node of roadNetworkStore.network.nodes) {
        const pos = mapToCanvas(node.position.x, node.position.y)
        if (pos.x >= x1 && pos.x <= x2 && pos.y >= y1 && pos.y <= y2) {
          selectedNodeIds.push(node.id)
        }
      }

      for (const edge of roadNetworkStore.network.edges) {
        const fromNode = roadNetworkStore.network.nodes.find((node) => node.id === edge.fromNodeId)
        const toNode = roadNetworkStore.network.nodes.find((node) => node.id === edge.toNodeId)
        if (!fromNode || !toNode) continue
        const fromPos = mapToCanvas(fromNode.position.x, fromNode.position.y)
        const toPos = mapToCanvas(toNode.position.x, toNode.position.y)
        const midX = (fromPos.x + toPos.x) / 2
        const midY = (fromPos.y + toPos.y) / 2
        if (midX >= x1 && midX <= x2 && midY >= y1 && midY <= y2) {
          selectedEdgeIds.push(edge.id)
        }
      }

      roadNetworkStore.bulkSelect(selectedNodeIds, selectedEdgeIds, addToSelection)
    }

    boxSelectStart = null
    boxSelectEnd = null
    scheduleRender()
    return
  }

  zoomPanController.endDrag()
  scheduleRender()
}

const handleMouseLeave = () => {
  isMouseOverCanvas.value = false
  tooltip.value.visible = false
  isDraggingNode = false
  draggedNodeId = null
  isDraggingVehicle = false
  draggedVehicleId = null
  boxSelectStart = null
  boxSelectEnd = null
  if (zoomPanController.isDragging.value) {
    zoomPanController.endDrag()
  }
  scheduleRender()
}

const handleWheel = (event: WheelEvent) => {
  event.preventDefault()
  if (!canvasRef.value) return

  const rect = canvasRef.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  const delta = event.deltaY < 0 ? 0.1 : -0.1

  zoomPanController.zoomAtPoint(x, y, delta)
  scheduleRender()
}

const handleContextMenu = (event: MouseEvent) => {
  event.preventDefault()
  if (!canvasRef.value) return

  const rect = canvasRef.value.getBoundingClientRect()
  const mapPos = canvasToMap(event.clientX - rect.left, event.clientY - rect.top)
  const nearestDepot = depotStore.findNearestDepot(mapPos)

  const items: ContextMenuItem[] = [
    {
      id: 'create-order',
      label: '创建订单',
      icon: '单',
      action: () => handleCreateOrder(mapPos, nearestDepot),
      disabled: !nearestDepot,
    },
  ]

  if (nearestDepot) {
    items.push({
      id: 'select-depot',
      label: `从 ${nearestDepot.name} 发货`,
      icon: nearestDepot.icon || '站',
      action: () => handleSelectDepot(nearestDepot),
    })
  }

  contextMenu.value.visible = true
  contextMenu.value.items = items
  contextMenu.value.position = { x: event.clientX, y: event.clientY }
}

const closeContextMenu = () => {
  contextMenu.value.visible = false
}

const handleCreateOrder = async (deliveryPos: Position, depot: Depot | null) => {
  if (!depot) return
  try {
    await orderStore.createOrder(depot.position, deliveryPos, 5)
  } catch (error) {
    console.error('Failed to create order:', error)
  }
}

const handleSelectDepot = (depot: Depot) => {
  console.log('Selected depot:', depot)
}

const updateObstacles = (nextObstacles: Obstacle[]) => {
  obstacles.value = nextObstacles
  scheduleRender()
}

defineExpose({
  updateObstacles,
  renderStats,
  renderMetrics,
  applyBenchmarkViewport,
  getCachedPath,
  setCachedPath,
  clearPathCache,
})

const handleResize = () => {
  initCanvas()
}

onMounted(() => {
  initCanvas()
  depotStore.fetchDepots()
  window.addEventListener('resize', handleResize)
  window.addEventListener('mousemove', handleWindowMouseMove)
  window.addEventListener('mouseup', handleMouseUp)
})

const handleWindowMouseMove = (event: MouseEvent) => {
  if (!canvasRef.value) return
  const rect = canvasRef.value.getBoundingClientRect()
  handlePointerMove(event.clientX - rect.left, event.clientY - rect.top)
}

onUnmounted(() => {
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
  }
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('mousemove', handleWindowMouseMove)
  window.removeEventListener('mouseup', handleMouseUp)
})

watch(
  [
    () => vehicleStore.vehicles,
    () => orderStore.orders,
    () => depotStore.depots,
    () => roadNetworkStore.network,
    () => roadNetworkStore.selectedNodeIds,
    () => roadNetworkStore.selectedEdgeIds,
    () => roadNetworkStore.tempEdgeStart,
    () => zoomPanController.state.value,
  ],
  () => {
    scheduleRender()
  },
  { deep: true },
)

watch(() => roadNetworkStore.network.version, clearPathCache)

watch(
  () => props.obstacles,
  (value) => {
    obstacles.value = value || []
    scheduleRender()
  },
  { deep: true },
)
</script>
