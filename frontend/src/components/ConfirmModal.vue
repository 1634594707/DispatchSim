<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="$emit('cancel')">
    <div class="bg-white rounded-2xl shadow-xl max-w-md w-full mx-4 p-6">
      <h3 class="text-xl font-semibold text-text mb-2">
        {{ title }}
      </h3>
      <p class="text-gray-600 mb-6">
        {{ message }}
      </p>

      <div class="flex gap-2">
        <button
          type="button"
          class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors font-medium"
          @click="$emit('cancel')"
        >
          {{ cancelText }}
        </button>
        <button
          type="button"
          class="flex-1 px-4 py-2 rounded-lg transition-colors font-medium"
          :class="danger ? 'bg-red-600 text-white hover:bg-red-700' : 'bg-primary text-white hover:bg-primary/90'"
          @click="$emit('confirm')"
        >
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  danger?: boolean
}

withDefaults(defineProps<Props>(), {
  confirmText: '确认',
  cancelText: '取消',
  danger: false,
})

defineEmits<{
  confirm: []
  cancel: []
}>()
</script>
