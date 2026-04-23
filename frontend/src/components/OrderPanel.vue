<template>
  <div class="h-full overflow-y-auto p-4">
    <!-- Filter Tabs -->
    <div class="flex gap-2 mb-4 overflow-x-auto">
      <button
        v-for="filter in filters"
        :key="filter.value"
        @click="activeFilter = filter.value"
        class="px-3 py-1.5 text-sm font-medium rounded-lg whitespace-nowrap transition-smooth cursor-pointer"
        :class="activeFilter === filter.value
          ? 'bg-primary text-white'
          : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
      >
        {{ filter.label }} ({{ getFilteredCount(filter.value) }})
      </button>
    </div>

    <Loading v-if="orderStore.loading && orderStore.orders.length === 0" label="正在加载订单..." centered />

    <div
      v-else-if="orderStore.error"
      class="mb-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"
    >
      {{ orderStore.error }}
    </div>

    <!-- Order List -->
    <div v-else-if="filteredOrders.length > 0" class="space-y-3">
      <div
        v-for="order in filteredOrders"
        :key="order.id"
        class="bg-white border border-gray-200 rounded-lg p-3 hover:shadow-md transition-smooth cursor-pointer"
        @click="toggleOrderDetails(order.id)"
      >
        <!-- Order Header -->
        <div class="flex items-start justify-between mb-2">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-sm font-semibold text-text">订单 #{{ order.id }}</span>
              <span
                class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium"
                :class="getStatusBadgeClass(order.status)"
              >
                <span class="w-1.5 h-1.5 rounded-full" :class="getStatusDotClass(order.status)"></span>
                {{ getStatusText(order.status) }}
              </span>
            </div>
            <div class="text-xs text-gray-600">
              优先级: {{ order.priority }} / 10
            </div>
          </div>
          <svg
            class="w-5 h-5 text-gray-400 transition-transform"
            :class="{ 'rotate-180': expandedOrders.has(order.id) }"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </div>

        <!-- Order Details (Expandable) -->
        <div v-if="expandedOrders.has(order.id)" class="mt-3 pt-3 border-t border-gray-200 space-y-2">
          <div class="grid grid-cols-2 gap-2 text-xs">
            <div>
              <span class="text-gray-600">取货点:</span>
              <span class="ml-1 font-mono text-text">({{ order.pickup.x.toFixed(1) }}, {{ order.pickup.y.toFixed(1) }})</span>
            </div>
            <div>
              <span class="text-gray-600">送货点:</span>
              <span class="ml-1 font-mono text-text">({{ order.delivery.x.toFixed(1) }}, {{ order.delivery.y.toFixed(1) }})</span>
            </div>
          </div>

          <div v-if="order.assignedVehicleId" class="text-xs">
            <span class="text-gray-600">分配车辆:</span>
            <span class="ml-1 font-semibold text-primary">车辆 #{{ order.assignedVehicleId }}</span>
          </div>

          <div class="text-xs text-gray-600">
            创建时间: {{ formatTime(order.createdAt) }}
          </div>

          <!-- Cancel Button -->
          <button
            v-if="order.status !== 'COMPLETED' && order.status !== 'CANCELLED'"
            @click.stop="handleCancelOrder(order.id)"
            class="w-full mt-2 px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-600 text-xs font-medium rounded-lg transition-smooth cursor-pointer"
          >
            取消订单
          </button>

          <button
            v-else
            @click.stop="handleArchiveOrder(order.id)"
            class="w-full mt-2 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-medium rounded-lg transition-smooth cursor-pointer"
          >
            归档订单
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="flex flex-col items-center justify-center py-12 text-center">
      <svg class="w-16 h-16 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
      <p class="text-gray-500 text-sm">暂无订单</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Loading from '@/components/Loading.vue'
import { useOrderStore } from '@/stores/order'
import type { OrderStatus } from '@/types'

const orderStore = useOrderStore()

// Filters
const filters = [
  { label: '全部', value: 'ALL' },
  { label: '待分配', value: 'PENDING' },
  { label: '配送中', value: 'DELIVERING' },
  { label: '已完成', value: 'COMPLETED' }
]

const activeFilter = ref('ALL')
const expandedOrders = ref(new Set<number>())

// Filtered orders
const filteredOrders = computed(() => {
  if (activeFilter.value === 'ALL') {
    return orderStore.orders
  }
  return orderStore.orders.filter(order => order.status === activeFilter.value)
})

// Get filtered count
const getFilteredCount = (filter: string) => {
  if (filter === 'ALL') return orderStore.orders.length
  return orderStore.orders.filter(order => order.status === filter).length
}

// Toggle order details
const toggleOrderDetails = (orderId: number) => {
  if (expandedOrders.value.has(orderId)) {
    expandedOrders.value.delete(orderId)
  } else {
    expandedOrders.value.add(orderId)
  }
}

// Status badge class
const getStatusBadgeClass = (status: OrderStatus) => {
  const classes = {
    CREATED: 'bg-gray-100 text-gray-700',
    PENDING: 'bg-yellow-100 text-yellow-700',
    ASSIGNED: 'bg-purple-100 text-purple-700',
    DELIVERING: 'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-gray-100 text-gray-500'
  }
  return classes[status] || classes.CREATED
}

// Status dot class
const getStatusDotClass = (status: OrderStatus) => {
  const classes = {
    CREATED: 'bg-gray-500',
    PENDING: 'bg-yellow-500',
    ASSIGNED: 'bg-purple-500',
    DELIVERING: 'bg-blue-500 animate-pulse',
    COMPLETED: 'bg-green-500',
    CANCELLED: 'bg-gray-400'
  }
  return classes[status] || classes.CREATED
}

// Status text
const getStatusText = (status: OrderStatus) => {
  const texts = {
    CREATED: '已创建',
    PENDING: '待分配',
    ASSIGNED: '已分配',
    DELIVERING: '配送中',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return texts[status] || status
}

// Format time
const formatTime = (date: Date) => {
  return new Date(date).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// Handle cancel order
const handleCancelOrder = async (orderId: number) => {
  if (confirm('确定要取消这个订单吗？')) {
    try {
      await orderStore.cancelOrder(orderId, '用户手动取消')
    } catch {
    }
  }
}

const handleArchiveOrder = async (orderId: number) => {
  if (confirm('确定要归档这个订单吗？')) {
    try {
      await orderStore.archiveOrder(orderId, '用户手动归档')
    } catch {
    }
  }
}
</script>
