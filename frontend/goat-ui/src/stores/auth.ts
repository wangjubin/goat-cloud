import {defineStore} from 'pinia'
import {loginApi, logoutApi, profileApi, refreshApi} from '@/api/auth'
import {storage} from '@/utils/storage'
import {usePermissionStore} from './permission'
import {useUserStore} from './user'
import type {LoginResponse} from '@/types/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: storage.getAccessToken(),
    refreshToken: storage.getRefreshToken(),
    loggedIn: false,
  }),
  actions: {
    async login(payload: { username: string; password: string }) {
      const result = await loginApi(payload)
      this.applyTokens(result)
      this.loggedIn = true
      await this.applyProfile(result)
      return result
    },
    async restoreProfile() {
      if (!this.accessToken) return
      const profile = await profileApi()
      useUserStore().setProfile(profile)
      await usePermissionStore().buildRoutes(profile.routes)
      this.loggedIn = true
    },
    async refresh() {
      const result = await refreshApi(this.refreshToken)
      this.applyTokens(result)
      this.applyProfile(result)
      this.loggedIn = true
    },
    async logout() {
      try {
        await logoutApi()
      } finally {
        this.logoutLocal()
      }
    },
    logoutLocal() {
      this.accessToken = ''
      this.refreshToken = ''
      this.loggedIn = false
      storage.clearAuth()
      useUserStore().clear()
      usePermissionStore().reset()
    },
    applyTokens(result: LoginResponse) {
      // Strip the "Bearer " prefix the backend adds — the prefix is HTTP-header
      // semantics, not part of the JWT. The request interceptor re-adds it.
      this.accessToken = (result.accessToken || '').replace(/^Bearer\s+/i, '')
      this.refreshToken = (result.refreshToken || '').replace(/^Bearer\s+/i, '')
      storage.setAccessToken(this.accessToken)
      storage.setRefreshToken(this.refreshToken)
    },
    applyProfile(result: LoginResponse) {
      useUserStore().setProfile(result.profile)
      return usePermissionStore().buildRoutes(result.profile.routes)
    },
  },
})
