<template>
  <Teleport to="body">
    <div
      v-if="visible"
      ref="menuRef"
      class="fixed z-50 bg-white rounded-lg shadow-xl border border-gray-200 py-1 min-w-[200px]"
      :style="{ left: `${position.x}px`, top: `${position.y}px` }"
      @click.stop
    >
      <button
        v-for="item in items"
        :key="item.id"
        class="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 transition-colors"
        :disabled="item.disabled"
        @click="handleItemClick(item)"
      >
        <span v-if="item.icon" class="text-lg">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </button>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import type { ContextMenuItem } from '@/types'

interface Props {
  visible: boolean
  position: { x: number; y: number }
  items: ContextMenuItem[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
}>()

const menuRef = ref<HTMLDivElement | null>(null)

// Handle item click
const handleItemClick = (item: ContextMenuItem) => {
  if (!item.disabled) {
    item.action()
    emit('close')
  }
}

// Handle click outside
const handleClickOutside = (event: MouseEvent) => {
  if (props.visible && menuRef.value && !menuRef.value.contains(event.target as Node)) {
    emit('close')
  }
}

// Handle ESC key
const handleEscKey = (event: KeyboardEvent) => {
  if (props.visible && event.key === 'Escape') {
    emit('close')
  }
}

// Adjust position to keep menu within viewport
const adjustPosition = () => {
  if (!menuRef.value) return

  const menu = menuRef.value
  const rect = menu.getBoundingClientRect()
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight

  let { x, y } = props.position

  // Adjust horizontal position
  if (x + rect.width > viewportWidth) {
    x = viewportWidth - rect.width - 10
  }

  // Adjust vertical position
  if (y + rect.height > viewportHeight) {
    y = viewportHeight - rect.height - 10
  }

  // Ensure minimum position
  x = Math.max(10, x)
  y = Math.max(10, y)

  menu.style.left = `${x}px`
  menu.style.top = `${y}px`
}

// Watch for visibility changes to adjust position
watch(() => props.visible, (newVisible) => {
  if (newVisible) {
    // Use nextTick to ensure DOM is updated
    setTimeout(() => {
      adjustPosition()
    }, 0)
  }
})

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleEscKey)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleEscKey)
})
</script>
