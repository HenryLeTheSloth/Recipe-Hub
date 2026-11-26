import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Aura from '@primeuix/themes/aura'
import { definePreset } from '@primeuix/themes'

import './styles/base.css'
import App from './App.vue'
import { api } from './api/http'
import router from './router'
import { onSessionLost } from './router/guard'
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
window.addEventListener('api:unauthenticated', () => onSessionLost(router, useAuthStore()))
// Admin giữ PrimeVue nhưng mang màu tờ lịch: đỏ lịch làm màu chính, giấy ngà làm nền.
const Bloc = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#fdf1f0', 100: '#fadcd9', 200: '#f4b5af', 300: '#ec867d', 400: '#e1554a', 500: '#d2281d',
      600: '#b8221a', 700: '#a81e15', 800: '#851912', 900: '#6b1610', 950: '#3d0b08',
    },
    colorScheme: { light: { surface: { 0: '#ffffff', 50: '#fbfaf6', 100: '#f4f3ee', 200: '#e9e6de', 300: '#d9d4c9', 400: '#b9b2a5', 500: '#8c857a', 600: '#5f5951', 700: '#45403a', 800: '#2e2a26', 900: '#1d1a17', 950: '#110f0d' } } },
  },
})
// PrimeVue 5 dùng giấy phép PrimeUI: key Community miễn phí cho cá nhân, đặt PRIMEUI_KEY trong .env ở gốc repo (không commit).
// darkModeSelector trỏ vào class không bao giờ bật: trang chỉ có giao diện sáng.
app.use(PrimeVue, { theme: { preset: Bloc, options: { darkModeSelector: '.app-dark' } }, license: import.meta.env.PRIMEUI_KEY })
app.mount('#app')
