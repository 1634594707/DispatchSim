<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="$emit('close')">
    <div class="bg-white rounded-2xl shadow-xl max-w-md w-full mx-4 p-6">
      <h3 class="text-xl font-semibold text-text mb-4">
        {{ mode === 'create' ? '新建出货点' : '编辑出货点' }}
      </h3>

      <form @submit.prevent="handleSubmit">
        <!-- Name -->
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-1">
            名称 <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.name"
            type="text"
            required
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
            placeholder="例如：主仓库"
          />
        </div>

        <!-- Position -->
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-1">
            位置 <span class="text-red-500">*</span>
          </label>
          <div class="flex gap-2">
            <input
              v-model.number="form.position.x"
              type="number"
              required
              min="0"
              :max="MAP_WIDTH"
              step="0.1"
              class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
              :placeholder="`X (0-${MAP_WIDTH})`"
            />
            <input
              v-model.number="form.position.y"
              type="number"
              required
              min="0"
              :max="MAP_HEIGHT"
              step="0.1"
              class="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
              :placeholder="`Y (0-${MAP_HEIGHT})`"
            />
          </div>
          <p class="text-xs text-gray-500 mt-1">提示：可以在地图上点击选择位置</p>
        </div>

        <!-- Icon -->
        <div class="mb-6">
          <label class="block text-sm font-medium text-gray-700 mb-1">
            图标
          </label>
          <div class="flex gap-2">
            <input
              v-model="form.icon"
              type="text"
              maxlength="2"
              class="w-20 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 text-center text-xl"
              placeholder="📍"
            />
            <div class="flex-1 flex flex-wrap gap-1">
              <button
                v-for="emoji in commonEmojis"
                :key="emoji"
                type="button"
                class="w-10 h-10 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-xl"
                @click="form.icon = emoji"
              >
                {{ emoji }}
              </button>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex gap-2">
          <button
            type="button"
            class="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors font-medium"
            @click="$emit('close')"
          >
            取消
          </button>
          <button
            type="submit"
            class="flex-1 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors font-medium"
          >
            {{ mode === 'create' ? '创建' : '保存' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { MAP_WIDTH, MAP_HEIGHT } from '@/constants/map'
import type { Depot } from '@/types'

interface Props {
  depot?: Depot | null
  mode: 'create' | 'edit'
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
  save: [depot: Omit<Depot, 'id' | 'createdAt'> | Depot]
}>()

const commonEmojis = ['📍', '🏢', '📦', '🏭', '🏪', '🏬', '🏛️', '🏗️']

const form = ref({
  name: '',
  position: { x: 20, y: 20 },
  icon: '📍',
})

// Initialize form with depot data if editing
watch(() => props.depot, (depot) => {
  if (depot) {
    form.value = {
      name: depot.name,
      position: { ...depot.position },
      icon: depot.icon || '📍',
    }
  }
}, { immediate: true })

const handleSubmit = () => {
  if (props.mode === 'edit' && props.depot) {
    emit('save', {
      ...props.depot,
      ...form.value,
    })
  } else {
    emit('save', form.value)
  }
}
</script>
