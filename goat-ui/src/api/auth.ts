import {http} from './client'
import type {LoginResponse, Profile} from '@/types/auth'

export function loginApi(payload: { username: string; password: string }) {
  return http.post<any, LoginResponse>('/auth/login', payload)
}

export function refreshApi(refreshToken: string) {
  return http.post<any, LoginResponse>('/auth/refresh', { refreshToken })
}

export function profileApi() {
  return http.get<any, Profile>('/auth/profile')
}

export function logoutApi() {
  return http.post<any, void>('/auth/logout')
}
