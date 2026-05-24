import {defineStore} from 'pinia'
import type {Profile} from '@/types/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    profile: null as Profile | null,
  }),
  getters: {
    permissions: (state) => state.profile?.permissions ?? [],
    roleCodes: (state) => state.profile?.roleCodes ?? [],
    user: (state) => state.profile?.user ?? null,
  },
  actions: {
    setProfile(profile: Profile) {
      this.profile = profile
    },
    clear() {
      this.profile = null
    },
  },
})
