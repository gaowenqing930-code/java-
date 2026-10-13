<template>
  <div class="app-layout">
    <header class="app-header">
      <div class="header-inner">
        <router-link :to="authStore.isLoggedIn ? '/home' : '/'" class="logo">
          <span class="logo-mark">AI</span>
          <span class="logo-text">智能面试陪练</span>
        </router-link>

        <nav class="main-nav">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            :class="[
              'nav-item',
              { active: isActive(item), 'nav-admin': item.admin, secondary: authStore.isAdmin && !item.admin },
            ]"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="header-right">
          <template v-if="authStore.isLoggedIn">
            <el-dropdown trigger="click" @command="handleCommand">
              <span class="user-entry">
                <span class="user-avatar">{{ initial }}</span>
                <span class="uname">{{ authStore.user?.username }}</span>
                <el-icon class="arrow"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">用户中心</el-dropdown-item>
                  <el-dropdown-item command="settings">设置</el-dropdown-item>
                  <el-dropdown-item divided command="help">帮助与反馈</el-dropdown-item>
                  <el-dropdown-item command="version">版本信息</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login" class="login-link">登录</router-link>
            <router-link to="/register"><span class="register-btn">免费注册</span></router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="app-main">
      <router-view />
    </main>

    <footer class="app-footer">
      <div class="footer-inner">
        <div class="footer-links">
          <router-link to="/help">帮助与反馈</router-link>
          <span class="dot">·</span>
          <router-link to="/version">版本信息</router-link>
        </div>
        <div class="copyright">© 2026 AI 智能面试与学习陪练平台</div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const initial = computed(() => (authStore.user?.username?.[0] || 'U').toUpperCase())

interface NavItem {
  path: string
  label: string
  /** 管理入口：管理员身份下排首位并高亮 */
  admin?: boolean
  /** 仅登录用户可见 */
  loginOnly?: boolean
}

/** 学习功能导航（管理员可见但降级为次要） */
const learnNav: NavItem[] = [
  { path: '/home', label: '工作台', loginOnly: true },
  { path: '/questions', label: '题库' },
  { path: '/ai/generate', label: 'AI 出题' },
  { path: '/interview', label: 'AI 面试' },
  { path: '/wrong', label: '错题本' },
  { path: '/report', label: '学习报告' },
]

/** 导航顺序：管理员身份下「后台管理」排第一并高亮，学习功能保持可见但降级为次要 */
const navItems = computed<NavItem[]>(() => {
  const visible = learnNav.filter((n) => !n.loginOnly || authStore.isLoggedIn)
  if (!authStore.isAdmin) return visible
  return [{ path: '/admin', label: '后台管理', admin: true }, ...visible]
})

/** 当前路由是否命中某项：精确匹配或子路径前缀 */
function isActive(item: NavItem): boolean {
  if (route.path === item.path) return true
  if (item.path !== '/') return route.path.startsWith(item.path + '/')
  return false
}

function handleCommand(command: string) {
  if (command === 'logout') {
    authStore.logout()
    router.push('/')
    return
  }
  router.push(`/${command}`)
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--paper-warm);
}

/* —— 顶部导航：白底细黑边 —— */
.app-header {
  height: 64px;
  background: var(--paper);
  border-bottom: 1px solid var(--ink);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  height: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}

/* —— Logo：黑底白字方块 + 黑色粗体 —— */
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}
.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  background: var(--ink);
  color: var(--paper);
  font-family: var(--mono);
  font-weight: 700;
  font-size: 16px;
  letter-spacing: 0.02em;
}
.logo-text {
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.04em;
  white-space: nowrap;
}

/* —— 导航：黑色文字 + 黑色下划线 active —— */
.main-nav {
  flex: 1;
  display: flex;
  align-items: stretch;
  gap: 6px;
  min-width: 0;
  height: 100%;
}
.nav-item {
  display: inline-flex;
  align-items: center;
  padding: 0 14px;
  font-size: 15px;
  font-weight: 400;
  color: var(--gray-500);
  text-decoration: none;
  position: relative;
  transition: color 0.2s ease;
  white-space: nowrap;
}
.nav-item::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 0;
  height: 3px;
  background: var(--ink);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.25s ease;
}
.nav-item:hover {
  color: var(--ink);
}
.nav-item.active {
  color: var(--ink);
  font-weight: 600;
}
.nav-item.active::after {
  transform: scaleX(1);
}

/* 管理员：后台管理黑色高亮 */
.nav-item.nav-admin {
  color: var(--ink);
  font-weight: 700;
}
.nav-item.nav-admin.active::after {
  background: var(--ink);
}
/* 管理员身份下：学习功能降级为次要 */
.nav-item.secondary {
  color: var(--gray-400);
  font-weight: 400;
}

/* —— 右侧 —— */
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.login-link {
  color: var(--ink);
  font-size: 15px;
  text-decoration: none;
  padding: 8px 2px;
  position: relative;
  transition: opacity 0.2s;
}
.login-link::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: var(--ink);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.25s ease;
}
.login-link:hover::after {
  transform: scaleX(1);
}

.register-btn {
  display: inline-block;
  background: var(--ink);
  color: var(--paper);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.05em;
  padding: 8px 18px;
  border: 1px solid var(--ink);
  cursor: pointer;
  transition: all 0.25s ease;
}
.register-btn:hover {
  background: var(--paper);
  color: var(--ink);
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 8px;
}
.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  background: var(--ink);
  color: var(--paper);
  font-family: var(--mono);
  font-weight: 700;
  font-size: 13px;
}
.uname {
  color: var(--ink);
  font-size: 14px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.arrow {
  color: var(--gray-400);
}

/* —— 主体 —— */
.app-main {
  flex: 1;
  padding: 0;
  background: var(--paper-warm);
}

/* —— 页脚：黑白 —— */
.app-footer {
  background: var(--paper);
  border-top: 1px solid var(--ink);
  padding: 18px 0;
  height: auto;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--gray-400);
  flex-wrap: wrap;
  gap: 8px;
}
.footer-links a {
  color: var(--gray-500);
  text-decoration: none;
  margin: 0 6px;
  transition: color 0.2s;
}
.footer-links a:hover {
  color: var(--ink);
}
.dot {
  color: var(--gray-400);
}
</style>
