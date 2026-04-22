import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import './style.css'
import App from './App.vue'
import { reportError } from './utils/errorHandler'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

app.config.errorHandler = (error) => {
  reportError(error, '界面运行异常')
}

window.addEventListener('unhandledrejection', (event) => {
  reportError(event.reason, '未处理的异步异常')
})

app.mount('#app')
