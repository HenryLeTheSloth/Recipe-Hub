import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { createMemoryHistory, createRouter } from 'vue-router'
import { api, ApiError } from '@/api/http'
import RecipeEditorPage from '../RecipeEditorPage.vue'

vi.mock('@/api/http', async (orig) => {
  const http = await orig<typeof import('@/api/http')>()
  return { ...http, api: vi.fn<typeof http.api>() }
})

const detail = (status: string) => ({
  id: 1, slug: 'trung-chien', title: 'Trứng chiên', summary: null, category: null, tags: [], servings: 2,
  prepMinutes: 5, cookMinutes: 5, difficulty: 'EASY', status, coverUrl: null, coverMediaId: null,
  author: { id: 7, username: 'cook', displayName: 'Cook', avatarUrl: null },
  ingredients: [], steps: [], rejectionReason: null, publishedAt: null, createdAt: '', updatedAt: '',
})

async function mountAt(url: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/me/recipes/new', component: RecipeEditorPage },
      { path: '/me/recipes/:id/edit', component: RecipeEditorPage },
      { path: '/:rest(.*)*', component: { render: () => null } },
    ],
  })
  await router.push(url)
  await router.isReady()
  const w = mount(RecipeEditorPage, {
    global: { plugins: [createPinia(), router, PrimeVue], stubs: { teleport: true } },
  })
  await flushPromises()
  return w
}

describe('RecipeEditorPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('editor warns when editing published recipe', async () => {
    vi.mocked(api).mockImplementation(async (path: string) => (path === '/me/recipes/1' ? detail('PUBLISHED') : []))
    const w = await mountAt('/me/recipes/1/edit')
    expect(w.text()).toContain('Lưu thay đổi sẽ gửi công thức đi duyệt lại')
  })

  it('editor shows missing fields from RECIPE_INCOMPLETE', async () => {
    vi.mocked(api).mockImplementation(async (path: string, init?: RequestInit) => {
      if (path === '/recipes' && init?.method === 'POST') return detail('DRAFT')
      if (path === '/recipes/1/submit') throw new ApiError(400, 'Còn thiếu: danh mục', 'RECIPE_INCOMPLETE')
      return []
    })
    const w = await mountAt('/me/recipes/new')
    await w.find('input[name=title]').setValue('Trứng chiên')
    await w.findAll('button').find((b) => b.text() === 'Gửi duyệt')!.trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Còn thiếu: danh mục')
  })

  it('editor stays read-only when the recipe is not found', async () => {
    vi.mocked(api).mockImplementation(async (path: string) => {
      if (path === '/me/recipes/9') throw new ApiError(404, 'Không tìm thấy công thức', 'NOT_FOUND')
      return []
    })
    const w = await mountAt('/me/recipes/9/edit')
    expect(w.text()).toContain('Không tìm thấy công thức')
    expect(w.find('fieldset').attributes('disabled')).toBeDefined()
    expect(w.findAll('button').some((b) => b.text() === 'Lưu nháp')).toBe(false)
  })

  it('a finishing upload attaches to its own step after an earlier step is deleted', async () => {
    let finish!: (v: unknown) => void
    const steps = ['A', 'B', 'C'].map((content, i) => ({ stepNo: i + 1, content, imageUrl: null, mediaId: null }))
    vi.mocked(api).mockImplementation(async (path: string, init?: RequestInit) => {
      if (path === '/me/recipes/1') return { ...detail('DRAFT'), steps }
      if (path === '/media') return new Promise((r) => (finish = r))
      if (path === '/recipes/1' && init?.method === 'PUT') return detail('DRAFT')
      return []
    })
    const w = await mountAt('/me/recipes/1/edit')
    const input = w.findAll('.steps input[type=file]')[1]!
    Object.defineProperty(input.element, 'files', { value: [new File([new Uint8Array(10)], 'b.png', { type: 'image/png' })] })
    await input.trigger('change')
    await w.findAll('button[aria-label="Xóa bước"]')[0]!.trigger('click')
    finish({ id: 5, url: '/api/media/5/file' })
    await flushPromises()
    await w.findAll('button').find((b) => b.text() === 'Lưu nháp')!.trigger('click')
    await flushPromises()
    const put = vi.mocked(api).mock.calls.find(([p, i]) => p === '/recipes/1' && i?.method === 'PUT')!
    const body = (put[1] as { json: { steps: { content: string; mediaId: number | null }[] } }).json
    expect(body.steps.map((s: { content: string; mediaId: number | null }) => [s.content, s.mediaId])).toEqual([['B', 5], ['C', null]])
  })
})
