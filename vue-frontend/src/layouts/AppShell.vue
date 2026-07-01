<template>
  <div class="shell">
    <!-- 彩色发光背景 -->
    <div class="blob-bg">
      <div class="blob blob-blue"></div>
      <div class="blob blob-purple"></div>
      <div class="blob blob-pink"></div>
      <div class="blob blob-orange"></div>
    </div>

    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="brand">
        <div class="brand__logo"><el-icon><Tickets /></el-icon></div>
        <div class="brand__text">
          <div class="brand__name">智工单</div>
          <div class="brand__sub">SmartTicket</div>
        </div>
      </div>

      <nav class="nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav__item"
          active-class="nav__item--active"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="user-card">
        <div class="user-card__avatar"></div>
        <div class="user-card__info">
          <div class="user-card__name">{{ user.name }}</div>
          <div class="user-card__role">{{ roleLabel }}</div>
        </div>
      </div>
    </aside>

    <!-- 主区 -->
    <main class="main">
      <header class="topbar">
        <div class="topbar__title">
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="topbar__right">
          <div class="topbar__search">
            <el-icon><Search /></el-icon>
            <span>搜索工单号 / 关键词</span>
          </div>
          <div class="topbar__bell"><el-icon><Bell /></el-icon></div>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="topbar__avatar"></div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <section class="content page-enter">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logout } from '@/api/auth'

const route = useRoute()
const router = useRouter()

async function onCommand(cmd) {
  if (cmd === 'logout') {
    try {
      await logout()
    } catch (e) {
      // 即便后端登出失败，前端也清理并跳登录
    }
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('realName')
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

// Phase 1 由登录写入；此处兜底
const role = localStorage.getItem('role') || 'EMPLOYEE'
const user = { name: localStorage.getItem('realName') || '演示用户' }

const NAV = {
  EMPLOYEE: [
    { path: '/app/submit', label: '提交工单', icon: 'Plus' },
    { path: '/app/tickets', label: '我的工单', icon: 'List' }
  ],
  ENGINEER: [
    { path: '/app/todo', label: '待办工单', icon: 'Files' },
    { path: '/app/home', label: '我的绩效', icon: 'TrendCharts' }
  ],
  ADMIN: [
    { path: '/app/home', label: '工单看板', icon: 'DataBoard' },
    { path: '/app/dispatch', label: '派单管理', icon: 'Share' },
    { path: '/app/home', label: '工程师管理', icon: 'User' },
    { path: '/app/home', label: '用户与角色', icon: 'Lock' },
    { path: '/app/home', label: '审计日志', icon: 'Document' }
  ]
}
const ROLE_LABEL = { EMPLOYEE: '报修人', ENGINEER: '工程师', ADMIN: '管理员' }

const navItems = computed(() => NAV[role] || NAV.EMPLOYEE)
const roleLabel = computed(() => ROLE_LABEL[role] || '报修人')
const pageTitle = computed(() => route.meta.title || 'SmartTicket 智工单')
</script>

<style scoped>
.shell {
  position: relative;
  display: flex;
  min-height: 100vh;
}
/* 侧边栏 */
.sidebar {
  position: relative;
  z-index: 1;
  width: var(--sidebar-w);
  display: flex;
  flex-direction: column;
  padding: 26px 16px 20px;
  background: var(--glass-sidebar-bg);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-right: 1px solid rgba(255, 255, 255, 0.6);
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 8px 18px;
}
.brand__logo {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #4285ff, #8c4ddc);
}
.brand__name {
  font-weight: 700;
  font-size: 16px;
}
.brand__sub {
  font-size: 10px;
  color: var(--apple-darkgray);
}
.nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}
.nav__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border-radius: 12px;
  color: #525254;
  text-decoration: none;
  font-size: 14px;
}
.nav__item--active {
  background: rgba(0, 113, 227, 0.12);
  color: var(--apple-blue);
  font-weight: 500;
}
.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.5);
}
.user-card__avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #66b3ff, #4d80f0);
}
.user-card__name {
  font-size: 13px;
  font-weight: 500;
}
.user-card__role {
  font-size: 11px;
  color: var(--apple-darkgray);
}
/* 主区 */
.main {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.topbar {
  height: var(--topbar-h);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--gap-content);
}
.topbar__title h1 {
  font-size: 22px;
  font-weight: 700;
}
.topbar__right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.topbar__search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 14px;
  border-radius: 20px;
  background: var(--glass-control-bg);
  border: 1px solid rgba(255, 255, 255, 0.8);
  color: #999;
  font-size: 13px;
}
.topbar__bell {
  width: 40px;
  height: 40px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--glass-control-bg);
  border: 1px solid rgba(255, 255, 255, 0.8);
}
.topbar__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #66b3ff, #4d80f0);
}
.content {
  flex: 1;
  padding: 4px var(--gap-content) var(--gap-content);
}
</style>
