<template>
  <div
    class="flex items-center justify-center gap-3 rounded-2xl border border-dashed border-slate-300 bg-white/80 px-4 py-6 text-sm text-slate-600"
    :class="centered ? 'min-h-[180px]' : ''"
  >
    <div class="h-5 w-5 animate-spin rounded-full border-2 border-slate-200 border-t-teal-700"></div>
    <span>{{ label }}</span>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
    centered?: boolean
  }>(),
  {
    label: '正在加载数据...',
    centered: false,
  },
)
</script>
