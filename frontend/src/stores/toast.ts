import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface ToastItem {
  id: number
  type: ToastType
  title: string
  message?: string
  duration: number
}

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<ToastItem[]>([])

  const removeToast = (id: number) => {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
  }

  const pushToast = (type: ToastType, title: string, message?: string, duration = 3500) => {
    const id = Date.now() + Math.floor(Math.random() * 1000)

    toasts.value.push({
      id,
      type,
      title,
      message,
      duration,
    })

    if (duration > 0) {
      window.setTimeout(() => {
        removeToast(id)
      }, duration)
    }

    return id
  }

  const success = (title: string, message?: string, duration?: number) => {
    return pushToast('success', title, message, duration)
  }

  const error = (title: string, message?: string, duration?: number) => {
    return pushToast('error', title, message, duration)
  }

  const warning = (title: string, message?: string, duration?: number) => {
    return pushToast('warning', title, message, duration)
  }

  const info = (title: string, message?: string, duration?: number) => {
    return pushToast('info', title, message, duration)
  }

  const clear = () => {
    toasts.value = []
  }

  return {
    toasts,
    pushToast,
    success,
    error,
    warning,
    info,
    removeToast,
    clear,
  }
})
