import type { DispatchStrategy } from '@/types'
import { apiClient, unwrapResponse } from './client'
import type {
  BatchOrderRequestDto,
  BatchOrderResponseDto,
  ResultDto,
  SimulationStatusDto,
  UpdateSimulationSpeedRequestDto,
  UpdateStrategyRequestDto,
} from './types'

const postCommand = async (path: string) => {
  const response = await apiClient.post<ResultDto | SimulationStatusDto>(path)
  return unwrapResponse(response)
}

export const startSimulationApi = async () => postCommand('/simulation/start')
export const stopSimulationApi = async () => postCommand('/simulation/stop')
export const pauseSimulationApi = async () => postCommand('/simulation/pause')
export const resumeSimulationApi = async () => postCommand('/simulation/resume')
export const tickSimulationApi = async () => postCommand('/simulation/tick')
export const stepSimulationApi = async () => postCommand('/simulation/step')
export const resetSimulationApi = async () => postCommand('/simulation/reset')

export const updateSimulationStrategyApi = async (strategy: DispatchStrategy) => {
  const payload: UpdateStrategyRequestDto = { strategy }
  const response = await apiClient.post<ResultDto | SimulationStatusDto>('/simulation/strategy', payload)
  return unwrapResponse(response)
}

export const updateSimulationSpeedApi = async (speed: number) => {
  const payload: UpdateSimulationSpeedRequestDto = { speed }
  const response = await apiClient.put<ResultDto | SimulationStatusDto>('/simulation/speed', payload)
  return unwrapResponse(response)
}

export const getSimulationStatusApi = async () => {
  const response = await apiClient.get<SimulationStatusDto>('/simulation/status')
  return unwrapResponse(response)
}

export const createBatchOrdersApi = async (payload: BatchOrderRequestDto) => {
  const response = await apiClient.post<BatchOrderResponseDto>('/simulation/batch-orders', payload)
  return unwrapResponse(response)
}
