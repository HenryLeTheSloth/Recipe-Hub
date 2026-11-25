import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { api } from '@/api/http'
import type { Role } from '@/api/admin'
import AdminRolesPage from '../AdminRolesPage.vue'

vi.mock('@/api/http', async (orig) => {
  const http = await orig<typeof import('@/api/http')>()
  return { ...http, api: vi.fn<typeof http.api>() }
})

const roles: Role[] = [
  { id: 1, code: 'ADMIN', name: 'Quản trị', description: null, system: true,
    permissionCodes: ['CATALOG_MANAGE', 'RECIPE_WRITE'], userCount: 1 },
  { id: 5, code: 'EDITOR', name: 'Biên tập', description: null, system: false,
    permissionCodes: ['RECIPE_WRITE'], userCount: 0 },
]
const permissions = [
  { code: 'CATALOG_MANAGE', description: 'Quản lý danh mục' },
  { code: 'RECIPE_WRITE', description: 'Viết công thức' },
]

async function mountPage() {
  vi.mocked(api).mockImplementation(async (path: string) => {
    if (path === '/admin/roles') return roles
    if (path === '/admin/permissions') return permissions
    return { ...roles[1] }
  })
  const w = mount(AdminRolesPage, { global: { plugins: [PrimeVue], stubs: { teleport: true } } })
  await flushPromises()
  return w
}

describe('AdminRolesPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('lists roles', async () => {
    const w = await mountPage()
    expect(w.text()).toContain('EDITOR')
    expect(w.text()).toContain('Biên tập')
  })

  it('disables permission checkboxes for ADMIN role', async () => {
    const w = await mountPage()
    await w.find('[aria-label="Sửa ADMIN"]').trigger('click')
    await flushPromises()
    const boxes = w.findAll('input[type=checkbox]')
    expect(boxes.length).toBe(2)
    for (const b of boxes) expect(b.attributes('disabled')).toBeDefined()
  })

  it('saves selected permission codes', async () => {
    const w = await mountPage()
    await w.find('[aria-label="Sửa EDITOR"]').trigger('click')
    await flushPromises()
    await w.find('input[type=checkbox][value=CATALOG_MANAGE]').setValue(true)
    await w.find('form').trigger('submit')
    await flushPromises()
    const put = vi.mocked(api).mock.calls.find(([p, init]) => p === '/admin/roles/5' && init?.method === 'PUT')
    expect(put).toBeDefined()
    const json = put![1]!.json as { permissionCodes: string[] }
    expect(json.permissionCodes).toEqual(expect.arrayContaining(['CATALOG_MANAGE', 'RECIPE_WRITE']))
  })

  it('disables delete for system roles', async () => {
    const w = await mountPage()
    expect(w.find('[aria-label="Xóa ADMIN"]').attributes('disabled')).toBeDefined()
    expect(w.find('[aria-label="Xóa EDITOR"]').attributes('disabled')).toBeUndefined()
  })
})
