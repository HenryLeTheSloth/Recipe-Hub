<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import SearchIcon from '@primeicons/vue/search'
import BrandMark from '@/components/BrandMark.vue'
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
    <RouterLink to="/" class="brand"><BrandMark />Recipe Hub</RouterLink>
    <nav aria-label="Chính">
      <RouterLink to="/recipes">Công thức</RouterLink>
      <template v-if="auth.me">
        <RouterLink v-if="auth.can('RECIPE_WRITE')" to="/me/recipes">Của tôi</RouterLink>
        <RouterLink v-if="auth.canAny(ADMIN_PERMISSIONS)" to="/admin">Quản trị</RouterLink>
        <RouterLink to="/account" class="me">{{ auth.me.displayName }}</RouterLink>
      </template>
      <template v-else>
        <RouterLink to="/login">Đăng nhập</RouterLink>
        <RouterLink to="/register" class="btn primary">Đăng ký</RouterLink>
      </template>
    </nav>
    <form class="search" role="search" @submit.prevent="search">
      <SearchIcon class="icon" aria-hidden="true" />
      <input v-model="q" type="search" placeholder="Tìm món theo tên" aria-label="Tìm công thức" class="input" />
    </form>
  </header>
  <main class="page">
    <RouterView />
  </main>
  <footer class="foot">
    <BrandMark />
    <p>Recipe Hub. Sổ công thức nấu từ những gì bạn đang có.</p>
  </footer>
</template>

<style scoped>
.topbar {
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-areas: 'brand search' 'nav nav';
  align-items: center;
  gap: 0.25rem 0.75rem;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0.75rem 1rem;
}
@media (min-width: 860px) {
  .topbar { grid-template-columns: auto minmax(240px, 360px) 1fr; grid-template-areas: 'brand search nav'; padding: 1rem 1.5rem; }
}
.brand {
  grid-area: brand;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 1.45rem;
  letter-spacing: -0.01em;
  color: var(--ink);
  text-decoration: none;
}
/* Mobile: liên kết thành một dải kẻ vuốt ngang, không gãy dòng. */
nav { grid-area: nav; display: flex; align-items: center; gap: 1.25rem; overflow-x: auto; scrollbar-width: none; border-top: 1px solid var(--rule); white-space: nowrap; }
nav .btn { min-height: 44px; padding: 0.3rem 0.9rem; margin-left: auto; }
@media (min-width: 860px) {
  .brand { font-size: 1.6rem; }
  nav { justify-content: flex-end; gap: 0.25rem 1rem; overflow: visible; border-top: 0; }
  nav .btn { min-height: 44px; padding: 0.55rem 1.1rem; margin-left: 0; }
}
nav a:not(.btn) { color: var(--ink); text-decoration: none; font-weight: 500; padding: 0.6rem 0; }
nav a:not(.btn):hover { color: var(--red); }
nav a.router-link-active:not(.btn) { color: var(--red); text-decoration: underline; text-decoration-thickness: 2px; text-underline-offset: 0.45em; }
.me { max-width: 12ch; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.search { grid-area: search; position: relative; }
.search .icon { position: absolute; left: 0.8rem; top: 50%; translate: 0 -50%; width: 1rem; height: 1rem; color: var(--muted); pointer-events: none; }
.search input { width: 100%; padding-left: 2.3rem; background: var(--paper); border-color: var(--field); }
.page { max-width: 1200px; margin: 0 auto; padding: 0.5rem 1rem 3rem; }
@media (min-width: 860px) { .page { padding: 0.5rem 1.5rem 4rem; } }
.foot {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  max-width: 1200px;
  margin: 0 auto;
  padding: 1.5rem 1rem 2.5rem;
  border-top: 1px solid var(--rule);
  color: var(--muted);
  font-size: 0.9rem;
}
.foot p { margin: 0; }
</style>
