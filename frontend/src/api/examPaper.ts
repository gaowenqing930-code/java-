import { get } from './request'
import type { QuestionVO } from './question'

/** 真题卷子（后端预置固定题目） */
export interface ExamPaperVO {
  id: number
  company: string
  year: number
  season: string
  round: string
  position: string
  /** 笔试 / 面试 */
  type: string
  heat: number
  /** 是否热门：1是 0否 */
  hot: number
  questionCount: number
  /** 卷子固定题目（详情接口返回） */
  questions?: QuestionVO[]
}

/** 真题卷子列表（type 可选：笔试/面试，不传返回全部） */
export function listExamPapersApi(type?: string) {
  return get<ExamPaperVO[]>('/exam-papers', { params: { type } })
}

/** 卷子详情（含固定题目） */
export function getExamPaperApi(id: number | string) {
  return get<ExamPaperVO>(`/exam-papers/${id}`)
}
