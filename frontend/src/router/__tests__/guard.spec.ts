import { describe, expect, it, vi } from 'vitest'
import type { RouteLocationNormalized, RouteMeta } from 'vue-router'
import type { Permission } from '@/api/auth'
import { ADMIN_PERMISSIONS, guard, onSessionLost } from '../guard'

function route(fullPath: string, meta: RouteMeta = {}) {
  return { fullPath, path: fullPath, meta } as RouteLocationNormalized
}
function user(perms: Permission[] | null) {
  return { isLoggedIn: perms !== null, canAny: (ps: Permission[]) => ps.some((p) => perms?.includes(p) ?? false) }
}
const anon = user(null)
const member = user(['RECIPE_WRITE'])

describe('guard', () => {
  it('lets anyone into public routes', () => {
    expect(guard(route('/'), anon)).toBe(true)
  })

  it('redirects anonymous to login with redirect', () => {
    expect(guard(route('/account', { auth: true }), anon)).toEqual({
      path: '/login',
      query: { redirect: '/account' },
    })
  })

  it('redirects anonymous to login on permission routes too', () => {
    expect(guard(route('/admin', { permission: ADMIN_PERMISSIONS }), anon)).toMatchObject({ path: '/login' })
  })

  it('sends to /403 when permission missing', () => {
    expect(guard(route('/admin/users', { auth: true, permission: 'USER_READ' }), member)).toBe('/403')
  })

  it('accepts any of permission list', () => {
    expect(guard(route('/admin', { auth: true, permission: ADMIN_PERMISSIONS }), user(['CATALOG_MANAGE']))).toBe(true)
  })

  it('lets logged-in user into auth route', () => {
    expect(guard(route('/account', { auth: true }), member)).toBe(true)
  })
})

describe('onSessionLost', () => {
  function setup(path: string, meta: RouteMeta) {
    const auth = {
      me: {} as unknown,
      get isLoggedIn() { return this.me !== null },
      canAny: () => true,
    }
    const router = { currentRoute: { value: route(path, meta) }, replace: vi.fn<(r: unknown) => void>() }
    onSessionLost(router, auth)
    return { auth, router }
  }

  it('clears me and sends protected page to login', () => {
    const { auth, router } = setup('/admin/users', { permission: 'USER_READ' })
    expect(auth.me).toBeNull()
    expect(router.replace).toHaveBeenCalledWith({ path: '/login', query: { redirect: '/admin/users' } })
  })

  it('stays on public page', () => {
    const { auth, router } = setup('/', {})
    expect(auth.me).toBeNull()
    expect(router.replace).not.toHaveBeenCalled()
  })
})
