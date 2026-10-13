<template>
  <div class="ai-generate">
    <el-container class="layout">
      <!-- 左侧：出题条件 -->
      <el-aside width="360px" class="aside">
        <el-card shadow="never" class="form-card">
          <template #header>
            <div class="card-head">
              <span>AI 智能出题</span>
              <el-tooltip content="未配置大模型 API Key 时自动从题库兜底抽题" placement="top">
                <el-icon class="hint-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>

          <el-form label-position="top">
            <el-form-item label="目标知识点（选择到三级）">
              <el-tree-select
                v-model="categoryId"
                :data="treeData"
                :props="treeProps"
                node-key="id"
                :render-after-expand="false"
                placeholder="选择知识点"
                popper-class="category-tree-popper"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="题型">
              <el-radio-group v-model="type">
                <el-radio-button value="SINGLE">单选</el-radio-button>
                <el-radio-button value="MULTIPLE">多选</el-radio-button>
                <el-radio-button value="JUDGE">判断</el-radio-button>
                <el-radio-button value="SHORT">简答</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="难度">
              <el-rate v-model="difficulty" :max="5" show-score />
            </el-form-item>

            <el-form-item label="生成数量">
              <el-input-number v-model="count" :min="1" :max="10" />
            </el-form-item>

            <el-button type="primary" size="large" class="generate-btn" :loading="loading" :disabled="!categoryId" @click="handleGenerate">
              {{ loading ? 'AI 出题中…' : '开始生成' }}
            </el-button>
            <div class="tip">提示：每用户每分钟限 5 次；重复题目会自动去重入库。</div>
          </el-form>
        </el-card>
      </el-aside>

      <!-- 右侧：生成结果 -->
      <el-main class="main">
        <el-alert
          v-if="result"
          :type="result.fromFallback ? 'warning' : 'success'"
          :title="result.message"
          :closable="false"
          show-icon
          class="result-alert"
        />

        <el-empty v-if="!result && !loading" description="选择知识点与条件后点击「开始生成」，AI 将按结构化 Prompt 输出四类题型题目" />

        <!-- 异步生成中：当前题数/总数 + 耗时计时器 -->
        <div v-else-if="loading" class="gen-progress">
          <div class="gen-progress-num">
            <span class="num">{{ doneNum }}</span>
            <span class="sep">/</span>
            <span class="total">{{ totalNum }}</span>
            <span class="unit">题</span>
          </div>
          <div class="gen-progress-text">{{ progressText }}</div>
          <div class="gen-progress-meta">
            耗时 <span class="time">{{ formatTime(elapsedSec) }}</span>
          </div>
          <div class="gen-progress-hint">正在后台生成题目，完成后自动展示</div>
        </div>

        <template v-else-if="result.questions.length">
          <el-card v-for="(q, idx) in result.questions" :key="q.id" shadow="never" class="question-card">
            <div class="q-head">
              <span class="q-index">第 {{ idx + 1 }} 题</span>
              <el-tag :type="typeTagType(q.type)" effect="light">{{ QUESTION_TYPE_LABEL[q.type] }}</el-tag>
              <el-rate :model-value="q.difficulty" disabled size="small" />
              <el-tag size="small" :type="q.source === 'AI' ? 'warning' : 'info'" effect="dark" class="source-tag">
                {{ q.source === 'AI' ? 'AI 生成' : '题库兜底' }}
              </el-tag>
            </div>

            <h3 class="q-stem">{{ q.stem }}</h3>

            <!-- 选择题（默认不标答案，展开后才高亮正确项） -->
            <div v-if="q.options && q.options.length" class="q-options">
              <div v-for="opt in q.options" :key="opt.key" class="q-option" :class="{ correct: expanded[idx] && isCorrectKey(q, opt.key) }">
                <span class="opt-key">{{ opt.key }}.</span>
                <span class="opt-text">{{ opt.text }}</span>
                <el-icon v-if="expanded[idx] && isCorrectKey(q, opt.key)" class="correct-icon"><CircleCheckFilled /></el-icon>
              </div>
            </div>

            <div class="q-toggle">
              <el-button text type="primary" size="small" @click="toggleExpand(idx)">
                {{ expanded[idx] ? '收起答案与解析' : '查看答案与解析' }}
                <el-icon class="toggle-icon"><component :is="expanded[idx] ? ArrowUp : ArrowDown" /></el-icon>
              </el-button>
            </div>

            <!-- 答案与解析区（默认收起） -->
            <div v-if="expanded[idx]" class="q-answer-area">
              <!-- 选择题答案 -->
              <div v-if="q.options && q.options.length" class="q-answer-line">
                <span class="sub-title">正确答案：</span>{{ q.answer }}
              </div>
              <!-- 判断题答案 -->
              <div v-else-if="q.type === 'JUDGE'" class="q-judge">
                参考答案：
                <el-tag :type="q.answer === 'true' ? 'success' : 'danger'" effect="dark">
                  {{ q.answer === 'true' ? '正确' : '错误' }}
                </el-tag>
              </div>
              <!-- 简答题答案 -->
              <div v-else class="q-short">
                <div class="sub-title">参考答案</div>
                <p class="answer-box">{{ q.answer }}</p>
                <div v-if="q.scorePoints && q.scorePoints.length" class="sub-title">评分点（总分 {{ totalScore(q) }} 分）</div>
                <el-table v-if="q.scorePoints && q.scorePoints.length" :data="q.scorePoints" size="small" border class="score-table">
                  <el-table-column prop="point" label="评分点" />
                  <el-table-column prop="score" label="分值" width="80" align="center" />
                </el-table>
              </div>

              <div v-if="q.analysis" class="q-analysis">
                <span class="sub-title">解析：</span>{{ q.analysis }}
              </div>
            </div>

            <div v-if="q.categoryNames.length" class="q-tags">
              <el-tag v-for="name in q.categoryNames" :key="name" size="small" type="success" effect="plain">
                {{ name }}
              </el-tag>
            </div>
          </el-card>
        </template>

        <el-empty v-else description="当前条件下题库无匹配题目可兜底" />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, ArrowUp, CircleCheckFilled, QuestionFilled } from '@element-plus/icons-vue'
import { getCategoryTreeApi, type CategoryNode } from '@/api/category'
import { QUESTION_TYPE_LABEL, type QuestionType, type QuestionVO } from '@/api/question'
import {
  submitGenerateTaskApi,
  getTaskStatusApi,
  buildGenTaskEventsUrl,
  type AiGenerateResult,
} from '@/api/ai'

const treeData = ref<CategoryNode[]>([])
const categoryId = ref<number>()
const type = ref<QuestionType>('SINGLE')
const difficulty = ref(3)
const count = ref(5)

const loading = ref(false)
const result = ref<AiGenerateResult | null>(null)
/** 每题答案/解析是否展开（默认收起） */
const expanded = ref<boolean[]>([])
/** 异步生成进度 */
const progress = ref(0)
const progressText = ref('')
/** 当前出题数 / 总数 */
const doneNum = ref(0)
const totalNum = ref(0)
/** 纯前端耗时计时器（仅展示，不落库） */
const elapsedSec = ref(0)
let elapsedTimer: number | null = null
let eventSource: EventSource | null = null
let pollTimer: number | null = null
/** SSE 静默兜底：长时间无进度事件则转轮询 */
let stallTimer: number | null = null

function startTimer() {
  stopTimer()
  elapsedSec.value = 0
  elapsedTimer = window.setInterval(() => {
    elapsedSec.value++
  }, 1000)
}

function stopTimer() {
  if (elapsedTimer) {
    clearInterval(elapsedTimer)
    elapsedTimer = null
  }
}

function formatTime(s: number) {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

/** 最终结束：全清（SSE + 轮询 + 兜底定时器 + 计时器） */
function clearAll() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (stallTimer) {
    clearTimeout(stallTimer)
    stallTimer = null
  }
  stopTimer()
}

/** 切轮询时只停 SSE 与兜底定时器，保留耗时计时器继续走 */
function stopSseAndStall() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  if (stallTimer) {
    clearTimeout(stallTimer)
    stallTimer = null
  }
}

/** 重置 SSE 静默兜底定时器：收到 progress 事件时调用，延长等待；到期仍无事件则转轮询 */
function armStallTimer(taskId: number, ms: number) {
  if (stallTimer) {
    clearTimeout(stallTimer)
  }
  stallTimer = window.setTimeout(() => {
    if (loading.value) startPolling(taskId)
  }, ms)
}

/** 完成后统一收尾 */
function onDone(data: { questions?: QuestionVO[]; message?: string }) {
  clearAll()
  result.value = {
    questions: data.questions || [],
    fromFallback: false,
    message: data.message || 'AI 出题成功',
  }
  expanded.value = result.value.questions.map(() => false)
  loading.value = false
}

function onFail(msg?: string) {
  clearAll()
  ElMessage.error(msg || 'AI 出题失败，请重试')
  loading.value = false
}

/** SSE 断线后的轮询兜底：只停 SSE，计时器继续走；每 3s 查一次任务状态直到 SUCCESS/FAILED */
function startPolling(taskId: number) {
  stopSseAndStall()
  if (pollTimer) {
    clearInterval(pollTimer)
  }
  pollTimer = window.setInterval(async () => {
    try {
      const t = await getTaskStatusApi(taskId)
      if (t.status === 'SUCCESS') {
        onDone({ questions: t.questions, message: t.message })
        return
      }
      if (t.status === 'FAILED') {
        onFail(t.message)
        return
      }
      // 后端只给百分比 progress，前端按总数反推当前题数
      const total = totalNum.value || 0
      const done = total ? Math.round(((t.progress || 0) / 100) * total) : 0
      progress.value = t.progress || 0
      doneNum.value = done
      totalNum.value = total
      progressText.value = `已生成 ${done}/${total} 题`
    } catch {
      // 查询失败，继续轮询
    }
  }, 3000)
}

function toggleExpand(idx: number) {
  expanded.value[idx] = !expanded.value[idx]
}

/** 树中仅三级（知识点）节点可选 */
const treeProps = {
  label: 'name',
  children: 'children',
  disabled: (data: CategoryNode) => data.level !== 3,
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

function isCorrectKey(q: QuestionVO, key: string) {
  return q.answer.split(',').map((k) => k.trim()).includes(key)
}

function totalScore(q: QuestionVO) {
  return (q.scorePoints || []).reduce((sum, sp) => sum + (sp.score || 0), 0)
}

async function handleGenerate() {
  if (!categoryId.value) {
    ElMessage.warning('请先选择知识点')
    return
  }
  loading.value = true
  result.value = null
  progress.value = 0
  progressText.value = '正在提交任务…'
  doneNum.value = 0
  totalNum.value = count.value
  clearAll()
  try {
    // 1. 提交异步任务，立即返回 taskId
    const t = await submitGenerateTaskApi({
      categoryId: categoryId.value,
      type: type.value,
      difficulty: difficulty.value,
      count: count.value,
    })
    progressText.value = '正在生成题目…'
    startTimer()

    // 2. SSE 订阅进度与结果
    eventSource = new EventSource(buildGenTaskEventsUrl(t.taskId))
    eventSource.addEventListener('progress', (e) => {
      try {
        const d = JSON.parse((e as MessageEvent).data)
        progress.value = d.percent || 0
        doneNum.value = d.done || 0
        totalNum.value = d.total || 0
        progressText.value = `已生成 ${d.done}/${d.total} 题`
        // SSE 活着：重置静默兜底，避免第一题生成慢导致误切轮询
        armStallTimer(t.taskId, 30000)
      } catch {
        /* ignore */
      }
    })
    eventSource.addEventListener('done', (e) => {
      try {
        onDone(JSON.parse((e as MessageEvent).data))
      } catch {
        /* ignore */
      }
    })
    eventSource.addEventListener('failed', (e) => {
      try {
        onFail(JSON.parse((e as MessageEvent).data).message)
      } catch {
        onFail()
      }
    })
    // 3. 连接断开：转轮询兜底（任务可能仍在后台生成）
    eventSource.onerror = () => startPolling(t.taskId)
    // 4. 静默兜底：初始 30s 无任何事件也转轮询；收到 progress 会自动重置延长
    armStallTimer(t.taskId, 30000)
  } catch (e: any) {
    ElMessage.error(e?.message || '提交任务失败')
    loading.value = false
  }
}

onMounted(async () => {
  treeData.value = await getCategoryTreeApi()
})

onBeforeUnmount(() => {
  clearAll()
})
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  border-right: 1px solid #e5e7eb;
  background: #fff;
  padding: 16px;
}

.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.hint-icon {
  color: #8b9bb4;
  cursor: help;
}

.generate-btn {
  width: 100%;
  margin-top: 4px;
}

.tip {
  margin-top: 10px;
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.6;
}

.main {
  background: #f6f7f9;
}

.result-alert {
  margin-bottom: 16px;
}

.gen-progress {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 40px 24px;
  text-align: center;
}

.gen-progress-num {
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
  color: #111827;
}

.gen-progress-num .sep {
  margin: 0 6px;
  color: #9ca3af;
}

.gen-progress-num .total {
  color: #9ca3af;
  font-weight: 500;
}

.gen-progress-num .unit {
  margin-left: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #6b7280;
}

.gen-progress-text {
  margin-top: 12px;
  font-size: 14px;
  color: #374151;
}

.gen-progress-meta {
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
}

.gen-progress-meta .time {
  font-variant-numeric: tabular-nums;
  color: #111827;
  font-weight: 600;
}

.gen-progress-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #9ca3af;
}

.question-card {
  margin-bottom: 16px;
}

.q-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.q-index {
  font-weight: 600;
  color: #1f2937;
}

.source-tag {
  margin-left: auto;
}

.q-stem {
  margin: 12px 0;
  line-height: 1.6;
}

.q-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.q-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f6f7f9;
  border: 1px solid transparent;
}

.q-option.correct {
  background: rgba(82, 196, 26, 0.08);
  border-color: rgba(82, 196, 26, 0.35);
}

.opt-key {
  font-weight: 600;
  color: #374151;
}

.opt-text {
  flex: 1;
}

.correct-icon {
  color: #52c41a;
}

.q-judge {
  margin-top: 4px;
}

.q-toggle {
  margin: 10px 0 2px;
}

.toggle-icon {
  margin-left: 2px;
}

.q-answer-area {
  margin-top: 4px;
  padding: 4px 0;
}

.q-answer-line {
  margin-bottom: 6px;
}

.sub-title {
  font-weight: 600;
  color: #374151;
  margin: 8px 0 6px;
  display: block;
}

.q-answer-line .sub-title,
.q-analysis .sub-title {
  display: inline;
  margin: 0 4px 0 0;
}

.answer-box {
  background: #f6f7f9;
  border-radius: 6px;
  padding: 10px 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  color: #374151;
}

.score-table {
  max-width: 520px;
}

.q-analysis {
  color: #6b7280;
  line-height: 1.7;
  white-space: pre-wrap;
}

.q-tags {
  margin-top: 12px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
</style>

<!-- 下拉树挂载在 body（teleport），需全局样式。
     el-tree-select 的树节点由 tree-select-option(继承 ElOption) 渲染为 .el-select-dropdown__item，
     L1/L2 因 disabled 回调带 is-disabled：默认灰字 + not-allowed，会让用户误以为不可点。
     这里仅恢复视觉：仍可点击展开下级，只是不能作为最终选中值。 -->
<style>
.category-tree-popper .el-select-dropdown__item.is-disabled {
  cursor: pointer;
  color: #303133;
}
.category-tree-popper .el-select-dropdown__item.is-disabled:hover {
  background-color: #f5f7fa;
}
</style>
