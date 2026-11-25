import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { ApiError } from '@/api/http'
import * as authApi from '@/api/auth'
import LoginPage from '../LoginPage.vue'

vi.mock('@/api/auth')

async function mountAt(url: string) {
  const stub = { render: () => null }
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: LoginPage },
      { path: '/:rest(.*)*', component: stub },
    ],
  })
  await router.push(url)
  await router.isReady()
  const w = mount(LoginPage, { global: { plugins: [createPinia(), router] } })
  return { w, router }
}

async function submit(w: Awaited<ReturnType<typeof mountAt>>['w']) {
  await w.find('input[type=email]').setValue('a@x.com')
  await w.find('input[type=password]').setValue('password1')
  await w.find('form').trigger('submit')
  await flushPromises()
}

describe('LoginPage', () => {
  beforeEach(() => vi.resetAllMocks())

  it('shows resend link when email not verified', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(403, 'x', 'EMAIL_NOT_VERIFIED'))
    vi.mocked(authApi.resendVerification).mockResolvedValue(undefined)
    const { w } = await mountAt('/login')
    await submit(w)
    expect(w.text()).toContain('Email chưa được xác thực')
    const resend = w.findAll('button').find((b) => b.text() === 'Gửi lại email xác thực')
    expect(resend).toBeDefined()
    await resend!.trigger('click')
    await flushPromises()
    expect(authApi.resendVerification).toHaveBeenCalledWith('a@x.com')
    expect(w.text()).toContain('Đã gửi lại')
  })

  it('shows plain error without resend for bad credentials', async () => {
    vi.mocked(authApi.login).mockRejectedValue(new ApiError(401, 'x', 'BAD_CREDENTIALS'))
    const { w } = await mountAt('/login')
    await submit(w)
    expect(w.text()).toContain('Email hoặc mật khẩu không đúng')
    expect(w.text()).not.toContain('Gửi lại email xác thực')
  })

  it('navigates to safe redirect after login', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      id: 1, email: 'a@x.com', username: 'a', displayName: 'A', avatarUrl: null, roles: [], permissions: [],
    })
    const { w, router } = await mountAt('/login?redirect=//evil.com')
    await submit(w)
    expect(router.currentRoute.value.fullPath).toBe('/')
  })

  it('navigates to local redirect after login', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      id: 1, email: 'a@x.com', username: 'a', displayName: 'A', avatarUrl: null, roles: [], permissions: [],
    })
    const { w, router } = await mountAt('/login?redirect=/account')
    await submit(w)
    expect(router.currentRoute.value.fullPath).toBe('/account')
  })

  it('disables submit while sending', async () => {
    let resolve!: (v: never) => void
    vi.mocked(authApi.login).mockReturnValue(new Promise((r) => (resolve = r)))
    const { w } = await mountAt('/login')
    await w.find('input[type=email]').setValue('a@x.com')
    await w.find('input[type=password]').setValue('password1')
    await w.find('form').trigger('submit')
    expect(w.find('button[type=submit]').attributes('disabled')).toBeDefined()
    resolve(undefined as never)
  })
})
