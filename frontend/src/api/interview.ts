import { post, get } from './request'

/** 简历项目简介 */
export interface ProjectBrief {
  name: string
  description: string
  /** 项目亮点：职责 / 技术难点 / 量化成果 */
  highlights?: string[]
}

/** 简历文件文本提取结果 */
export interface ResumeExtractVO {
  fileName: string
  text: string
}

/** 简历解析结果 */
export interface ResumeParseVO {
  sessionId: number
  techStack: string[]
  projects: ProjectBrief[]
}

/** 评分点 */
export interface ScorePoint {
  point: string
  score: number
}

/** 面试题视图 */
export interface InterviewQuestionVO {
  no: number
  question: string
  status: 'PENDING' | 'SCORED'
  userAnswer?: string
  aiScore?: number
  aiComment?: string
  scorePoints?: ScorePoint[]
  referenceAnswer?: string
  /** AI 建议是否可结束面试（仅当前题返回时有效） */
  suggestFinish?: boolean | null
  /** 建议结束的原因 */
  finishReason?: string
}

/** 面试结果 */
export interface InterviewResultVO {
  sessionId: number
  totalScore: number
  summary: string
  suggestion: string
  finishTime?: string
  questions: InterviewQuestionVO[]
}

/** 作答响应：未结束返回下一题，结束返回完整结果；出题失败返回 needRetry=true */
export interface InterviewAnswerVO {
  finished: boolean
  currentQuestion?: InterviewQuestionVO
  result?: InterviewResultVO
  /** AI 出下一题失败：答案已保存，需调 nextInterviewApi 重试出题 */
  needRetry?: boolean
}

/** 面试历史列表项 */
export interface InterviewSessionVO {
  sessionId: number
  totalScore?: number
  answeredCount?: number
  status?: string
  techStack?: string
  createTime?: string
  finishTime?: string
}

/** 上传简历文件（.docx/.doc/.pdf），提取文本供确认 */
export function extractResumeApi(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return post<ResumeExtractVO>('/interview/resume/extract', fd, { timeout: 60000 })
}

/** 创建 AI 面试会话（解析简历，AI 调用需 10-20s，放宽到 2 分钟） */
export function createInterviewSessionApi(resumeText: string) {
  return post<ResumeParseVO>('/interview/sessions', { resumeText }, { timeout: 120000 })
}

/** 开始面试：AI 生成第 1 题（单题生成较快） */
export function startInterviewApi(sessionId: number) {
  return post<InterviewQuestionVO>(`/interview/sessions/${sessionId}/start`, undefined, { timeout: 120000 })
}

/**
 * 提交作答：AI 结合回答引导式生成下一题；
 * 用户可随时主动结束（调 finishInterviewApi），达兜底上限后自动评分，评分耗时较长，放宽到 5 分钟
 */
export function answerInterviewApi(sessionId: number, no: number, answer: string) {
  return post<InterviewAnswerVO>(`/interview/sessions/${sessionId}/answer`, { no, answer }, { timeout: 300000 })
}

/** 出题失败后重试生成下一题（answer 返回 needRetry=true 时调用） */
export function nextInterviewApi(sessionId: number) {
  return post<InterviewQuestionVO>(`/interview/sessions/${sessionId}/next`, undefined, { timeout: 120000 })
}

/** 主动结束面试：统一评分已答题目并生成总结 */
export function finishInterviewApi(sessionId: number) {
  return post<InterviewResultVO>(`/interview/sessions/${sessionId}/finish`, undefined, { timeout: 300000 })
}

/** 获取面试结果 */
export function getInterviewResultApi(sessionId: number) {
  return get<InterviewResultVO>(`/interview/sessions/${sessionId}/result`)
}

/** 我的面试历史列表 */
export function listMyInterviewsApi() {
  return get<InterviewSessionVO[]>('/interview/sessions/my')
}
