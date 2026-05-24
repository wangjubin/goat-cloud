export interface AuthUser {
  userId: number
  username: string
  nickname: string
  deptId: number
}

export interface RouteRecordPayload {
  menuId: number
  name: string
  path: string
  component: string
  icon?: string
  visible?: boolean
  keepAlive?: boolean
  externalLink?: boolean
  children?: RouteRecordPayload[]
}

export interface Profile {
  user: AuthUser
  roleCodes: string[]
  permissions: string[]
  routes: RouteRecordPayload[]
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  profile: Profile
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}
