import { get } from './request'

export interface OptionDTO {
  key: string
  text: string
}

export interface ScorePointDTO {
  point: string
  score: number
}

export type QuestionType = 'SINGLE' | 'MULTIPLE' | 'JUDGE' | 'SHORT'

export interface QuestionVO {
  id: number
  type: QuestionType
  difficulty: number
  stem: string
  options: OptionDTO[]
  answer: string
  scorePoints: ScorePointDTO[]
  analysis: string
  source: 'MANUAL' | 'AI'
  status: number
  categoryIds: number[]
  categoryNames: string[]
  tagNames: string[]
  createTime: string
}

/** 五维组合筛选条件 */
export interface QuestionQuery {
  domainId?: number
  directionId?: number
  pointId?: number
  type?: string
  difficulty?: number
  keyword?: string
  pageNum: number
  pageSize: number
}

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  records: T[]
}

export const QUESTION_TYPE_LABEL: Record<QuestionType, string> = {
  SINGLE: '单选题',
  MULTIPLE: '多选题',
  JUDGE: '判断题',
  SHORT: '简答题',
}

/** 题目组合分页筛选 */
export function listQuestionsApi(params: QuestionQuery) {
  return get<PageResult<QuestionVO>>('/questions', { params })
}

/** 题目详情 */
export function getQuestionApi(id: number) {
  return get<QuestionVO>(`/questions/${id}`)
}
