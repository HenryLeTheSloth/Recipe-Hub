import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import type { Permission } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import AdminLayout from '../AdminLayout.vue'

async function mountWith(permissions: Permission[]) {
  const pinia = createPinia()
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/:rest(.*)*', component: { render: () => null } }],
  })
  await router.push('/admin')
  const w = mount(AdminLayout, { global: { plugins: [pinia, router] } })
  useAuthStore().me = {
    id: 1, email: 'a@x.com', username: 'a', displayName: 'A', avatarUrl: null, roles: [], permissions,
  }
  await w.vm.$nextTick()
  return w
}

describe('AdminLayout', () => {
  it('hides roles link without ROLE_MANAGE', async () => {
    const w = await mountWith(['USER_READ'])
    expect(w.text()).toContain('Người dùng')
    expect(w.text()).not.toContain('Vai trò')
  })

  it('hides users link without USER_READ', async () => {
    const w = await mountWith(['ROLE_MANAGE'])
    expect(w.text()).toContain('Vai trò')
    expect(w.text()).not.toContain('Người dùng')
  })
})
