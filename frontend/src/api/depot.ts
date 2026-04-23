import { apiClient, unwrapList, unwrapResponse } from './client'
import type { DepotDto, DepotUpsertRequestDto, PageResult } from './types'

export const getDepotsApi = async (page = 0, size = 200) => {
  const response = await apiClient.get<PageResult<DepotDto> | DepotDto[]>('/depots', {
    params: { page, size },
  })
  return unwrapList(unwrapResponse(response))
}

export const createDepotApi = async (payload: DepotUpsertRequestDto) => {
  const response = await apiClient.post<DepotDto>('/depots', payload)
  return unwrapResponse(response)
}

export const updateDepotApi = async (id: number, payload: DepotUpsertRequestDto) => {
  const response = await apiClient.put<DepotDto>(`/depots/${id}`, payload)
  return unwrapResponse(response)
}

export const deleteDepotApi = async (id: number) => {
  const response = await apiClient.delete<void>(`/depots/${id}`)
  return unwrapResponse(response)
}
