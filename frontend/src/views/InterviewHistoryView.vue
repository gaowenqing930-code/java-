<template>
  <div class="ih-page">
    <div class="page-head">
      <h2 class="page-title">AI 面试记录</h2>
      <router-link to="/interview">
        <el-button type="primary" plain size="small">发起新面试</el-button>
      </router-link>
    </div>

    <el-card shadow="never" class="panel">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column label="技术栈" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="t in stackOf(row.techStack)" :key="t" size="small" class="stack-tag">{{ t }}</el-tag>
            <span v-if="!stackOf(row.techStack).length" class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="总分" width="100">
          <template #default="{ row }">
            <span :class="row.totalScore != null ? 'score-num' : 'muted'">
              {{ row.totalScore != null ? row.totalScore + ' 分' : '未评分' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="已答" width="80">
          <template #default="{ row }">{{ row.answeredCount ?? 0 }} 题</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="面试时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.finishTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button size="small" :disabled="row.status !== 'FINISHED'" @click="openDetail(row.sessionId)">
              查看结果
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !list.length" description="暂无面试记录，去发起一次 AI 面试吧" />
    </el-card>

    <!-- 结果详情抽屉 -->
    <el-drawer v-model="drawer" title="面试结果详情" size="480px">
      <div v-if="detailLoading" class="drawer-loading" v-loading="true"></div>
      <div v-else-if="detail">
        <div class="result-head">
          <div class="score-block">
            <div class="score-num" :class="scoreClass(detail.totalScore)">{{ detail.totalScore }}</div>
            <div class="score-label">总分（满分 100）</div>
          </div>
        </div>
        <h4>面试评价</h4>
        <p class="summary">{{ detail.summary }}</p>
        <h4>提升建议</h4>
        <p class="suggestion">{{ detail.suggestion }}</p>

        <el-divider content-position="left">逐题明细</el-divider>
        <div v-for="q in detail.questions" :key="q.no" class="detail-item">
          <div class="detail-head">
            <span class="d-no">第 {{ q.no }} 题</span>
            <el-tag size="small" :type="scoreTag(q.aiScore)" effect="light">
              {{ q.aiScore }} / 10 分
            </el-tag>
          </div>
          <p class="d-q">{{ q.question }}</p>
          <div class="d-answer">
            <div class="d-label">你的回答</div>
            <p class="d-text">{{ q.userAnswer || '（未作答）' }}</p>
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
      </div>
      <div v-else class="muted">加载失败或该场次暂无结果</div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getInterviewResultApi,
  listMyInterviewsApi,
  type InterviewResultVO,
  type InterviewSessionVO,
} from '@/api/interview'

const list = ref<InterviewSessionVO[]>([])
const loading = ref(false)

const drawer = ref(false)
const detailLoading = ref(false)
const detail = ref<InterviewResultVO | null>(null)

async function load() {
  loading.value = true
  try {
    list.value = await listMyInterviewsApi()
  } finally {
    loading.value = false
  }
}

async function openDetail(sessionId: number) {
  drawer.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getInterviewResultApi(sessionId)
  } finally {
    detailLoading.value = false
  }
}

function stackOf(s?: string): string[] {
  if (!s) return []
  return s.split(',').map((t) => t.trim()).filter(Boolean)
}
function statusLabel(s?: string): string {
  if (s === 'FINISHED') return '已完成'
  if (s === 'IN_PROGRESS') return '进行中'
  return '已创建'
}
function statusTag(s?: string): 'success' | 'warning' | 'info' {
  if (s === 'FINISHED') return 'success'
  if (s === 'IN_PROGRESS') return 'warning'
  return 'info'
}
function formatDateTime(s?: string): string {
  if (!s) return '—'
  const d = new Date(s)
  if (Number.isNaN(d.getTime())) return '—'
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
function scoreClass(score?: number): string {
  if (score == null) return ''
  if (score >= 80) return 's-good'
  if (score >= 60) return 's-mid'
  return 's-low'
}
function scoreTag(score?: number): 'success' | 'warning' | 'danger' {
  if (score == null) return 'warning'
  if (score >= 8) return 'success'
  if (score >= 5) return 'warning'
  return 'danger'
}

onMounted(load)
</script>

<style scoped>
.ih-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1b1c;
}
.panel {
  border-radius: 12px;
}
.stack-tag {
  margin-right: 4px;
  margin-bottom: 2px;
}
.muted {
  color: #9ca3af;
}
.score-num {
  font-weight: 600;
  color: #1a1b1c;
}
.drawer-loading {
  min-height: 200px;
}
.result-head {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.score-block {
  text-align: center;
}
.score-num.s-good {
  color: #16a34a;
}
.score-num.s-mid {
  color: #d97706;
}
.score-num.s-low {
  color: #dc2626;
}
.score-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}
.summary,
.suggestion {
  color: #374151;
  line-height: 1.7;
  margin: 4px 0 12px;
}
.detail-item {
  border: 1px solid #eef0f2;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
}
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.d-no {
  font-weight: 600;
  color: #1a1b1c;
}
.d-q {
  font-weight: 500;
  color: #374151;
  line-height: 1.6;
  margin: 4px 0 8px;
}
.d-label {
  font-size: 12px;
  color: #6b7280;
  margin: 6px 0 2px;
}
.d-text {
  color: #374151;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
