import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { Order, OrderStatus, Position } from '@/types'
import {
  archiveOrderApi,
  cancelOrderApi,
  createOrderApi,
  getArchivedOrdersApi,
  getOrderByIdApi,
  getOrdersApi,
  restoreOrderApi,
} from '@/api/order'
import { normalizeOrder } from '@/api/types'
import { getErrorMessage, reportError } from '@/utils/errorHandler'
import { useToastStore } from './toast'

export const useOrderStore = defineStore('order', () => {
  const orders = ref<Order[]>([])
  const archivedOrdersState = ref<Order[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const toastStore = useToastStore()

  const setOrders = (nextOrders: Order[]) => {
    orders.value = nextOrders
  }

  const orderCount = computed(() => orders.value.length)

  const ordersByStatus = computed(() => {
    return orders.value.reduce((acc, order) => {
      if (!acc[order.status]) {
        acc[order.status] = []
      }
      acc[order.status].push(order)
      return acc
    }, {} as Record<OrderStatus, Order[]>)
  })

  const pendingOrders = computed(() => orders.value.filter((order) => order.status === 'PENDING'))
  const assignedOrders = computed(() => orders.value.filter((order) => order.status === 'ASSIGNED'))
  const deliveringOrders = computed(() => orders.value.filter((order) => order.status === 'DELIVERING'))
  const completedOrders = computed(() => orders.value.filter((order) => order.status === 'COMPLETED'))

  const completionRate = computed(() => {
    if (orders.value.length === 0) {
      return 0
    }

    return (completedOrders.value.length / orders.value.length) * 100
  })

  const averageDeliveryTime = computed(() => {
    const completed = completedOrders.value.filter((order) => order.completedAt)
    if (completed.length === 0) {
      return 0
    }

    const totalTime = completed.reduce((sum, order) => {
      return sum + (order.completedAt!.getTime() - order.createdAt.getTime())
    }, 0)

    return totalTime / completed.length / 1000
  })

  const fetchOrders = async (status?: OrderStatus) => {
    loading.value = true
    error.value = null

    try {
      const data = await getOrdersApi(status)
      orders.value = data.map(normalizeOrder)
      return orders.value
    } catch (err) {
      error.value = getErrorMessage(err, '加载订单失败')
      reportError(err, '订单加载失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchOrderById = async (id: number) => {
    loading.value = true
    error.value = null

    try {
      const data = await getOrderByIdApi(id)
      const normalized = normalizeOrder(data)
      updateOrder(id, normalized)
      return normalized
    } catch (err) {
      error.value = getErrorMessage(err, '加载订单详情失败')
      reportError(err, '订单详情加载失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchArchivedOrders = async () => {
    loading.value = true
    error.value = null

    try {
      archivedOrdersState.value = (await getArchivedOrdersApi({ page: 0, size: 200 })).map(normalizeOrder)
      return archivedOrdersState.value
    } catch (err) {
      error.value = getErrorMessage(err, '加载归档订单失败')
      reportError(err, '归档订单加载失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const createOrder = async (pickup: Position, delivery: Position, priority: number = 5): Promise<Order> => {
    loading.value = true
    error.value = null

    try {
      const created = await createOrderApi({ pickup, delivery, priority })
      const normalized = normalizeOrder(created)
      orders.value.unshift(normalized)
      toastStore.success('订单已创建', `订单 #${normalized.id} 已提交`)
      return normalized
    } catch (err) {
      error.value = getErrorMessage(err, '创建订单失败')
      reportError(err, '创建订单失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateOrder = (id: number, updates: Partial<Order>) => {
    const index = orders.value.findIndex((order) => order.id === id)

    if (index === -1) {
      if ('pickup' in updates && 'delivery' in updates && 'priority' in updates && 'status' in updates && 'createdAt' in updates) {
        orders.value.unshift(updates as Order)
      }
      return
    }

    orders.value[index] = { ...orders.value[index], ...updates }
  }

  const updateOrderStatus = (id: number, status: OrderStatus) => {
    const order = orders.value.find((item) => item.id === id)
    if (!order) {
      return
    }

    order.status = status
    if (status === 'COMPLETED') {
      order.completedAt = new Date()
    }
  }

  const assignVehicle = (orderId: number, vehicleId: number) => {
    const order = orders.value.find((item) => item.id === orderId)
    if (!order) {
      return
    }

    order.assignedVehicleId = vehicleId
    order.status = 'ASSIGNED'
  }

  const startDelivery = (orderId: number) => {
    updateOrderStatus(orderId, 'DELIVERING')
  }

  const completeOrder = (orderId: number) => {
    updateOrderStatus(orderId, 'COMPLETED')
  }

  const cancelOrder = async (orderId: number, reason?: string) => {
    loading.value = true
    error.value = null

    try {
      const data = await cancelOrderApi(orderId, { reason })
      const normalized = normalizeOrder(data)
      updateOrder(orderId, normalized)
      toastStore.info('订单已取消', `订单 #${normalized.id} 已取消`)
      return normalized
    } catch (err) {
      error.value = getErrorMessage(err, '取消订单失败')
      reportError(err, '取消订单失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const getOrderById = (id: number) => {
    return orders.value.find((order) => order.id === id)
  }

  const getOrdersByVehicle = (vehicleId: number) => {
    return orders.value.filter((order) => order.assignedVehicleId === vehicleId)
  }

  const clearOrders = () => {
    orders.value = []
    archivedOrdersState.value = []
  }

  const removeOrder = (id: number) => {
    const index = orders.value.findIndex((order) => order.id === id)
    if (index !== -1) {
      orders.value.splice(index, 1)
    }
  }

  const archiveOrder = async (orderId: number, reason?: string) => {
    loading.value = true
    error.value = null

    try {
      const normalized = normalizeOrder(await archiveOrderApi(orderId, { reason }))
      removeOrder(orderId)
      archivedOrdersState.value = [
        normalized,
        ...archivedOrdersState.value.filter((order) => order.id !== orderId),
      ]
      toastStore.success('订单已归档', `订单 #${orderId} 已归档`)
      return normalized
    } catch (err) {
      error.value = getErrorMessage(err, '归档订单失败')
      reportError(err, '归档订单失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const restoreOrder = async (orderId: number) => {
    loading.value = true
    error.value = null

    try {
      const normalized = normalizeOrder(await restoreOrderApi(orderId))
      archivedOrdersState.value = archivedOrdersState.value.filter((order) => order.id !== orderId)
      orders.value = [normalized, ...orders.value.filter((order) => order.id !== orderId)]
      toastStore.success('订单已恢复', `订单 #${orderId} 已恢复`)
      return normalized
    } catch (err) {
      error.value = getErrorMessage(err, '恢复订单失败')
      reportError(err, '恢复订单失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const archivedOrders = computed(() => archivedOrdersState.value)
  const activeOrders = computed(() => orders.value.filter((order) => !order.archived))

  return {
    orders,
    archivedOrdersState,
    loading,
    error,
    orderCount,
    ordersByStatus,
    pendingOrders,
    assignedOrders,
    deliveringOrders,
    completedOrders,
    archivedOrders,
    activeOrders,
    completionRate,
    averageDeliveryTime,
    setOrders,
    fetchOrders,
    fetchOrderById,
    fetchArchivedOrders,
    createOrder,
    updateOrder,
    updateOrderStatus,
    assignVehicle,
    startDelivery,
    completeOrder,
    cancelOrder,
    archiveOrder,
    restoreOrder,
    getOrderById,
    getOrdersByVehicle,
    clearOrders,
    removeOrder,
  }
})
