import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'
import type { LoginVO } from '@/types/api'
import router from '@/router'

const TOKEN_KEY = 'token'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const nickname = ref<string>('')
  const username = ref<string>('')
  const role = ref<string>('') // 用户角色：ADMIN / USER

  /** 是否管理员 */
  const isAdmin = computed(() => role.value === 'ADMIN')

  /** 登录 */
  async function doLogin(data: { username: string; password: string }) {
    const res: LoginVO = await loginApi(data)
    token.value = res.token
    nickname.value = res.nickname
    username.value = res.username
    role.value = res.role // 保存角色信息
    localStorage.setItem(TOKEN_KEY, res.token)
  }

  /** 获取当前用户 */
  async function fetchCurrentUser() {
    const res = await getCurrentUser()
    nickname.value = res.nickname
    username.value = res.username
    role.value = res.role // 刷新角色信息
  }

  /** 登出 */
  async function doLogout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      nickname.value = ''
      username.value = ''
      role.value = ''
      localStorage.removeItem(TOKEN_KEY)
      router.push('/login')
    }
  }

  /** 是否已登录 */
  function isLoggedIn(): boolean {
    return !!token.value
  }

  return { token, nickname, username, role, isAdmin, doLogin, fetchCurrentUser, doLogout, isLoggedIn }
})
