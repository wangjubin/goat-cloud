import axios, {AxiosError, type AxiosInstance} from 'axios'
import {ElMessage} from 'element-plus'
import {storage} from '@/utils/storage'

let isRefreshing = false
let refreshQueue: Array<{ resolve: () => void; reject: (error: unknown) => void }> = []

export function createHttp(onUnauthorized: () => Promise<void>, onLogout: () => void): AxiosInstance {
  const instance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE || '/api',
    timeout: 15000,
  })

  instance.interceptors.request.use((config) => {
    const token = storage.getAccessToken()
    if (token) {
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  })

  instance.interceptors.response.use(
    (response) => {
      const payload = response.data
      if (payload?.code === 0) {
        return payload.data
      }
      if (payload?.code === 4010 || payload?.code === 4011) {
        return handleRefresh(instance, response.config, onUnauthorized, onLogout)
      }
      return Promise.reject(payload)
    },
    (error: AxiosError) => {
      const status = error.response?.status
      if (status === 401) {
        return handleRefresh(instance, error.config ?? {}, onUnauthorized, onLogout)
      }
      ElMessage.error(error.message || 'Network error')
      return Promise.reject(error)
    },
  )

  return instance
}

async function handleRefresh(
  instance: AxiosInstance,
  originalConfig: any,
  onUnauthorized: () => Promise<void>,
  onLogout: () => void,
) {
  const requestUrl = String(originalConfig?.url || '')
  const isRefreshRequest = requestUrl.includes('/auth/refresh')

  if (!storage.getRefreshToken()) {
    onLogout()
    return Promise.reject(new Error('No refresh token'))
  }

  if (isRefreshRequest) {
    onLogout()
    return Promise.reject(new Error('Refresh token expired'))
  }

  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      refreshQueue.push({
        resolve: () => {
          instance(originalConfig).then(resolve).catch(reject)
        },
        reject,
      })
    })
  }

  isRefreshing = true
  try {
    await onUnauthorized()
    refreshQueue.forEach((entry) => entry.resolve())
    refreshQueue = []
    return instance(originalConfig)
  } catch (error) {
    onLogout()
    refreshQueue.forEach((entry) => entry.reject(error))
    refreshQueue = []
    return Promise.reject(error)
  } finally {
    isRefreshing = false
  }
}

// Default http instance with no-op token refresh (for use outside stores)
const noop = async () => {}
const noop2 = () => {}
const http = createHttp(noop, noop2)
export default http
