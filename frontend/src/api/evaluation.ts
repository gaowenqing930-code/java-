import { post } from './request'
import type { EvaluationVO } from './practice'

/** 重试 AI 评测（评测失败/待评测的作答，AI 评分需 10-30s） */
export function retryEvaluationApi(answerId: number) {
  return post<EvaluationVO>(`/evaluations/${answerId}/retry`, undefined, { timeout: 120000 })
}
