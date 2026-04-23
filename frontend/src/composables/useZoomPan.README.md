# useZoomPan Composable

## Overview

The `useZoomPan` composable provides state management for map zoom and pan functionality in the DispatchSim frontend. It implements Requirements 1.1, 1.2, 1.3, and 1.6 from the map-and-order-enhancements specification.

## Features

### ✅ Requirement 1.1: Zoom Level State (0.1x to 10x constraint)
- Zoom level is constrained between 0.1x (10%) and 10x (1000%)
- Provides `zoomIn()` and `zoomOut()` methods for 10% increments
- Provides `setZoom(level)` for direct zoom control
- All zoom operations automatically constrain values to valid range

### ✅ Requirement 1.2: Pan Offset State (x, y coordinates)
- Tracks pan offset in pixels (offsetX, offsetY)
- Provides `pan(deltaX, deltaY)` method for relative panning
- Supports cumulative pan operations

### ✅ Requirement 1.3: localStorage Persistence
- Automatically saves state to localStorage on every change
- Loads saved state on component mount
- Gracefully handles invalid or missing localStorage data
- Storage key: `dispatch-sim-zoom-pan-state`

### ✅ Requirement 1.6: Coordinate Transformations
- `mapToScreen(x, y)`: Transforms map coordinates to screen coordinates
- `screenToMap(x, y)`: Transforms screen coordinates to map coordinates
- Both methods apply zoom and pan transformations correctly

## API Reference

### State Properties

```typescript
interface ZoomPanState {
  zoom: number      // Current zoom level (0.1 - 10.0)
  offsetX: number   // Pan offset X in pixels
  offsetY: number   // Pan offset Y in pixels
}
```

### Methods

#### `zoomIn(): void`
Increases zoom level by 10% (0.1). Constrained to maximum 10x.

#### `zoomOut(): void`
Decreases zoom level by 10% (0.1). Constrained to minimum 0.1x.

#### `setZoom(level: number): void`
Sets zoom level to specific value. Automatically constrains to [0.1, 10.0] range.

**Parameters:**
- `level`: Target zoom level

#### `pan(deltaX: number, deltaY: number): void`
Pans the viewport by delta amounts.

**Parameters:**
- `deltaX`: Horizontal pan distance in pixels
- `deltaY`: Vertical pan distance in pixels

#### `reset(): void`
Resets zoom to 1.0 and pan offsets to (0, 0).

#### `mapToScreen(x: number, y: number): { x: number; y: number }`
Transforms map coordinates to screen coordinates by applying zoom and pan.

**Formula:**
```
screenX = mapX * zoom + offsetX
screenY = mapY * zoom + offsetY
```

**Parameters:**
- `x`: Map X coordinate
- `y`: Map Y coordinate

**Returns:** Screen coordinates `{ x, y }`

#### `screenToMap(x: number, y: number): { x: number; y: number }`
Transforms screen coordinates to map coordinates by reversing zoom and pan.

**Formula:**
```
mapX = (screenX - offsetX) / zoom
mapY = (screenY - offsetY) / zoom
```

**Parameters:**
- `x`: Screen X coordinate
- `y`: Screen Y coordinate

**Returns:** Map coordinates `{ x, y }`

## Usage Example

```typescript
import { useZoomPan } from '@/composables/useZoomPan'

// In your component
const {
  zoom,
  offsetX,
  offsetY,
  zoomIn,
  zoomOut,
  setZoom,
  pan,
  reset,
  mapToScreen,
  screenToMap,
} = useZoomPan()

// Zoom controls
zoomIn()  // Zoom to 110%
zoomOut() // Zoom to 100%
setZoom(2.5) // Zoom to 250%

// Pan controls
pan(10, 20)  // Pan right 10px, down 20px
pan(-5, -10) // Pan left 5px, up 10px

// Reset
reset() // Back to zoom 1.0, offset (0, 0)

// Coordinate transformations
const screenPos = mapToScreen(100, 100)
const mapPos = screenToMap(200, 200)
```

## Integration with MapVisualization

To integrate with the existing `MapVisualization.vue` component:

```vue
<script setup lang="ts">
import { useZoomPan } from '@/composables/useZoomPan'

const {
  zoom,
  offsetX,
  offsetY,
  zoomIn,
  zoomOut,
  pan,
  mapToScreen,
  screenToMap,
} = useZoomPan()

// Mouse wheel zoom handler
const handleWheel = (event: WheelEvent) => {
  event.preventDefault()
  if (event.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

// Mouse drag pan handler
let isDragging = false
let lastX = 0
let lastY = 0

const handleMouseDown = (event: MouseEvent) => {
  isDragging = true
  lastX = event.clientX
  lastY = event.clientY
}

const handleMouseMove = (event: MouseEvent) => {
  if (!isDragging) return
  
  const deltaX = event.clientX - lastX
  const deltaY = event.clientY - lastY
  
  pan(deltaX, deltaY)
  
  lastX = event.clientX
  lastY = event.clientY
}

const handleMouseUp = () => {
  isDragging = false
}

// Apply transformations to canvas rendering
const renderEntity = (ctx: CanvasRenderingContext2D, entity: any) => {
  const screenPos = mapToScreen(entity.x, entity.y)
  ctx.fillRect(screenPos.x, screenPos.y, 10 * zoom.value, 10 * zoom.value)
}
</script>

<template>
  <canvas
    @wheel="handleWheel"
    @mousedown="handleMouseDown"
    @mousemove="handleMouseMove"
    @mouseup="handleMouseUp"
    @mouseleave="handleMouseUp"
  />
</template>
```

## Testing

The composable includes comprehensive unit tests in `useZoomPan.test.ts`:

- ✅ Default initialization
- ✅ Zoom constraints (0.1 - 10.0)
- ✅ Zoom in/out operations
- ✅ Pan operations
- ✅ Reset functionality
- ✅ Coordinate transformations (map ↔ screen)
- ✅ localStorage persistence
- ✅ localStorage loading on mount
- ✅ Invalid data handling

## Demo Component

A demo component `ZoomPanDemo.vue` is provided to visualize and test the composable:

- Interactive zoom/pan controls
- Real-time state display
- Visual transformation demo with shapes
- Coordinate transformation examples
- localStorage persistence indicator

## Implementation Notes

### Design Decisions

1. **10% Zoom Step**: Chosen for smooth, predictable zoom behavior matching common UI patterns
2. **Reactive State**: Uses Vue 3 `ref` and `computed` for optimal reactivity
3. **localStorage Key**: Namespaced as `dispatch-sim-zoom-pan-state` to avoid conflicts
4. **Graceful Degradation**: Falls back to defaults if localStorage is unavailable or corrupted
5. **Computed Properties**: Exposes state as readonly computed refs to prevent external mutation

### Performance Considerations

- State changes trigger localStorage writes (debouncing may be added if needed)
- Coordinate transformations are pure functions with O(1) complexity
- No memory leaks: uses Vue lifecycle hooks properly

### Future Enhancements

Potential improvements for future tasks:
- Pan boundary constraints (requires map dimensions)
- Zoom to point (zoom centered on cursor position)
- Smooth zoom/pan animations
- Touch gesture support (pinch-to-zoom)
- Minimap integration

## Related Requirements

This composable satisfies the following acceptance criteria from Requirement 1:

- ✅ **1.1**: Zoom level state with 0.1x to 10x constraint
- ✅ **1.2**: Pan offset state (x, y coordinates)
- ✅ **1.3**: localStorage persistence for zoom/pan state
- ✅ **1.6**: Coordinate transformation functions (mapToScreen, screenToMap)

Additional acceptance criteria (1.4, 1.5, 1.7) will be implemented in subsequent tasks:
- 1.4: Mouse wheel zoom handler (Task 1.2)
- 1.5: Mouse drag pan handler (Task 1.2)
- 1.7: Canvas re-rendering (Task 1.3)
