<template>
  <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="$emit('close')">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md mx-4">
      <!-- Header -->
      <div class="flex items-center justify-between p-4 border-b border-gray-200">
        <h3 class="text-lg font-semibold text-text">生成网格路网</h3>
        <button
          @click="$emit('close')"
          class="p-1 hover:bg-gray-100 rounded transition-colors cursor-pointer"
          aria-label="关闭"
        >
          <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Content -->
      <div class="p-4 space-y-4">
        <!-- Grid Size -->
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">行数</label>
            <input
              v-model.number="options.rows"
              type="number"
              min="2"
              max="20"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">列数</label>
            <input
              v-model.number="options.cols"
              type="number"
              min="2"
              max="20"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            />
          </div>
        </div>

        <!-- Spacing -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">
            节点间距 (米)
          </label>
          <input
            v-model.number="options.spacing"
            type="number"
            min="5"
            max="50"
            step="5"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
          />
        </div>

        <!-- Offset -->
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">起始 X</label>
            <input
              v-model.number="options.offsetX"
              type="number"
              min="0"
              :max="MAP_WIDTH"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">起始 Y</label>
            <input
              v-model.number="options.offsetY"
              type="number"
              min="0"
              :max="MAP_HEIGHT"
              class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            />
          </div>
        </div>

        <!-- Bidirectional -->
        <div>
          <label class="flex items-center gap-2 cursor-pointer">
            <input
              v-model="options.bidirectional"
              type="checkbox"
              class="w-4 h-4 text-primary border-gray-300 rounded focus:ring-primary"
            />
            <span class="text-sm text-gray-700">双向道路</span>
          </label>
        </div>

        <!-- Preview Info -->
        <div class="bg-gray-50 rounded-lg p-3 text-sm">
          <p class="text-gray-700 mb-1">预览:</p>
          <ul class="text-gray-600 space-y-1">
            <li>• 节点数: <span class="font-mono font-semibold">{{ previewNodeCount }}</span></li>
            <li>• 边数: <span class="font-mono font-semibold">{{ previewEdgeCount }}</span></li>
            <li>• 网格大小: <span class="font-mono font-semibold">{{ gridWidth }}m × {{ gridHeight }}m</span></li>
          </ul>
        </div>
      </div>

      <!-- Footer -->
      <div class="flex items-center justify-end gap-2 p-4 border-t border-gray-200">
        <button
          @click="$emit('close')"
          class="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer"
        >
          取消
        </button>
        <button
          @click="handleGenerate"
          class="px-4 py-2 bg-primary hover:bg-primary/90 text-white rounded-lg transition-colors cursor-pointer"
        >
          生成
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { MAP_WIDTH, MAP_HEIGHT } from '@/constants/map'

interface GridGeneratorOptions {
  rows: number
  cols: number
  spacing: number
  offsetX: number
  offsetY: number
  bidirectional: boolean
}

const emit = defineEmits<{
  'generate': [options: GridGeneratorOptions]
  'close': []
}>()

// Default options
const options = ref<GridGeneratorOptions>({
  rows: 5,
  cols: 5,
  spacing: 10,
  offsetX: 20,
  offsetY: 20,
  bidirectional: true,
})

// Computed preview values
const previewNodeCount = computed(() => {
  return options.value.rows * options.value.cols
})

const previewEdgeCount = computed(() => {
  const { rows, cols, bidirectional } = options.value
  const horizontalEdges = rows * (cols - 1)
  const verticalEdges = (rows - 1) * cols
  const totalEdges = horizontalEdges + verticalEdges
  return bidirectional ? totalEdges : totalEdges
})

const gridWidth = computed(() => {
  return (options.value.cols - 1) * options.value.spacing
})

const gridHeight = computed(() => {
  return (options.value.rows - 1) * options.value.spacing
})

// Methods
const handleGenerate = () => {
  emit('generate', { ...options.value })
}
</script>
