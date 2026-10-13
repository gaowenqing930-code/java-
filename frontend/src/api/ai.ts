import { post, get, getToken } from './request'
import type { QuestionVO } from './question'

/** AI 出题请求 */
export interface AiGenerateRequest {
  categoryId: number
  type: 'SINGLE' | 'MULTIPLE' | 'JUDGE' | 'SHORT'
  difficulty: number
  count: number
}

/** AI 出题结果 */
export interface AiGenerateResult {
  questions: QuestionVO[]
  /** 是否走题库兜底（模型不可用/多次失败时） */
  fromFallback: boolean
  message: string
}

/** AI 出题异步任务 VO（提交/轮询共用） */
export interface AiGenTaskVO {
  taskId: number
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'
  progress: number
  questions?: QuestionVO[]
  message?: string
}

/** AI 智能出题（同步，按用户限流，兼容旧调用） */
export function generateAiQuestionsApi(params: AiGenerateRequest) {
  return post<AiGenerateResult>('/ai/questions/generate', params, { timeout: 120000 })
}

/** 提交异步出题任务：立即返回 taskId，不阻塞等待 */
export function submitGenerateTaskApi(params: AiGenerateRequest) {
  return post<AiGenTaskVO>('/ai/questions/generate/task', params, { timeout: 15000 })
}

/** 查询任务状态（SSE 断线时的轮询兜底） */
export function getTaskStatusApi(taskId: number) {
  return get<AiGenTaskVO>(`/ai/questions/task/${taskId}`, { timeout: 15000 })
}

/** 构造 SSE 订阅地址：EventSource 不能带自定义 header，token 走 query */
export function buildGenTaskEventsUrl(taskId: number) {
  return `/api/ai/questions/task/${taskId}/events?token=${encodeURIComponent(getToken() || '')}`
}
