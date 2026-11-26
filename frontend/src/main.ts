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
// Admin giữ PrimeVue nhưng mang màu Bếp xanh: xanh rừng làm màu chính, kem làm nền.
const Kitchen = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#eef5f1', 100: '#d5e6dc', 200: '#aecdb9', 300: '#7fae93', 400: '#4f8a6c', 500: '#2f6b50',
      600: '#245c45', 700: '#1e4d3a', 800: '#173d2e', 900: '#143528', 950: '#0b1f17',
    },
    colorScheme: { light: { surface: { 0: '#ffffff', 50: '#fbfaf6', 100: '#f7f4ec', 200: '#ece8dd', 300: '#e3ded2', 400: '#b9bdb6', 500: '#767c75', 600: '#5b635e', 700: '#434a46', 800: '#2d3430', 900: '#1c2420', 950: '#111714' } } },
  },
})
// PrimeVue 5 dùng giấy phép PrimeUI: key Community miễn phí cho cá nhân, đặt PRIMEUI_KEY trong .env ở gốc repo (không commit).
// darkModeSelector trỏ vào class không bao giờ bật: trang chỉ có giao diện sáng.
app.use(PrimeVue, { theme: { preset: Kitchen, options: { darkModeSelector: '.app-dark' } }, license: import.meta.env.PRIMEUI_KEY })
app.mount('#app')
