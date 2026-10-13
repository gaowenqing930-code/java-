<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-row">
        <div>
          <h2>欢迎回来，{{ authStore.user?.username }}</h2>
          <p class="welcome-desc">
            今天也来刷几道题吧，坚持就是进步
          </p>
        </div>
        <div class="welcome-actions">
          <router-link to="/questions">
            <el-button type="primary">开始刷题</el-button>
          </router-link>
          <router-link to="/interview">
            <el-button type="success" plain>AI 模拟面试</el-button>
          </router-link>
        </div>
      </div>
    </el-card>

    <!-- 学习数据概览 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-num">{{ report?.practiceCount ?? '—' }}</div>
        <div class="stat-label">练习次数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ report?.totalAnswered ?? '—' }}</div>
        <div class="stat-label">累计答题</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ rateText }}</div>
        <div class="stat-label">总体正确率</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ report?.weakPoints?.length ?? '—' }}</div>
        <div class="stat-label">薄弱知识点</div>
      </div>
    </div>

    <el-row :gutter="20" class="content-row">
      <!-- 最近练习 -->
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-head">
              <span>最近练习</span>
              <router-link to="/questions" class="more-link">去练习 →</router-link>
            </div>
          </template>
          <el-empty v-if="!historyItems.length" description="还没有练习记录，去题库开始第一练吧" />
          <div v-else>
            <div v-for="h in historyItems" :key="h.id" class="history-item">
              <div class="h-left">
                <div class="h-mode">{{ modeText(h.mode) }}</div>
                <div class="h-time">{{ formatTime(h.createTime) }}</div>
              </div>
              <div class="h-mid">
                <span class="h-rate" :class="rateColor(h.correctRate)">{{ h.correctRate }}%</span>
                <span class="h-correct">{{ h.correctCount }}/{{ h.answeredCount }} 题</span>
              </div>
              <div class="h-right">
                <el-tag v-if="h.score != null" size="small" :type="scoreType(h.score)">
                  {{ h.score }} 分
                </el-tag>
                <el-tag v-else size="small" type="info">练习中</el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 薄弱知识点 -->
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-head">
              <span>待巩固知识点</span>
              <router-link to="/report" class="more-link">学习报告 →</router-link>
            </div>
          </template>
          <el-empty v-if="!(report?.weakPoints?.length)" description="暂无明显薄弱点，继续保持" />
          <div v-else>
            <div v-for="w in report.weakPoints" :key="w.name" class="weak-item">
              <div class="weak-info">
                <span class="weak-name">{{ w.name }}</span>
                <span class="weak-domain">{{ w.domain }}</span>
              </div>
              <el-progress :percentage="w.errorRate" :stroke-width="8" color="#ef4444" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getReportApi, type StudyReportVO } from '@/api/stats'
import { historyApi } from '@/api/practice'

const authStore = useAuthStore()
const report = ref<StudyReportVO | null>(null)

const rateText = computed(() => (report.value ? `${report.value.overallRate}%` : '—'))

interface HistoryItem {
  id: number
  mode: string
  totalCount: number
  correctCount: number
  score: number | null
  answeredCount: number
  correctRate: number
  createTime: string
}

const historyItems = ref<HistoryItem[]>([])

const MODE_TEXT: Record<string, string> = {
  PRACTICE: '专项练习',
  EXAM: '真题',
  AI: 'AI 练习',
  PAPER_BANK: '真题套卷',
}

function modeText(mode: string) {
  return MODE_TEXT[mode] || mode || '练习'
}

function formatTime(t: string) {
  return t ? t.replace('T', ' ').slice(5, 16) : ''
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

onMounted(async () => {
  // 页面刷新后恢复登录态
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
      totalCount: x.totalCount,
      correctCount: x.correctCount,
      score: x.score,
      answeredCount: x.answeredCount,
      correctRate: x.totalCount > 0 ? Math.round((x.correctCount / x.totalCount) * 100) : 0,
      createTime: x.createTime,
    }))
  } catch (e) {
    ElMessage.warning('学习数据加载失败，请稍后重试')
  }
})
</script>

<style scoped>
.dashboard {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 20px 48px;
}

.welcome-card {
  border-radius: 14px;
  margin-bottom: 20px;
}

.welcome-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.welcome-card h2 {
  margin: 0 0 6px;
  color: #111827;
}

.welcome-desc {
  margin: 0;
  color: #6b7280;
}

.welcome-actions {
  display: flex;
  gap: 12px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 16px;
  text-align: center;
  border: 1px solid #eef0f3;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #2563eb;
}

.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
}

.content-row {
  row-gap: 20px;
}

.panel-card {
  border-radius: 14px;
  height: 100%;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.more-link {
  font-size: 13px;
  color: #2563eb;
  text-decoration: none;
  font-weight: 400;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
}

.history-item:last-child {
  border-bottom: none;
}

.h-mode {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.h-time {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.h-mid {
  display: flex;
  align-items: center;
  gap: 10px;
}

.h-rate {
  font-weight: 700;
  font-size: 15px;
}

.rate-good {
  color: #16a34a;
}
.rate-mid {
  color: #d97706;
}
.rate-low {
  color: #dc2626;
}

.h-correct {
  font-size: 13px;
  color: #6b7280;
}

.weak-item {
  padding: 10px 0;
}

.weak-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.weak-name {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.weak-domain {
  font-size: 12px;
  color: #9ca3af;
}
</style>
