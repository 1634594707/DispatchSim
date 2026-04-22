<template>
  <MainLayout />
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import MainLayout from '@/components/MainLayout.vue'
import { connectWebsocket, disconnectWebsocket } from '@/services/websocket'

onMounted(() => {
  connectWebsocket()
})

onUnmounted(() => {
  disconnectWebsocket()
})
</script>
