<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ADMIN_PERMISSIONS } from '@/router/guard'

const auth = useAuthStore()
const router = useRouter()
const q = ref('')
function search() {
  router.push({ path: '/recipes', query: q.value.trim() ? { q: q.value.trim() } : {} })
  q.value = ''
}
</script>

<template>
  <header class="topbar">
    <RouterLink to="/" class="brand">🍲 Recipe Hub</RouterLink>
    <form class="search" role="search" @submit.prevent="search">
      <input v-model="q" type="search" placeholder="Tìm công thức…" aria-label="Tìm công thức" />
    </form>
    <nav>
      <RouterLink to="/recipes">Công thức</RouterLink>
      <template v-if="auth.me">
        <RouterLink v-if="auth.can('RECIPE_WRITE')" to="/me/recipes">Của tôi</RouterLink>
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
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 0.75rem 1.25rem;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.brand { font-weight: 700; font-size: 1.1rem; color: var(--text); text-decoration: none; }
nav { display: flex; gap: 1rem; align-items: center; }
nav a:not(.btn) { color: var(--text); text-decoration: none; }
nav .btn { text-decoration: none; padding: 0.4rem 0.9rem; }
.search { flex: 1 1 200px; max-width: 360px; }
.search input {
  width: 100%; box-sizing: border-box; font: inherit; padding: 0.45rem 0.75rem;
  border: 1px solid var(--border); border-radius: 999px; background: var(--bg); color: var(--text);
}
.page { padding: 0 1rem; }
</style>
