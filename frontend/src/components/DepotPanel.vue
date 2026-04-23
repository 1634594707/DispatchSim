<template>
  <div class="bg-white rounded-2xl shadow-lg border border-gray-200 p-4 h-full flex flex-col">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-text">出货点管理</h3>
      <button
        class="px-3 py-1.5 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors text-sm font-medium flex items-center gap-1"
        @click="showCreateModal = true"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        新建出货点
      </button>
    </div>

    <!-- Search/Filter -->
    <div class="mb-4">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="搜索出货点名称..."
        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 text-sm"
      />
    </div>

    <!-- Depot List -->
    <div class="flex-1 overflow-y-auto space-y-2">
      <div
        v-for="depot in filteredDepots"
        :key="depot.id"
        class="p-3 border border-gray-200 rounded-lg hover:border-primary/50 transition-colors cursor-pointer"
        @click="selectDepot(depot)"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-xl">{{ depot.icon || '📍' }}</span>
              <h4 class="font-semibold text-text">{{ depot.name }}</h4>
            </div>
            <div class="text-xs text-gray-600 space-y-0.5">
              <div>位置: ({{ depot.position.x.toFixed(1) }}, {{ depot.position.y.toFixed(1) }})</div>
              <div>待处理订单: {{ depot.pendingOrderCount || 0 }}</div>
            </div>
          </div>
          <div class="flex gap-1">
            <button
              class="p-1.5 hover:bg-gray-100 rounded transition-colors"
              title="编辑"
              @click.stop="editDepot(depot)"
            >
              <svg class="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </button>
            <button
              class="p-1.5 hover:bg-red-50 rounded transition-colors"
              title="删除"
              @click.stop="confirmDelete(depot)"
            >
              <svg class="w-4 h-4 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div v-if="filteredDepots.length === 0" class="text-center py-8 text-gray-500 text-sm">
        {{ searchQuery ? '未找到匹配的出货点' : '暂无出货点' }}
      </div>
    </div>

    <!-- CSV Import/Export -->
    <div class="mt-4 pt-4 border-t border-gray-200 flex gap-2">
      <button
        class="flex-1 px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm font-medium flex items-center justify-center gap-1"
        @click="triggerImport"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
        </svg>
        导入CSV
      </button>
      <button
        class="flex-1 px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm font-medium flex items-center justify-center gap-1"
        @click="exportCSV"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10" />
        </svg>
        导出CSV
      </button>
    </div>

    <!-- Hidden file input for CSV import -->
    <input
      ref="fileInputRef"
      type="file"
      accept=".csv"
      class="hidden"
      @change="handleFileImport"
    />

    <!-- Create/Edit Modal -->
    <DepotModal
      v-if="showCreateModal || showEditModal"
      :depot="editingDepot"
      :mode="showEditModal ? 'edit' : 'create'"
      @close="closeModal"
      @save="handleSave"
    />

    <!-- Delete Confirmation Modal -->
    <ConfirmModal
      v-if="showDeleteModal"
      title="确认删除"
      :message="`确定要删除出货点 &quot;${deletingDepot?.name}&quot; 吗？${deletingDepot?.pendingOrderCount ? `该出货点还有 ${deletingDepot.pendingOrderCount} 个待处理订单。` : ''}`"
      confirm-text="删除"
      cancel-text="取消"
      :danger="true"
      @confirm="handleDelete"
      @cancel="showDeleteModal = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useDepotStore } from '@/stores/depot'
import type { Depot } from '@/types'
import DepotModal from './DepotModal.vue'
import ConfirmModal from './ConfirmModal.vue'

const depotStore = useDepotStore()

// State
const searchQuery = ref('')
const showCreateModal = ref(false)
const showEditModal = ref(false)
const showDeleteModal = ref(false)
const editingDepot = ref<Depot | null>(null)
const deletingDepot = ref<Depot | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

// Computed
const filteredDepots = computed(() => {
  if (!searchQuery.value) {
    return depotStore.depots
  }
  const query = searchQuery.value.toLowerCase()
  return depotStore.depots.filter(depot =>
    depot.name.toLowerCase().includes(query)
  )
})

// Methods
const selectDepot = (depot: Depot) => {
  console.log('Selected depot:', depot)
  // TODO: Emit event or navigate to depot details
}

const editDepot = (depot: Depot) => {
  editingDepot.value = { ...depot }
  showEditModal.value = true
}

const confirmDelete = (depot: Depot) => {
  deletingDepot.value = depot
  showDeleteModal.value = true
}

const handleDelete = async () => {
  if (!deletingDepot.value) return

  try {
    await depotStore.deleteDepot(deletingDepot.value.id)
    showDeleteModal.value = false
    deletingDepot.value = null
  } catch (error) {
    console.error('Failed to delete depot:', error)
  }
}

const closeModal = () => {
  showCreateModal.value = false
  showEditModal.value = false
  editingDepot.value = null
}

const handleSave = async (depot: Omit<Depot, 'id' | 'createdAt'> | Depot) => {
  try {
    if ('id' in depot) {
      // Edit existing depot
      await depotStore.updateDepot(depot.id, depot)
    } else {
      // Create new depot
      await depotStore.createDepot(depot)
    }
    closeModal()
  } catch (error) {
    console.error('Failed to save depot:', error)
  }
}

const triggerImport = () => {
  fileInputRef.value?.click()
}

const handleFileImport = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  try {
    const text = await file.text()
    const lines = text.split('\n').filter(line => line.trim())
    
    // Skip header
    const dataLines = lines.slice(1)
    
    for (const line of dataLines) {
      const [name, x, y, icon] = line.split(',').map(s => s.trim())
      if (name && x && y) {
        await depotStore.createDepot({
          name,
          position: { x: parseFloat(x), y: parseFloat(y) },
          icon: icon || '📍',
        })
      }
    }
    
    // Reset file input
    if (target) target.value = ''
  } catch (error) {
    console.error('Failed to import CSV:', error)
  }
}

const exportCSV = () => {
  const headers = ['名称', 'X坐标', 'Y坐标', '图标']
  const rows = depotStore.depots.map(depot => [
    depot.name,
    depot.position.x,
    depot.position.y,
    depot.icon || '📍',
  ])
  
  const csv = [
    headers.join(','),
    ...rows.map(row => row.join(','))
  ].join('\n')
  
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `depots_${new Date().toISOString().split('T')[0]}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}
</script>
