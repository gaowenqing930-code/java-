import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { public: true, title: '注册' },
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'landing',
        component: () => import('@/views/LandingView.vue'),
        meta: { public: true, title: '首页' },
      },
      {
        path: 'home',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '工作台' },
      },
      {
        path: 'questions',
        name: 'questions',
        component: () => import('@/views/QuestionHomeView.vue'),
        meta: { title: '题库' },
      },
      {
        path: 'questions/practice',
        name: 'question-practice',
        component: () => import('@/views/PracticeView.vue'),
        meta: { title: '做题' },
      },
      {
        path: 'exam/:paperId',
        name: 'exam-paper',
        component: () => import('@/views/ExamPaperView.vue'),
        meta: { title: '真题套卷' },
      },
      {
        path: 'ai/generate',
        name: 'ai-generate',
        component: () => import('@/views/AiGenerateView.vue'),
        meta: { title: 'AI 出题' },
      },
      {
        path: 'interview',
        name: 'interview',
        component: () => import('@/views/InterviewView.vue'),
        meta: { title: 'AI 面试' },
      },
      {
        path: 'interview/history',
        name: 'interview-history',
        component: () => import('@/views/InterviewHistoryView.vue'),
        meta: { title: 'AI 面试记录' },
      },
      {
        path: 'wrong',
        name: 'wrong',
        component: () => import('@/views/WrongQuestionView.vue'),
        meta: { title: '错题本' },
      },
      {
        path: 'report',
        name: 'report',
        component: () => import('@/views/StudyReportView.vue'),
        meta: { title: '学习报告' },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/UserCenterView.vue'),
        meta: { title: '用户中心' },
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('@/views/SettingsView.vue'),
        meta: { title: '设置' },
      },
      {
        path: 'help',
        name: 'help',
        component: () => import('@/views/HelpView.vue'),
        meta: { public: true, title: '帮助与反馈' },
      },
      {
        path: 'version',
        name: 'version',
        component: () => import('@/views/VersionView.vue'),
        meta: { public: true, title: '版本信息' },
      },
      {
        path: 'admin',
        name: 'admin',
        component: () => import('@/views/AdminView.vue'),
        meta: { title: '后台管理', admin: true },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：
// - 公开路由（首页落地页/帮助/版本/登录注册）无需登录
// - 其余路由未登录 → 跳登录页并携带 redirect 回跳
// - 管理员路由非管理员 → 回工作台
// - 已登录访问登录/注册页 → 回工作台
router.beforeEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} - AI智能面试与学习陪练平台` : 'AI智能面试与学习陪练平台'

  const token = localStorage.getItem('token')

  // 管理员专属路由校验
  if (to.meta.admin) {
    if (!token) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    let role = ''
    try {
      role = (JSON.parse(localStorage.getItem('user') || '{}') as { role?: string }).role || ''
    } catch {
      role = ''
    }
    if (role !== 'ADMIN') {
      return { path: '/home' }
    }
    return true
  }

  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.public && token && (to.path === '/login' || to.path === '/register')) {
    return { path: '/home' }
  }
  return true
})

export default router
