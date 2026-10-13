import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  getProfileApi,
  loginApi,
  registerApi,
  updateProfileApi,
  type LoginParams,
  type RegisterParams,
  type UpdateProfileParams,
  type UserInfo,
} from '@/api/auth'
import { clearToken, getToken, setToken } from '@/api/request'

const USER_KEY = 'user'

function readStoredUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(getToken())
  const user = ref<UserInfo | null>(readStoredUser())

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  function setAuth(newToken: string, newUser: UserInfo) {
    token.value = newToken
    user.value = newUser
    setToken(newToken)
    localStorage.setItem(USER_KEY, JSON.stringify(newUser))
  }

  async function login(params: LoginParams) {
    const data = await loginApi(params)
    setAuth(data.token, data.user)
  }

  async function register(params: RegisterParams) {
    await registerApi(params)
  }

  /** 根据 Token 刷新用户信息（页面刷新后恢复登录态） */
  async function fetchProfile() {
    const profile = await getProfileApi()
    user.value = profile
    localStorage.setItem(USER_KEY, JSON.stringify(profile))
    return profile
  }

  /** 更新求职目标（专业 + 目标职业），成功后同步本地缓存 */
  async function updateProfile(params: UpdateProfileParams) {
    const profile = await updateProfileApi(params)
    user.value = profile
    localStorage.setItem(USER_KEY, JSON.stringify(profile))
    return profile
  }

  function logout() {
    token.value = ''
    user.value = null
    clearToken()
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isLoggedIn, isAdmin, setAuth, login, register, fetchProfile, updateProfile, logout }
})
