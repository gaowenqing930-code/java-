<template>
  <div class="wrong-page">
    <div class="page-head">
      <h2 class="page-title">错题本</h2>
      <div class="head-actions">
        <el-select v-model="typeFilter" placeholder="题型" clearable style="width: 140px" @change="load(1)">
          <el-option label="单选题" value="SINGLE" />
          <el-option label="多选题" value="MULTIPLE" />
          <el-option label="判断题" value="JUDGE" />
          <el-option label="简答题" value="SHORT" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索题干关键词" clearable style="width: 200px" @keyup.enter="load(1)" @clear="load(1)">
          <template #append>
            <el-button @click="load(1)">搜索</el-button>
          </template>
        </el-input>
        <el-button type="warning" plain :loading="repracticing" @click="startRepractice">
          一键重练（{{ total }}）
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="list-empty"><el-skeleton :rows="6" animated /></div>
    <el-empty v-else-if="list.length === 0" description="暂无错题，答错的题目会自动收录到这里" />

    <div v-else class="wrong-list">
      <div v-for="(item, i) in list" :key="item.id" class="wrong-item">
        <div class="w-head">
          <span class="w-no">{{ (pageNum - 1) * pageSize + i + 1 }}</span>
          <el-tag size="small" :type="typeTag(item.question.type)" effect="light">{{ typeLabel(item.question.type) }}</el-tag>
          <el-tag size="small" type="danger" effect="plain" v-if="item.wrongCount > 1">错 {{ item.wrongCount }} 次</el-tag>
          <span class="w-time">{{ formatDate(item.updateTime) }}</span>
          <el-button size="small" type="primary" link @click="removeItem(item)">移除（已掌握）</el-button>
        </div>
        <div class="w-stem">{{ item.question.stem }}</div>

        <div v-if="isChoice(item.question)" class="w-options">
          <div v-for="opt in item.question.options" :key="opt.key" class="w-opt">
            <span class="opt-key">{{ opt.key }}</span>
            <span class="opt-text">{{ opt.text }}</span>
          </div>
        </div>
        <div class="w-answer">
          <span>正确答案：<b class="ok-text">{{ answerText(item.question) }}</b></span>
          <span v-if="item.question.answerText" class="w-answer-note">{{ item.question.answerText }}</span>
        </div>
        <div v-if="item.question.analysis" class="analysis-toggle" @click="toggle(item.id)">
          <span class="analysis-arrow">{{ expanded.has(item.id) ? '▾' : '▸' }}</span>
          {{ expanded.has(item.id) ? '收起解析' : '展开解析' }}
        </div>
        <div v-if="item.question.analysis && expanded.has(item.id)" class="analysis-text">{{ item.question.analysis }}</div>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { repracticeApi, removeWrongApi, wrongPageApi, type WrongQuestionVO } from '@/api/wrong'
import { QUESTION_TYPE_LABEL, type QuestionType, type QuestionVO } from '@/api/question'

const router = useRouter()
const list = ref<WrongQuestionVO[]>([])
const loading = ref(false)
const repracticing = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const typeFilter = ref('')
const keyword = ref('')
const expanded = ref<Set<number>>(new Set())

async function load(page = pageNum.value) {
  loading.value = true
  try {
    const data = await wrongPageApi({
      type: typeFilter.value || undefined,
      keyword: keyword.value || undefined,
      pageNum: page,
      pageSize: pageSize.value,
    })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function toggle(id: number) {
  const s = new Set(expanded.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  expanded.value = s
}

async function removeItem(item: WrongQuestionVO) {
  try {
    await ElMessageBox.confirm('标记为已掌握并从错题本移除？', '移除错题', { type: 'warning' })
  } catch {
    return
  }
  await removeWrongApi(item.id)
  ElMessage.success('已移除')
  load()
}

async function startRepractice() {
  if (!total.value) return
  repracticing.value = true
  try {
    const practice = await repracticeApi(Math.min(total.value, 20))
    router.push({
      path: '/questions/practice',
      query: { mode: 'WRONG', title: '错题重练', practiceId: practice.id },
    })
  } finally {
    repracticing.value = false
  }
}

function isChoice(q: QuestionVO): boolean {
  return q.type === 'SINGLE' || q.type === 'MULTIPLE'
}

function answerText(q: QuestionVO): string {
  if (q.type === 'JUDGE') return q.answer === 'true' ? '正确' : '错误'
  return q.answer
}

function typeLabel(type: string): string {
  return QUESTION_TYPE_LABEL[type as QuestionType] || type
}

function typeTag(t: QuestionType): 'primary' | 'warning' | 'success' | 'info' {
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

function formatDate(s: string): string {
  const d = new Date(s)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

onMounted(() => load(1))
</script>

<style scoped>
.wrong-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.list-empty {
  padding: 20px 0;
}

.wrong-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wrong-item {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-left: 4px solid #ea6668;
  border-radius: 10px;
  padding: 14px 16px;
}

.w-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.w-no {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #fff1f0;
  color: #ea6668;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.w-time {
  font-size: 12px;
  color: #9ca3af;
  margin-left: auto;
}

.w-stem {
  font-size: 15px;
  color: #1f2937;
  line-height: 1.6;
  margin-bottom: 8px;
}

.w-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 8px;
}

.w-opt {
  display: flex;
  gap: 10px;
  padding: 6px 10px;
  background: #fafbfc;
  border-radius: 6px;
  font-size: 13px;
}

.opt-key {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #f3f4f6;
  color: #374151;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.opt-text {
  flex: 1;
  line-height: 1.5;
}

.w-answer {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 13px;
  color: #4b5563;
}

.ok-text {
  color: #52c41a;
}

.w-answer-note {
  color: #9ca3af;
  font-size: 12px;
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

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
