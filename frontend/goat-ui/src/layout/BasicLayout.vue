<template>
  <el-container class="layout-container">
    <aside class="layout-aside" :class="{ collapsed: sidebarCollapsed }">
      <div class="layout-logo">
        <div class="logo-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="8" fill="url(#logo-gradient)" />
            <path d="M8 10L14 7L20 10V18L14 21L8 18V10Z" stroke="white" stroke-width="1.5" fill="none"/>
            <circle cx="14" cy="14" r="3" fill="white" fill-opacity="0.9"/>
            <defs>
              <linearGradient id="logo-gradient" x1="0" y1="0" x2="28" y2="28">
                <stop stop-color="#6366f1"/>
                <stop offset="1" stop-color="#8b5cf6"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <transition name="fade">
          <span v-if="!sidebarCollapsed" class="logo-text">Goat Cloud</span>
        </transition>
      </div>

      <el-scrollbar class="layout-menu-scroll">
        <el-menu
          :default-active="route.path"
          router
          unique-opened
          background-color="transparent"
          text-color="var(--tc-menu-text)"
          active-text-color="var(--tc-menu-text-active)"
          class="menu-panel"
        >
          <MenuNode v-for="item in visibleRoutes" :key="String(item.name)" :route="item" />
        </el-menu>
      </el-scrollbar>

      <div class="layout-sidebar-footer">
        <div class="sidebar-version" v-if="!sidebarCollapsed">
          <span class="version-dot"></span>
          <span>v1.0.0</span>
        </div>
      </div>
    </aside>

    <el-container class="layout-container-view">
      <header class="layout-header">
        <div class="header-left">
          <el-button text class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
            <el-icon :size="20">
              <component :is="sidebarCollapsed ? 'Expand' : 'Fold'" />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.path !== '/dashboard'">{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-button text class="header-action" title="通知">
            <el-badge :value="3" :max="99">
              <el-icon :size="20"><Bell /></el-icon>
            </el-badge>
          </el-button>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-avatar-wrapper">
              <div class="user-avatar">
                {{ userInitial }}
              </div>
              <div class="user-info">
                <span class="user-name">{{ userStore.user?.nickname || 'Admin' }}</span>
                <span class="user-role">{{ userStore.roleCodes[0] || '管理员' }}</span>
              </div>
              <el-icon class="avatar-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  系统设置
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="layout-main">
        <RouterView v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </RouterView>
      </main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { Fold, Expand, Bell, User, Setting, SwitchButton, ArrowDown } from '@element-plus/icons-vue'
import { usePermissionStore } from '@/stores/permission'
import { useUserStore } from '@/stores/user'
import { useAuthStore } from '@/stores/auth'
import MenuNode from './components/MenuNode.vue'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const userStore = useUserStore()
const authStore = useAuthStore()

const sidebarCollapsed = ref(false)

const visibleRoutes = computed<any[]>(() => permissionStore.routes.filter((item) => item.meta?.visible !== false))
const pageTitle = computed(() => String(route.meta.title || 'Dashboard'))
const userInitial = computed(() => {
  const name = userStore.user?.nickname || 'Admin'
  return name.charAt(0).toUpperCase()
})

async function handleCommand(command: string) {
  if (command === 'logout') {
    await authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: var(--tc-bg);
}

/* ── Sidebar ── */
.layout-aside {
  width: 240px;
  min-height: 100vh;
  background: var(--tc-menu-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 100;
}

.layout-aside.collapsed {
  width: 64px;
}

.layout-logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.logo-icon {
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
  letter-spacing: -0.02em;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.layout-menu-scroll {
  flex: 1;
  overflow: hidden;
}

.menu-panel {
  border: none !important;
  background: transparent !important;
  padding: 8px;
}

.menu-panel :deep(.el-sub-menu__title),
.menu-panel :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  border-radius: 8px;
  margin: 2px 0;
  color: var(--tc-menu-text) !important;
  transition: all 0.2s ease;
}

.menu-panel :deep(.el-sub-menu .el-menu) {
  background: transparent !important;
}

.menu-panel :deep(.el-menu-item.is-active) {
  background: var(--tc-primary) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.menu-panel :deep(.el-menu-item:hover),
.menu-panel :deep(.el-sub-menu__title:hover) {
  background: var(--tc-menu-hover) !important;
  color: #fff !important;
}

.layout-sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.sidebar-version {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.3);
  font-size: 11px;
}

.version-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
}

/* ── Main Container ── */
.layout-container-view {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* ── Header ── */
.layout-header {
  height: var(--tc-top-height);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: var(--tc-surface);
  border-bottom: 1px solid var(--tc-border-light);
  box-shadow: var(--tc-shadow-xs);
  z-index: 50;
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.collapse-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  color: var(--tc-text-secondary);
  transition: all 0.2s ease;
}

.collapse-btn:hover {
  background: var(--tc-border-light);
  color: var(--tc-text);
}

.user-avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: var(--tc-radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-avatar-wrapper:hover {
  background: var(--tc-border-light);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--tc-primary-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--tc-text);
  line-height: 1.2;
}

.user-role {
  font-size: 11px;
  color: var(--tc-text-secondary);
  line-height: 1.2;
}

.avatar-arrow {
  color: var(--tc-subtle);
  font-size: 12px;
}

.header-action {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  color: var(--tc-text-secondary);
}

.header-action:hover {
  background: var(--tc-border-light);
  color: var(--tc-text);
}

/* ── Main Content ── */
.layout-main {
  flex: 1;
  overflow: auto;
  padding: 0;
}

/* ── Page Transitions ── */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

@media (max-width: 960px) {
  .layout-container {
    flex-direction: column;
  }

  .layout-aside {
    width: 100% !important;
    min-height: auto;
  }

  .layout-menu-scroll {
    height: auto;
    max-height: 260px;
  }

  .user-info {
    display: none;
  }
}
</style>
