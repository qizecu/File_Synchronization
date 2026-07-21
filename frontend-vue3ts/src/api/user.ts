import { get, post, put, del } from '@/utils/http'
import type { SysUserVO, UserCreateDTO, UserUpdateDTO, PageResult } from '@/types/api'

/** 用户列表（分页） */
export function getUserList(params: { page: number; size: number; keyword?: string }): Promise<PageResult<SysUserVO>> {
  return get<PageResult<SysUserVO>>('/users', params)
}

/** 新增用户 */
export function createUser(data: UserCreateDTO): Promise<void> {
  return post<void>('/users', data)
}

/** 编辑用户 */
export function updateUser(id: number, data: UserUpdateDTO): Promise<void> {
  return put<void>(`/users/${id}`, data)
}

/** 删除用户 */
export function deleteUser(id: number): Promise<void> {
  return del<void>(`/users/${id}`)
}

/** 重置密码 */
export function resetUserPassword(id: number, data: ResetPasswordDTO): Promise<void> {
  return put<void>(`/users/${id}/reset-password`, data)
}
