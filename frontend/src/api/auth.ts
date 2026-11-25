import { api } from './http'

export type Permission =
  | 'RECIPE_WRITE'
  | 'COMMUNITY_PARTICIPATE'
  | 'AI_BOWL_USE'
  | 'MEDIA_UPLOAD'
  | 'CONTENT_MODERATE'
  | 'CATALOG_MANAGE'
  | 'USER_READ'
  | 'USER_MANAGE'
  | 'ROLE_MANAGE'

export interface Me {
  id: number
  email: string
  username: string
  displayName: string
  avatarUrl: string | null
  roles: string[]
  permissions: Permission[]
}

export interface RegisterInput {
  email: string
  username: string
  displayName: string
  password: string
}

const post = (path: string, json: unknown) => api<void>(path, { method: 'POST', json })

export const getMe = () => api<Me>('/me')
export const login = (email: string, password: string) =>
  api<Me>('/auth/login', { method: 'POST', json: { email, password } })
export const logout = () => api<void>('/auth/logout', { method: 'POST' })
export const register = (input: RegisterInput) => post('/auth/register', input)
export const verifyEmail = (token: string) => post('/auth/verify-email', { token })
export const resendVerification = (email: string) => post('/auth/resend-verification', { email })
export const forgotPassword = (email: string) => post('/auth/forgot-password', { email })
export const resetPassword = (token: string, newPassword: string) =>
  post('/auth/reset-password', { token, newPassword })
export const changePassword = (currentPassword: string, newPassword: string) =>
  api<void>('/me/password', { method: 'PUT', json: { currentPassword, newPassword } })
