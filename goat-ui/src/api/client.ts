import {createHttp} from './http'
import {useAuthStore} from '@/stores/auth'

export const http = createHttp(
  async () => {
    const authStore = useAuthStore()
    await authStore.refresh()
  },
  () => {
    const authStore = useAuthStore()
    authStore.logoutLocal()
  },
)
