import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    rolldownOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = id.replace(/\\/g, '/')

          if (normalizedId.includes('node_modules/zrender')) {
            return 'zrender-vendor'
          }

          if (normalizedId.includes('node_modules/echarts/core')) {
            return 'echarts-core'
          }

          if (normalizedId.includes('node_modules/echarts/charts')) {
            return 'echarts-charts'
          }

          if (normalizedId.includes('node_modules/echarts/components')) {
            return 'echarts-components'
          }

          if (normalizedId.includes('node_modules/echarts/renderers')) {
            return 'echarts-renderers'
          }

          if (normalizedId.includes('node_modules/echarts')) {
            return 'echarts-vendor'
          }

          if (normalizedId.includes('node_modules/@stomp') || normalizedId.includes('node_modules/sockjs')) {
            return 'realtime-vendor'
          }
        },
      },
    },
    chunkSizeWarningLimit: 700,
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  }
})
