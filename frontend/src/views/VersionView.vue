<template>
  <div class="version-page">
    <el-card shadow="never" class="panel">
      <div class="version-head">
        <div class="version-badge">v{{ version }}</div>
        <div class="version-info">
          <h2>AI 智能面试与学习陪练平台</h2>
          <p>面向计算机专业求职者的 AI 刷题 · 出题 · 面试一站式训练平台</p>
        </div>
      </div>

      <el-divider content-position="left">功能清单</el-divider>
      <div class="feature-list">
        <div v-for="f in features" :key="f.name" class="feature-row">
          <span class="feature-name">{{ f.name }}</span>
          <span class="feature-desc">{{ f.desc }}</span>
        </div>
      </div>

      <el-divider content-position="left">技术栈</el-divider>
      <div class="tech-tags">
        <el-tag v-for="t in techs" :key="t" size="large" effect="plain">{{ t }}</el-tag>
      </div>

      <el-divider content-position="left">更新日志</el-divider>
      <el-timeline>
        <el-timeline-item v-for="log in logs" :key="log.version" :timestamp="log.date" placement="top">
          <h4 class="log-title">{{ log.version }}</h4>
          <ul class="log-list">
            <li v-for="item in log.items" :key="item">{{ item }}</li>
          </ul>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
const version = '1.0.0'

const features = [
  { name: '题库练习', desc: '面向计算机专业的专项练习与企业笔试/面试真题，选择题即时判分，主答题 AI 评分' },
  { name: 'AI 智能出题', desc: '按知识点与难度一键生成专属题目，自动收录错题' },
  { name: 'AI 模拟面试', desc: '上传简历后由 AI 以对话形式逐题追问，10 题答完统一评分并给出建议' },
  { name: '错题本', desc: '自动收录错题，支持反复巩固练习' },
  { name: '学习报告', desc: '按知识领域与知识点统计正确率，定位薄弱环节' },
  { name: '用户中心', desc: '个人信息、学习数据概览与练习历史' },
]

const techs = ['Java 21', 'Spring Boot', 'MyBatis-Plus', 'MySQL', 'Redis', 'Vue 3', 'Element Plus', 'Spring AI', '豆包大模型']

const logs = [
  {
    version: 'v1.0.0',
    date: '2026-09-26',
    items: [
      '统一页面布局：新增全局导航栏与页脚，全站页面可自由跳转',
      '新增公开落地页，未登录用户可浏览产品介绍、帮助与版本信息',
      '新增工作台：登录后展示学习数据概览、最近练习与薄弱知识点',
      '新增用户中心、设置、帮助与反馈、版本信息页面',
      '路由权限体系完善：公开页 / 登录页 / 管理员页分级控制',
      'AI 智能面试改为对话式逐题出题，全部答完后统一评分',
    ],
  },
  {
    version: 'v0.9.0',
    date: '2026-09-20',
    items: ['AI 模拟面试功能上线：简历解析、对话式出题、评分与总结'],
  },
  {
    version: 'v0.8.0',
    date: '2026-09-15',
    items: ['AI 智能出题上线，接入豆包大模型'],
  },
  {
    version: 'v0.5.0',
    date: '2026-09-10',
    items: ['题库与专项练习上线，支持选择题即时判分'],
  },
  {
    version: 'v0.1.0',
    date: '2026-09-01',
    items: ['项目初始化：Spring Boot + Vue3 基础框架、登录注册、JWT 鉴权'],
  },
]
</script>

<style scoped>
.version-page {
  max-width: 820px;
  margin: 0 auto;
  padding: 24px 20px 48px;
}

.panel {
  border-radius: 14px;
}

.version-head {
  display: flex;
  align-items: center;
  gap: 20px;
}

.version-badge {
  width: 76px;
  height: 76px;
  border-radius: 16px;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  flex-shrink: 0;
}

.version-info h2 {
  margin: 0 0 6px;
  color: #111827;
}

.version-info p {
  margin: 0;
  color: #6b7280;
}

.feature-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 10px 20px;
}

.feature-row {
  padding: 8px 0;
}

.feature-name {
  font-weight: 600;
  color: #374151;
  margin-right: 10px;
}

.feature-desc {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.log-title {
  margin: 0 0 6px;
  color: #1f2937;
}

.log-list {
  margin: 0;
  padding-left: 18px;
  color: #4b5563;
  line-height: 1.8;
}
</style>
