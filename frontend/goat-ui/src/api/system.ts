import {http} from './client'
import type {PageResponse} from '@/types/auth'

export type CommonStatus = 'ENABLED' | 'DISABLED'
export type MenuType = 'DIRECTORY' | 'MENU' | 'BUTTON'
export type DataScope = 'ALL' | 'DEPT' | 'DEPT_AND_CHILD' | 'SELF' | 'CUSTOM'

export interface UserItem {
  userId: number
  username: string
  nickname: string
  deptId: number
  deptName?: string
  phone?: string
  email?: string
  status: CommonStatus
  superAdmin?: boolean
}

export interface UserDetail extends UserItem {
  remark?: string
  roleIds?: number[]
}

export interface RoleItem {
  roleId: number
  roleCode: string
  roleName: string
  dataScope: DataScope
  status: CommonStatus
  remark?: string
}

export interface RolePermission {
  roleId: number
  dataScope: DataScope
  menuIds: number[]
  deptIds: number[]
}

export interface DeptTreeItem {
  deptId: number
  parentId: number
  deptName: string
  deptCode: string
  children?: DeptTreeItem[]
}

export interface MenuTreeItem {
  menuId: number
  parentId: number
  menuName: string
  menuType: MenuType
  routePath?: string
  componentPath?: string
  permissionCode?: string
  icon?: string
  sortNo?: number
  visible?: boolean
  keepAlive?: boolean
  externalLink?: boolean
  status: CommonStatus
  remark?: string
  children?: MenuTreeItem[]
}

export function fetchUsers(payload: Record<string, unknown>) {
  return http.post<any, PageResponse<UserItem>>('/system/user/page', payload)
}

export function fetchUserDetail(userId: number) {
  return http.get<any, UserDetail>(`/system/user/${userId}`)
}

export function createUser(payload: Record<string, unknown>) {
  return http.post<any, void>('/system/user/create', payload)
}

export function updateUser(payload: Record<string, unknown>) {
  return http.post<any, void>('/system/user/update', payload)
}

export function deleteUsers(ids: number[]) {
  return http.post<any, void>('/system/user/delete', {ids})
}

export function changeUserStatus(id: number, status: CommonStatus) {
  return http.post<any, void>('/system/user/status', {id, status})
}

export function resetUserPassword(userId: number) {
  return http.post<any, { newPassword: string }>('/system/user/reset-password', { userId })
}

export function assignUserRoles(userId: number, roleIds: number[]) {
  return http.post<any, void>('/system/user/assign-roles', {userId, roleIds})
}

export function fetchRoles(payload: Record<string, unknown>) {
  return http.post<any, PageResponse<RoleItem>>('/system/role/page', payload)
}

export function fetchRoleDetail(roleId: number) {
  return http.get<any, RoleItem>(`/system/role/${roleId}`)
}

export function saveRole(payload: Record<string, unknown>) {
  return http.post<any, void>('/system/role/save', payload)
}

export function deleteRoles(ids: number[]) {
  return http.post<any, void>('/system/role/delete', {ids})
}

export function changeRoleStatus(id: number, status: CommonStatus) {
  return http.post<any, void>('/system/role/status', {id, status})
}

export function fetchRolePermissions(roleId: number) {
  return http.get<any, RolePermission>(`/system/role/${roleId}/permissions`)
}

export function assignRolePermissions(payload: {
  roleId: number
  menuIds: number[]
  dataScope: DataScope
  deptIds: number[]
}) {
  return http.post<any, void>('/system/role/assign-permissions', payload)
}

export function fetchDepts(payload: Record<string, unknown>) {
  return http.post<any, PageResponse<any>>('/system/dept/page', payload)
}

export function fetchDeptTree(payload: Record<string, unknown> = {}) {
  return http.post<any, DeptTreeItem[]>('/system/dept/tree', payload)
}

export function fetchMenus() {
  return http.get<any, MenuTreeItem[]>('/system/menu/tree')
}

export function fetchManageMenus() {
  return http.get<any, MenuTreeItem[]>('/system/menu/manage-tree')
}

export function fetchMenuDetail(menuId: number) {
  return http.get<any, MenuTreeItem>(`/system/menu/${menuId}`)
}

export function saveMenu(payload: Record<string, unknown>) {
  return http.post<any, void>('/system/menu/save', payload)
}

export function deleteMenu(menuId: number) {
  return http.post<any, void>(`/system/menu/delete/${menuId}`)
}
