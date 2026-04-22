<template>
  <div
    v-if="isOpen"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
    @click="close"
  >
    <div
      class="bg-white rounded-2xl shadow-2xl border border-gray-200 p-6 max-w-md w-full mx-4"
      @click.stop
    >
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-xl font-heading font-semibold text-text">键盘快捷键</h3>
        <button
          @click="close"
          class="p-1 hover:bg-gray-100 rounded-lg transition-smooth cursor-pointer"
          aria-label="关闭"
        >
          <svg class="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <div class="space-y-3">
        <div v-for="shortcut in shortcuts" :key="shortcut.key" class="flex items-center justify-between">
          <span class="text-sm text-gray-700">{{ shortcut.description }}</span>
          <kbd class="px-2 py-1 bg-gray-100 border border-gray-300 rounded text-xs font-mono text-gray-700">
            {{ shortcut.key }}
          </kbd>
        </div>
      </div>

      <div class="mt-6 pt-4 border-t border-gray-200">
        <button
          @click="close"
          class="w-full px-4 py-2 bg-primary hover:bg-primary/90 text-white font-medium rounded-lg transition-smooth cursor-pointer"
        >
          关闭
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const isOpen = ref(false)

const shortcuts = [
  { key: 'Space', description: '启动/暂停仿真' },
  { key: 'Ctrl+S', description: '停止仿真' },
  { key: 'Ctrl+N', description: '创建新订单' },
  { key: '1', description: '切换到订单标签' },
  { key: '2', description: '切换到车辆标签' },
  { key: '?', description: '显示快捷键帮助' }
]

const open = () => {
  isOpen.value = true
}

const close = () => {
  isOpen.value = false
}

defineExpose({
  open,
  close
})
</script>
