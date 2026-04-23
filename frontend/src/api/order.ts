import { apiClient, unwrapList, unwrapResponse } from './client'
import type {
  ArchiveOrderRequestDto,
  CancelOrderRequestDto,
  CreateOrderRequestDto,
  OrderDto,
  PageResult,
} from './types'

export const createOrderApi = async (payload: CreateOrderRequestDto) => {
  const response = await apiClient.post<OrderDto>('/orders', payload)
  return unwrapResponse(response)
}

export const getOrdersApi = async (status?: string) => {
  const response = await apiClient.get<OrderDto[] | PageResult<OrderDto>>('/orders', {
    params: status ? { status } : undefined,
  })
  return unwrapList(unwrapResponse(response))
}

export const getOrderByIdApi = async (id: number) => {
  const response = await apiClient.get<OrderDto>(`/orders/${id}`)
  return unwrapResponse(response)
}

export const cancelOrderApi = async (id: number, payload: CancelOrderRequestDto = {}) => {
  const response = await apiClient.post<OrderDto>(`/orders/${id}/cancel`, payload)
  return unwrapResponse(response)
}

export const archiveOrderApi = async (id: number, payload: ArchiveOrderRequestDto = {}) => {
  const response = await apiClient.post<OrderDto>(`/orders/${id}/archive`, payload)
  return unwrapResponse(response)
}

export const restoreOrderApi = async (id: number) => {
  const response = await apiClient.post<OrderDto>(`/orders/${id}/restore`)
  return unwrapResponse(response)
}

export const getArchivedOrdersApi = async (params?: {
  archivedFrom?: string
  archivedTo?: string
  reason?: string
  orderNo?: string
  page?: number
  size?: number
}) => {
  const response = await apiClient.get<PageResult<OrderDto>>('/orders/archived', { params })
  return unwrapList(unwrapResponse(response))
}
