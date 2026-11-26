import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  // Dùng chung .env ở gốc repo; chỉ VITE_* và PRIMEUI_* được đưa ra client (ADMIN_* thì không)
  envDir: '..',
  envPrefix: ['VITE_', 'PRIMEUI_'],
  server: {
    proxy: { '/api': 'http://localhost:8080' },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
