import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { addFavorite, removeFavorite } from '@/api/community'
import { ApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import FavoriteButton from '../FavoriteButton.vue'

vi.mock('@/api/community', () => ({ addFavorite: vi.fn<typeof addFavorite>(), removeFavorite: vi.fn<typeof removeFavorite>() }))

async function setup(loggedIn: boolean) {
  const pinia = createPinia()
  setActivePinia(pinia)
  if (loggedIn) useAuthStore().me = { id: 1, email: 'a@b.c', username: 'a', displayName: 'A', avatarUrl: null, roles: [], permissions: [] }
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/:p(.*)*', component: { render: () => null } }] })
  await router.push('/recipes/x')
  const w = mount(FavoriteButton, { props: { recipeId: 5, favorited: false }, global: { plugins: [pinia, router] } })
  return { w, router }
}

describe('FavoriteButton', () => {
  beforeEach(() => vi.resetAllMocks())

  it('rolls back on error', async () => {
    let fail!: (e: Error) => void
    vi.mocked(addFavorite).mockReturnValue(new Promise((_, rej) => (fail = rej)))
    const { w } = await setup(true)
    const btn = w.find('button')
    expect(btn.attributes('aria-pressed')).toBe('false')
    await btn.trigger('click')
    expect(btn.attributes('aria-pressed')).toBe('true') // lạc quan
    fail(new ApiError(500, 'Lỗi máy chủ'))
    await flushPromises()
    expect(btn.attributes('aria-pressed')).toBe('false')
    expect(w.find('[role=alert]').text()).toContain('Lỗi máy chủ')
  })

  it('ignores a second click while a request is pending', async () => {
    vi.mocked(addFavorite).mockReturnValue(new Promise(() => {}))
    const { w } = await setup(true)
    await w.find('button').trigger('click')
    await w.find('button').trigger('click')
    expect(addFavorite).toHaveBeenCalledTimes(1)
    expect(removeFavorite).not.toHaveBeenCalled()
  })

  it('redirects anonymous to login', async () => {
    const { w, router } = await setup(false)
    await w.find('button').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/login?redirect=%2Frecipes%2Fx')
    expect(addFavorite).not.toHaveBeenCalled()
  })
})
