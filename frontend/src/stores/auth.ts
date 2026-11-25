import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import type { Me, Permission } from '@/api/auth'
import { api, ApiError } from '@/api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({ me: null as Me | null }),
  getters: {
    isLoggedIn: (s) => s.me !== null,
  },
  actions: {
    async fetchMe() {
      try {
        this.me = await authApi.getMe()
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) this.me = null
        else throw e
      }
    },
    async login(email: string, password: string) {
      this.me = await authApi.login(email, password)
    },
    async logout() {
      try {
        await authApi.logout()
      } finally {
        this.me = null
      }
      // Logout xóa cookie XSRF-TOKEN; lấy lại ngay để POST kế tiếp (vd. đăng nhập lại) không bị 403.
      await api('/auth/csrf').catch(() => {})
    },
    can(p: Permission) {
      return this.me?.permissions.includes(p) ?? false
    },
    canAny(ps: Permission[]) {
      return ps.some((p) => this.can(p))
    },
  },
})
