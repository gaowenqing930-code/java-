import axios, { type AxiosResponse, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

/** 后端统一返回体结构 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

const TOKEN_KEY = 'token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

const request = axios.create({
  baseURL: '/api',
  // 默认 60s：普通接口足够；AI 生成/评测等长耗时接口在各自 api 里单独放宽
  timeout: 60000,
})

// 请求拦截：自动携带 Token
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：解包统一返回体，统一处理 401/403/错误提示
request.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult
    if (result.code !== 200) {
      ElMessage.error(result.message || '请求失败')
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    // 运行时已解包为 data 字段，类型上通过 AxiosResponse 透传
    return result.data as unknown as AxiosResponse
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message
    if (status === 401) {
      clearToken()
      localStorage.removeItem('user')
      if (window.location.pathname !== '/login') {
        ElMessage.error(message || '登录已过期，请重新登录')
        window.location.href = '/login'
      }
    } else {
      const isTimeout = error.code === 'ECONNABORTED' || /timeout/i.test(error.message || '')
      ElMessage.error(isTimeout ? '请求超时，请稍后重试' : message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

/** 通用请求方法（响应已解包为 data 字段） */
export function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request.get(url, config) as Promise<T>
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request.post(url, data, config) as Promise<T>
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request.put(url, data, config) as Promise<T>
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request.delete(url, config) as Promise<T>
}

export default request
