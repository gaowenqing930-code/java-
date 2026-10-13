import { get, put } from './request'
import type { QuestionVO } from './question'
import type { PageResult } from './wrong'

export interface AdminUserVO {
  id: number
  username: string
  email: string
  role: string
  status: number
}

export interface OperationLogVO {
  id: number
  userId: number | null
  action: string
  detail: string
  createTime: string
}

/** 用户分页 */
export function adminUserPageApi(params: { keyword?: string; pageNum?: number; pageSize?: number }) {
  return get<PageResult<AdminUserVO>>('/admin/users', { params })
}

/** 启用/禁用用户 */
export function adminUpdateUserStatusApi(id: number, status: number) {
  return put(`/admin/users/${id}/status`, null, { params: { status } })
}

/** 题目分页（含下架） */
export function adminQuestionPageApi(params: {
  type?: string
  difficulty?: number
  keyword?: string
  status?: number
  pageNum?: number
  pageSize?: number
}) {
  return get<PageResult<QuestionVO>>('/admin/questions', { params })
}

/** 题目上下架 */
export function adminUpdateQuestionStatusApi(id: number, status: number) {
  return put(`/admin/questions/${id}/status`, null, { params: { status } })
}

/** 日志分页 */
export function adminLogPageApi(params: { action?: string; pageNum?: number; pageSize?: number }) {
  return get<PageResult<OperationLogVO>>('/admin/logs', { params })
}
