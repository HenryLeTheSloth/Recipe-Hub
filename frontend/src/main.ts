import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Aura from '@primeuix/themes/aura'

import './styles/base.css'
import App from './App.vue'
import { api } from './api/http'
import router from './router'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
app.use(createPinia())

// Lấy cookie CSRF và phiên hiện tại TRƯỚC khi router chạy guard lần đầu (reload /account không bị đá về /login).
try {
  await api('/auth/csrf')
  await useAuthStore().fetchMe()
} catch {
  // Backend chưa chạy: vẫn mount; từng trang tự báo lỗi khi gọi API.
}

app.use(router)
app.use(PrimeVue, { theme: { preset: Aura } })
app.mount('#app')
