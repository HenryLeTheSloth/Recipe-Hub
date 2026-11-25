import type { RouteLocationNormalizedLoaded, RouteLocationRaw } from 'vue-router'
import type { Permission } from '@/api/auth'

declare module 'vue-router' {
  interface RouteMeta {
    auth?: boolean
    /** Cần ít nhất một quyền trong danh sách. */
    permission?: Permission | Permission[]
  }
}

export const ADMIN_PERMISSIONS: Permission[] = ['USER_READ', 'ROLE_MANAGE', 'CATALOG_MANAGE', 'CONTENT_MODERATE']

type GuardAuth = { isLoggedIn: boolean; canAny(ps: Permission[]): boolean }

export function guard(to: RouteLocationNormalizedLoaded, auth: GuardAuth): true | RouteLocationRaw {
  const perm = to.meta.permission
  if ((to.meta.auth || perm) && !auth.isLoggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  if (perm && !auth.canAny(Array.isArray(perm) ? perm : [perm])) return '/403'
  return true
}

/** Server báo phiên đã mất: xóa me, rời trang hiện tại nếu trang đó cần đăng nhập/quyền. */
export function onSessionLost(
  router: { currentRoute: { value: RouteLocationNormalizedLoaded }; replace(r: RouteLocationRaw): unknown },
  auth: GuardAuth & { me: unknown },
) {
  auth.me = null
  const r = guard(router.currentRoute.value, auth)
  if (r !== true) router.replace(r)
}
