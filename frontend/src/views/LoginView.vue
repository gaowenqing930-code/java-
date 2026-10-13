<template>
  <div class="auth-page">
    <!-- 左：黑色品牌区 -->
    <aside class="auth-brand">
      <div class="brand-grid"></div>
      <div class="brand-content">
        <span class="brand-mark">AI</span>
        <h1 class="brand-title">智能面试<br />陪练平台</h1>
        <p class="brand-slogan">题库 · AI 出题 · 主观评测 · 学习报告</p>
        <ul class="brand-feats">
          <li><span class="feat-no">01</span><span>多方向题库练习</span></li>
          <li><span class="feat-no">02</span><span>AI 智能生成题目</span></li>
          <li><span class="feat-no">03</span><span>主观题智能评测</span></li>
          <li><span class="feat-no">04</span><span>个性化学习报告</span></li>
        </ul>
        <div class="brand-foot">© 2026 · AI INTERVIEW COACH</div>
      </div>
    </aside>

    <!-- 右：白色表单区 -->
    <section class="auth-panel">
      <div class="panel-inner">
        <div class="panel-head">
          <span class="eyebrow">SIGN IN</span>
          <h2>欢迎回来</h2>
          <p>登录以继续你的学习之旅</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="auth-switch">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>
        <router-link to="/" class="back-home">← 返回首页</router-link>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为6-32位', trigger: 'blur' },
  ],
}

async function handleLogin() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login({ ...form })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/home'
    router.push(redirect)
  } catch {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
  background: var(--paper);
}

/* —— 左黑品牌区 —— */
.auth-brand {
  position: relative;
  flex: 1 1 50%;
  background: var(--ink);
  color: var(--paper);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-grid {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, rgba(255, 255, 255, 0.12) 1px, transparent 1.5px);
  background-size: 26px 26px;
  opacity: 0.5;
}
.brand-grid::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 130px 130px;
}
.brand-content {
  position: relative;
  z-index: 1;
  padding: 48px;
  max-width: 460px;
}
.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  background: var(--paper);
  color: var(--ink);
  font-family: var(--mono);
  font-weight: 700;
  font-size: 22px;
  margin-bottom: 28px;
}
.brand-title {
  margin: 0 0 16px;
  font-size: 42px;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: 0.02em;
}
.brand-slogan {
  margin: 0 0 36px;
  font-size: 15px;
  color: rgba(255, 255, 255, 0.55);
  font-family: var(--mono);
  letter-spacing: 0.04em;
}
.brand-feats {
  list-style: none;
  margin: 0 0 40px;
  padding: 0;
}
.brand-feats li {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 11px 0;
  font-size: 15px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
}
.feat-no {
  font-family: var(--mono);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 0.1em;
}
.brand-foot {
  font-family: var(--mono);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 0.06em;
}

/* —— 右白表单区 —— */
.auth-panel {
  flex: 1 1 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background: var(--paper);
}
.panel-inner {
  width: 100%;
  max-width: 400px;
}
.panel-head {
  margin-bottom: 32px;
}
.eyebrow {
  display: inline-block;
  font-family: var(--mono);
  font-size: 12px;
  letter-spacing: 0.28em;
  color: var(--gray-400);
  margin-bottom: 14px;
}
.panel-head h2 {
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 800;
  color: var(--ink);
}
.panel-head p {
  margin: 0;
  color: var(--gray-500);
  font-size: 14px;
}
.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
}
.auth-switch {
  margin-top: 20px;
  text-align: center;
  color: var(--gray-500);
  font-size: 14px;
}
.auth-switch a {
  color: var(--ink);
  font-weight: 600;
  border-bottom: 1px solid var(--ink);
  padding-bottom: 1px;
}
.back-home {
  display: block;
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: var(--gray-400);
  text-decoration: none;
  transition: color 0.2s;
}
.back-home:hover {
  color: var(--ink);
}

/* 移动端：品牌区收起，只留表单 */
@media (max-width: 860px) {
  .auth-brand {
    display: none;
  }
  .auth-panel {
    flex: 1 1 100%;
  }
}
</style>
