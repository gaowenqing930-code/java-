<template>
  <div class="exam-paper">
    <el-page-header @back="$router.push('/questions')" class="back-bar">
      <template #content>
        <span class="back-title">真题套卷</span>
      </template>
    </el-page-header>

    <div v-if="paper" class="paper-header">
      <div class="paper-logo-lg">{{ paper.company.slice(0, 2) }}</div>
      <div class="paper-head-info">
        <div class="paper-head-title">
          {{ paper.year }}年{{ paper.season }}-{{ paper.company }}-{{ paper.round }}{{ paper.position }}
        </div>
        <div class="paper-head-meta">
          <el-tag :type="paper.type === '笔试' ? 'primary' : 'warning'" effect="light">{{ paper.type }}真题</el-tag>
          <span>{{ paper.questionCount }} 题</span>
          <span>热度 🔥 {{ paper.heat }}</span>
        </div>
      </div>
      <el-button type="primary" size="large" class="start-btn" :loading="starting" @click="onStart">
        开始答题
      </el-button>
    </div>

    <el-card shadow="never" class="question-list">
      <div v-if="questions.length" class="list-title">本卷收录题目（{{ questions.length }} 道）</div>
      <div v-for="(q, i) in questions" :key="q.id" class="q-item">
        <div class="q-line">
          <span class="q-index">{{ i + 1 }}.</span>
          <el-tag :type="typeTagType(q.type)" size="small" effect="light">{{ typeLabel(q.type) }}</el-tag>
          <span class="q-stem">{{ q.stem }}</span>
        </div>
      </div>
      <el-empty v-if="!questions.length" description="本卷题目待录入，暂不可答题" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getExamPaperApi, type ExamPaperVO } from '@/api/examPaper'
import { QUESTION_TYPE_LABEL, type QuestionVO, type QuestionType } from '@/api/question'
import { createPracticeApi } from '@/api/practice'

const route = useRoute()
const router = useRouter()
const paperId = computed(() => String(route.params.paperId))

const paper = ref<ExamPaperVO | null>(null)
const questions = ref<QuestionVO[]>([])
const starting = ref(false)

function typeLabel(type: string): string {
  return QUESTION_TYPE_LABEL[type as QuestionType] || type
}

function typeTagType(type: QuestionType): 'primary' | 'warning' | 'success' | 'info' {
  switch (type) {
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

/** 卷子详情：后端返回预置固定题目（整套卷子直接作答） */
onMounted(async () => {
  try {
    const data = await getExamPaperApi(paperId.value)
    paper.value = data
    questions.value = data.questions || []
  } catch {
    ElMessage.error('卷子加载失败')
  }
})

async function onStart() {
  if (!paper.value || starting.value) return
  starting.value = true
  try {
    const data = await createPracticeApi({
      mode: 'EXAM',
      paperId: Number(paperId.value),
      count: questions.value.length,
    })
    router.push({
      path: '/questions/practice',
      query: {
        practiceId: String(data.id),
        mode: 'EXAM',
        title: `${paper.value.year}年${paper.value.season}-${paper.value.company}-${paper.value.round}${paper.value.position}`,
      },
    })
  } catch {
    ElMessage.error('创建练习失败，请稍后重试')
  } finally {
    starting.value = false
  }
}
</script>

<style scoped>
.exam-paper {
  max-width: 900px;
  margin: 0 auto;
  padding: 16px 20px;
}

.back-bar {
  margin-bottom: 16px;
}

.back-title {
  font-size: 16px;
  font-weight: 600;
}

.paper-header {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.paper-logo-lg {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  background: linear-gradient(135deg, #9bbbf4, #8bc8ea);
  color: #fff;
  font-weight: 700;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.paper-head-info {
  flex: 1;
  min-width: 0;
}

.paper-head-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
  line-height: 1.5;
}

.paper-head-meta {
  display: flex;
  gap: 16px;
  align-items: center;
  font-size: 13px;
  color: #9ca3af;
}

.start-btn {
  flex-shrink: 0;
}

.question-list {
  padding: 4px 8px;
}

.list-title {
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.q-item {
  padding: 12px 4px;
  border-bottom: 1px solid #f0f0f0;
}

.q-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.q-index {
  color: #9ca3af;
  font-size: 13px;
  flex-shrink: 0;
}

.q-stem {
  color: #1f2937;
  line-height: 1.6;
}
</style>
