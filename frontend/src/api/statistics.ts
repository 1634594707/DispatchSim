import { apiClient, unwrapResponse } from './client'
import type {
  OrderStatisticsDto,
  PerformanceMetricsDto,
  StatisticsOverviewDto,
  StrategyStatisticsDto,
  VehicleStatisticsDto,
} from './types'

export const getStatisticsOverviewApi = async () => {
  const response = await apiClient.get<StatisticsOverviewDto>('/statistics/overview')
  return unwrapResponse(response)
}

export const getOrderStatisticsApi = async () => {
  const response = await apiClient.get<OrderStatisticsDto>('/statistics/orders')
  return unwrapResponse(response)
}

export const getVehicleStatisticsApi = async () => {
  const response = await apiClient.get<VehicleStatisticsDto>('/statistics/vehicles')
  return unwrapResponse(response)
}

export const getStrategyStatisticsApi = async () => {
  const response = await apiClient.get<StrategyStatisticsDto>('/statistics/strategies')
  return unwrapResponse(response)
}

export const getPerformanceMetricsApi = async () => {
  const response = await apiClient.get<PerformanceMetricsDto>('/statistics/performance')
  return unwrapResponse(response)
}
