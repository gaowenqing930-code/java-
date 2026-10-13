import { get } from './request'

export interface DomainStat {
  name: string
  total: number
  correct: number
  rate: number
}

export interface PointStat {
  name: string
  domain: string
  total: number
  correct: number
  rate: number
  mastery: number
}

export interface WeakPoint {
  name: string
  domain: string
  errorRate: number
}

export interface TrendPoint {
  date: string
  practiceCount: number
  correctRate: number
}

export interface StudyReportVO {
  days: number
  practiceCount: number
  totalAnswered: number
  overallRate: number
  domainStats: DomainStat[]
  pointStats: PointStat[]
  weakPoints: WeakPoint[]
  trend: TrendPoint[]
  suggestion: string
}

/** 学习报告（默认近 30 天） */
export function getReportApi(days = 30) {
  return get<StudyReportVO>('/stats/report', { params: { days } })
}
