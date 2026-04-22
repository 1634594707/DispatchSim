import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { getOrderStatisticsApi, getPerformanceMetricsApi, getStatisticsOverviewApi, getStrategyStatisticsApi, getVehicleStatisticsApi } from '@/api/statistics'
import type { PerformanceMetricsDto } from '@/api/types'
import type { DispatchStrategy } from '@/types'
import { getErrorMessage, reportError } from '@/utils/errorHandler'

export const useStatisticsStore = defineStore('statistics', () => {
  const loading = ref(false)
  const error = ref<string | null>(null)

  const overview = ref({
    totalOrders: 0,
    completedOrders: 0,
    completionRate: 0,
    averageDeliveryTime: 0,
    vehicleUtilization: 0,
    avgWaitingTime: 0,
    avgDispatchTime: 0,
    reDispatchCount: 0,
    pendingOrdersCount: 0,
    deliveringOrdersCount: 0,
    activeVehicles: 0,
    totalDistance: 0,
    averageDistance: 0,
  })

  const orderStats = ref({
    pendingOrders: 0,
    assignedOrders: 0,
    deliveringOrders: 0,
    completedOrders: 0,
    cancelledOrders: 0,
  })

  const vehicleStats = ref({
    totalVehicles: 0,
    idleVehicles: 0,
    deliveringVehicles: 0,
    faultyVehicles: 0,
    offlineVehicles: 0,
  })

  const strategyStats = ref<{ strategy: DispatchStrategy; count: number; percentage: number }[]>([])
  const performanceMetrics = ref<PerformanceMetricsDto>({
    latestDispatchDurationMs: 0,
    averageDispatchDurationMs: 0,
    websocketMessagesLastMinute: 0,
    slowQueryCount: 0,
    slowQueries: [],
    dispatchMetrics: [],
    websocketMetrics: [],
  })
  const history = ref<{
    time: string
    totalOrders: number
    completedOrders: number
    utilization: number
    idleVehicles: number
    deliveringVehicles: number
    faultyVehicles: number
    offlineVehicles: number
  }[]>([])
  let refreshTimer: ReturnType<typeof setInterval> | null = null

  const totalOrders = computed(() => overview.value.totalOrders)
  const completedOrders = computed(() => overview.value.completedOrders)
  const completionRate = computed(() => overview.value.completionRate)
  const averageDeliveryTime = computed(() => overview.value.averageDeliveryTime)
  const vehicleUtilization = computed(() => overview.value.vehicleUtilization)
  const activeVehicles = computed(() => overview.value.activeVehicles)
  const totalDistance = computed(() => overview.value.totalDistance)
  const averageDistance = computed(() => overview.value.averageDistance)
  const pendingOrdersCount = computed(() => overview.value.pendingOrdersCount)
  const deliveringOrdersCount = computed(() => overview.value.deliveringOrdersCount)
  const avgWaitingTime = computed(() => overview.value.avgWaitingTime)
  const avgDispatchTime = computed(() => overview.value.avgDispatchTime)
  const reDispatchCount = computed(() => overview.value.reDispatchCount)

  const vehicleStatusDistribution = computed(() => {
    const total = vehicleStats.value.totalVehicles
    if (total === 0) {
      return []
    }

    return [
      {
        name: '空闲',
        value: vehicleStats.value.idleVehicles,
        percentage: (vehicleStats.value.idleVehicles / total) * 100,
        color: '#10B981',
      },
      {
        name: '配送中',
        value: vehicleStats.value.deliveringVehicles,
        percentage: (vehicleStats.value.deliveringVehicles / total) * 100,
        color: '#3B82F6',
      },
      {
        name: '故障',
        value: vehicleStats.value.faultyVehicles,
        percentage: (vehicleStats.value.faultyVehicles / total) * 100,
        color: '#EF4444',
      },
    ]
  })

  const orderStatusDistribution = computed(() => {
    const total = overview.value.totalOrders
    if (total === 0) {
      return []
    }

    return [
      {
        name: '待分配',
        value: orderStats.value.pendingOrders,
        percentage: (orderStats.value.pendingOrders / total) * 100,
        color: '#F59E0B',
      },
      {
        name: '配送中',
        value: orderStats.value.deliveringOrders,
        percentage: (orderStats.value.deliveringOrders / total) * 100,
        color: '#3B82F6',
      },
      {
        name: '已完成',
        value: orderStats.value.completedOrders,
        percentage: (orderStats.value.completedOrders / total) * 100,
        color: '#10B981',
      },
    ]
  })

  const fetchStatistics = async () => {
    loading.value = true
    error.value = null

    try {
      const [overviewResponse, orderResponse, vehicleResponse, strategyResponse, performanceResponse] = await Promise.all([
        getStatisticsOverviewApi(),
        getOrderStatisticsApi(),
        getVehicleStatisticsApi(),
        getStrategyStatisticsApi(),
        getPerformanceMetricsApi(),
      ])

      overview.value = {
        totalOrders: overviewResponse.totalOrders ?? 0,
        completedOrders: overviewResponse.completedOrders ?? 0,
        completionRate: overviewResponse.completionRate ?? 0,
        averageDeliveryTime: overviewResponse.averageDeliveryTime ?? overviewResponse.avgDeliveryTime ?? 0,
        vehicleUtilization: overviewResponse.vehicleUtilization ?? overviewResponse.avgUtilization ?? 0,
        avgWaitingTime: overviewResponse.avgWaitingTime ?? 0,
        avgDispatchTime: overviewResponse.avgDispatchTime ?? 0,
        reDispatchCount: overviewResponse.reDispatchCount ?? 0,
        pendingOrdersCount: overviewResponse.pendingOrdersCount ?? orderResponse.pendingOrders ?? 0,
        deliveringOrdersCount: overviewResponse.deliveringOrdersCount ?? orderResponse.deliveringOrders ?? 0,
        activeVehicles: overviewResponse.activeVehicles ?? 0,
        totalDistance: overviewResponse.totalDistance ?? 0,
        averageDistance: overviewResponse.averageDistance ?? 0,
      }

      orderStats.value = {
        pendingOrders: orderResponse.pendingOrders ?? 0,
        assignedOrders: orderResponse.assignedOrders ?? 0,
        deliveringOrders: orderResponse.deliveringOrders ?? 0,
        completedOrders: orderResponse.completedOrders ?? 0,
        cancelledOrders: orderResponse.cancelledOrders ?? 0,
      }

      vehicleStats.value = {
        totalVehicles: vehicleResponse.totalVehicles ?? 0,
        idleVehicles: vehicleResponse.idleVehicles ?? 0,
        deliveringVehicles: vehicleResponse.deliveringVehicles ?? 0,
        faultyVehicles: vehicleResponse.faultyVehicles ?? 0,
        offlineVehicles: vehicleResponse.offlineVehicles ?? 0,
      }

      strategyStats.value = strategyResponse.items ?? strategyResponse.strategies ?? []
      performanceMetrics.value = performanceResponse
      history.value = [
        ...history.value,
        {
          time: new Date().toLocaleTimeString(),
          totalOrders: overview.value.totalOrders,
          completedOrders: overview.value.completedOrders,
          utilization: overview.value.vehicleUtilization,
          idleVehicles: vehicleStats.value.idleVehicles,
          deliveringVehicles: vehicleStats.value.deliveringVehicles,
          faultyVehicles: vehicleStats.value.faultyVehicles,
          offlineVehicles: vehicleStats.value.offlineVehicles,
        },
      ].slice(-12)
    } catch (err) {
      error.value = getErrorMessage(err, '加载统计数据失败')
      reportError(err, '统计数据加载失败')
      throw err
    } finally {
      loading.value = false
    }
  }

  const startAutoRefresh = () => {
    if (refreshTimer) {
      return
    }

    refreshTimer = setInterval(() => {
      void fetchStatistics().catch(() => {})
    }, 10000)
  }

  const stopAutoRefresh = () => {
    if (!refreshTimer) {
      return
    }

    clearInterval(refreshTimer)
    refreshTimer = null
  }

  const applyOverviewSnapshot = (snapshot: Partial<typeof overview.value>) => {
    overview.value = {
      ...overview.value,
      ...snapshot,
    }
  }

  return {
    loading,
    error,
    orderStats,
    vehicleStats,
    strategyStats,
    performanceMetrics,
    history,
    totalOrders,
    completedOrders,
    completionRate,
    averageDeliveryTime,
    avgDeliveryTime: averageDeliveryTime,
    vehicleUtilization,
    avgUtilization: vehicleUtilization,
    activeVehicles,
    totalDistance,
    averageDistance,
    avgWaitingTime,
    avgDispatchTime,
    reDispatchCount,
    pendingOrdersCount,
    deliveringOrdersCount,
    vehicleStatusDistribution,
    orderStatusDistribution,
    applyOverviewSnapshot,
    fetchStatistics,
    startAutoRefresh,
    stopAutoRefresh,
  }
})
