<template>
  <div class="absolute top-3 right-3 bg-white/95 backdrop-blur-md rounded-xl shadow-lg border border-gray-200 z-30 w-64 max-h-[calc(100%-1.5rem)] overflow-y-auto">
    <div class="flex items-center justify-between p-3 border-b border-gray-200">
      <h3 class="text-sm font-semibold text-text">地图编辑器</h3>
      <button
        class="p-1 hover:bg-gray-100 rounded transition-colors cursor-pointer"
        @click="collapsed = !collapsed"
        :aria-label="collapsed ? '展开' : '收起'"
      >
        <svg
          class="w-4 h-4 text-gray-600 transition-transform"
          :class="{ 'rotate-180': collapsed }"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
    </div>

    <div v-if="!collapsed" class="p-3 space-y-3">
      <div>
        <label class="block text-xs font-medium text-gray-700 mb-2">编辑模式</label>
        <div class="grid grid-cols-3 gap-1">
          <button
            v-for="mode in modes"
            :key="mode.value"
            @click="currentMode = mode.value"
            class="px-2 py-1.5 text-xs rounded-lg transition-colors cursor-pointer flex flex-col items-center gap-1"
            :class="currentMode === mode.value ? 'bg-primary text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
            :title="mode.description"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="mode.icon" />
            </svg>
            <span>{{ mode.label }}</span>
          </button>
        </div>
      </div>

      <div class="bg-gray-50 rounded-lg p-2 text-xs">
        <div class="flex justify-between mb-1">
          <span class="text-gray-600">节点数:</span>
          <span class="font-mono font-semibold">{{ nodeCount }}</span>
        </div>
        <div class="flex justify-between mb-1">
          <span class="text-gray-600">边数:</span>
          <span class="font-mono font-semibold">{{ edgeCount }}</span>
        </div>
        <div class="flex justify-between mb-1">
          <span class="text-gray-600">车辆数:</span>
          <span class="font-mono font-semibold">{{ vehicleCount }}</span>
        </div>
        <div class="flex justify-between mb-1">
          <span class="text-gray-600">已选择:</span>
          <span class="font-mono font-semibold">{{ selectedCount }}</span>
        </div>
        <div class="flex gap-1 mt-1 pt-1 border-t border-gray-200">
          <button
            @click="roadNetworkStore.undo()"
            :disabled="!canUndo"
            class="flex-1 px-2 py-1 text-xs rounded transition-colors cursor-pointer"
            :class="canUndo ? 'bg-gray-200 hover:bg-gray-300 text-gray-700' : 'bg-gray-100 text-gray-400 cursor-not-allowed'"
          >
            撤销
          </button>
          <button
            @click="roadNetworkStore.redo()"
            :disabled="!canRedo"
            class="flex-1 px-2 py-1 text-xs rounded transition-colors cursor-pointer"
            :class="canRedo ? 'bg-gray-200 hover:bg-gray-300 text-gray-700' : 'bg-gray-100 text-gray-400 cursor-not-allowed'"
          >
            重做
          </button>
        </div>
      </div>

      <div class="bg-blue-50 border border-blue-200 rounded-lg p-2 text-xs text-blue-800">
        <p class="font-medium mb-1">{{ currentModeInstructions.title }}</p>
        <p class="text-blue-700">{{ currentModeInstructions.description }}</p>
      </div>

      <div class="space-y-2">
        <button
          @click="showGridGenerator = true"
          class="w-full px-3 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-medium rounded-lg transition-colors cursor-pointer flex items-center justify-center gap-2"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM14 5a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1V5zM4 15a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1v-4zM14 15a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z" />
          </svg>
          生成网格
        </button>

        <div class="grid grid-cols-2 gap-2">
          <button
            @click="fileInput?.click()"
            class="px-3 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-medium rounded-lg transition-colors cursor-pointer"
          >
            导入 GeoJSON
          </button>
          <button
            @click="handleExport"
            class="px-3 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-medium rounded-lg transition-colors cursor-pointer"
          >
            导出 GeoJSON
          </button>
        </div>

        <button
          @click="roadNetworkStore.resetToDefaultNetwork()"
          class="w-full px-3 py-2 bg-amber-50 hover:bg-amber-100 text-amber-700 text-xs font-medium rounded-lg transition-colors cursor-pointer"
        >
          重置默认路网
        </button>

        <button
          @click="handleClearAll"
          class="w-full px-3 py-2 bg-red-50 hover:bg-red-100 text-red-700 text-xs font-medium rounded-lg transition-colors cursor-pointer"
        >
          清空路网
        </button>
      </div>
    </div>

    <GridGeneratorModal
      v-if="showGridGenerator"
      @generate="handleGenerateGrid"
      @close="showGridGenerator = false"
    />

    <input
      ref="fileInput"
      type="file"
      accept=".json,.geojson"
      class="hidden"
      @change="handleFileSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import GridGeneratorModal from './GridGeneratorModal.vue'
import { useRoadNetworkStore } from '@/stores/roadNetwork'
import { useVehicleStore } from '@/stores/vehicle'

export interface EditorMode {
  value: 'view' | 'add-node' | 'add-edge' | 'select' | 'move' | 'delete' | 'add-vehicle' | 'move-vehicle' | 'delete-vehicle'
  label: string
  icon: string
  description: string
}

interface GridGeneratorOptions {
  rows: number
  cols: number
  spacing: number
  offsetX: number
  offsetY: number
  bidirectional: boolean
}

const roadNetworkStore = useRoadNetworkStore()
const vehicleStore = useVehicleStore()

const collapsed = ref(true)
const currentMode = ref<EditorMode['value']>('view')
const showGridGenerator = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const modes: EditorMode[] = [
  { value: 'view', label: '查看', icon: 'M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z', description: '浏览地图、车辆和路网布局' },
  { value: 'add-node', label: '节点', icon: 'M12 4v16m8-8H4', description: '点击地图添加路网节点' },
  { value: 'add-edge', label: '边', icon: 'M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1', description: '依次点击两个节点创建道路边' },
  { value: 'select', label: '选择', icon: 'M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5M7.188 2.239l.777 2.897M5.136 7.965l-2.898-.777M13.95 4.05l-2.122 2.122m-5.657 5.656l-2.12 2.122', description: '点击或框选节点与边，支持 Ctrl 多选' },
  { value: 'move', label: '移点', icon: 'M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4', description: '拖动路网节点调整道路位置' },
  { value: 'delete', label: '删点', icon: 'M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16', description: '点击节点或边删除路网元素' },
  { value: 'add-vehicle', label: '加车', icon: 'M3 13l1-3h16l1 3M5 13v4a1 1 0 001 1h1a1 1 0 001-1v-1h8v1a1 1 0 001 1h1a1 1 0 001-1v-4M7 13h10M7 10l1-3h8l1 3', description: '点击地图并吸附到最近路网节点，新增无人车' },
  { value: 'move-vehicle', label: '挪车', icon: 'M8 7V3m0 0L5 6m3-3l3 3m8 11v4m0 0l-3-3m3 3l3-3M3 12h18', description: '拖动车辆到其他节点，释放时自动吸附' },
  { value: 'delete-vehicle', label: '删车', icon: 'M9 7h6m-7 3l1 8h6l1-8M10 10v5m4-5v5M6 7l1-2h10l1 2', description: '点击车辆即可删除' },
]

const nodeCount = computed(() => roadNetworkStore.nodeCount)
const edgeCount = computed(() => roadNetworkStore.edgeCount)
const vehicleCount = computed(() => vehicleStore.vehicleCount)
const selectedCount = computed(() => roadNetworkStore.selectedCount)
const canUndo = computed(() => roadNetworkStore.canUndo)
const canRedo = computed(() => roadNetworkStore.canRedo)

const currentModeInstructions = computed(() => {
  const mode = modes.find((item) => item.value === currentMode.value)
  return {
    title: mode?.label || '',
    description: mode?.description || '',
  }
})

watch(currentMode, (mode) => {
  roadNetworkStore.setEditorMode(mode)
}, { immediate: true })

const handleGenerateGrid = (options: GridGeneratorOptions) => {
  roadNetworkStore.generateGrid(options)
  showGridGenerator.value = false
}

const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) {
    return
  }

  try {
    const geojson = JSON.parse(await file.text())
    roadNetworkStore.importFromGeoJSON(geojson)
  } catch (error) {
    console.error('GeoJSON import failed:', error)
    alert('GeoJSON 文件格式错误，请检查文件内容')
  } finally {
    target.value = ''
  }
}

const handleExport = () => {
  const geojson = roadNetworkStore.exportToGeoJSON()
  const blob = new Blob([JSON.stringify(geojson, null, 2)], { type: 'application/geo+json' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = 'road-network.geojson'
  anchor.click()
  URL.revokeObjectURL(url)
}

const handleClearAll = () => {
  if (confirm('确定要清空整个路网吗？')) {
    roadNetworkStore.clearNetwork()
  }
}
</script>
