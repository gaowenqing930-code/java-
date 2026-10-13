<template>
  <div class="interview-page">
    <!-- 步骤一：上传简历 -->
    <div v-if="step === 'upload'" class="panel">
      <el-card shadow="never" class="form-card">
        <template #header>
          <div class="card-head">
            <span>AI 模拟面试</span>
            <el-tag size="small" type="warning" effect="light">简历驱动 · 10 题 · AI 评分</el-tag>
          </div>
        </template>
        <p class="desc">
          上传你的简历文件（Word / PDF），自动提取内容；也可以直接粘贴简历文本（技术栈 + 项目经历）。
          AI 会根据简历内容以对话形式逐题提问（共 10 题），你逐题作答，全部答完后 AI 统一按得分点评分，
          并给出总分、评价与提升建议。
        </p>
        <el-form label-position="top">
          <el-form-item label="上传简历文件（.doc / .docx / .pdf）">
            <div
              class="upload-box"
              :class="{ 'is-dragover': dragOver }"
              @click="triggerFileInput"
              @dragover.prevent="dragOver = true"
              @dragleave.prevent="dragOver = false"
              @drop.prevent="handleDrop"
            >
              <input
                ref="fileInputRef"
                type="file"
                accept=".doc,.docx,.pdf"
                style="display:none"
                @change="handleNativeFile"
              />
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">点击选择 或 拖拽文件到此处</div>
              <div v-if="fileName" class="file-name">{{ fileName }}</div>
              <div class="el-upload__tip">仅支持 Word / PDF 文本型简历，扫描件（图片型 PDF）无法提取文字</div>
            </div>
          </el-form-item>
          <el-form-item label="简历文本">
            <el-input
              v-model="resumeText"
              type="textarea"
              :rows="10"
              :placeholder="fileHint"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="full-btn" :loading="creating || extracting" :disabled="!resumeText.trim()" @click="handleCreate">
            {{ extracting ? '正在提取文件文本…' : creating ? 'AI 解析简历中…' : '解析简历' }}
          </el-button>
        </el-form>
      </el-card>
    </div>

    <!-- 简历解析确认 -->
    <div v-if="step === 'confirm'" class="panel">
      <el-card shadow="never" class="form-card">
        <template #header>
          <div class="card-head"><span>简历解析结果</span></div>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="技术栈">
            <el-tag v-for="t in parsed?.techStack" :key="t" size="small" class="stack-tag">{{ t }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="项目经历">
            <div v-if="parsed?.projects.length">
              <div v-for="p in parsed?.projects" :key="p.name" class="project-item">
                <b>{{ p.name }}</b>：{{ p.description }}
                <ul v-if="p.highlights && p.highlights.length" class="project-highlights">
                  <li v-for="(h, i) in p.highlights" :key="i">{{ h }}</li>
                </ul>
              </div>
            </div>
            <span v-else class="muted">未识别到明确项目，AI 将主要围绕技术栈提问</span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="actions">
          <el-button @click="resetAll">重新输入</el-button>
          <el-button type="primary" :loading="starting" @click="handleStart">
            {{ starting ? 'AI 准备第一题中…' : '开始面试' }}
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 步骤二：对话式作答 -->
    <div v-if="step === 'interview'" class="panel">
      <el-card shadow="never" class="chat-card">
        <div class="chat-top">
          <div class="chat-meta">
            <span class="chat-count">已答 <b>{{ answeredCount }}</b> 题</span>
            <span class="chat-hint">AI 将根据你的回答引导式提问，可随时结束</span>
          </div>
          <el-button size="small" type="warning" plain :disabled="scoring || answeredCount === 0" @click="handleFinish">
            结束面试
          </el-button>
        </div>
        <div ref="chatBodyRef" class="chat-body">
          <div v-for="m in messages" :key="m.id" class="msg-row" :class="m.role">
            <div class="avatar">{{ m.role === 'ai' ? 'AI' : '我' }}</div>
            <div class="bubble">
              <div v-if="m.type === 'question'" class="q-tag">第 {{ m.no }} 题</div>
              <div class="msg-text">{{ m.content }}</div>
              <div v-if="m.suggestFinish" class="suggest-tip">
                AI 认为技术栈已测评较充分，可结束面试{{ m.finishReason ? '：' + m.finishReason : '' }}（也可继续作答）
              </div>
            </div>
          </div>
          <div v-if="scoring" class="msg-row ai">
            <div class="avatar">AI</div>
            <div class="bubble typing">AI 正在根据你的回答出题…</div>
          </div>
        </div>
        <div class="chat-input">
          <el-input
            v-model="answerText"
            type="textarea"
            :rows="2"
            resize="none"
            :disabled="scoring || retryPending"
            placeholder="输入你的回答，回车发送（Shift+Enter 换行）"
            @keydown.enter.exact.prevent="handleSend"
          />
          <el-button v-if="retryPending" type="primary" :loading="scoring" @click="handleRetry">
            重试出题
          </el-button>
          <el-button v-else type="primary" :loading="scoring" :disabled="!answerText.trim()" @click="handleSend">
            发送
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 步骤三：面试结果 -->
    <div v-if="step === 'result' && result" class="panel">
      <el-card shadow="never" class="result-card">
        <div class="result-head">
          <div class="score-block">
            <div class="score-num" :class="scoreClass">{{ result.totalScore }}</div>
            <div class="score-label">总分（满分 100）</div>
          </div>
          <div class="result-right">
            <h3>面试评价</h3>
            <p class="summary">{{ result.summary }}</p>
          </div>
        </div>
        <div class="suggestion-block">
          <h4>提升建议</h4>
          <p class="suggestion">{{ result.suggestion }}</p>
        </div>

        <el-divider content-position="left">逐题明细</el-divider>
        <div v-for="q in result.questions" :key="q.no" class="detail-item">
          <div class="detail-head">
            <span class="d-no">第 {{ q.no }} 题</span>
            <el-tag size="small" :type="q.aiScore! >= 8 ? 'success' : q.aiScore! >= 5 ? 'warning' : 'danger'" effect="light">
              {{ q.aiScore }} / 10 分
            </el-tag>
          </div>
          <p class="d-q">{{ q.question }}</p>
          <div class="d-answer">
            <div class="d-label">你的回答</div>
            <p class="d-text">{{ q.userAnswer }}</p>
          </div>
          <div class="d-comment">
            <div class="d-label">AI 评语</div>
            <p class="d-text">{{ q.aiComment }}</p>
          </div>
          <div class="d-ref" v-if="q.referenceAnswer">
            <div class="d-label">参考要点</div>
            <p class="d-text">{{ q.referenceAnswer }}</p>
          </div>
        </div>

        <div class="actions">
          <el-button @click="resetAll">再来一轮</el-button>
          <router-link to="/questions"><el-button type="primary" plain>去做专项练习</el-button></router-link>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  answerInterviewApi,
  createInterviewSessionApi,
  extractResumeApi,
  finishInterviewApi,
  nextInterviewApi,
  startInterviewApi,
  type InterviewQuestionVO,
  type InterviewResultVO,
  type ResumeParseVO,
} from '@/api/interview'

type Step = 'upload' | 'confirm' | 'interview' | 'result'

/** 对话消息 */
interface ChatMsg {
  id: number
  role: 'ai' | 'user'
  content: string
  type?: 'question'
  no?: number
  suggestFinish?: boolean
  finishReason?: string
}

const step = ref<Step>('upload')
const resumeText = ref('')
const creating = ref(false)
const extracting = ref(false)
const starting = ref(false)
const scoring = ref(false)
const finishing = ref(false)
/** AI 出下一题失败：等待用户点击「重试出题」 */
const retryPending = ref(false)
const fileName = ref('')
const dragOver = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
let msgSeq = 0

/** 上传文件后提取到的简历文本 placeholder 提示 */
const fileHint = ref('请粘贴简历内容，例如：&#10;熟练掌握 Java、Spring Boot、MySQL、Redis……&#10;项目：AI 智能校园平台（Spring Boot + Vue3），负责后端接口开发……')

/** 选择文件后自动提取文本并填入输入框 */
async function handleRawFile(file: File) {
  const ext = (file.name.split('.').pop() || '').toLowerCase()
  if (!['doc', 'docx', 'pdf'].includes(ext)) {
    ElMessage.warning('仅支持 .doc / .docx / .pdf 格式')
    return
  }
  extracting.value = true
  try {
    const vo = await extractResumeApi(file)
    resumeText.value = vo.text
    fileName.value = vo.fileName || file.name
    fileHint.value = `已从「${vo.fileName || file.name}」提取简历文本，可修改后确认`
    ElMessage.success(`已提取「${vo.fileName || file.name}」内容`)
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    extracting.value = false
  }
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleNativeFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) handleRawFile(file)
  input.value = '' // 清空，允许重复选择同一文件
}

function handleDrop(e: DragEvent) {
  dragOver.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) handleRawFile(file)
}

const parsed = ref<ResumeParseVO | null>(null)
const current = ref<InterviewQuestionVO | null>(null)
const answerText = ref('')
const result = ref<InterviewResultVO | null>(null)
const messages = ref<ChatMsg[]>([])
const chatBodyRef = ref<HTMLElement | null>(null)

/** 已答题数 */
const answeredCount = computed(() => (current.value ? current.value.no - 1 : 0))
const scoreClass = computed(() => {
  const s = result.value?.totalScore ?? 0
  if (s >= 85) return 'score-good'
  if (s >= 70) return 'score-mid'
  if (s >= 55) return 'score-low'
  return 'score-bad'
})

/** 追加一条对话消息并滚动到底部 */
function pushMsg(m: Omit<ChatMsg, 'id'>) {
  messages.value.push({ ...m, id: ++msgSeq })
  scrollChat()
}

function scrollChat() {
  nextTick(() => {
    const el = chatBodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function aiQuestionMsg(q: InterviewQuestionVO) {
  pushMsg({
    role: 'ai',
    content: q.question,
    type: 'question',
    no: q.no,
    suggestFinish: !!q.suggestFinish,
    finishReason: q.finishReason || '',
  })
}

async function handleCreate() {
  creating.value = true
  try {
    parsed.value = await createInterviewSessionApi(resumeText.value.trim())
    step.value = 'confirm'
  } finally {
    creating.value = false
  }
}

async function handleStart() {
  if (!parsed.value) return
  starting.value = true
  try {
    current.value = await startInterviewApi(parsed.value.sessionId)
    answerText.value = ''
    messages.value = []
    msgSeq = 0
    aiQuestionMsg(current.value)
    step.value = 'interview'
  } finally {
    starting.value = false
  }
}

/** 发送回答：先追加自己的消息，再等 AI 出下一题 */
async function handleSend() {
  if (!parsed.value || !current.value || !answerText.value.trim() || scoring.value) return
  const myAnswer = answerText.value.trim()
  pushMsg({ role: 'user', content: myAnswer })
  answerText.value = ''
  scoring.value = true
  try {
    const vo = await answerInterviewApi(parsed.value.sessionId, current.value.no, myAnswer)
    if (vo.finished && vo.result) {
      result.value = vo.result
      step.value = 'result'
    } else if (vo.currentQuestion) {
      current.value = vo.currentQuestion
      aiQuestionMsg(vo.currentQuestion)
    } else if (vo.needRetry) {
      // 出题失败：答案已保存，进入等待重试态，防止死锁（后端顺序校验会拒绝旧题号）
      retryPending.value = true
      pushMsg({ role: 'ai', content: 'AI 出下一题失败，请点击下方「重试出题」继续面试。' })
    }
  } catch {
    /* 网络/服务异常：保持当前题可重发；若后端已保存答案，重发会收到顺序校验提示，属正常保护 */
    retryPending.value = false
  } finally {
    scoring.value = false
  }
}

/** 出题失败后重试生成下一题 */
async function handleRetry() {
  if (!parsed.value || scoring.value) return
  scoring.value = true
  try {
    const q = await nextInterviewApi(parsed.value.sessionId)
    current.value = q
    retryPending.value = false
    aiQuestionMsg(q)
  } catch {
    /* 重试仍失败：保持 retryPending，用户可再次点击，也可直接结束面试 */
  } finally {
    scoring.value = false
  }
}

/** 主动结束面试：确认后统一评分 */
async function handleFinish() {
  if (!parsed.value || scoring.value || answeredCount.value === 0) return
  try {
    await ElMessageBox.confirm(
      `结束面试后将对你已回答的 ${answeredCount.value} 道题统一评分并生成评价与建议，确定结束吗？`,
      '结束面试',
      { confirmButtonText: '结束并评分', cancelButtonText: '继续面试', type: 'warning' },
    )
  } catch {
    return
  }
  finishing.value = true
  scoring.value = true
  try {
    result.value = await finishInterviewApi(parsed.value.sessionId)
    step.value = 'result'
  } catch {
    /* 错误已由拦截器提示 */
  } finally {
    finishing.value = false
    scoring.value = false
  }
}

function resetAll() {
  step.value = 'upload'
  resumeText.value = ''
  parsed.value = null
  current.value = null
  answerText.value = ''
  result.value = null
  messages.value = []
  msgSeq = 0
  retryPending.value = false
  fileName.value = ''
  fileHint.value = '请粘贴简历内容，例如：&#10;熟练掌握 Java、Spring Boot、MySQL、Redis……&#10;项目：AI 智能校园平台（Spring Boot + Vue3），负责后端接口开发……'
}
</script>

<style scoped>
.interview-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 24px 16px;
}

.upload-box {
  width: 100%;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  padding: 30px 16px;
  text-align: center;
  color: var(--el-text-color-secondary);
  transition: border-color 0.2s, color 0.2s;
  cursor: pointer;
  box-sizing: border-box;
}
.upload-box:hover {
  border-color: var(--el-color-primary);
}
.upload-box.is-dragover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.file-name {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-color-primary);
  word-break: break-all;
}
.panel {
  background: #f6f7f9;
  min-height: 70vh;
}

.form-card,
.chat-card,
.result-card {
  border-radius: 10px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.desc {
  color: #6b7280;
  line-height: 1.7;
  margin-bottom: 16px;
}

.full-btn {
  width: 100%;
}

.stack-tag {
  margin: 2px 6px 2px 0;
}

.project-item {
  padding: 4px 0;
}

.project-highlights {
  margin: 4px 0 0 0;
  padding-left: 18px;
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
}

.muted {
  color: #9ca3af;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.q-top {
  margin-bottom: 8px;
}

.q-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.q-no {
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
}

.q-progress {
  flex: 1;
}

.q-stem {
  line-height: 1.7;
  color: #111827;
}

/* 对话式聊天窗口 */
.chat-card {
  display: flex;
  flex-direction: column;
}

.chat-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0 12px;
  border-bottom: 1px solid #f0f1f3;
}

.chat-count {
  color: #1f2937;
  font-weight: 500;
}

.chat-count b {
  color: #1d4ed8;
  font-size: 18px;
  margin: 0 2px;
}

.chat-hint {
  color: #9ca3af;
  font-size: 13px;
  margin-left: 12px;
}

.chat-body {
  min-height: 320px;
  max-height: 52vh;
  overflow-y: auto;
  padding: 16px 8px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: #fafbfc;
  border-radius: 8px;
  margin-top: 12px;
}

.msg-row {
  display: flex;
  gap: 10px;
  max-width: 86%;
}

.msg-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.avatar {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}

.msg-row.ai .avatar {
  background: linear-gradient(135deg, #2563eb, #4f8df9);
}

.msg-row.user .avatar {
  background: #10b981;
}

.bubble {
  background: #fff;
  border: 1px solid #e8eaee;
  border-radius: 10px;
  padding: 10px 14px;
  line-height: 1.7;
  color: #1f2937;
  font-size: 14px;
  min-width: 0;
}

.msg-row.user .bubble {
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.q-tag {
  display: inline-block;
  background: #2563eb;
  color: #fff;
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 10px;
  margin-bottom: 6px;
}

.msg-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.suggest-tip {
  margin-top: 8px;
  padding: 6px 10px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 6px;
  color: #b45309;
  font-size: 13px;
  line-height: 1.6;
}

.bubble.typing {
  color: #9ca3af;
  font-style: italic;
}

.chat-input {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  margin-top: 12px;
}

.chat-input .el-input {
  flex: 1;
}

.chat-input .el-button {
  height: 48px;
  margin-left: 0;
}

.result-head {
  display: flex;
  gap: 28px;
  align-items: flex-start;
  padding: 8px 0 12px;
}

.score-block {
  text-align: center;
  flex-shrink: 0;
}

.score-num {
  font-size: 56px;
  font-weight: 700;
  line-height: 1;
}

.score-good {
  color: #16a34a;
}
.score-mid {
  color: #d97706;
}
.score-low {
  color: #ea580c;
}
.score-bad {
  color: #dc2626;
}

.score-label {
  margin-top: 6px;
  color: #6b7280;
  font-size: 13px;
}

.result-right h3 {
  margin: 4px 0 8px;
}

.summary {
  color: #374151;
  line-height: 1.7;
  margin: 0;
}

.suggestion-block {
  background: #f0f7ff;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 8px;
}

.suggestion-block h4 {
  margin: 0 0 6px;
  color: #1d4ed8;
}

.suggestion {
  margin: 0;
  color: #374151;
  line-height: 1.8;
  white-space: pre-wrap;
}

.detail-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.d-no {
  font-weight: 600;
  color: #1f2937;
}

.d-q {
  font-weight: 500;
  color: #111827;
  margin: 4px 0 10px;
}

.d-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 8px;
}

.d-text {
  margin: 4px 0 0;
  color: #374151;
  line-height: 1.7;
  white-space: pre-wrap;
  background: #f9fafb;
  border-radius: 6px;
  padding: 8px 10px;
}

.d-ref .d-text {
  background: #f0fdf4;
}
</style>
