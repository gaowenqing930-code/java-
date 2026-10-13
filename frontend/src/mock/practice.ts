/**
 * 专项练习模块 Mock 数据（先模拟几组，后续对接真实题库）
 * group: 卡片所属模块分组
 * keyword: 点击卡片后带入做题列表的搜索词
 */
export interface PracticeCard {
  id: string
  name: string
  /** 题目总数 */
  total: number
  /** 已做数量（后续对接真实练习数据） */
  done: number
  /** 新题数（角标） */
  newCount?: number
  /** 所属模块组 */
  group: '推荐' | '编程语言' | '数据结构' | '基础算法' | '计算机基础' | '数据库'
  /** 点击后带入做题列表的关键词 */
  keyword: string
  /** 与哪些求职方向相关（用于"为你推荐"排序） */
  relatedMajors: string[]
}

export const PRACTICE_CARDS: PracticeCard[] = [
  // —— 推荐区 ——
  { id: 'java', name: 'Java', total: 874, done: 0, group: '推荐', keyword: 'Java', relatedMajors: ['Java', '后端', '全栈'] },
  { id: 'sql', name: 'SQL', total: 352, done: 0, group: '推荐', keyword: 'SQL', relatedMajors: ['Java', '后端', '数据'] },
  { id: 'os', name: '操作系统', total: 1617, done: 0, group: '推荐', keyword: '操作系统', relatedMajors: ['Java', '后端', '嵌入式'] },
  { id: 'network', name: '网络基础', total: 1820, done: 0, group: '推荐', keyword: '网络', relatedMajors: ['Java', '后端', '前端'] },
  { id: 'db', name: '数据库', total: 1159, done: 0, group: '推荐', keyword: '数据库', relatedMajors: ['Java', '后端', '数据'] },
  { id: 'linux', name: 'Linux', total: 638, done: 0, group: '推荐', keyword: 'Linux', relatedMajors: ['后端', '运维'] },
  { id: 'prompt', name: '提示词工程', total: 100, done: 0, newCount: 100, group: '推荐', keyword: '提示词', relatedMajors: ['AI', '产品'] },
  { id: 'rag', name: 'RAG', total: 192, done: 0, newCount: 192, group: '推荐', keyword: 'RAG', relatedMajors: ['AI', '大模型'] },

  // —— 数据结构 ——
  { id: 'array', name: '数组', total: 190, done: 0, group: '数据结构', keyword: '数组', relatedMajors: ['Java', '前端', '算法'] },
  { id: 'string', name: '字符串', total: 76, done: 0, group: '数据结构', keyword: '字符串', relatedMajors: ['Java', '前端', '算法'] },
  { id: 'linkedlist', name: '链表', total: 193, done: 0, group: '数据结构', keyword: '链表', relatedMajors: ['Java', '算法'] },
  { id: 'stack', name: '栈', total: 197, done: 0, group: '数据结构', keyword: '栈', relatedMajors: ['Java', '算法'] },
  { id: 'queue', name: '队列', total: 154, done: 0, group: '数据结构', keyword: '队列', relatedMajors: ['Java', '算法'] },
  { id: 'tree', name: '树', total: 268, done: 0, group: '数据结构', keyword: '树', relatedMajors: ['Java', '算法'] },

  // —— 基础算法 ——
  { id: 'hash', name: '哈希', total: 120, done: 0, group: '基础算法', keyword: '哈希', relatedMajors: ['Java', '算法'] },
  { id: 'recursion', name: '递归', total: 88, done: 0, group: '基础算法', keyword: '递归', relatedMajors: ['Java', '算法'] },
  { id: 'dp', name: '动态规划', total: 210, done: 0, group: '基础算法', keyword: '动态规划', relatedMajors: ['算法'] },
  { id: 'greedy', name: '贪心', total: 76, done: 0, group: '基础算法', keyword: '贪心', relatedMajors: ['算法'] },
  { id: 'sort', name: '排序', total: 95, done: 0, group: '基础算法', keyword: '排序', relatedMajors: ['Java', '算法'] },

  // —— 编程语言 ——
  { id: 'python', name: 'Python', total: 520, done: 0, group: '编程语言', keyword: 'Python', relatedMajors: ['Python', 'AI', '数据'] },
  { id: 'cpp', name: 'C++', total: 486, done: 0, group: '编程语言', keyword: 'C++', relatedMajors: ['C++', '嵌入式'] },
  { id: 'js', name: 'JavaScript', total: 312, done: 0, group: '编程语言', keyword: 'JavaScript', relatedMajors: ['前端', '全栈'] },
]

/** 模块分组展示顺序 */
export const GROUP_ORDER = ['推荐', '编程语言', '数据结构', '基础算法', '计算机基础', '数据库'] as const
