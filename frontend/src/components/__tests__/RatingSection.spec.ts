import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { listRatings } from '@/api/community'
import { useAuthStore } from '@/stores/auth'
import RatingSection from '../RatingSection.vue'

vi.mock('@/api/community', () => ({ listRatings: vi.fn<typeof listRatings>(), putMyRating: vi.fn(), deleteMyRating: vi.fn(), hideRating: vi.fn() }))

async function setup(meId: number, authorId: number) {
  const pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().me = { id: meId, email: 'a@b.c', username: 'a', displayName: 'A', avatarUrl: null, roles: [], permissions: ['COMMUNITY_PARTICIPATE'] }
  vi.mocked(listRatings).mockResolvedValue({
    content: [{ id: 1, stars: 4, review: 'Ngon', author: { id: 9, username: 'm', displayName: 'M', avatarUrl: null }, hidden: false, updatedAt: '' }],
    page: { size: 20, number: 0, totalElements: 1, totalPages: 1 },
  })
  const w = mount(RatingSection, { props: { recipeId: 5, authorId }, global: { plugins: [pinia], stubs: { RouterLink: true } } })
  await flushPromises()
  return w
}

describe('RatingSection', () => {
  beforeEach(() => vi.resetAllMocks())

  it('author does not see rating form', async () => {
    const w = await setup(7, 7)
    expect(w.find('form').exists()).toBe(false)
    expect(w.text()).toContain('Ngon')
  })

  it('prefills form from own rating and offers delete', async () => {
    const w = await setup(9, 7)
    expect(w.find('textarea').element.value).toBe('Ngon')
    expect(w.text()).toContain('Xóa đánh giá')
  })
})
