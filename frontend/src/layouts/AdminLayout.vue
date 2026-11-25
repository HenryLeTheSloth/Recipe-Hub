<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import type { Permission } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()

const ITEMS: { to: string; label: string; permission: Permission }[] = [
  { to: '/admin/users', label: 'Người dùng', permission: 'USER_READ' },
  { to: '/admin/roles', label: 'Vai trò', permission: 'ROLE_MANAGE' },
  { to: '/admin/categories', label: 'Danh mục', permission: 'CATALOG_MANAGE' },
  { to: '/admin/tags', label: 'Tag', permission: 'CATALOG_MANAGE' },
  { to: '/admin/ingredients', label: 'Nguyên liệu', permission: 'CATALOG_MANAGE' },
  { to: '/admin/recipes', label: 'Kiểm duyệt', permission: 'CONTENT_MODERATE' },
]
const items = computed(() => ITEMS.filter((i) => auth.can(i.permission)))
</script>

<template>
  <div class="admin">
    <aside>
      <RouterLink to="/" class="brand">🍲 Recipe Hub</RouterLink>
      <p class="section">Quản trị</p>
      <nav>
        <RouterLink v-for="i in items" :key="i.to" :to="i.to">{{ i.label }}</RouterLink>
      </nav>
      <p v-if="auth.me" class="who">{{ auth.me.displayName }}</p>
    </aside>
    <main>
      <p v-if="route.path === '/admin' || route.path === '/admin/'" class="welcome">Chọn một mục ở thanh bên.</p>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin { display: grid; grid-template-columns: 220px 1fr; min-height: 100vh; }
aside {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 1rem;
  background: var(--surface);
  border-right: 1px solid var(--border);
}
.brand { font-weight: 700; color: var(--text); text-decoration: none; margin-bottom: 1rem; }
.section { color: var(--muted); font-size: 0.8rem; text-transform: uppercase; letter-spacing: 0.05em; margin: 0; }
nav { display: flex; flex-direction: column; gap: 0.15rem; }
nav a { padding: 0.45rem 0.6rem; border-radius: 6px; color: var(--text); text-decoration: none; }
nav a.router-link-active { background: var(--bg); color: var(--accent); font-weight: 600; }
.who { margin-top: auto; color: var(--muted); font-size: 0.9rem; }
main { padding: 1.5rem; min-width: 0; }
.welcome { color: var(--muted); }
@media (max-width: 720px) {
  .admin { grid-template-columns: 1fr; grid-template-rows: auto 1fr; }
  aside { border-right: 0; border-bottom: 1px solid var(--border); }
  .who { display: none; }
}
</style>
