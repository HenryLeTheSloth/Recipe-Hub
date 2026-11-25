import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { api } from '@/api/http'
import AdminRecipesPage from '../AdminRecipesPage.vue'

vi.mock('@/api/http', async (orig) => {
  const http = await orig<typeof import('@/api/http')>()
  return { ...http, api: vi.fn<typeof http.api>() }
})

const card = {
  id: 1, slug: 'trung-chien', title: 'Trứng chiên', summary: null, coverUrl: null, categoryName: 'Món chính',
  difficulty: 'EASY', totalMinutes: 10, authorName: 'Cook', status: 'PENDING', publishedAt: null,
}

describe('AdminRecipesPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('reject dialog requires reason', async () => {
    vi.mocked(api).mockImplementation(async (path: string) =>
      path.startsWith('/admin/recipes?') ? { content: [card], page: { size: 20, number: 0, totalElements: 1, totalPages: 1 } } : undefined,
    )
    const w = mount(AdminRecipesPage, { global: { plugins: [PrimeVue], stubs: { teleport: true } } })
    await flushPromises()
    await w.findAll('button').find((b) => b.text() === 'Từ chối')!.trigger('click')
    const confirm = () => w.findAll('button').find((b) => b.text() === 'Xác nhận')!
    expect(confirm().attributes('disabled')).toBeDefined()
    await w.find('textarea').setValue('Thiếu ảnh')
    expect(confirm().attributes('disabled')).toBeUndefined()
    await w.find('form').trigger('submit')
    await flushPromises()
    expect(vi.mocked(api)).toHaveBeenCalledWith('/admin/recipes/1/reject', { method: 'POST', json: { reason: 'Thiếu ảnh' } })
  })
})
