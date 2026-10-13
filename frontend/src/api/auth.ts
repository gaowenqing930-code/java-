import { get, post, put } from './request'

export interface UserInfo {
  id: number
  username: string
  email: string
  role: string
  /** 专业方向（多选，逗号分隔） */
  major?: string
  /** 目标职业 */
  targetJob?: string
}

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  email: string
  password: string
}

export interface LoginResult {
  token: string
  user: UserInfo
}

/** 用户注册 */
export function registerApi(params: RegisterParams) {
  return post<UserInfo>('/auth/register', params)
}

/** 用户登录 */
export function loginApi(params: LoginParams) {
  return post<LoginResult>('/auth/login', params)
}

/** 获取当前登录用户信息 */
export function getProfileApi() {
  return get<UserInfo>('/users/profile')
}

export interface UpdateProfileParams {
  major?: string
  targetJob?: string
}

/** 更新求职目标（专业 + 目标职业） */
export function updateProfileApi(params: UpdateProfileParams) {
  return put<UserInfo>('/users/profile', params)
}
