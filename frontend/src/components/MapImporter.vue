<template>
  <div>
    <!-- Import Button -->
    <button
      @click="showModal = true"
      class="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-teal-700 text-white rounded-lg hover:bg-teal-800 transition-smooth cursor-pointer shadow-md"
      aria-label="导入地图"
    >
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
      </svg>
      <span class="text-sm font-medium">导入地图</span>
    </button>

    <!-- Modal -->
    <div
      v-if="showModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
      @click.self="closeModal"
    >
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-2xl mx-4 max-h-[90vh] overflow-hidden">
        <!-- Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <h3 class="text-xl font-heading font-semibold text-text">导入地图数据</h3>
          <button
            @click="closeModal"
            class="p-2 hover:bg-gray-100 rounded-lg transition-smooth cursor-pointer"
            aria-label="关闭"
          >
            <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- Content -->
        <div class="p-6 overflow-y-auto max-h-[calc(90vh-140px)]">
          <!-- Tabs -->
          <div class="flex gap-2 mb-6 border-b border-gray-200">
            <button
              @click="activeTab = 'upload'"
              class="px-4 py-2 text-sm font-medium transition-smooth cursor-pointer"
              :class="activeTab === 'upload' 
                ? 'text-teal-700 border-b-2 border-teal-700' 
                : 'text-gray-500 hover:text-gray-700'"
            >
              上传文件
            </button>
            <button
              @click="activeTab = 'paste'"
              class="px-4 py-2 text-sm font-medium transition-smooth cursor-pointer"
              :class="activeTab === 'paste' 
                ? 'text-teal-700 border-b-2 border-teal-700' 
                : 'text-gray-500 hover:text-gray-700'"
            >
              粘贴 JSON
            </button>
            <button
              @click="activeTab = 'sample'"
              class="px-4 py-2 text-sm font-medium transition-smooth cursor-pointer"
              :class="activeTab === 'sample' 
                ? 'text-teal-700 border-b-2 border-teal-700' 
                : 'text-gray-500 hover:text-gray-700'"
            >
              示例数据
            </button>
          </div>

          <!-- Upload Tab -->
          <div v-if="activeTab === 'upload'" class="space-y-4">
            <div
              class="border-2 border-dashed border-gray-300 rounded-xl p-8 text-center hover:border-teal-700 transition-smooth cursor-pointer"
              @dragover.prevent
              @drop.prevent="handleFileDrop"
              @click="fileInput?.click()"
            >
              <svg class="w-12 h-12 mx-auto text-gray-400 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
              <p class="text-sm text-gray-600 mb-1">拖拽文件到此处或点击上传</p>
              <p class="text-xs text-gray-400">支持 JSON 格式</p>
              <input
                ref="fileInput"
                type="file"
                accept=".json"
                class="hidden"
                @change="handleFileSelect"
              />
            </div>
          </div>

          <!-- Paste Tab -->
          <div v-if="activeTab === 'paste'" class="space-y-4">
            <textarea
              v-model="jsonInput"
              class="w-full h-64 px-4 py-3 border border-gray-300 rounded-xl font-mono text-sm focus:outline-none focus:ring-2 focus:ring-teal-700 focus:border-transparent resize-none"
              placeholder='粘贴 JSON 数据，例如：
{
  "obstacles": [
    {
      "id": "building-1",
      "type": "BUILDING",
      "x": 20,
      "y": 20,
      "width": 30,
      "height": 20,
      "label": "仓库A"
    }
  ]
}'
            ></textarea>
            <button
              @click="importFromJSON"
              class="w-full px-4 py-2 bg-teal-700 text-white rounded-lg hover:bg-teal-800 transition-smooth cursor-pointer"
            >
              导入数据
            </button>
          </div>

          <!-- Sample Tab -->
          <div v-if="activeTab === 'sample'" class="space-y-4">
            <div class="bg-blue-50 border border-blue-200 rounded-xl p-4 mb-4">
              <div class="flex gap-3">
                <svg class="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <div class="text-sm text-blue-800">
                  <p class="font-medium mb-1">示例地图包含：</p>
                  <ul class="list-disc list-inside space-y-1 text-blue-700">
                    <li>3个建筑物（仓库、配送中心、停车场）</li>
                    <li>2条道路</li>
                    <li>1个绿地</li>
                  </ul>
                </div>
              </div>
            </div>

            <div class="bg-gray-50 rounded-xl p-4 max-h-64 overflow-y-auto">
              <pre class="text-xs font-mono text-gray-700">{{ JSON.stringify(sampleData, null, 2) }}</pre>
            </div>

            <button
              @click="loadSampleData"
              class="w-full px-4 py-2 bg-teal-700 text-white rounded-lg hover:bg-teal-800 transition-smooth cursor-pointer"
            >
              加载示例数据
            </button>
          </div>

          <!-- Error Message -->
          <div v-if="errorMessage" class="mt-4 p-4 bg-red-50 border border-red-200 rounded-xl">
            <div class="flex gap-3">
              <svg class="w-5 h-5 text-red-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p class="text-sm text-red-800">{{ errorMessage }}</p>
            </div>
          </div>

          <!-- Success Message -->
          <div v-if="successMessage" class="mt-4 p-4 bg-green-50 border border-green-200 rounded-xl">
            <div class="flex gap-3">
              <svg class="w-5 h-5 text-green-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p class="text-sm text-green-800">{{ successMessage }}</p>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50">
          <button
            @click="closeModal"
            class="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200 rounded-lg transition-smooth cursor-pointer"
          >
            关闭
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Obstacle } from '@/types'

const emit = defineEmits<{
  import: [obstacles: Obstacle[]]
}>()

const showModal = ref(false)
const activeTab = ref<'upload' | 'paste' | 'sample'>('sample')
const jsonInput = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

// Sample data
const sampleData = {
  obstacles: [
    {
      id: 'building-1',
      type: 'BUILDING',
      x: 20,
      y: 60,
      width: 30,
      height: 25,
      label: '仓库A'
    },
    {
      id: 'building-2',
      type: 'BUILDING',
      x: 90,
      y: 65,
      width: 35,
      height: 30,
      label: '配送中心'
    },
    {
      id: 'parking-1',
      type: 'PARKING',
      x: 10,
      y: 10,
      width: 25,
      height: 20,
      label: '停车场'
    },
    {
      id: 'road-1',
      type: 'ROAD',
      x: 0,
      y: 45,
      width: 140,
      height: 8,
      label: '主干道'
    },
    {
      id: 'road-2',
      type: 'ROAD',
      x: 65,
      y: 0,
      width: 8,
      height: 100,
      label: '次干道'
    },
    {
      id: 'green-1',
      type: 'GREEN_SPACE',
      x: 100,
      y: 15,
      width: 20,
      height: 20,
      label: '公园'
    }
  ]
}

const closeModal = () => {
  showModal.value = false
  errorMessage.value = ''
  successMessage.value = ''
  jsonInput.value = ''
}

const validateObstacles = (data: any): Obstacle[] => {
  if (!data || !Array.isArray(data.obstacles)) {
    throw new Error('数据格式错误：需要包含 obstacles 数组')
  }

  const validTypes = ['BUILDING', 'ROAD', 'PARKING', 'GREEN_SPACE']

  return data.obstacles.map((obs: any, index: number) => {
    if (!obs.id || typeof obs.id !== 'string') {
      throw new Error(`障碍物 ${index + 1}: 缺少有效的 id`)
    }
    if (!validTypes.includes(obs.type)) {
      throw new Error(`障碍物 ${index + 1}: 类型必须是 ${validTypes.join(', ')} 之一`)
    }
    if (typeof obs.x !== 'number' || typeof obs.y !== 'number') {
      throw new Error(`障碍物 ${index + 1}: x 和 y 必须是数字`)
    }
    if (typeof obs.width !== 'number' || typeof obs.height !== 'number') {
      throw new Error(`障碍物 ${index + 1}: width 和 height 必须是数字`)
    }

    return {
      id: obs.id,
      type: obs.type,
      x: obs.x,
      y: obs.y,
      width: obs.width,
      height: obs.height,
      label: obs.label || ''
    }
  })
}

const importFromJSON = () => {
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const data = JSON.parse(jsonInput.value)
    const obstacles = validateObstacles(data)
    
    emit('import', obstacles)
    successMessage.value = `成功导入 ${obstacles.length} 个障碍物`
    
    setTimeout(() => {
      closeModal()
    }, 1500)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '导入失败'
  }
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    readFile(file)
  }
}

const handleFileDrop = (event: DragEvent) => {
  const file = event.dataTransfer?.files[0]
  if (file) {
    readFile(file)
  }
}

const readFile = (file: File) => {
  errorMessage.value = ''
  successMessage.value = ''

  if (!file.name.endsWith('.json')) {
    errorMessage.value = '请上传 JSON 格式文件'
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const content = e.target?.result as string
      const data = JSON.parse(content)
      const obstacles = validateObstacles(data)
      
      emit('import', obstacles)
      successMessage.value = `成功导入 ${obstacles.length} 个障碍物`
      
      setTimeout(() => {
        closeModal()
      }, 1500)
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : '文件解析失败'
    }
  }
  reader.onerror = () => {
    errorMessage.value = '文件读取失败'
  }
  reader.readAsText(file)
}

const loadSampleData = () => {
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const obstacles = validateObstacles(sampleData)
    emit('import', obstacles)
    successMessage.value = `成功加载 ${obstacles.length} 个示例障碍物`
    
    setTimeout(() => {
      closeModal()
    }, 1500)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载失败'
  }
}
</script>
