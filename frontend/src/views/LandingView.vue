<template>
  <div class="landing">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <div class="hero-badge">面向计算机专业的 AI 求职陪练</div>
        <h1 class="hero-title">
          刷题 · 出题 · 面试，<br />
          <span class="gradient">一站式 AI 求职训练场</span>
        </h1>
        <p class="hero-sub">
          覆盖数据结构、算法、Java、SQL、操作系统等核心方向的题库，AI 智能出题与模拟面试，
          每道题都有 AI 评分与解析，学习报告帮你定位薄弱点。
        </p>
        <div class="hero-actions">
          <router-link to="/questions">
            <el-button type="primary" size="large" round class="cta-primary">
              开始刷题
            </el-button>
          </router-link>
          <router-link to="/interview">
            <el-button size="large" round class="cta-ghost">体验 AI 面试</el-button>
          </router-link>
        </div>
        <p class="hero-note">未登录可浏览本页，登录后解锁全部学习功能</p>
      </div>
    </section>

    <!-- 功能入口 -->
    <section class="section">
      <div class="section-inner">
        <h2 class="section-title">四大核心功能</h2>
        <div class="feature-grid">
          <router-link v-for="f in features" :key="f.path" :to="f.path" class="feature-card">
            <div class="feature-icon" :style="{ background: f.bg }">{{ f.icon }}</div>
            <h3>{{ f.name }}</h3>
            <p>{{ f.desc }}</p>
            <span class="feature-more">立即体验 →</span>
          </router-link>
        </div>
      </div>
    </section>

    <!-- 产品特色 -->
    <section class="section section-alt">
      <div class="section-inner">
        <h2 class="section-title">为什么选择我们</h2>
        <div class="value-grid">
          <div v-for="v in values" :key="v.title" class="value-card">
            <h3>{{ v.title }}</h3>
            <p>{{ v.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="cta-band">
      <div class="section-inner">
        <h2>准备好开始了吗？</h2>
        <p>注册即可开启你的 AI 求职训练之旅</p>
        <div class="cta-actions">
          <router-link to="/register">
            <el-button type="primary" size="large" round>免费注册</el-button>
          </router-link>
          <router-link to="/login">
            <el-button size="large" round>已有账号，登录</el-button>
          </router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
interface Feature {
  path: string
  icon: string
  name: string
  desc: string
  bg: string
}

const features: Feature[] = [
  {
    path: '/questions',
    icon: '题',
    name: '题库练习',
    desc: '专项练习 + 企业笔试/面试真题，选择题即时判分，主答题 AI 评分',
    bg: 'linear-gradient(135deg, #2563eb, #3b82f6)',
  },
  {
    path: '/ai/generate',
    icon: 'AI',
    name: 'AI 智能出题',
    desc: '按知识点一键生成专属题目，含答案与解析，练完自动进错题本',
    bg: 'linear-gradient(135deg, #7c3aed, #a855f7)',
  },
  {
    path: '/interview',
    icon: '面',
    name: 'AI 模拟面试',
    desc: '上传简历，AI 以对话形式逐题追问 10 题，结束后统一评分给出建议',
    bg: 'linear-gradient(135deg, #059669, #10b981)',
  },
  {
    path: '/wrong',
    icon: '错',
    name: '错题本与报告',
    desc: '自动收录错题，学习报告定位薄弱知识点，针对性巩固',
    bg: 'linear-gradient(135deg, #d97706, #f59e0b)',
  },
]

interface Value {
  title: string
  desc: string
}

const values: Value[] = [
  { title: 'AI 全程陪伴', desc: '出题、判分、面试、总结均由 AI 完成，模拟真实面试官追问' },
  { title: '面向求职实战', desc: '覆盖计算机核心方向，题目贴近企业笔试与面试考察点' },
  { title: '数据驱动提升', desc: '学习报告按知识领域与知识点统计正确率，精准定位薄弱环节' },
  { title: '闭环学习路径', desc: '选择方向 → 智能出题 → 在线答题 → AI 评测 → 错题巩固 → 报告复盘' },
]
</script>

<style scoped>
.landing {
  min-height: 100vh;
}

.hero {
  background: linear-gradient(160deg, #eff6ff 0%, #f5f3ff 55%, #ecfeff 100%);
  padding: 88px 20px 72px;
  text-align: center;
}

.hero-inner {
  max-width: 860px;
  margin: 0 auto;
}

.hero-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 999px;
  background: #fff;
  color: #2563eb;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid #dbeafe;
  margin-bottom: 20px;
}

.hero-title {
  font-size: 44px;
  line-height: 1.25;
  color: #111827;
  margin: 0 0 20px;
  font-weight: 700;
}

.gradient {
  background: linear-gradient(90deg, #2563eb, #7c3aed);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-sub {
  font-size: 17px;
  color: #4b5563;
  line-height: 1.8;
  max-width: 640px;
  margin: 0 auto 32px;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.cta-primary {
  padding: 14px 40px;
}

.cta-ghost {
  padding: 14px 40px;
}

.hero-note {
  margin-top: 20px;
  font-size: 13px;
  color: #9ca3af;
}

.section {
  padding: 64px 20px;
  background: #fff;
}

.section-alt {
  background: #f8fafc;
}

.section-inner {
  max-width: 1080px;
  margin: 0 auto;
}

.section-title {
  text-align: center;
  font-size: 28px;
  color: #111827;
  margin: 0 0 40px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.feature-card {
  display: block;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 28px 24px;
  text-decoration: none;
  transition: transform 0.2s, box-shadow 0.2s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.12);
}

.feature-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
}

.feature-card h3 {
  font-size: 17px;
  color: #111827;
  margin: 0 0 8px;
}

.feature-card p {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.7;
  margin: 0 0 14px;
  min-height: 48px;
}

.feature-more {
  font-size: 13px;
  color: #2563eb;
  font-weight: 500;
}

.value-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.value-card {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  border: 1px solid #eef0f3;
}

.value-card h3 {
  font-size: 16px;
  color: #111827;
  margin: 0 0 8px;
}

.value-card p {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.7;
  margin: 0;
}

.cta-band {
  background: linear-gradient(120deg, #1e3a8a, #3730a3);
  padding: 64px 20px;
  text-align: center;
}

.cta-band h2 {
  color: #fff;
  font-size: 28px;
  margin: 0 0 12px;
}

.cta-band p {
  color: #c7d2fe;
  font-size: 16px;
  margin: 0 0 28px;
}

.cta-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}
</style>
