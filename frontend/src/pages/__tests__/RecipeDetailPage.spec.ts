import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { api, ApiError } from '@/api/http'
import RecipeDetailPage from '../RecipeDetailPage.vue'

vi.mock('@/api/http', async (orig) => {
  const http = await orig<typeof import('@/api/http')>()
  return { ...http, api: vi.fn<typeof http.api>() }
})

const detail = {
  id: 1, slug: 'trung-chien', title: 'Trứng chiên', summary: 'Nhanh', category: { id: 1, name: 'Món chính', slug: 'mon-chinh', sortOrder: 1 },
  tags: [], servings: 2, prepMinutes: 5, cookMinutes: 5, difficulty: 'EASY', status: 'REJECTED', coverUrl: null,
  author: { id: 7, username: 'cook', displayName: 'Cook', avatarUrl: null },
  ingredients: [{ ingredientId: 3, name: 'Trứng gà', emoji: '🥚', quantity: 2, unit: 'PIECE', note: null }],
  steps: [{ stepNo: 1, content: 'Chiên', imageUrl: null }],
  rejectionReason: 'Thiếu ảnh', publishedAt: null, createdAt: '2026-11-01T00:00:00Z', updatedAt: '2026-11-01T00:00:00Z',
}

async function mountAt(slug: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/recipes/:slug', component: RecipeDetailPage },
      { path: '/:rest(.*)*', component: { render: () => null } },
    ],
  })
  await router.push('/recipes/' + slug)
  await router.isReady()
  const w = mount(RecipeDetailPage, { global: { plugins: [createPinia(), router] } })
  await flushPromises()
  return w
}

describe('RecipeDetailPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('detail shows not-found message on 404', async () => {
    vi.mocked(api).mockRejectedValue(new ApiError(404, 'x', 'NOT_FOUND'))
    expect((await mountAt('nope')).text()).toContain('Không tìm thấy công thức')
  })

  it('detail shows rejection reason to author', async () => {
    vi.mocked(api).mockResolvedValue(detail)
    const w = await mountAt('trung-chien')
    expect(vi.mocked(api)).toHaveBeenCalledWith('/recipes/trung-chien')
    expect(w.text()).toContain('Thiếu ảnh')
    expect(w.text()).toContain('2 cái/quả')
  })
})
