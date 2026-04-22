import { apiClient, unwrapList, unwrapResponse } from './client'
import type { TriggerFaultRequestDto, VehicleDto } from './types'

export const getVehiclesApi = async (status?: string) => {
  const response = await apiClient.get<VehicleDto[]>('/vehicles', {
    params: status ? { status } : undefined,
  })
  return unwrapList(unwrapResponse(response))
}

export const getVehicleByIdApi = async (id: number) => {
  const response = await apiClient.get<VehicleDto>(`/vehicles/${id}`)
  return unwrapResponse(response)
}

export const triggerFaultApi = async (id: number, payload: TriggerFaultRequestDto = {}) => {
  const response = await apiClient.post<VehicleDto>(`/vehicles/${id}/fault`, payload)
  return unwrapResponse(response)
}

export const recoverVehicleApi = async (id: number) => {
  const response = await apiClient.post<VehicleDto>(`/vehicles/${id}/recover`)
  return unwrapResponse(response)
}
