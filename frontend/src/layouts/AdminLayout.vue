<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import type { Permission } from '@/api/auth'
import BrandMark from '@/components/BrandMark.vue'
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
      <RouterLink to="/" class="brand"><BrandMark />Recipe Hub</RouterLink>
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
.admin { display: grid; grid-template-columns: 232px 1fr; min-height: 100vh; }
aside {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 1rem;
  background: var(--surface);
  border-right: 1px solid var(--border);
}
.brand { display: inline-flex; align-items: center; gap: 0.5rem; font-weight: 800; font-size: 1.25rem; color: var(--accent); text-decoration: none; margin-bottom: 1rem; }
.section { color: var(--muted); font-weight: 700; font-size: 0.9rem; margin: 0 0 0.25rem; padding: 0 0.75rem; }
nav { display: flex; flex-direction: column; gap: 0.15rem; }
nav a { display: flex; align-items: center; min-height: 44px; padding: 0 0.75rem; border-radius: var(--radius); color: var(--text); font-weight: 500; text-decoration: none; }
nav a:hover { background: var(--sage); color: var(--accent-deep); }
nav a.router-link-active { background: var(--accent); color: #fff; font-weight: 600; }
.who { margin-top: auto; color: var(--muted); font-size: 0.9rem; }
main { padding: 1.5rem; min-width: 0; }
.welcome { color: var(--muted); }
@media (max-width: 720px) {
  .admin { grid-template-columns: 1fr; grid-template-rows: auto 1fr; }
  aside { border-right: 0; border-bottom: 1px solid var(--border); }
  nav { flex-direction: row; overflow-x: auto; scrollbar-width: none; }
  nav a { flex: none; }
  .who { display: none; }
}
</style>
