import { get, post } from './request'
import type { QuestionVO } from './question'

export interface PointResult {
  point: string
  earned: number
  total: number
  hit: boolean
}

export interface EvaluationVO {
  id: number
  evalType: 'AI' | 'RULE'
  score: number | null
  pointResults: PointResult[]
  comment: string
  status: 'PENDING' | 'SUCCESS' | 'FAILED'
}

export interface PracticeItemVO {
  question: QuestionVO
  answerRecordId: number | null
  userAnswer: string | null
  correct: boolean | null
  score: number | null
  evaluation: EvaluationVO | null
}

export interface PracticeDetailVO {
  id: number
  mode: 'PAPER_BANK' | 'EXAM' | 'AI' | 'ADAPTIVE' | 'WRONG'
  conditionJson: string
  totalCount: number
  correctCount: number
  score: number
  status: number
  createTime: string
  finishTime: string | null
  durationSeconds: number | null
  items: PracticeItemVO[]
}

export interface PracticeHistoryVO {
  id: number
  mode: PracticeDetailVO['mode']
  totalCount: number
  correctCount: number
  score: number
  answeredCount: number
  durationSeconds: number | null
  createTime: string
  finishTime: string | null
}

export const PRACTICE_MODE_LABEL: Record<PracticeDetailVO['mode'], string> = {
  PAPER_BANK: '题库练习',
  EXAM: '真题套卷',
  AI: 'AI 练习',
  ADAPTIVE: '自适应练习',
  WRONG: '错题重练',
}

export interface TypeQuota {
  type: 'SINGLE' | 'MULTIPLE' | 'JUDGE' | 'SHORT'
  count: number
}

export interface CreatePracticeParams {
  mode: PracticeDetailVO['mode']
  categoryId?: number
  keyword?: string
  type?: string
  difficulty?: number
  count: number
  /** EXAM 模式题型配额（如笔试 5单选+2多选+2简答） */
  typeQuota?: TypeQuota[]
}

export interface AnswerBody {
  questionId: number
  userAnswer: string
}

/** 创建练习（四种模式组题） */
export function createPracticeApi(params: CreatePracticeParams) {
  return post<PracticeDetailVO>('/practices', params)
}

/** 提交单题答案（幂等；主观题同步触发 AI 评测，需 10-30s） */
export function submitAnswerApi(practiceId: number, body: AnswerBody) {
  return post<{ answerId: number; questionId: number; correct: boolean | null; score: number | null }>(
    `/practices/${practiceId}/answers`,
    body,
    { timeout: 120000 },
  )
}

/** 交卷：批量提交并结算成绩（含 AI 评测，放宽超时） */
export function submitAllApi(practiceId: number, answers: AnswerBody[]) {
  return post<PracticeDetailVO>(`/practices/${practiceId}/submit`, { answers }, { timeout: 180000 })
}

/** 练习详情（含逐题结果） */
export function getPracticeApi(practiceId: number) {
  return get<PracticeDetailVO>(`/practices/${practiceId}`)
}

/** 历史答题记录（本人已完成练习，按时间倒序） */
export function historyApi() {
  return get<PracticeHistoryVO[]>('/practices/history')
}
