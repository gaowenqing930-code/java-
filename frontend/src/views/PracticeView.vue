<template>
  <div class="practice-page">
    <!-- 顶栏 -->
    <div class="top-bar">
      <button class="back-btn" @click="onBackClick">←</button>
      <span class="title">{{ title }}<el-tag v-if="practiceId" size="small" class="mode-tag">{{ modeLabel }}</el-tag></span>
      <el-radio-group v-if="!practiceId && !creating && !finished" v-model="questionCount" size="small" class="count-picker">
        <el-radio-button :value="5">5 题</el-radio-button>
        <el-radio-button :value="10">10 题</el-radio-button>
        <el-radio-button :value="15">15 题</el-radio-button>
      </el-radio-group>
      <el-button size="small" @click="openHistory">记录</el-button>
      <span class="timer">⏱ {{ formatTime(seconds) }}</span>
      <span class="answered-stat">已答 {{ answeredCount }}/{{ total }}</span>
      <el-button v-if="!finished && questions.length > 0" type="danger" size="small" @click="confirmVisible = true">交卷</el-button>
    </div>

    <!-- 创建中 -->
    <div v-if="creating" class="empty-card">
      <el-skeleton :rows="6" animated />
      <p class="creating-tip">正在组题{{ modeLabel }}…</p>
    </div>

    <!-- 做题主区 -->
    <div v-else-if="!finished && questions.length > 0" class="quiz-layout">
      <!-- 左：当前题 -->
      <div class="quiz-main">
        <div class="q-nav">
          <span class="q-no">第 {{ currentIndex + 1 }} 题</span>
          <el-tag :type="typeTagType(current.type)" effect="light">{{ typeLabel(current.type) }}</el-tag>
          <el-rate :model-value="current.difficulty" disabled size="small" />
        </div>
        <h2 class="stem">{{ current.stem }}</h2>

        <!-- 选择题 -->
        <div v-if="isChoice(current)" class="options">
          <div
            v-for="opt in current.options"
            :key="opt.key"
            class="option"
            :class="{ picked: isPicked(opt.key) }"
            @click="pick(current.type, opt.key)"
          >
            <span class="opt-key">{{ opt.key }}</span>
            <span class="opt-text">{{ opt.text }}</span>
          </div>
        </div>

        <!-- 判断题 -->
        <div v-else-if="current.type === 'JUDGE'" class="judge-box">
          <el-button
            size="large"
            :type="isPicked('true') ? 'success' : 'default'"
            @click="pick('JUDGE', 'true')"
          >✓ 正确</el-button>
          <el-button
            size="large"
            :type="isPicked('false') ? 'danger' : 'default'"
            @click="pick('JUDGE', 'false')"
          >✗ 错误</el-button>
        </div>

        <!-- 简答题 -->
        <div v-else class="short-box">
          <el-input
            v-model="shortAnswer"
            type="textarea"
            :rows="6"
            placeholder="请输入你的作答…（交卷后由 AI 按评分点评测打分）"
          />
        </div>

        <div class="nav-btns">
          <el-button :disabled="currentIndex === 0" @click="currentIndex--">上一题</el-button>
          <el-button type="primary" :disabled="currentIndex === total - 1" @click="currentIndex++">下一题</el-button>
        </div>
      </div>

      <!-- 右：答题卡 -->
      <div class="answer-card">
        <div class="ac-title">答题卡</div>
        <div class="ac-grid">
          <div
            v-for="n in displayedNumbers"
            :key="n"
            class="ac-no"
            :class="noClass(n - 1)"
            @click="jumpTo(n - 1)"
          >
            {{ n }}
          </div>
        </div>
        <div class="ac-pager">
          <el-button size="small" :disabled="currentPage === 1" @click="currentPage--">上一页</el-button>
          <span class="ac-page">第 {{ currentPage }}/{{ totalPages }} 页</span>
          <el-button size="small" :disabled="currentPage === totalPages" @click="currentPage++">下一页</el-button>
        </div>
        <div class="ac-legend">
          <span><i class="dot answered"></i>已答</span>
          <span><i class="dot current"></i>当前</span>
          <span><i class="dot todo"></i>未答</span>
        </div>
      </div>
    </div>

    <!-- 空题库 -->
    <div v-else-if="!finished" class="empty-card">
      <el-empty description="当前条件下没有可练习的题目" />
      <el-button @click="goHome">返回题库</el-button>
    </div>

    <!-- 交卷确认 -->
    <el-dialog v-model="confirmVisible" title="确认交卷" width="420px">
      <p class="confirm-text">
        你还有 <b>{{ total - answeredCount }}</b> 题未作答，用时
        <b>{{ formatTime(seconds) }}</b>，确认交卷吗？
      </p>
      <template #footer>
        <el-button @click="confirmVisible = false">继续答题</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPaper">确认交卷</el-button>
      </template>
    </el-dialog>

    <!-- 回退确认：答题中离开会丢失进度 -->
    <el-dialog v-model="leaveVisible" title="离开练习" width="420px">
      <p class="confirm-text">
        已答 <b>{{ answeredCount }}/{{ total }}</b> 题，用时 <b>{{ formatTime(seconds) }}</b>。
        交卷可保存本次成绩到答题记录；直接离开将<el-text type="danger">放弃本次作答</el-text>。
      </p>
      <template #footer>
        <el-button @click="leaveVisible = false">继续答题</el-button>
        <el-button @click="leaveWithoutSubmit">放弃离开</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAndLeave">交卷并查看成绩</el-button>
      </template>
    </el-dialog>

    <!-- 历史答题记录 -->
    <el-drawer v-model="historyVisible" title="历史答题记录" size="480px">
      <div v-if="historyLoading" class="drawer-tip">
        <el-skeleton :rows="5" animated />
      </div>
      <div v-else-if="historyList.length === 0" class="drawer-tip">
        <el-empty description="暂无答题记录，快去练一组吧" :image-size="80" />
      </div>
      <div v-else class="history-list">
        <div
          v-for="h in historyList"
          :key="h.id"
          class="history-item"
          @click="viewHistory(h)"
        >
          <div class="h-head">
            <el-tag size="small" effect="plain">{{ PRACTICE_MODE_LABEL[h.mode] }}</el-tag>
            <span class="h-time">{{ formatDateTime(h.createTime) }}</span>
          </div>
          <div class="h-stats">
            <span>答对 <b class="ok-text">{{ h.correctCount }}</b>/{{ h.totalCount }}</span>
            <span>得分 <b>{{ h.score }}</b></span>
            <span>正确率 <b class="accent-text">{{ accuracyOf(h) }}%</b></span>
            <span>用时 {{ formatTime(h.durationSeconds ?? 0) }}</span>
          </div>
          <div class="h-foot">
            <span>已答 {{ h.answeredCount }}/{{ h.totalCount }} 题</span>
            <span class="h-view">查看详情 →</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 成绩结果 -->
    <div v-if="finished && result" class="result-wrap">
      <div class="result-header">
        <div class="result-item">
          <div class="result-label">用时</div>
          <div class="result-val">{{ formatTime(result.durationSeconds ?? seconds) }}</div>
        </div>
        <div class="result-item">
          <div class="result-label">得分</div>
          <div class="result-val accent">{{ result.score }}/{{ answeredItems.length * 10 }}</div>
        </div>
        <div class="result-item">
          <div class="result-label">答对</div>
          <div class="result-val">{{ result.correctCount }}/{{ answeredItems.length }}</div>
        </div>
        <div class="result-item">
          <div class="result-label">正确率</div>
          <div class="result-val accent">{{ accuracy }}%</div>
        </div>
      </div>

      <div class="review-list">
        <template v-for="(item, i) in result.items" :key="item.question.id">
          <div
            v-if="isAnswered(item)"
            class="review-item"
            :class="reviewClass(item)"
          >
          <div class="review-q">
            <el-tag size="small" :type="reviewTagType(item)" effect="light">
              {{ reviewIcon(item) }}
            </el-tag>
            <el-tag size="small" effect="plain">{{ typeLabel(item.question.type) }}</el-tag>
            <span class="review-stem">{{ i + 1 }}. {{ item.question.stem }}</span>
          </div>

          <template v-if="item.evaluation">
            <!-- 主观题：AI 评测卡片 -->
            <div class="eval-card">
              <div class="eval-head">
                <el-tag :type="item.evaluation.evalType === 'AI' ? 'warning' : 'info'" size="small" effect="dark">
                  {{ item.evaluation.evalType === 'AI' ? 'AI 智能评测' : '规则评测' }}
                </el-tag>
                <span class="eval-score">得分 {{ item.evaluation.score ?? '-' }}/{{ totalScoreOf(item) }}</span>
                <el-button
                  v-if="item.evaluation.status === 'FAILED'"
                  size="small"
                  type="warning"
                  plain
                  @click="retryEval(item)"
                >重试评测</el-button>
                <el-tag v-else-if="item.evaluation.status === 'PENDING'" size="small" type="warning">待评测</el-tag>
              </div>
              <div v-if="item.evaluation.pointResults.length" class="point-list">
                <div
                  v-for="pr in item.evaluation.pointResults"
                  :key="pr.point"
                  class="point-item"
                  :class="pr.hit ? 'hit' : 'miss'"
                >
                  <span class="point-icon">{{ pr.hit ? '✓' : '✗' }}</span>
                  <span class="point-text">{{ pr.point }}</span>
                  <span class="point-score">{{ pr.earned }}/{{ pr.total }}</span>
                </div>
              </div>
              <div v-if="item.evaluation.comment" class="eval-comment">💬 {{ item.evaluation.comment }}</div>
              <div v-if="item.evaluation.status === 'FAILED'" class="eval-failed-tip">
                模型评测失败，可点击「重试评测」重新评分。
              </div>
            </div>
          </template>
          <template v-else>
            <div class="review-a">
              你的答案：<b :class="item.correct === false ? 'bad-text' : 'ok-text'">{{ item.userAnswer || '未作答' }}</b>
              <span v-if="item.score != null" class="review-score">（{{ item.score }} 分）</span>
            </div>
            <div class="review-a">
              正确答案：<b class="ok-text">{{ correctAnswerText(item) }}</b>
            </div>
          </template>

          <div v-if="item.question.analysis" class="analysis-toggle" @click="toggleAnalysis(item.question.id)">
            <span class="analysis-arrow">{{ expanded.has(item.question.id) ? '▾' : '▸' }}</span>
            {{ expanded.has(item.question.id) ? '收起解析' : '展开解析' }}
          </div>
          <div v-if="item.question.analysis && expanded.has(item.question.id)" class="analysis-text">{{ item.question.analysis }}</div>
          </div>
        </template>
      </div>

      <div class="result-actions">
        <el-button @click="goHome">返回题库</el-button>
        <el-button type="primary" @click="restart">再来一组</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createPracticeApi,
  submitAllApi,
  historyApi,
  getPracticeApi,
  PRACTICE_MODE_LABEL,
  type PracticeDetailVO,
  type PracticeHistoryVO,
  type PracticeItemVO,
  type TypeQuota,
} from '@/api/practice'
import { retryEvaluationApi } from '@/api/evaluation'
import { QUESTION_TYPE_LABEL, type QuestionType, type QuestionVO } from '@/api/question'

const route = useRoute()
const router = useRouter()
const title = computed(() => (route.query.title ? String(route.query.title) : '综合练习'))
const mode = computed(() => (route.query.mode as PracticeDetailVO['mode']) || 'PAPER_BANK')
const modeLabel = computed(() => PRACTICE_MODE_LABEL[mode.value])

const creating = ref(false)
const practiceId = ref<number | null>(null)
const questions = ref<QuestionVO[]>([])
const answers = ref<Record<number, string[]>>({}) // questionId -> 选中的 key 或 [文本]
const questionCount = ref(loadCountPref()) // 卡片练习出题数：5/10/15（与设置页"学习偏好"双向同步）
const currentIndex = ref(0)
const seconds = ref(0)
const finished = ref(false)
const confirmVisible = ref(false)
const leaveVisible = ref(false)
const submitting = ref(false)
const result = ref<PracticeDetailVO | null>(null)
// 结果页解析展开集合
const expanded = ref<Set<number>>(new Set())
// 历史答题记录
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyList = ref<PracticeHistoryVO[]>([])

// 答题卡分页：每页 30 题
const PAGE_SIZE = 30
const currentPage = ref(1)

let timer: ReturnType<typeof setInterval> | null = null

/** 学习偏好存取：与设置页"每轮练习题目数量"双向同步 */
const PREF_KEY = 'study_prefs'
interface StudyPrefs {
  questionCount: number
  showAnalysis: boolean
  difficulty: string
}

function loadCountPref(): number {
  try {
    const raw = localStorage.getItem(PREF_KEY)
    if (!raw) return 10
    const p = JSON.parse(raw) as Partial<StudyPrefs>
    const c = Number(p.questionCount)
    return c === 5 || c === 10 || c === 15 ? c : 10
  } catch {
    return 10
  }
}

function saveCountPref(c: number) {
  try {
    const raw = localStorage.getItem(PREF_KEY)
    const p = raw ? (JSON.parse(raw) as Partial<StudyPrefs>) : {}
    p.questionCount = c
    localStorage.setItem(PREF_KEY, JSON.stringify(p))
  } catch {
    /* 偏好保存失败不影响做题 */
  }
}

/** 难度偏好 → 后端 difficulty（1-5）。MIX 不限制；进阶(4)题库少，由 AI 生成难题补足 */
function mapDifficulty(pref: string): number | undefined {
  switch (pref) {
    case 'EASY':
      return 2
    case 'MIDDLE':
      return 3
    case 'HARD':
      return 4
    default:
      return undefined
  }
}

function loadDiffPref(): string {
  try {
    const raw = localStorage.getItem(PREF_KEY)
    if (!raw) return 'MIX'
    return (JSON.parse(raw) as Partial<StudyPrefs>).difficulty || 'MIX'
  } catch {
    return 'MIX'
  }
}

watch(questionCount, (v) => saveCountPref(v))

const current = computed(() => questions.value[currentIndex.value] as QuestionVO)
const total = computed(() => questions.value.length)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))
const displayedNumbers = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return Array.from({ length: Math.min(PAGE_SIZE, total.value - start) }, (_, i) => start + i + 1)
})

const answeredCount = computed(() =>
  questions.value.filter((q) => (answers.value[q.id] || []).length > 0).length,
)
/** 结果页已答的题（未答整题不展示） */
const answeredItems = computed(() =>
  (result.value?.items || []).filter((it) => isAnswered(it)),
)
/** 正确率 = 答对数 / 已答题数 */
const accuracy = computed(() => {
  if (!answeredItems.value.length || !result.value) return 0
  return Math.round((result.value.correctCount / answeredItems.value.length) * 100)
})

const shortAnswer = computed({
  get: () => (answers.value[current.value?.id] || [''])[0] || '',
  set: (val: string) => {
    if (current.value) answers.value[current.value.id] = [val]
  },
})

function formatTime(s: number): string {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

function typeLabel(type: string): string {
  return QUESTION_TYPE_LABEL[type as QuestionType] || type
}

function typeTagType(t: QuestionType): 'primary' | 'warning' | 'success' | 'info' {
  switch (t) {
    case 'SINGLE':
      return 'primary'
    case 'MULTIPLE':
      return 'warning'
    case 'JUDGE':
      return 'success'
    default:
      return 'info'
  }
}

function isChoice(q: QuestionVO): boolean {
  return q.type === 'SINGLE' || q.type === 'MULTIPLE'
}

function isPicked(key: string): boolean {
  return (answers.value[current.value?.id] || []).includes(key)
}

function pick(type: QuestionType, key: string) {
  const qid = current.value.id
  const cur = answers.value[qid] || []
  if (type === 'MULTIPLE') {
    const idx = cur.indexOf(key)
    if (idx >= 0) cur.splice(idx, 1)
    else cur.push(key)
    answers.value[qid] = [...cur]
  } else {
    answers.value[qid] = [key]
  }
}

function noClass(idx: number): string {
  const q = questions.value[idx]
  const answered = q && (answers.value[q.id] || []).length > 0
  const cls = []
  if (idx === currentIndex.value) cls.push('current')
  if (answered) cls.push('answered')
  return cls.join(' ')
}

function jumpTo(idx: number) {
  currentIndex.value = idx
  currentPage.value = Math.floor(idx / PAGE_SIZE) + 1
}

/** 组答案提交体 */
function buildAnswerBody(): { questionId: number; userAnswer: string }[] {
  return Object.entries(answers.value)
    .filter(([, v]) => v.length > 0)
    .map(([qid, v]) => ({
      questionId: Number(qid),
      userAnswer: v[0] === undefined ? '' : v.join(','),
    }))
}

async function submitPaper() {
  if (!practiceId.value) return
  submitting.value = true
  stopTimer() // 点击交卷即停计时，不等 AI 主观题评分完成
  try {
    result.value = await submitAllApi(practiceId.value, buildAnswerBody())
    finished.value = true
    confirmVisible.value = false
    leaveVisible.value = false
  } finally {
    submitting.value = false
  }
}

/** 左上角回退：答题中弹确认，防止误退丢进度 */
function onBackClick() {
  if (finished.value) {
    goHome()
    return
  }
  leaveVisible.value = true
}

/** 放弃离开：不交卷直接返回 */
function leaveWithoutSubmit() {
  leaveVisible.value = false
  goHome()
}

/** 交卷并离开：先交卷（记录历史），完成后停留在结果页 */
async function submitAndLeave() {
  await submitPaper()
  if (finished.value) {
    goHome()
  }
}

/** 解析折叠切换 */
function toggleAnalysis(qid: number) {
  const s = new Set(expanded.value)
  if (s.has(qid)) s.delete(qid)
  else s.add(qid)
  expanded.value = s
}

/** 历史记录 */
async function openHistory() {
  historyVisible.value = true
  historyLoading.value = true
  try {
    historyList.value = await historyApi()
  } catch {
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

/** 查看某次历史记录详情（复用结果页渲染） */
async function viewHistory(h: PracticeHistoryVO) {
  historyVisible.value = false
  try {
    const detail = await getPracticeApi(h.id)
    result.value = detail
    finished.value = true
    seconds.value = detail.durationSeconds ?? 0
    stopTimer()
    expanded.value = new Set()
  } catch {
    /* 错误已由拦截器提示 */
  }
}

function formatDateTime(s: string): string {
  const d = new Date(s)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function accuracyOf(h: PracticeHistoryVO): number {
  if (!h.totalCount) return 0
  return Math.round((h.correctCount / h.totalCount) * 100)
}

async function retryEval(item: PracticeItemVO) {
  if (!item.answerRecordId || !result.value) return
  try {
    const ev = await retryEvaluationApi(item.answerRecordId)
    item.evaluation = ev
    ElMessage.success('评测完成')
  } catch {
    /* 错误已由拦截器提示 */
  }
}

function reviewClass(item: PracticeItemVO): string {
  if (item.evaluation) {
    return item.evaluation.status === 'FAILED' ? 'bad' : item.evaluation.status === 'SUCCESS' ? 'ok' : ''
  }
  if (item.correct == null) return ''
  return item.correct ? 'ok' : 'bad'
}

/** 结果页只展示已答的题：未作答整题隐藏 */
function isAnswered(item: PracticeItemVO): boolean {
  return !!(item.userAnswer && item.userAnswer.trim() !== '')
}

function reviewTagType(item: PracticeItemVO): 'success' | 'danger' | 'info' | 'warning' {
  if (item.evaluation) {
    if (item.evaluation.status === 'FAILED') return 'danger'
    if (item.evaluation.status === 'PENDING') return 'info'
    return item.evaluation.score != null && item.evaluation.score >= totalScoreOf(item) * 0.6 ? 'success' : 'warning'
  }
  if (item.correct == null) return 'info'
  return item.correct ? 'success' : 'danger'
}

function reviewIcon(item: PracticeItemVO): string {
  if (item.evaluation) {
    if (item.evaluation.status === 'FAILED') return '评测失败'
    if (item.evaluation.status === 'PENDING') return '待评测'
    return item.evaluation.score != null && item.evaluation.score >= totalScoreOf(item) * 0.6 ? '✓' : '✗'
  }
  if (item.correct == null) return '未答'
  return item.correct ? '✓' : '✗'
}

function totalScoreOf(item: PracticeItemVO): number {
  const points = item.question.scorePoints || []
  if (!points.length) return 10
  return points.reduce((s, p) => s + (p.score || 0), 0)
}

function correctAnswerText(item: PracticeItemVO): string {
  const q = item.question
  if (q.type === 'JUDGE') return q.answer === 'true' ? '正确' : '错误'
  if (q.type === 'SHORT') return q.answer
  return q.answer
}

async function createPractice() {
  creating.value = true
  try {
    // 真题模式："再来一组"复用路由携带的组卷参数
    let typeQuota: TypeQuota[] | undefined
    const rawQuota = route.query.typeQuota ? String(route.query.typeQuota) : ''
    if (rawQuota) {
      try {
        typeQuota = JSON.parse(decodeURIComponent(rawQuota)) as TypeQuota[]
      } catch {
        typeQuota = undefined
      }
    }
    const data = await createPracticeApi({
      mode: mode.value,
      keyword: route.query.keyword ? String(route.query.keyword) : undefined,
      // 卡片练习按"题目难度倾向"抽题（真题保持全难度）
      difficulty: mode.value === 'PAPER_BANK' ? mapDifficulty(loadDiffPref()) : undefined,
      typeQuota,
      count: typeQuota ? typeQuota.reduce((s, q) => s + q.count, 0) : questionCount.value,
    })
    practiceId.value = data.id
    questions.value = data.items.map((it) => it.question)
    answers.value = {}
    currentIndex.value = 0
    currentPage.value = 1
    startTimer()
  } catch {
    /* 无题等错误已由拦截器提示 */
  } finally {
    creating.value = false
  }
}

/** 已创建好的练习（真题等）：直接加载并恢复已答内容 */
async function loadExisting(practiceIdStr: string) {
  creating.value = true
  try {
    const data = await getPracticeApi(Number(practiceIdStr))
    practiceId.value = data.id
    questions.value = data.items.map((it) => it.question)
    // 恢复已答内容（多选题答案逗号连接）
    const restored: Record<number, string[]> = {}
    for (const it of data.items) {
      if (it.userAnswer && it.userAnswer.trim() !== '') {
        restored[it.question.id] = it.userAnswer.split(',').filter(Boolean)
      }
    }
    answers.value = restored
    currentIndex.value = 0
    currentPage.value = 1
    if (data.status === 1) {
      finished.value = true
      result.value = data
      seconds.value = data.durationSeconds ?? 0
    } else {
      startTimer()
    }
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    creating.value = false
  }
}

function restart() {
  result.value = null
  finished.value = false
  seconds.value = 0
  confirmVisible.value = false
  leaveVisible.value = false
  expanded.value = new Set()
  historyVisible.value = false
  createPractice()
}

function goHome() {
  router.push('/questions')
}

function startTimer() {
  stopTimer()
  timer = setInterval(() => {
    seconds.value++
  }, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(() => {
  const pid = route.query.practiceId ? String(route.query.practiceId) : ''
  if (pid) {
    loadExisting(pid)
  } else {
    createPractice()
  }
})

onUnmounted(() => stopTimer())
</script>

<style scoped>
.practice-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px 20px;
}

.top-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.back-btn {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 8px;
  padding: 5px 12px;
  cursor: pointer;
  color: #374151;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 8px;
}

.mode-tag {
  margin-left: 4px;
}

.count-picker {
  margin-left: 8px;
}

.timer {
  margin-left: auto;
  font-family: 'Consolas', monospace;
  font-size: 15px;
  font-weight: 600;
  color: #ea6668;
  background: #fff1f0;
  padding: 4px 12px;
  border-radius: 6px;
}

.answered-stat {
  font-size: 13px;
  color: #6b7280;
}

.creating-tip {
  text-align: center;
  color: #6b7280;
  margin-top: 12px;
}

.quiz-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.quiz-main {
  flex: 1;
  min-width: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 24px;
}

.q-nav {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.q-no {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
}

.stem {
  font-size: 18px;
  color: #1f2937;
  line-height: 1.6;
  margin: 0 0 20px;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  background: #fff;
}

.option.picked {
  border-color: #2563eb;
  background: rgba(37, 99, 235, 0.06);
}

.opt-key {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #f3f4f6;
  color: #374151;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
  font-size: 13px;
}

.option.picked .opt-key {
  background: #2563eb;
  color: #fff;
}

.opt-text {
  flex: 1;
  line-height: 1.5;
}

.judge-box {
  display: flex;
  gap: 20px;
}

.short-box {
  margin-bottom: 4px;
}

.nav-btns {
  margin-top: 24px;
  display: flex;
  justify-content: space-between;
}

/* 右侧答题卡 */
.answer-card {
  width: 260px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
}

.ac-title {
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.ac-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}

.ac-no {
  height: 32px;
  border-radius: 6px;
  background: #f3f4f6;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  cursor: pointer;
  border: 1.5px solid transparent;
}

.ac-no.answered {
  background: #2563eb;
  color: #fff;
}

.ac-no.current {
  border-color: #ea6668;
  font-weight: 700;
}

.ac-no.current.answered {
  background: #1d4ed8;
}

.ac-pager {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.ac-page {
  font-size: 12px;
  color: #6b7280;
}

.ac-legend {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #6b7280;
}

.ac-legend .dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
  margin-right: 4px;
  vertical-align: middle;
}

.ac-legend .dot.answered {
  background: #2563eb;
}

.ac-legend .dot.current {
  border: 1.5px solid #ea6668;
  background: #fff;
}

.ac-legend .dot.todo {
  background: #f3f4f6;
}

.empty-card {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  text-align: center;
}

.confirm-text {
  color: #374151;
  line-height: 1.7;
}

/* 结果页 */
.result-wrap {
  max-width: 900px;
  margin: 0 auto;
}

.result-header {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.result-item {
  flex: 1;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
}

.result-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.result-val {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.result-val.accent {
  color: #2563eb;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-item {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-left: 4px solid #d1d5db;
  border-radius: 8px;
  padding: 14px 16px;
}

.review-item.ok {
  border-left-color: #52c41a;
}

.review-item.bad {
  border-left-color: #ea6668;
}

.review-q {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 8px;
}

.review-stem {
  flex: 1;
  line-height: 1.6;
  color: #1f2937;
}

.review-a {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
}

.review-score {
  color: #9ca3af;
}

.ok-text {
  color: #52c41a;
}

.bad-text {
  color: #ea6668;
}

.analysis-toggle {
  margin-top: 8px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #2563eb;
  cursor: pointer;
  user-select: none;
  padding: 2px 0;
}

.analysis-toggle:hover {
  color: #1d4ed8;
}

.analysis-arrow {
  font-size: 11px;
}

.analysis-text {
  margin-top: 8px;
  background: #f6f7f9;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
}

/* 历史答题记录 */
.drawer-tip {
  padding: 24px 0;
  text-align: center;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.history-item:hover {
  border-color: #bfdbfe;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
}

.h-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.h-time {
  font-size: 12px;
  color: #9ca3af;
}

.h-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 8px;
}

.accent-text {
  color: #2563eb;
  font-weight: 600;
}

.h-foot {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #9ca3af;
}

.h-view {
  color: #2563eb;
}

/* AI 评测卡片 */
.eval-card {
  margin-top: 8px;
  background: #fafbfc;
  border: 1px solid #eef0f2;
  border-radius: 8px;
  padding: 12px;
}

.eval-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.eval-score {
  font-weight: 600;
  color: #374151;
  font-size: 14px;
}

.point-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.point-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 13px;
}

.point-item.hit {
  background: rgba(82, 196, 26, 0.08);
  color: #389e0d;
}

.point-item.miss {
  background: rgba(234, 102, 104, 0.08);
  color: #cf1322;
}

.point-icon {
  font-weight: 700;
  flex-shrink: 0;
}

.point-text {
  flex: 1;
}

.point-score {
  font-weight: 600;
  flex-shrink: 0;
}

.eval-comment {
  margin-top: 10px;
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
  background: #fff;
  border-radius: 6px;
  padding: 8px 10px;
}

.eval-failed-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #d46b08;
}

.result-actions {
  margin-top: 24px;
  text-align: center;
}
</style>
