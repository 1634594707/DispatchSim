import { apiClient, unwrapResponse } from './client'
import type {
  ReplayControlRequestDto,
  ReplayEventDto,
  ReplayFrameDto,
  ReplaySessionDto,
} from './types'

export const getReplaySessionsApi = async () => {
  const response = await apiClient.get<ReplaySessionDto[]>('/simulation/replay/sessions')
  return unwrapResponse(response)
}

export const getReplaySessionEventsApi = async (sessionId: string) => {
  const response = await apiClient.get<ReplayEventDto[]>(`/simulation/replay/sessions/${sessionId}/events`)
  return unwrapResponse(response)
}

export const getReplayEventsByTimeRangeApi = async (startTime: string, endTime: string) => {
  const response = await apiClient.get<ReplayEventDto[]>('/simulation/replay/events', {
    params: { startTime, endTime },
  })
  return unwrapResponse(response)
}

export const replayControlApi = async (payload: ReplayControlRequestDto) => {
  const response = await apiClient.post<ReplayFrameDto>('/simulation/replay/control', payload)
  return unwrapResponse(response)
}
