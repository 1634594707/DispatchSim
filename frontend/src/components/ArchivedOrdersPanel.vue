<template>
  <div class="bg-white rounded-2xl shadow-lg border border-gray-200 p-4 h-full flex flex-col">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-lg font-semibold text-text">已归档订单</h3>
      <span class="text-sm text-gray-600">共 {{ orderStore.archivedOrders.length }} 个</span>
    </div>

    <!-- Filters -->
    <div class="mb-4 space-y-2">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="搜索订单编号..."
        class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 text-sm"
      />
      
      <div class="flex gap-2">
        <select
          v-model="filterReason"
          class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 text-sm"
        >
          <option value="">所有原因</option>
          <option value="completed">已完成</option>
          <option value="cancelled">已取消</option>
          <option value="expired">已过期</option>
        </select>
        
        <select
          v-model="sortBy"
          class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 text-sm"
        >
          <option value="archivedAt">归档时间</option>
          <option value="createdAt">创建时间</option>
          <option value="id">订单编号</option>
        </select>
      </div>
    </div>

    <!-- Order List -->
    <div class="flex-1 overflow-y-auto space-y-2">
      <div
        v-for="order in filteredOrders"
        :key="order.id"
        class="p-3 border border-gray-200 rounded-lg hover:border-primary/50 transition-colors"
      >
        <div class="flex items-start justify-between mb-2">
          <div>
            <div class="font-semibold text-text">订单 #{{ order.id }}</div>
            <div class="text-xs text-gray-600">
              归档于: {{ formatDate(order.archivedAt) }}
            </div>
          </div>
          <button
            class="px-2 py-1 text-xs bg-primary text-white rounded hover:bg-primary/90 transition-colors"
            @click="handleRestore(order)"
          >
            恢复
          </button>
        </div>

        <div class="text-xs text-gray-600 space-y-1">
          <div>状态: {{ getStatusText(order.status) }}</div>
          <div>优先级: {{ order.priority }}/10</div>
          <div v-if="order.archivalReason">原因: {{ order.archivalReason }}</div>
          <div>取货: ({{ order.pickup.x.toFixed(1) }}, {{ order.pickup.y.toFixed(1) }})</div>
          <div>送货: ({{ order.delivery.x.toFixed(1) }}, {{ order.delivery.y.toFixed(1) }})</div>
        </div>
      </div>

      <div v-if="filteredOrders.length === 0" class="text-center py-8 text-gray-500 text-sm">
        {{ searchQuery || filterReason ? '未找到匹配的订单' : '暂无归档订单' }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useOrderStore } from '@/stores/order'
import type { Order } from '@/types'

const orderStore = useOrderStore()

// State
const searchQuery = ref('')
const filterReason = ref('')
const sortBy = ref<'archivedAt' | 'createdAt' | 'id'>('archivedAt')

// Computed
const filteredOrders = computed(() => {
  let filtered = orderStore.archivedOrders

  // Search filter
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(order =>
      order.id.toString().includes(query)
    )
  }

  // Reason filter
  if (filterReason.value) {
    filtered = filtered.filter(order => {
      if (filterReason.value === 'completed') return order.status === 'COMPLETED'
      if (filterReason.value === 'cancelled') return order.status === 'CANCELLED'
      if (filterReason.value === 'expired') return order.archivalReason?.includes('过期')
      return true
    })
  }

  // Sort
  filtered = [...filtered].sort((a, b) => {
    if (sortBy.value === 'archivedAt') {
      return (b.archivedAt?.getTime() || 0) - (a.archivedAt?.getTime() || 0)
    } else if (sortBy.value === 'createdAt') {
      return b.createdAt.getTime() - a.createdAt.getTime()
    } else {
      return b.id - a.id
    }
  })

  return filtered
})

// Methods
const handleRestore = async (order: Order) => {
  try {
    await orderStore.restoreOrder(order.id)
  } catch (error) {
    console.error('Failed to restore order:', error)
  }
}

const formatDate = (date?: Date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    CREATED: '已创建',
    PENDING: '待分配',
    ASSIGNED: '已分配',
    DELIVERING: '配送中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
  }
  return texts[status] || status
}
</script>
