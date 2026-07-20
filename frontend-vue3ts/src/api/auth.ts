import { post, get } from '@/utils/http'
import type { LoginDTO, LoginVO } from '@/types/api'

/** 登录 */
export function login(data: LoginDTO): Promise<LoginVO> {
  return post<LoginVO>('/auth/login', data)
}

/** 获取当前用户信息 */
export function getCurrentUser(): Promise<LoginVO> {
  return get<LoginVO>('/auth/me')
}

/** 登出 */
export function logout(): Promise<void> {
  return post<void>('/auth/logout')
}
