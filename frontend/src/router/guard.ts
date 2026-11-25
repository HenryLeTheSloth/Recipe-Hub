import type { RouteLocationNormalized, RouteLocationRaw } from 'vue-router'
import type { Permission } from '@/api/auth'

declare module 'vue-router' {
  interface RouteMeta {
    auth?: boolean
    /** Cần ít nhất một quyền trong danh sách. */
    permission?: Permission | Permission[]
  }
}

export const ADMIN_PERMISSIONS: Permission[] = ['USER_READ', 'ROLE_MANAGE', 'CATALOG_MANAGE', 'CONTENT_MODERATE']

export function guard(
  to: RouteLocationNormalized,
  auth: { isLoggedIn: boolean; canAny(ps: Permission[]): boolean },
): true | RouteLocationRaw {
  const perm = to.meta.permission
  if ((to.meta.auth || perm) && !auth.isLoggedIn) return { path: '/login', query: { redirect: to.fullPath } }
  if (perm && !auth.canAny(Array.isArray(perm) ? perm : [perm])) return '/403'
  return true
}
