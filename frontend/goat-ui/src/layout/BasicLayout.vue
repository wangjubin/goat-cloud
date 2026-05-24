<template>
  <el-container class="layout-container">
    <aside class="layout-aside">
      <div class="layout-logo">
        <span class="layout-logo-mark">T</span>
        <span class="layout-logo-title">Techen Cloud</span>
      </div>
      <el-scrollbar class="layout-menu-scroll">
        <el-menu
          :default-active="route.path"
          router
          unique-opened
          background-color="transparent"
          text-color="#bfcbd9"
          active-text-color="#ffffff"
          class="menu-panel"
        >
          <MenuNode v-for="item in visibleRoutes" :key="String(item.name)" :route="item" />
        </el-menu>
      </el-scrollbar>
    </aside>
    <el-container class="layout-container-view">
      <header class="layout-header">
        <div class="layout-header-left">
          <el-button text class="header-icon" title="折叠菜单">
            <el-icon><Fold /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="layout-header-right">
          <span class="user-name">{{ userStore.user?.nickname || 'Administrator' }}</span>
          <span class="role-name">{{ userStore.roleCodes.join(', ') || 'SYSTEM_ADMIN' }}</span>
          <el-button text class="header-icon" title="退出登录" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
          </el-button>
        </div>
      </header>
      <nav class="layout-tags">
        <RouterLink to="/dashboard" class="tag-item" :class="{ active: route.path === '/dashboard' }">Dashboard</RouterLink>
        <RouterLink v-if="route.path !== '/dashboard'" :to="route.path" class="tag-item active">
          {{ pageTitle }}
        </RouterLink>
      </nav>
      <main class="layout-main">
        <RouterView />
      </main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {RouterLink, RouterView, useRoute, useRouter} from 'vue-router'
import {Fold, SwitchButton} from '@element-plus/icons-vue'
import {usePermissionStore} from '@/stores/permission'
import {useUserStore} from '@/stores/user'
import {useAuthStore} from '@/stores/auth'
import MenuNode from './components/MenuNode.vue'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const userStore = useUserStore()
const authStore = useAuthStore()

const visibleRoutes = computed<any[]>(() => permissionStore.routes.filter((item) => item.meta?.visible !== false))
const pageTitle = computed(() => String(route.meta.title || 'Dashboard'))

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: var(--tc-bg);
}

.layout-aside {
  width: 220px;
  min-height: 100vh;
  background: var(--tc-menu-bg);
  color: var(--tc-menu-text);
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
  z-index: 12;
}

.layout-logo {
  height: 50px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  color: #fff;
  background: #002140;
  overflow: hidden;
}

.layout-logo-mark {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  display: grid;
  place-items: center;
  background: var(--tc-primary);
  font-weight: 700;
}

.layout-logo-title {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.layout-menu-scroll {
  height: calc(100vh - 50px);
}

.menu-panel {
  border: none;
  background: transparent;
}

.menu-panel :deep(.el-sub-menu__title),
.menu-panel :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  color: var(--tc-menu-text);
}

.menu-panel :deep(.el-sub-menu .el-menu) {
  background: var(--tc-menu-sub) !important;
}

.menu-panel :deep(.el-menu-item.is-active) {
  background: var(--tc-menu-active) !important;
  color: #fff !important;
}

.menu-panel :deep(.el-menu-item:hover),
.menu-panel :deep(.el-sub-menu__title:hover) {
  background: rgba(64, 158, 255, 0.18) !important;
}

.layout-container-view {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.layout-header {
  height: var(--tc-top-height);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 15px 0 0;
  background: #fff;
  border-bottom: 1px solid var(--tc-border);
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}

.layout-header-left,
.layout-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 50px;
  height: 50px;
  border-radius: 0;
  font-size: 18px;
}

.user-name {
  color: #303133;
  font-size: 14px;
}

.role-name {
  color: var(--tc-subtle);
  font-size: 12px;
}

.layout-tags {
  height: var(--tc-tags-height);
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 0 15px;
  background: #fff;
  border-bottom: 1px solid var(--tc-border);
}

.tag-item {
  height: 26px;
  line-height: 24px;
  padding: 0 15px;
  border: 1px solid var(--tc-border);
  border-radius: 2px;
  color: #606266;
  font-size: 12px;
  text-decoration: none;
  background: #fff;
}

.tag-item:hover {
  color: var(--tc-primary);
  border-color: #b3d8ff;
  background: var(--tc-primary-soft);
}

.tag-item.active {
  color: #fff;
  background: var(--tc-primary);
  border-color: var(--tc-primary);
}

.layout-main {
  flex: 1;
  overflow: auto;
}

@media (max-width: 960px) {
  .layout-container {
    flex-direction: column;
  }

  .layout-aside {
    width: 100%;
    min-height: auto;
  }

  .layout-menu-scroll {
    height: auto;
    max-height: 260px;
  }
}
</style>
