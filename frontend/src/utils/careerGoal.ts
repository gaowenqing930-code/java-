/**
 * 求职目标（专业 + 目标职业）读取
 * 数据源优先级：登录用户（数据库 user.major / user.target_job）→ 本地缓存 careerGoal
 */
import type { UserInfo } from '@/api/auth'

export interface CareerGoal {
  majors: string[]
  position: string
}

export const CAREER_GOAL_KEY = 'careerGoal'

/** 读取已存目标（未设置返回 null；跳过的记录 position='skip'） */
export function readCareerGoal(): CareerGoal | null {
  try {
    const raw = localStorage.getItem(CAREER_GOAL_KEY)
    return raw ? (JSON.parse(raw) as CareerGoal) : null
  } catch {
    return null
  }
}

/** 从登录用户（数据库字段）解析求职目标 */
export function toCareerGoal(user: UserInfo | null): CareerGoal | null {
  if (user && (user.targetJob || user.major)) {
    const majors = user.major
      ? user.major
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean)
      : []
    return { majors, position: user.targetJob || '' }
  }
  return null
}

/** 综合读取已填写的求职目标：优先数据库（user），回退本地缓存 */
export function readFilledGoal(user: UserInfo | null): CareerGoal | null {
  const fromUser = toCareerGoal(user)
  if (fromUser) return fromUser
  const g = readCareerGoal()
  if (g && g.position !== 'skip' && (g.majors?.length || g.position)) return g
  return null
}
