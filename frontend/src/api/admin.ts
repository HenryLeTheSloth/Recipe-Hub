import { api } from './http'
import type { Permission } from './auth'

export interface Page<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export interface AdminUser {
  id: number
  email: string
  username: string
  displayName: string
  locked: boolean
  emailVerified: boolean
  roles: string[]
  createdAt: string
}

export interface Role {
  id: number
  code: string
  name: string
  description: string | null
  system: boolean
  permissionCodes: Permission[]
  userCount: number
}

export interface RoleInput {
  code?: string
  name: string
  description: string | null
  permissionCodes: Permission[]
}

export interface PermissionInfo {
  code: Permission
  description: string
}

export const listUsers = (q: string, page: number, size: number) =>
  api<Page<AdminUser>>('/admin/users?' + new URLSearchParams({ q, page: String(page), size: String(size) }))
export const setUserLocked = (id: number, locked: boolean) =>
  api<AdminUser>(`/admin/users/${id}/lock`, { method: 'PATCH', json: { locked } })
export const setUserRoles = (id: number, roleCodes: string[]) =>
  api<AdminUser>(`/admin/users/${id}/roles`, { method: 'PUT', json: { roleCodes } })

export const listRoles = () => api<Role[]>('/admin/roles')
export const listPermissions = () => api<PermissionInfo[]>('/admin/permissions')
export const createRole = (r: RoleInput) => api<Role>('/admin/roles', { method: 'POST', json: r })
export const updateRole = (id: number, r: RoleInput) => api<Role>(`/admin/roles/${id}`, { method: 'PUT', json: r })
export const deleteRole = (id: number) => api<void>(`/admin/roles/${id}`, { method: 'DELETE' })
