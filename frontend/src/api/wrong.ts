import { get, post, del } from './request'
import type { QuestionVO } from './question'
import type { PracticeDetailVO } from './practice'

export interface WrongQuestionVO {
  id: number
  question: QuestionVO
  lastAnswerId: number | null
  wrongCount: number
  status: number
  createTime: string
  updateTime: string
}

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}

/** 错题分页列表 */
export function wrongPageApi(params: { type?: string; keyword?: string; pageNum?: number; pageSize?: number }) {
  return get<PageResult<WrongQuestionVO>>('/wrong-questions', params)
}

/** 移除错题 */
export function removeWrongApi(id: number) {
  return del(`/wrong-questions/${id}`)
}

/** 一键重练：创建错题重练练习 */
export function repracticeApi(count = 10) {
  return post<PracticeDetailVO>(`/wrong-questions/repractice?count=${count}`, {})
}
