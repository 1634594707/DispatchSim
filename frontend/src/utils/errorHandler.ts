import axios from 'axios'
import { useToastStore } from '@/stores/toast'

export const AppErrorType = {
  NETWORK: 'NETWORK',
  TIMEOUT: 'TIMEOUT',
  UNAUTHORIZED: 'UNAUTHORIZED',
  FORBIDDEN: 'FORBIDDEN',
  NOT_FOUND: 'NOT_FOUND',
  VALIDATION: 'VALIDATION',
  SERVER: 'SERVER',
  WEBSOCKET: 'WEBSOCKET',
  UNKNOWN: 'UNKNOWN',
} as const

export type AppErrorType = (typeof AppErrorType)[keyof typeof AppErrorType]

export interface AppError extends Error {
  type: AppErrorType
  statusCode?: number
  details?: unknown
}

const errorMessageMap: Record<AppErrorType, string> = {
  NETWORK: '网络连接失败，请检查服务是否已启动',
  TIMEOUT: '请求超时，请稍后重试',
  UNAUTHORIZED: '登录状态失效，请重新认证',
  FORBIDDEN: '当前操作没有权限执行',
  NOT_FOUND: '请求的资源不存在',
  VALIDATION: '提交的数据格式不正确',
  SERVER: '服务端处理失败，请稍后重试',
  WEBSOCKET: '实时连接异常，系统将尝试重连',
  UNKNOWN: '发生未知错误，请稍后重试',
}

const createAppError = (
  type: AppErrorType,
  message?: string,
  statusCode?: number,
  details?: unknown,
): AppError => {
  const error = new Error(message || errorMessageMap[type]) as AppError
  error.type = type
  error.statusCode = statusCode
  error.details = details
  return error
}

export const normalizeError = (input: unknown): AppError => {
  if (typeof input === 'object' && input && 'type' in input && 'message' in input) {
    return input as AppError
  }

  if (axios.isAxiosError(input)) {
    if (input.code === 'ECONNABORTED') {
      return createAppError(AppErrorType.TIMEOUT, errorMessageMap.TIMEOUT, 408, input)
    }

    const statusCode = input.response?.status
    const serverMessage =
      input.response?.data?.message ||
      input.response?.data?.error ||
      input.message

    if (!statusCode) {
      return createAppError(AppErrorType.NETWORK, serverMessage || errorMessageMap.NETWORK, undefined, input)
    }

    if (statusCode === 400 || statusCode === 422) {
      return createAppError(AppErrorType.VALIDATION, serverMessage || errorMessageMap.VALIDATION, statusCode, input)
    }

    if (statusCode === 401) {
      return createAppError(AppErrorType.UNAUTHORIZED, serverMessage || errorMessageMap.UNAUTHORIZED, statusCode, input)
    }

    if (statusCode === 403) {
      return createAppError(AppErrorType.FORBIDDEN, serverMessage || errorMessageMap.FORBIDDEN, statusCode, input)
    }

    if (statusCode === 404) {
      return createAppError(AppErrorType.NOT_FOUND, serverMessage || errorMessageMap.NOT_FOUND, statusCode, input)
    }

    if (statusCode >= 500) {
      return createAppError(AppErrorType.SERVER, serverMessage || errorMessageMap.SERVER, statusCode, input)
    }
  }

  if (input instanceof Error) {
    return createAppError(AppErrorType.UNKNOWN, input.message, undefined, input)
  }

  return createAppError(AppErrorType.UNKNOWN)
}

export const getErrorMessage = (input: unknown, fallback?: string) => {
  const normalized = normalizeError(input)
  return normalized.message || fallback || errorMessageMap[normalized.type]
}

export const reportError = (input: unknown, title = '操作失败') => {
  const normalized = normalizeError(input)
  const toastStore = useToastStore()
  toastStore.error(title, normalized.message || errorMessageMap[normalized.type])
  return normalized
}

export const reportWebsocketError = (input: unknown) => {
  const normalized = normalizeError(input)
  const toastStore = useToastStore()
  toastStore.warning('实时连接异常', normalized.message || errorMessageMap.WEBSOCKET)
  return normalized
}
