import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { api, ApiError } from '@/api/http'
import type { Ingredient } from '@/api/catalog'
import AdminIngredientsPage from '../AdminIngredientsPage.vue'

vi.mock('@/api/http', async (orig) => {
  const http = await orig<typeof import('@/api/http')>()
  return { ...http, api: vi.fn<typeof http.api>() }
})

const items: Ingredient[] = [
  { id: 7, name: 'Lá mơ', slug: 'la-mo', group: 'VEGETABLE', emoji: '🌿', status: 'PENDING' },
  { id: 1, name: 'Cà chua', slug: 'ca-chua', group: 'VEGETABLE', emoji: '🍅', status: 'APPROVED' },
]

async function mountPage(onWrite: () => Promise<unknown> = async () => items[0]) {
  vi.mocked(api).mockImplementation(async (path: string, init?: RequestInit) => {
    if (path.startsWith('/admin/ingredients?'))
      return { content: items, page: { size: 20, number: 0, totalElements: 2, totalPages: 1 } }
    if (init?.method) return onWrite()
    return undefined
  })
  const w = mount(AdminIngredientsPage, { global: { plugins: [PrimeVue], stubs: { teleport: true } } })
  await flushPromises()
  return w
}

describe('AdminIngredientsPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    // jsdom không có matchMedia; paginator của DataTable dùng nó.
    window.matchMedia = vi.fn<typeof window.matchMedia>(
      () => ({ matches: false, addEventListener() {}, removeEventListener() {} }) as unknown as MediaQueryList,
    )
  })

  it('lists ingredients with group label and status', async () => {
    const w = await mountPage()
    expect(w.text()).toContain('Lá mơ')
    expect(w.text()).toContain('Rau củ')
    expect(w.text()).toContain('Chờ duyệt')
  })

  it('approve sends status APPROVED', async () => {
    const w = await mountPage()
    expect(w.find('[aria-label="Duyệt Cà chua"]').exists()).toBe(false)
    await w.find('[aria-label="Duyệt Lá mơ"]').trigger('click')
    await flushPromises()
    const put = vi.mocked(api).mock.calls.find(([p, i]) => p === '/admin/ingredients/7' && i?.method === 'PUT')
    expect(put).toBeDefined()
    expect(put![1]!.json).toEqual({ name: 'Lá mơ', group: 'VEGETABLE', emoji: '🌿', status: 'APPROVED' })
  })

  it('shows IN_USE message on delete conflict', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const w = await mountPage(async () => {
      throw new ApiError(409, 'Đang được sử dụng', 'IN_USE')
    })
    await w.find('[aria-label="Xóa Cà chua"]').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Nguyên liệu đang được dùng, hãy gộp thay vì xóa')
  })
})
