<template>
  <div class="absolute bottom-20 right-4 bg-white/90 backdrop-blur-sm rounded-lg shadow-md border border-gray-200 flex flex-col">
    <button
      class="p-2 hover:bg-gray-100 transition-colors border-b border-gray-200 rounded-t-lg"
      title="放大 (Zoom In)"
      @click="$emit('zoom-in')"
    >
      <svg class="w-5 h-5 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
      </svg>
    </button>
    <button
      class="p-2 hover:bg-gray-100 transition-colors border-b border-gray-200"
      title="重置 (Reset)"
      @click="$emit('reset')"
    >
      <svg class="w-5 h-5 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
      </svg>
    </button>
    <button
      class="p-2 hover:bg-gray-100 transition-colors rounded-b-lg"
      title="缩小 (Zoom Out)"
      @click="$emit('zoom-out')"
    >
      <svg class="w-5 h-5 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4" />
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
defineEmits<{
  'zoom-in': []
  'zoom-out': []
  'reset': []
}>()
</script>
