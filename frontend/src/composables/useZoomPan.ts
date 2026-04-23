import { ref, computed, watch, onMounted, onUnmounted, type ComputedRef } from 'vue'

export interface ZoomPanState {
  zoom: number // Current zoom level (0.1 - 10.0)
  offsetX: number // Pan offset X in pixels
  offsetY: number // Pan offset Y in pixels
}

export interface ZoomPanController {
  state: ComputedRef<ZoomPanState>
  zoom: ComputedRef<number>
  offsetX: ComputedRef<number>
  offsetY: ComputedRef<number>
  zoomIn: () => void
  zoomOut: () => void
  setZoom: (level: number) => void
  zoomAtPoint: (screenX: number, screenY: number, delta: number) => void
  pan: (deltaX: number, deltaY: number) => void
  setPan: (x: number, y: number) => void
  reset: () => void
  mapToScreen: (x: number, y: number) => { x: number; y: number }
  screenToMap: (x: number, y: number) => { x: number; y: number }
  startDrag: (screenX: number, screenY: number) => void
  updateDrag: (screenX: number, screenY: number) => void
  endDrag: () => void
  isDragging: ComputedRef<boolean>
  setMapBoundaries: (
    canvasWidth: number,
    canvasHeight: number,
    mapWidth: number,
    mapHeight: number,
    padding: number
  ) => void
}

const STORAGE_KEY = 'dispatch-sim-zoom-pan-state'
const MIN_ZOOM = 0.1
const MAX_ZOOM = 10.0
const ZOOM_STEP = 0.1 // 10% zoom step

let viewport = {
  canvasWidth: 0,
  canvasHeight: 0,
  padding: 0,
}

/**
 * Composable for managing map zoom and pan state with localStorage persistence
 * 
 * Features:
 * - Zoom level constrained between 0.1x and 10x
 * - Pan offset tracking (x, y coordinates)
 * - Mouse drag pan with boundary constraints
 * - localStorage persistence for state restoration
 * - Coordinate transformation utilities (map ↔ screen)
 * 
 * @returns ZoomPanController with state and control methods
 */
export function useZoomPan(): ZoomPanController {
  // Load initial state from localStorage or use defaults
  const loadState = (): ZoomPanState => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) {
        const parsed = JSON.parse(stored) as ZoomPanState
        // Validate loaded state
        return {
          zoom: constrainZoom(parsed.zoom ?? 1),
          offsetX: parsed.offsetX ?? 0,
          offsetY: parsed.offsetY ?? 0,
        }
      }
    } catch (error) {
      console.warn('Failed to load zoom/pan state from localStorage:', error)
    }
    return {
      zoom: 1,
      offsetX: 0,
      offsetY: 0,
    }
  }

  // Reactive state
  const zoom = ref<number>(1)
  const offsetX = ref<number>(0)
  const offsetY = ref<number>(0)

  // Drag state
  const isDragging = ref<boolean>(false)
  const dragStartX = ref<number>(0)
  const dragStartY = ref<number>(0)
  const dragStartOffsetX = ref<number>(0)
  const dragStartOffsetY = ref<number>(0)

  // Computed state object
  const state = computed<ZoomPanState>(() => ({
    zoom: zoom.value,
    offsetX: offsetX.value,
    offsetY: offsetY.value,
  }))

  /**
   * Constrain zoom level to valid range [0.1, 10.0]
   */
  const constrainZoom = (value: number): number => {
    return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value))
  }

  /**
   * Constrain pan offset to map boundaries
   */
  const getPanBounds = (currentZoom: number) => {
    const { canvasWidth, canvasHeight, padding } = viewport
    if (!canvasWidth || !canvasHeight) {
      return {
        minX: -Infinity,
        maxX: Infinity,
        minY: -Infinity,
        maxY: Infinity,
      }
    }

    const contentMinX = padding
    const contentMaxX = canvasWidth - padding
    const contentMinY = padding
    const contentMaxY = canvasHeight - padding

    const scaledMinX = contentMinX * currentZoom
    const scaledMaxX = contentMaxX * currentZoom
    const scaledMinY = contentMinY * currentZoom
    const scaledMaxY = contentMaxY * currentZoom

    let minX = canvasWidth - scaledMaxX
    let maxX = -scaledMinX
    let minY = canvasHeight - scaledMaxY
    let maxY = -scaledMinY

    if (minX > maxX) {
      const centeredX = (canvasWidth - (contentMinX + contentMaxX) * currentZoom) / 2
      minX = centeredX
      maxX = centeredX
    }

    if (minY > maxY) {
      const centeredY = (canvasHeight - (contentMinY + contentMaxY) * currentZoom) / 2
      minY = centeredY
      maxY = centeredY
    }

    return { minX, maxX, minY, maxY }
  }

  const constrainPan = (x: number, y: number, currentZoom = zoom.value): { x: number; y: number } => {
    const bounds = getPanBounds(currentZoom)
    return {
      x: Math.max(bounds.minX, Math.min(bounds.maxX, x)),
      y: Math.max(bounds.minY, Math.min(bounds.maxY, y)),
    }
  }

  /**
   * Save current state to localStorage
   */
  const saveState = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state.value))
    } catch (error) {
      console.warn('Failed to save zoom/pan state to localStorage:', error)
    }
  }

  /**
   * Increase zoom level by 10%
   */
  const zoomIn = () => {
    setZoom(zoom.value + ZOOM_STEP)
  }

  /**
   * Decrease zoom level by 10%
   */
  const zoomOut = () => {
    setZoom(zoom.value - ZOOM_STEP)
  }

  /**
   * Set zoom level to specific value (constrained to valid range)
   */
  const setZoom = (level: number) => {
    zoom.value = constrainZoom(level)
    const constrained = constrainPan(offsetX.value, offsetY.value)
    offsetX.value = constrained.x
    offsetY.value = constrained.y
  }

  const zoomAtPoint = (screenX: number, screenY: number, delta: number) => {
    const nextZoom = constrainZoom(zoom.value + delta)
    if (nextZoom === zoom.value) return

    const mapX = (screenX - offsetX.value) / zoom.value
    const mapY = (screenY - offsetY.value) / zoom.value
    const nextOffsetX = screenX - mapX * nextZoom
    const nextOffsetY = screenY - mapY * nextZoom
    const constrained = constrainPan(nextOffsetX, nextOffsetY, nextZoom)

    zoom.value = nextZoom
    offsetX.value = constrained.x
    offsetY.value = constrained.y
  }

  /**
   * Pan the viewport by delta amounts
   * @param deltaX - Horizontal pan distance in pixels
   * @param deltaY - Vertical pan distance in pixels
   */
  const pan = (deltaX: number, deltaY: number) => {
    const constrained = constrainPan(offsetX.value + deltaX, offsetY.value + deltaY)
    offsetX.value = constrained.x
    offsetY.value = constrained.y
  }

  /**
   * Set pan offset to specific values (constrained to boundaries)
   * @param x - Horizontal pan offset in pixels
   * @param y - Vertical pan offset in pixels
   */
  const setPan = (x: number, y: number) => {
    const constrained = constrainPan(x, y)
    offsetX.value = constrained.x
    offsetY.value = constrained.y
  }

  /**
   * Reset zoom and pan to default values
   */
  const reset = () => {
    zoom.value = 1
    const constrained = constrainPan(0, 0, 1)
    offsetX.value = constrained.x
    offsetY.value = constrained.y
  }

  /**
   * Transform map coordinates to screen coordinates
   * Applies zoom and pan transformations
   * 
   * @param x - Map X coordinate
   * @param y - Map Y coordinate
   * @returns Screen coordinates {x, y}
   */
  const mapToScreen = (x: number, y: number): { x: number; y: number } => {
    return {
      x: x * zoom.value + offsetX.value,
      y: y * zoom.value + offsetY.value,
    }
  }

  /**
   * Transform screen coordinates to map coordinates
   * Reverses zoom and pan transformations
   * 
   * @param x - Screen X coordinate
   * @param y - Screen Y coordinate
   * @returns Map coordinates {x, y}
   */
  const screenToMap = (x: number, y: number): { x: number; y: number } => {
    return {
      x: (x - offsetX.value) / zoom.value,
      y: (y - offsetY.value) / zoom.value,
    }
  }

  /**
   * Start drag operation
   * @param screenX - Screen X coordinate where drag started
   * @param screenY - Screen Y coordinate where drag started
   */
  const startDrag = (screenX: number, screenY: number) => {
    isDragging.value = true
    dragStartX.value = screenX
    dragStartY.value = screenY
    dragStartOffsetX.value = offsetX.value
    dragStartOffsetY.value = offsetY.value
  }

  /**
   * Update drag operation
   * @param screenX - Current screen X coordinate
   * @param screenY - Current screen Y coordinate
   */
  const updateDrag = (screenX: number, screenY: number) => {
    if (!isDragging.value) return

    const deltaX = screenX - dragStartX.value
    const deltaY = screenY - dragStartY.value

    const newOffsetX = dragStartOffsetX.value + deltaX
    const newOffsetY = dragStartOffsetY.value + deltaY

    const constrained = constrainPan(newOffsetX, newOffsetY)
    offsetX.value = constrained.x
    offsetY.value = constrained.y
  }

  /**
   * End drag operation
   */
  const endDrag = () => {
    isDragging.value = false
  }

  /**
   * Set map boundaries for pan constraints
   * Should be called when canvas size changes
   * @param canvasWidth - Canvas width in pixels
   * @param canvasHeight - Canvas height in pixels
   */
  const setMapBoundaries = (
    canvasWidth: number,
    canvasHeight: number,
    _mapWidth: number,
    _mapHeight: number,
    padding: number
  ) => {
    viewport = {
      canvasWidth,
      canvasHeight,
      padding,
    }

    const constrained = constrainPan(offsetX.value, offsetY.value)
    offsetX.value = constrained.x
    offsetY.value = constrained.y
  }

  // Watch state changes and persist to localStorage
  watch(state, () => {
    saveState()
  })

  // Load state on mount
  onMounted(() => {
    const initialState = loadState()
    zoom.value = initialState.zoom
    offsetX.value = initialState.offsetX
    offsetY.value = initialState.offsetY
  })

  // Cleanup on unmount
  onUnmounted(() => {
    // Ensure drag is ended if component unmounts during drag
    if (isDragging.value) {
      endDrag()
    }
  })

  return {
    state,
    zoom: computed(() => zoom.value),
    offsetX: computed(() => offsetX.value),
    offsetY: computed(() => offsetY.value),
    zoomIn,
    zoomOut,
    setZoom,
    zoomAtPoint,
    pan,
    setPan,
    reset,
    mapToScreen,
    screenToMap,
    startDrag,
    updateDrag,
    endDrag,
    isDragging: computed(() => isDragging.value),
    setMapBoundaries,
  }
}
