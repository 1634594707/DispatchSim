<template>
  <div ref="containerRef" class="h-full min-h-[280px] w-full"></div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { init, type EChartsOption, type EChartsType } from '@/lib/echarts'

const props = defineProps<{
  option: EChartsOption
}>()

const containerRef = ref<HTMLDivElement | null>(null)
let chart: EChartsType | null = null

const render = () => {
  if (!containerRef.value) {
    return
  }

  if (!chart) {
    chart = init(containerRef.value)
  }

  chart.setOption(props.option, true)
  chart.resize()
}

onMounted(() => {
  render()
  window.addEventListener('resize', render)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', render)
  chart?.dispose()
  chart = null
})
</script>
