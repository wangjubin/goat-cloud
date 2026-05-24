const ACCESS_TOKEN_KEY = 'goat_access_token'
const REFRESH_TOKEN_KEY = 'goat_refresh_token'

export const storage = {
  getAccessToken() {
    return localStorage.getItem(ACCESS_TOKEN_KEY) || ''
  },
  setAccessToken(token: string) {
    localStorage.setItem(ACCESS_TOKEN_KEY, token)
  },
  getRefreshToken() {
    return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
  },
  setRefreshToken(token: string) {
    localStorage.setItem(REFRESH_TOKEN_KEY, token)
  },
  clearAuth() {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  },
}
