import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { api, ApiError } from '@/api/http'
import * as authApi from '@/api/auth'
import { useAuthStore } from '../auth'

vi.mock('@/api/auth')
vi.mock('@/api/http', async (orig) => {
  const http = await orig<typeof import('@/api/http')>()
  return { ...http, api: vi.fn<typeof http.api>() }
})

const me = {
  id: 1,
  email: 'a@x.com',
  username: 'a',
  displayName: 'A',
  avatarUrl: null,
  roles: ['MEMBER'],
  permissions: ['RECIPE_WRITE' as const],
}

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
  })

  it('can() checks permissions of me', () => {
    const s = useAuthStore()
    expect(s.can('RECIPE_WRITE')).toBe(false)
    s.me = me
    expect(s.isLoggedIn).toBe(true)
    expect(s.can('RECIPE_WRITE')).toBe(true)
    expect(s.can('USER_READ')).toBe(false)
    expect(s.canAny(['USER_READ', 'RECIPE_WRITE'])).toBe(true)
    expect(s.canAny(['USER_READ'])).toBe(false)
  })

  it('fetchMe sets me', async () => {
    vi.mocked(authApi.getMe).mockResolvedValue(me)
    const s = useAuthStore()
    await s.fetchMe()
    expect(s.me).toEqual(me)
  })

  it('fetchMe sets null on 401', async () => {
    vi.mocked(authApi.getMe).mockRejectedValue(new ApiError(401, 'x'))
    const s = useAuthStore()
    s.me = me
    await expect(s.fetchMe()).resolves.toBeUndefined()
    expect(s.me).toBeNull()
  })

  it('fetchMe rethrows other errors', async () => {
    vi.mocked(authApi.getMe).mockRejectedValue(new ApiError(0, 'down'))
    await expect(useAuthStore().fetchMe()).rejects.toMatchObject({ status: 0 })
  })

  it('login stores me from response', async () => {
    vi.mocked(authApi.login).mockResolvedValue(me)
    const s = useAuthStore()
    await s.login('a@x.com', 'pw')
    expect(authApi.login).toHaveBeenCalledWith('a@x.com', 'pw')
    expect(s.me).toEqual(me)
  })

  it('logout clears me and refetches the CSRF cookie', async () => {
    vi.mocked(authApi.logout).mockResolvedValue(undefined)
    vi.mocked(api).mockResolvedValue(undefined)
    const s = useAuthStore()
    s.me = me
    await s.logout()
    expect(s.me).toBeNull()
    expect(api).toHaveBeenCalledWith('/auth/csrf')
  })

  it('logout clears me even when the request fails', async () => {
    vi.mocked(authApi.logout).mockRejectedValue(new ApiError(0, 'down'))
    vi.mocked(api).mockResolvedValue(undefined)
    const s = useAuthStore()
    s.me = me
    await expect(s.logout()).rejects.toBeInstanceOf(ApiError)
    expect(s.me).toBeNull()
  })
})
