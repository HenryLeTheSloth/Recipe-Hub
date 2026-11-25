import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import PrimeVue from 'primevue/config'
import { api } from '@/api/http'
import RecipeListPage from '../RecipeListPage.vue'

vi.mock('@/api/http', async (orig) => {
  const http = await orig<typeof import('@/api/http')>()
  return { ...http, api: vi.fn<typeof http.api>() }
})

const card = {
  id: 1, slug: 'trung-chien', title: 'Trứng chiên', summary: null, coverUrl: null, categoryName: 'Món chính',
  difficulty: 'EASY', totalMinutes: 10, authorName: 'Cook', status: 'PUBLISHED', publishedAt: '2026-11-01T00:00:00Z',
}

describe('RecipeListPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    window.matchMedia = vi.fn<typeof window.matchMedia>(
      () => ({ matches: false, addEventListener() {}, removeEventListener() {} }) as unknown as MediaQueryList,
    )
  })

  it('list page calls API with ingredientIds from URL', async () => {
    vi.mocked(api).mockImplementation(async (path: string) => {
      if (path.startsWith('/recipes?')) return { content: [card], page: { size: 12, number: 0, totalElements: 1, totalPages: 1 } }
      if (path.startsWith('/ingredients')) return [
        { id: 3, name: 'Trứng gà', slug: 'trung-ga', group: 'EGG_DAIRY', emoji: '🥚', status: 'APPROVED' },
        { id: 4, name: 'Cà chua', slug: 'ca-chua', group: 'VEGETABLE', emoji: '🍅', status: 'APPROVED' },
      ]
      return []
    })
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/recipes', component: RecipeListPage },
        { path: '/:rest(.*)*', component: { render: () => null } },
      ],
    })
    await router.push('/recipes?ingredientIds=3,4')
    await router.isReady()
    const w = mount(RecipeListPage, { global: { plugins: [PrimeVue, router], stubs: { teleport: true } } })
    await flushPromises()

    const call = vi.mocked(api).mock.calls.find(([p]) => p.startsWith('/recipes?'))!
    expect(new URLSearchParams(call[0].split('?')[1]).get('ingredientIds')).toBe('3,4')
    expect(w.text()).toContain('Trứng chiên')
    expect(w.text()).toContain('🥚')
  })
})
