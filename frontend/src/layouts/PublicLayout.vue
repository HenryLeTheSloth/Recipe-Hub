<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ADMIN_PERMISSIONS } from '@/router/guard'

const auth = useAuthStore()
</script>

<template>
  <header class="topbar">
    <RouterLink to="/" class="brand">🍲 Recipe Hub</RouterLink>
    <nav>
      <template v-if="auth.me">
        <RouterLink v-if="auth.canAny(ADMIN_PERMISSIONS)" to="/admin">Quản trị</RouterLink>
        <RouterLink to="/account">{{ auth.me.displayName }}</RouterLink>
      </template>
      <template v-else>
        <RouterLink to="/login">Đăng nhập</RouterLink>
        <RouterLink to="/register" class="btn primary">Đăng ký</RouterLink>
      </template>
    </nav>
  </header>
  <main class="page">
    <RouterView />
  </main>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.25rem;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.brand { font-weight: 700; font-size: 1.1rem; color: var(--text); text-decoration: none; }
nav { display: flex; gap: 1rem; align-items: center; }
nav a:not(.btn) { color: var(--text); text-decoration: none; }
nav .btn { text-decoration: none; padding: 0.4rem 0.9rem; }
.page { padding: 0 1rem; }
</style>
