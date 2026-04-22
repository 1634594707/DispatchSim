import axios from 'axios'
import type { AxiosResponse } from 'axios'
import type { ApiEnvelope, PageResult } from './types'
import { normalizeError } from '@/utils/errorHandler'

const DEFAULT_TIMEOUT = 10000

const resolveBaseUrl = () => {
  const envBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
  return envBaseUrl || '/api'
}

export const apiClient = axios.create({
  baseURL: resolveBaseUrl(),
  timeout: DEFAULT_TIMEOUT,
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('dispatch-sim-token')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    return Promise.reject(normalizeError(error))
  },
)

export const unwrapResponse = <T>(response: AxiosResponse<T | ApiEnvelope<T>>): T => {
  const payload = response.data as T | ApiEnvelope<T>

  if (
    payload &&
    typeof payload === 'object' &&
    'data' in payload &&
    (payload as ApiEnvelope<T>).data !== undefined
  ) {
    return (payload as ApiEnvelope<T>).data as T
  }

  return payload as T
}

export const unwrapList = <T>(payload: T[] | PageResult<T>): T[] => {
  if (Array.isArray(payload)) {
    return payload
  }

  if (Array.isArray(payload.content)) {
    return payload.content
  }

  if (Array.isArray(payload.records)) {
    return payload.records
  }

  if (Array.isArray(payload.items)) {
    return payload.items
  }

  return []
}
