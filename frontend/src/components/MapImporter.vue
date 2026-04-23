<template>
  <div>
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

    <div
      v-if="showModal"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
      @click.self="closeModal"
    >
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-2xl mx-4 max-h-[90vh] overflow-hidden">
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

        <div class="p-6 overflow-y-auto max-h-[calc(90vh-140px)]">
          <div class="flex gap-2 mb-6 border-b border-gray-200">
            <button
              @click="activeTab = 'upload'"
              class="px-4 py-2 text-sm font-medium transition-smooth cursor-pointer"
              :class="activeTab === 'upload' ? 'text-teal-700 border-b-2 border-teal-700' : 'text-gray-500 hover:text-gray-700'"
            >
              上传文件
            </button>
            <button
              @click="activeTab = 'paste'"
              class="px-4 py-2 text-sm font-medium transition-smooth cursor-pointer"
              :class="activeTab === 'paste' ? 'text-teal-700 border-b-2 border-teal-700' : 'text-gray-500 hover:text-gray-700'"
            >
              粘贴 JSON
            </button>
            <button
              @click="activeTab = 'sample'"
              class="px-4 py-2 text-sm font-medium transition-smooth cursor-pointer"
              :class="activeTab === 'sample' ? 'text-teal-700 border-b-2 border-teal-700' : 'text-gray-500 hover:text-gray-700'"
            >
              示例数据
            </button>
          </div>

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

          <div v-if="activeTab === 'paste'" class="space-y-4">
            <textarea
              v-model="jsonInput"
              class="w-full h-64 px-4 py-3 border border-gray-300 rounded-xl font-mono text-sm focus:outline-none focus:ring-2 focus:ring-teal-700 focus:border-transparent resize-none"
              :placeholder="jsonPlaceholder"
            ></textarea>
            <button
              @click="importFromJSON"
              class="w-full px-4 py-2 bg-teal-700 text-white rounded-lg hover:bg-teal-800 transition-smooth cursor-pointer"
            >
              导入数据
            </button>
          </div>

          <div v-if="activeTab === 'sample'" class="space-y-4">
            <div class="bg-blue-50 border border-blue-200 rounded-xl p-4 mb-4">
              <div class="flex gap-3">
                <svg class="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <div class="text-sm text-blue-800">
                  <p class="font-medium mb-1">示例园区包含：</p>
                  <ul class="list-disc list-inside space-y-1 text-blue-700">
                    <li>环形主路、中央脊路、门岗接入路</li>
                    <li>仓库区、交叉转运区、分拨中心、行政后勤区</li>
                    <li>访客停车、车辆待发区、员工停车与雨水花园</li>
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

          <div v-if="errorMessage" class="mt-4 p-4 bg-red-50 border border-red-200 rounded-xl">
            <div class="flex gap-3">
              <svg class="w-5 h-5 text-red-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p class="text-sm text-red-800">{{ errorMessage }}</p>
            </div>
          </div>

          <div v-if="successMessage" class="mt-4 p-4 bg-green-50 border border-green-200 rounded-xl">
            <div class="flex gap-3">
              <svg class="w-5 h-5 text-green-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p class="text-sm text-green-800">{{ successMessage }}</p>
            </div>
          </div>
        </div>

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

const jsonPlaceholder = `{
  "obstacles": [
    {
      "id": "building-1",
      "type": "BUILDING",
      "x": 40,
      "y": 120,
      "width": 60,
      "height": 30,
      "label": "Warehouse A"
    }
  ]
}`

const sampleData = {
  obstacles: [
    { id: 'west-loop-north', type: 'ROAD', x: 100, y: 610, width: 360, height: 16, label: 'West Campus North Loop' },
    { id: 'west-loop-south', type: 'ROAD', x: 100, y: 330, width: 360, height: 16, label: 'West Campus South Loop' },
    { id: 'west-loop-west', type: 'ROAD', x: 100, y: 330, width: 16, height: 296, label: 'West Campus West Loop' },
    { id: 'west-loop-east', type: 'ROAD', x: 444, y: 330, width: 16, height: 296, label: 'West Campus East Loop' },
    { id: 'west-spine', type: 'ROAD', x: 268, y: 330, width: 20, height: 296, label: 'West Campus Spine' },
    { id: 'gate-road', type: 'ROAD', x: 0, y: 466, width: 116, height: 12, label: 'Gate Access' },
    { id: 'intercity-link', type: 'ROAD', x: 460, y: 392, width: 360, height: 24, label: 'Inter-District Connector' },
    { id: 'east-loop-north', type: 'ROAD', x: 820, y: 610, width: 260, height: 16, label: 'Remote District North Loop' },
    { id: 'east-loop-south', type: 'ROAD', x: 820, y: 330, width: 260, height: 16, label: 'Remote District South Loop' },
    { id: 'east-loop-west', type: 'ROAD', x: 820, y: 330, width: 16, height: 296, label: 'Remote District West Loop' },
    { id: 'east-loop-east', type: 'ROAD', x: 1064, y: 330, width: 16, height: 296, label: 'Remote District East Loop' },
    { id: 'east-spine', type: 'ROAD', x: 946, y: 330, width: 20, height: 296, label: 'Remote District Spine' },

    { id: 'security-gatehouse', type: 'BUILDING', x: 24, y: 486, width: 38, height: 46, label: 'Security Gatehouse' },
    { id: 'warehouse-a', type: 'BUILDING', x: 150, y: 500, width: 120, height: 84, label: 'Warehouse A' },
    { id: 'warehouse-b', type: 'BUILDING', x: 294, y: 500, width: 120, height: 84, label: 'Warehouse B' },
    { id: 'cross-dock', type: 'BUILDING', x: 150, y: 370, width: 264, height: 82, label: 'Cross Dock' },
    { id: 'admin-office', type: 'BUILDING', x: 168, y: 232, width: 96, height: 62, label: 'Admin Office' },
    { id: 'fleet-workshop', type: 'BUILDING', x: 288, y: 232, width: 104, height: 62, label: 'Fleet Workshop' },
    { id: 'charging-hub', type: 'BUILDING', x: 406, y: 232, width: 42, height: 62, label: 'Charging Hub' },
    { id: 'remote-hub', type: 'BUILDING', x: 864, y: 490, width: 180, height: 96, label: 'Remote Delivery Hub' },
    { id: 'retail-center', type: 'BUILDING', x: 864, y: 368, width: 180, height: 76, label: 'Retail Fulfillment Center' },
    { id: 'district-office', type: 'BUILDING', x: 864, y: 232, width: 90, height: 60, label: 'District Office' },
    { id: 'service-center', type: 'BUILDING', x: 976, y: 232, width: 68, height: 60, label: 'Service Point' },

    { id: 'truck-staging', type: 'PARKING', x: 132, y: 130, width: 220, height: 56, label: 'Truck Staging Yard' },
    { id: 'visitor-parking', type: 'PARKING', x: 370, y: 130, width: 88, height: 56, label: 'Visitor Parking' },
    { id: 'remote-parking', type: 'PARKING', x: 852, y: 130, width: 200, height: 58, label: 'Remote Fleet Parking' },

    { id: 'west-buffer', type: 'GREEN_SPACE', x: 112, y: 660, width: 160, height: 52, label: 'Landscape Buffer' },
    { id: 'rain-garden', type: 'GREEN_SPACE', x: 302, y: 660, width: 146, height: 52, label: 'Rain Garden' },
    { id: 'district-pond', type: 'GREEN_SPACE', x: 1088, y: 336, width: 56, height: 220, label: 'Retention Pond' }
  ]
}

const closeModal = () => {
  showModal.value = false
  errorMessage.value = ''
  successMessage.value = ''
  jsonInput.value = ''
}

const validateObstacles = (data: unknown): Obstacle[] => {
  if (!data || typeof data !== 'object' || !Array.isArray((data as { obstacles?: unknown[] }).obstacles)) {
    throw new Error('数据格式错误：需要包含 obstacles 数组')
  }

  const validTypes = ['BUILDING', 'ROAD', 'PARKING', 'GREEN_SPACE']

  return (data as { obstacles: Record<string, unknown>[] }).obstacles.map((obs, index) => {
    if (typeof obs.id !== 'string' || !obs.id) {
      throw new Error(`障碍物 ${index + 1}: 缺少有效 id`)
    }
    if (typeof obs.type !== 'string' || !validTypes.includes(obs.type)) {
      throw new Error(`障碍物 ${index + 1}: type 必须是 ${validTypes.join(', ')} 之一`)
    }
    if (typeof obs.x !== 'number' || typeof obs.y !== 'number') {
      throw new Error(`障碍物 ${index + 1}: x 和 y 必须为数字`)
    }
    if (typeof obs.width !== 'number' || typeof obs.height !== 'number') {
      throw new Error(`障碍物 ${index + 1}: width 和 height 必须为数字`)
    }

    return {
      id: obs.id,
      type: obs.type as Obstacle['type'],
      x: obs.x,
      y: obs.y,
      width: obs.width,
      height: obs.height,
      label: typeof obs.label === 'string' ? obs.label : ''
    }
  })
}

const emitImportResult = (obstacles: Obstacle[], actionText: string) => {
  emit('import', obstacles)
  successMessage.value = `${actionText} ${obstacles.length} 个地图元素`
  window.setTimeout(() => {
    closeModal()
  }, 1500)
}

const importFromJSON = () => {
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const data = JSON.parse(jsonInput.value)
    emitImportResult(validateObstacles(data), '成功导入')
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
  const file = event.dataTransfer?.files?.[0]
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
  reader.onload = (event) => {
    try {
      const content = event.target?.result as string
      const data = JSON.parse(content)
      emitImportResult(validateObstacles(data), '成功导入')
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
    emitImportResult(validateObstacles(sampleData), '成功加载')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '加载失败'
  }
}
</script>
