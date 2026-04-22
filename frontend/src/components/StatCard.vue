<template>
  <div class="group relative overflow-hidden rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm transition-all duration-200 hover:shadow-md cursor-default">
    <div class="absolute inset-x-0 top-0 h-1 rounded-t-2xl" :class="accentClass" />
    <div class="flex items-start justify-between">
      <div class="flex-1 min-w-0">
        <p class="text-[11px] font-semibold uppercase tracking-widest text-slate-400">{{ title }}</p>
        <p class="mt-1.5 text-2xl font-bold tracking-tight text-slate-900" style="font-family:'Fira Code',monospace">{{ value }}</p>
        <div v-if="trend !== undefined" class="mt-1.5 flex items-center gap-1 text-xs font-semibold" :class="trend >= 0 ? 'text-emerald-600' : 'text-rose-600'">
          <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path v-if="trend >= 0" stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 15l7-7 7 7" />
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M19 9l-7 7-7-7" />
          </svg>
          <span>{{ trend >= 0 ? '+' : '' }}{{ trend }}%</span>
        </div>
      </div>
      <div class="flex flex-col items-end gap-2">
        <div class="flex h-10 w-10 items-center justify-center rounded-xl" :class="iconBgClass">
          <svg class="h-5 w-5" :class="iconColorClass" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path v-if="icon === 'orders'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            <path v-else-if="icon === 'check'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            <path v-else-if="icon === 'clock'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            <path v-else-if="icon === 'truck'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17a2 2 0 11-4 0 2 2 0 014 0zM19 17a2 2 0 11-4 0 2 2 0 014 0z" />
            <path v-else-if="icon === 'gauge'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            <path v-else-if="icon === 'alert'" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>
        <svg v-if="sparkline.length >= 2" class="h-6 w-16" viewBox="0 0 64 24" preserveAspectRatio="none">
          <polyline
            fill="none"
            :stroke="sparkStroke"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            :points="sparkPoints"
          />
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title: string
  value: string | number
  icon: 'orders' | 'check' | 'clock' | 'truck' | 'gauge' | 'alert'
  color: 'blue' | 'green' | 'purple' | 'orange' | 'red'
  trend?: number
  sparkline?: number[]
}

const props = withDefaults(defineProps<Props>(), {
  trend: undefined,
  sparkline: () => [],
})

const colorMap: Record<string, { bg: string; text: string; accent: string; stroke: string }> = {
  blue:   { bg: 'bg-blue-50',     text: 'text-blue-600',     accent: 'bg-gradient-to-r from-blue-500 to-cyan-400',    stroke: '#3b82f6' },
  green:  { bg: 'bg-emerald-50',  text: 'text-emerald-600',  accent: 'bg-gradient-to-r from-emerald-500 to-teal-400', stroke: '#10b981' },
  purple: { bg: 'bg-violet-50',   text: 'text-violet-600',   accent: 'bg-gradient-to-r from-violet-500 to-fuchsia-400', stroke: '#8b5cf6' },
  orange: { bg: 'bg-amber-50',    text: 'text-amber-600',    accent: 'bg-gradient-to-r from-amber-500 to-orange-400',  stroke: '#f59e0b' },
  red:    { bg: 'bg-rose-50',     text: 'text-rose-600',     accent: 'bg-gradient-to-r from-rose-500 to-red-400',      stroke: '#f43f5e' },
}

const iconBgClass   = computed(() => colorMap[props.color]?.bg    ?? 'bg-slate-50')
const iconColorClass = computed(() => colorMap[props.color]?.text ?? 'text-slate-600')
const accentClass   = computed(() => colorMap[props.color]?.accent ?? 'bg-slate-400')
const sparkStroke   = computed(() => colorMap[props.color]?.stroke ?? '#64748b')

const sparkPoints = computed(() => {
  const data = props.sparkline
  if (data.length < 2) return ''
  const min = Math.min(...data)
  const max = Math.max(...data)
  const range = max - min || 1
  return data.map((v, i) => {
    const x = (i / (data.length - 1)) * 64
    const y = 24 - ((v - min) / range) * 20 - 2
    return `${x},${y}`
  }).join(' ')
})
</script>
