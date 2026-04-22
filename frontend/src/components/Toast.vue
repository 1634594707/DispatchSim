<template>
  <div class="fixed top-20 right-3 sm:right-4 z-[70] flex w-[calc(100vw-1.5rem)] max-w-sm flex-col gap-3 sm:top-24">
    <transition-group name="toast">
      <div
        v-for="toast in toastStore.toasts"
        :key="toast.id"
        class="rounded-2xl border bg-white/95 p-4 shadow-xl backdrop-blur-md"
        :class="typeClass[toast.type]"
      >
        <div class="flex items-start gap-3">
          <div class="mt-0.5 h-2.5 w-2.5 rounded-full" :class="dotClass[toast.type]"></div>
          <div class="min-w-0 flex-1">
            <div class="text-sm font-semibold text-slate-900">{{ toast.title }}</div>
            <div v-if="toast.message" class="mt-1 text-sm text-slate-600">{{ toast.message }}</div>
          </div>
          <button
            class="rounded-md px-2 py-1 text-xs text-slate-500 transition-smooth hover:bg-slate-100 hover:text-slate-700"
            @click="toastStore.removeToast(toast.id)"
          >
            关闭
          </button>
        </div>
      </div>
    </transition-group>
  </div>
</template>

<script setup lang="ts">
import { useToastStore } from '@/stores/toast'

const toastStore = useToastStore()

const typeClass = {
  success: 'border-emerald-200',
  error: 'border-rose-200',
  warning: 'border-amber-200',
  info: 'border-sky-200',
}

const dotClass = {
  success: 'bg-emerald-500',
  error: 'bg-rose-500',
  warning: 'bg-amber-500',
  info: 'bg-sky-500',
}
</script>
