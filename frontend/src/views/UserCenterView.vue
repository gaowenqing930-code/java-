<template>
  <div class="profile-page">
    <CareerGoalDialog ref="goalDialog" />
    <el-row :gutter="20">
      <!-- 个人信息 -->
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="panel">
          <div class="profile-head">
            <el-avatar :size="64" class="big-avatar">{{ initial }}</el-avatar>
            <div class="profile-meta">
              <h3>{{ authStore.user?.username }}</h3>
              <el-tag v-if="authStore.isAdmin" size="small" type="warning" effect="dark">管理员</el-tag>
              <el-tag v-else size="small" type="info" effect="plain">普通用户</el-tag>
            </div>
          </div>
          <el-descriptions :column="1" class="profile-desc">
            <el-descriptions-item label="用户名">{{ authStore.user?.username }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ authStore.user?.email || '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ authStore.isAdmin ? '管理员' : '普通用户' }}</el-descriptions-item>
            <el-descriptions-item label="求职目标">
              <span v-if="goalText" class="goal-text">{{ goalText }}</span>
              <span v-else class="goal-empty">未设置</span>
            </el-descriptions-item>
          </el-descriptions>
          <el-button size="small" type="primary" plain class="goal-edit-btn" @click="goalDialog?.maybeAsk(true)">
            修改求职目标
          </el-button>
        </el-card>
      </el-col>

      <!-- 学习概览 -->
      <el-col :xs="24" :md="16">
        <el-card shadow="never" class="panel">
          <template #header><span class="card-title">学习概览</span></template>
          <div class="mini-stats">
            <div class="mini-stat">
              <div class="mini-num">{{ report?.practiceCount ?? '—' }}</div>
              <div class="mini-label">练习次数</div>
            </div>
            <div class="mini-stat">
              <div class="mini-num">{{ report?.totalAnswered ?? '—' }}</div>
              <div class="mini-label">累计答题</div>
            </div>
            <div class="mini-stat">
              <div class="mini-num">{{ report ? report.overallRate + '%' : '—' }}</div>
              <div class="mini-label">总体正确率</div>
            </div>
            <div class="mini-stat">
              <div class="mini-num">{{ report?.weakPoints?.length ?? '—' }}</div>
              <div class="mini-label">薄弱知识点</div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="panel">
          <template #header>
            <div class="card-head">
              <span class="card-title">练习历史</span>
              <router-link to="/questions" class="more-link">去练习 →</router-link>
            </div>
          </template>
          <el-empty v-if="!historyItems.length" description="暂无练习记录" />
          <el-table v-else :data="historyItems" class="history-table" @row-click="openHistory">
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="模式">
              <template #default="{ row }">{{ modeText(row.mode) }}</template>
            </el-table-column>
            <el-table-column label="答题" width="90">
              <template #default="{ row }">{{ row.correctCount }}/{{ row.answeredCount }}</template>
            </el-table-column>
            <el-table-column label="正确率" width="100">
              <template #default="{ row }">
                <span :class="rateColor(row.correctRate)">{{ row.correctRate }}%</span>
              </template>
            </el-table-column>
            <el-table-column label="得分" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.score != null" size="small" :type="scoreType(row.score)">{{ row.score }} 分</el-tag>
                <el-tag v-else size="small" type="info">练习中</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="" width="70">
              <template #default>
                <span class="row-detail-link">查看详情 →</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="panel">
          <template #header>
            <div class="card-head">
              <span class="card-title">AI 面试记录</span>
              <router-link to="/interview/history" class="more-link">查看记录 →</router-link>
            </div>
          </template>
          <div class="interview-entry">
            <div class="entry-desc">回看历次 AI 模拟面试的总分、评价与逐题评语，针对薄弱点再练习。</div>
            <router-link to="/interview/history">
              <el-button type="primary" plain size="small">我的面试记录</el-button>
            </router-link>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getReportApi, type StudyReportVO } from '@/api/stats'
import { historyApi } from '@/api/practice'
import CareerGoalDialog from '@/components/CareerGoalDialog.vue'
import { readFilledGoal } from '@/utils/careerGoal'

const authStore = useAuthStore()
const router = useRouter()
const report = ref<StudyReportVO | null>(null)
const goalDialog = ref<InstanceType<typeof CareerGoalDialog>>()

const initial = computed(() => (authStore.user?.username?.[0] || 'U').toUpperCase())

const goalText = ref('')
function refreshGoalText() {
  const g = readFilledGoal(authStore.user)
  if (g) {
    goalText.value = g.majors?.length ? `${g.majors.join(' / ')} · ${g.position}` : g.position
  } else {
    goalText.value = ''
  }
}
refreshGoalText()

onMounted(() => {
  window.addEventListener('career-goal-changed', refreshGoalText)
})
onUnmounted(() => {
  window.removeEventListener('career-goal-changed', refreshGoalText)
})

interface HistoryItem {
  id: number
  mode: string
  correctCount: number
  answeredCount: number
  score: number | null
  correctRate: number
  createTime: string
}

const historyItems = ref<HistoryItem[]>([])

const MODE_TEXT: Record<string, string> = {
  PRACTICE: '专项练习',
  EXAM: '真题套卷',
  AI: 'AI 练习',
  PAPER_BANK: '专项练习',
}

function modeText(mode: string) {
  return MODE_TEXT[mode] || mode || '练习'
}

function formatTime(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

function rateColor(rate: number) {
  if (rate >= 80) return 'rate-good'
  if (rate >= 60) return 'rate-mid'
  return 'rate-low'
}

function scoreType(score: number) {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

/** 点击历史行：跳转答题页查看该次练习（PracticeView 按 practiceId 加载结果） */
function openHistory(row: HistoryItem) {
  router.push({
    path: '/questions/practice',
    query: { practiceId: String(row.id), mode: row.mode },
  })
}

onMounted(async () => {
  if (authStore.isLoggedIn && !authStore.user) {
    try {
      await authStore.fetchProfile()
    } catch {
      authStore.logout()
      return
    }
  }
  try {
    const [r, h] = await Promise.all([getReportApi(30), historyApi()])
    report.value = r
    historyItems.value = (h || []).map((x) => ({
      id: x.id,
      mode: x.mode,
      correctCount: x.correctCount,
      answeredCount: x.answeredCount,
      score: x.score,
      correctRate: x.totalCount > 0 ? Math.round((x.correctCount / x.totalCount) * 100) : 0,
      createTime: x.createTime,
    }))
  } catch {
    /* 数据加载失败保持空态 */
  }
})
</script>

<style scoped>
.profile-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 20px 48px;
}

.panel {
  border-radius: 14px;
  margin-bottom: 20px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.big-avatar {
  background: #2563eb;
  font-size: 24px;
  font-weight: 600;
}

.profile-meta h3 {
  margin: 0 0 6px;
  color: #111827;
}

.profile-meta .el-tag {
  margin-right: 6px;
}

.goal-text {
  color: #374151;
}

.goal-empty {
  color: #9ca3af;
}

.goal-edit-btn {
  margin-top: 12px;
}

.card-title {
  font-weight: 600;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.more-link {
  font-size: 13px;
  color: #2563eb;
  text-decoration: none;
  font-weight: 400;
}

.interview-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.entry-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
}

.mini-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 12px;
}

.mini-stat {
  background: #f8fafc;
  border-radius: 10px;
  padding: 16px;
  text-align: center;
}

.mini-num {
  font-size: 22px;
  font-weight: 700;
  color: #2563eb;
}

.mini-label {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.rate-good {
  color: #16a34a;
  font-weight: 600;
}
.rate-mid {
  color: #d97706;
  font-weight: 600;
}
.rate-low {
  color: #dc2626;
  font-weight: 600;
}

.history-table :deep(.el-table__row) {
  cursor: pointer;
}

.row-detail-link {
  color: #2563eb;
  font-size: 13px;
  white-space: nowrap;
}
</style>
