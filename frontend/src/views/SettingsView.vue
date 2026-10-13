<template>
  <div class="settings-page">
    <el-card shadow="never" class="panel">
      <template #header><span class="card-title">账号信息</span></template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ authStore.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ authStore.user?.email || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ authStore.isAdmin ? '管理员' : '普通用户' }}</el-descriptions-item>
      </el-descriptions>
      <div class="block-note">
        <el-alert
          title="修改密码、绑定手机等功能将在后续版本开放"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
    </el-card>

    <el-card shadow="never" class="panel">
      <template #header><span class="card-title">学习偏好</span></template>
      <el-form label-position="left" label-width="160px">
        <el-form-item label="每轮练习题目数量">
          <el-radio-group v-model="prefs.questionCount" @change="savePrefs">
            <el-radio-button :value="5">5 题</el-radio-button>
            <el-radio-button :value="10">10 题</el-radio-button>
            <el-radio-button :value="15">15 题</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="提交后立即显示解析">
          <el-switch v-model="prefs.showAnalysis" @change="savePrefs" />
        </el-form-item>
        <el-form-item label="题目难度倾向">
          <el-radio-group v-model="prefs.difficulty" @change="savePrefs">
            <el-radio-button value="EASY">基础</el-radio-button>
            <el-radio-button value="MIDDLE">中等</el-radio-button>
            <el-radio-button value="HARD">进阶</el-radio-button>
            <el-radio-button value="MIX">混合</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div class="block-note">
        <el-alert
          title="偏好将保存在本机浏览器，部分选项将在后续版本生效"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
    </el-card>

    <el-card shadow="never" class="panel">
      <template #header><span class="card-title">会话</span></template>
      <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const PREF_KEY = 'study_prefs'
const router = useRouter()
const authStore = useAuthStore()

interface StudyPrefs {
  questionCount: number
  showAnalysis: boolean
  difficulty: string
}

const defaults: StudyPrefs = { questionCount: 10, showAnalysis: true, difficulty: 'MIX' }

function loadPrefs(): StudyPrefs {
  try {
    const raw = localStorage.getItem(PREF_KEY)
    return raw ? { ...defaults, ...(JSON.parse(raw) as Partial<StudyPrefs>) } : { ...defaults }
  } catch {
    return { ...defaults }
  }
}

const prefs = reactive<StudyPrefs>(loadPrefs())

function savePrefs() {
  localStorage.setItem(PREF_KEY, JSON.stringify(prefs))
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

onMounted(() => {
  if (authStore.isLoggedIn && !authStore.user) {
    authStore.fetchProfile().catch(() => authStore.logout())
  }
})
</script>

<style scoped>
.settings-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 24px 20px 48px;
}

.panel {
  border-radius: 14px;
  margin-bottom: 20px;
}

.card-title {
  font-weight: 600;
}

.block-note {
  margin-top: 16px;
}
</style>
