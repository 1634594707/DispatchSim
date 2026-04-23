<template>
  <div class="p-4 space-y-4">
    <div class="bg-white rounded-lg shadow p-4">
      <h2 class="text-lg font-semibold mb-4">Zoom & Pan Controls</h2>
      
      <!-- State Display -->
      <div class="grid grid-cols-3 gap-4 mb-4">
        <div class="bg-gray-50 p-3 rounded">
          <div class="text-sm text-gray-600">Zoom Level</div>
          <div class="text-2xl font-bold">{{ (zoom * 100).toFixed(0) }}%</div>
        </div>
        <div class="bg-gray-50 p-3 rounded">
          <div class="text-sm text-gray-600">Offset X</div>
          <div class="text-2xl font-bold">{{ offsetX.toFixed(0) }}px</div>
        </div>
        <div class="bg-gray-50 p-3 rounded">
          <div class="text-sm text-gray-600">Offset Y</div>
          <div class="text-2xl font-bold">{{ offsetY.toFixed(0) }}px</div>
        </div>
      </div>

      <!-- Controls -->
      <div class="flex flex-wrap gap-2">
        <button
          @click="zoomIn"
          class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition"
        >
          Zoom In (+10%)
        </button>
        <button
          @click="zoomOut"
          class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition"
        >
          Zoom Out (-10%)
        </button>
        <button
          @click="() => pan(10, 0)"
          class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 transition"
        >
          Pan Right
        </button>
        <button
          @click="() => pan(-10, 0)"
          class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 transition"
        >
          Pan Left
        </button>
        <button
          @click="() => pan(0, 10)"
          class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 transition"
        >
          Pan Down
        </button>
        <button
          @click="() => pan(0, -10)"
          class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 transition"
        >
          Pan Up
        </button>
        <button
          @click="reset"
          class="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600 transition"
        >
          Reset
        </button>
      </div>

      <!-- Zoom Slider -->
      <div class="mt-4">
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Zoom Level: {{ (zoom * 100).toFixed(0) }}%
        </label>
        <input
          type="range"
          :value="zoom"
          @input="(e) => setZoom(parseFloat((e.target as HTMLInputElement).value))"
          min="0.1"
          max="10"
          step="0.1"
          class="w-full"
        />
        <div class="flex justify-between text-xs text-gray-500 mt-1">
          <span>10%</span>
          <span>100%</span>
          <span>1000%</span>
        </div>
      </div>
    </div>

    <!-- Visual Demo -->
    <div class="bg-white rounded-lg shadow p-4">
      <h2 class="text-lg font-semibold mb-4">Visual Demo</h2>
      <div
        class="relative bg-gray-100 border-2 border-gray-300 rounded overflow-hidden"
        style="width: 600px; height: 400px;"
      >
        <!-- Grid background -->
        <svg class="absolute inset-0 w-full h-full">
          <defs>
            <pattern id="grid" width="50" height="50" patternUnits="userSpaceOnUse">
              <path d="M 50 0 L 0 0 0 50" fill="none" stroke="gray" stroke-width="0.5" />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#grid)" />
        </svg>

        <!-- Transformed content -->
        <div
          class="absolute"
          :style="{
            transform: `translate(${offsetX}px, ${offsetY}px) scale(${zoom})`,
            transformOrigin: '0 0',
          }"
        >
          <!-- Origin marker -->
          <div class="absolute w-4 h-4 bg-red-500 rounded-full -translate-x-2 -translate-y-2" />
          
          <!-- Sample shapes -->
          <div
            class="absolute w-20 h-20 bg-blue-500 rounded"
            style="left: 50px; top: 50px;"
          />
          <div
            class="absolute w-16 h-16 bg-green-500 rounded-full"
            style="left: 150px; top: 100px;"
          />
          <div
            class="absolute w-24 h-12 bg-purple-500 rounded"
            style="left: 100px; top: 200px;"
          />
        </div>
      </div>

      <!-- Coordinate Transform Demo -->
      <div class="mt-4 grid grid-cols-2 gap-4">
        <div class="bg-gray-50 p-3 rounded">
          <div class="text-sm font-medium text-gray-700 mb-2">Map → Screen</div>
          <div class="text-xs text-gray-600">
            Map (100, 100) → Screen {{ JSON.stringify(mapToScreen(100, 100)) }}
          </div>
        </div>
        <div class="bg-gray-50 p-3 rounded">
          <div class="text-sm font-medium text-gray-700 mb-2">Screen → Map</div>
          <div class="text-xs text-gray-600">
            Screen (200, 200) → Map {{ JSON.stringify(screenToMap(200, 200)) }}
          </div>
        </div>
      </div>
    </div>

    <!-- localStorage Info -->
    <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
      <div class="flex items-start">
        <svg class="w-5 h-5 text-blue-500 mt-0.5 mr-2" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
        </svg>
        <div>
          <div class="text-sm font-medium text-blue-800">State Persistence</div>
          <div class="text-sm text-blue-700 mt-1">
            Zoom and pan state is automatically saved to localStorage. Refresh the page to see it restored!
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useZoomPan } from '@/composables/useZoomPan'

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
</script>
