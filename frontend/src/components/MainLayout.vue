<template>
  <div class="min-h-screen bg-background transition-colors duration-300">
    <!-- Navbar -->
    <nav class="fixed top-2 sm:top-4 left-2 sm:left-4 right-2 sm:right-4 z-50 bg-white/95 backdrop-blur-md rounded-xl sm:rounded-2xl shadow-lg border border-gray-300">
      <div class="px-3 sm:px-6 py-3 sm:py-4">
        <div class="flex items-center justify-between">
          <!-- Logo and Title -->
          <div class="flex items-center gap-2 sm:gap-3">
            <div class="w-8 h-8 sm:w-10 sm:h-10 bg-primary rounded-lg flex items-center justify-center">
              <svg class="w-5 h-5 sm:w-6 sm:h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
              </svg>
            </div>
            <h1 class="text-base sm:text-xl font-heading font-semibold text-text">DispatchSim</h1>
          </div>

          <!-- Navigation Links -->
          <div class="flex items-center gap-2 sm:gap-3">
            <div
              class="hidden rounded-full border px-3 py-1 text-xs font-medium sm:flex sm:items-center sm:gap-2"
              :class="connectionBadgeClass"
            >
              <span class="h-2.5 w-2.5 rounded-full" :class="connectionDotClass"></span>
              <span>{{ connectionLabel }}</span>
            </div>
            <router-link
              to="/"
              class="px-2 sm:px-4 py-1.5 sm:py-2 rounded-lg text-xs sm:text-base font-medium transition-smooth cursor-pointer"
              :class="$route.name === 'dashboard' 
                ? 'bg-teal-700 text-white shadow-sm ring-1 ring-teal-800/40' 
                : 'text-slate-700 hover:bg-gray-200'"
              aria-label="Dashboard 页面"
            >
              <span class="hidden sm:inline">Dashboard</span>
              <span class="sm:hidden">仪表盘</span>
            </router-link>
            <router-link
              to="/statistics"
              class="px-2 sm:px-4 py-1.5 sm:py-2 rounded-lg text-xs sm:text-base font-medium transition-smooth cursor-pointer"
              :class="$route.name === 'statistics' 
                ? 'bg-teal-700 text-white shadow-sm ring-1 ring-teal-800/40' 
                : 'text-slate-700 hover:bg-gray-200'"
              aria-label="Statistics 页面"
            >
              <span class="hidden sm:inline">Statistics</span>
              <span class="sm:hidden">统计</span>
            </router-link>
          </div>
        </div>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="pt-16 sm:pt-24 px-2 sm:px-4">
      <router-view />
    </main>
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import Toast from '@/components/Toast.vue'
import { useWebsocketStatus } from '@/services/websocket'

const { connectionState } = useWebsocketStatus()

const connectionLabel = computed(() => {
  const textMap = {
    connected: '实时已连接',
    connecting: '实时连接中',
    reconnecting: '实时重连中',
    error: '实时连接异常',
    disconnected: '实时未连接',
  }

  return textMap[connectionState.value]
})

const connectionBadgeClass = computed(() => {
  const classMap = {
    connected: 'border-emerald-200 bg-emerald-50 text-emerald-700',
    connecting: 'border-sky-200 bg-sky-50 text-sky-700',
    reconnecting: 'border-amber-200 bg-amber-50 text-amber-700',
    error: 'border-rose-200 bg-rose-50 text-rose-700',
    disconnected: 'border-slate-200 bg-slate-50 text-slate-600',
  }

  return classMap[connectionState.value]
})

const connectionDotClass = computed(() => {
  const classMap = {
    connected: 'bg-emerald-500',
    connecting: 'bg-sky-500 animate-pulse',
    reconnecting: 'bg-amber-500 animate-pulse',
    error: 'bg-rose-500',
    disconnected: 'bg-slate-400',
  }

  return classMap[connectionState.value]
})
</script>
