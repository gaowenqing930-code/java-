/**
 * 真题 Mock 数据（企业笔试/面试真题套卷，先模拟，后续对接真实题库）
 */
export interface ExamPaper {
  id: string
  company: string
  year: number
  season: '秋招' | '春招' | '社招'
  /** 具体场次描述，如 9月17号 */
  round: string
  position: string
  type: '笔试' | '面试'
  /** 热度（浏览量） */
  heat: number
  questionCount: number
  /** 企业标签（用于热门公司筛选） */
  hot?: boolean
  tags?: string[]
}

export const EXAM_PAPERS: ExamPaper[] = [
  // —— 笔试真题 ——
  { id: 'h-ai-0917', company: '华为', year: 2025, season: '秋招', round: '9月17号', position: 'AI 岗', type: '笔试', heat: 379, questionCount: 42, hot: true },
  { id: 'ali-dev-sp1', company: '阿里云', year: 2025, season: '春招', round: '第一批笔试', position: '研发岗', type: '笔试', heat: 155, questionCount: 38, hot: true },
  { id: 'mt-product-sp', company: '美团', year: 2025, season: '春招', round: '综合能力测试', position: '产品&运营笔试', type: '笔试', heat: 324, questionCount: 45, hot: true },
  { id: 'boc-sp2', company: '中国银行', year: 2025, season: '春招', round: '模拟卷2', position: '通用岗', type: '笔试', heat: 481, questionCount: 60 },
  { id: 'cmcc-sp2', company: '中国移动', year: 2025, season: '春招', round: '模拟卷2', position: '技术岗', type: '笔试', heat: 863, questionCount: 55 },
  { id: 'h-ai-0903', company: '华为', year: 2025, season: '秋招', round: '9月3号', position: 'AI 岗', type: '笔试', heat: 551, questionCount: 40 },
  { id: 'cu-cl-qz10', company: '中国联通', year: 2025, season: '秋招', round: '模拟卷10', position: '综合类', type: '笔试', heat: 448, questionCount: 50 },
  { id: 'mt-tech-qz1', company: '美团', year: 2025, season: '秋招', round: '第一批笔试', position: '技术岗', type: '笔试', heat: 629, questionCount: 46, hot: true },
  { id: 'cmb-sp1', company: '招商银行', year: 2025, season: '春招', round: '模拟卷', position: '金融科技岗', type: '笔试', heat: 705, questionCount: 52 },
  { id: 'jd-java-qz', company: '京东', year: 2025, season: '秋招', round: '第二批笔试', position: 'Java 工程师', type: '笔试', heat: 432, questionCount: 44, hot: true },
  { id: 'ctrip-be-qz', company: '携程', year: 2025, season: '秋招', round: '第一批笔试', position: '后端研发', type: '笔试', heat: 289, questionCount: 36, hot: true },
  { id: 'mi-fe-sp', company: '小米集团', year: 2025, season: '春招', round: '综合笔试', position: '前端开发', type: '笔试', heat: 267, questionCount: 33, hot: true },

  // —— 面试真题 ——
  { id: 'byted-be-m1', company: '字节跳动', year: 2025, season: '秋招', round: '一面', position: '后端研发', type: '面试', heat: 921, questionCount: 28 },
  { id: 'tx-java-m2', company: '腾讯', year: 2025, season: '秋招', round: '二面', position: 'Java 工程师', type: '面试', heat: 756, questionCount: 31 },
  { id: 'ali-fe-m1', company: '阿里巴巴', year: 2025, season: '秋招', round: '一面', position: '前端开发', type: '面试', heat: 634, questionCount: 26 },
  { id: 'mt-java-m1', company: '美团', year: 2025, season: '秋招', round: '一面', position: 'Java 工程师', type: '面试', heat: 588, questionCount: 29 },
  { id: 'baidu-ai-m2', company: '百度', year: 2025, season: '春招', round: '二面', position: '算法工程师', type: '面试', heat: 473, questionCount: 24 },
  { id: 'jd-python-m1', company: '京东', year: 2025, season: '春招', round: '一面', position: 'Python 后端', type: '面试', heat: 356, questionCount: 22 },
]

/** 热门公司 */
export const HOT_COMPANIES = ['美团', '阿里巴巴', '京东', '华为', '携程', '小米集团']
