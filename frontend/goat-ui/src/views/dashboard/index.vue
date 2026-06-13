<template>
  <div class="dashboard">
    <!-- Welcome Banner -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <h1>欢迎回来，{{ userStore.user?.nickname || 'Admin' }}</h1>
        <p>Goat Cloud AI 平台 · 让智能触手可及</p>
      </div>
      <div class="welcome-decoration">
        <div class="deco-circle deco-1"></div>
        <div class="deco-circle deco-2"></div>
        <div class="deco-circle deco-3"></div>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="stats-grid">
      <div v-for="stat in statsCards" :key="stat.title" class="stat-card" :style="{ '--accent': stat.color }">
        <div class="stat-icon" :style="{ background: stat.gradient }">
          <el-icon :size="24"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-label">{{ stat.title }}</span>
          <strong class="stat-value">{{ stat.value }}</strong>
        </div>
        <div class="stat-trend" v-if="stat.trend">
          <el-icon :size="14"><component :is="stat.trend > 0 ? 'Top' : 'Bottom'" /></el-icon>
          <span>{{ Math.abs(stat.trend) }}%</span>
        </div>
      </div>
    </div>

    <!-- Content Grid -->
    <el-row :gutter="20" class="content-grid">
      <!-- Quick Actions -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="quick-actions-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">快捷操作</span>
            </div>
          </template>
          <div class="action-grid">
            <router-link v-for="action in quickActions" :key="action.path" :to="action.path" class="action-item">
              <div class="action-icon" :style="{ background: action.gradient }">
                <el-icon :size="20"><component :is="action.icon" /></el-icon>
              </div>
              <span class="action-label">{{ action.label }}</span>
            </router-link>
          </div>
        </el-card>
      </el-col>

      <!-- Today's Activity -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="activity-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">今日活动</span>
              <el-button text type="primary" size="small">查看全部</el-button>
            </div>
          </template>
          <div class="activity-stats">
            <div class="activity-item">
              <div class="activity-icon" style="background: var(--tc-info-soft); color: var(--tc-info);">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="activity-info">
                <span class="activity-value">{{ stats.todayConversations || 0 }}</span>
                <span class="activity-label">对话数</span>
              </div>
            </div>
            <div class="activity-item">
              <div class="activity-icon" style="background: var(--tc-success-soft); color: var(--tc-success);">
                <el-icon><ChatLineRound /></el-icon>
              </div>
              <div class="activity-info">
                <span class="activity-value">{{ stats.todayMessages || 0 }}</span>
                <span class="activity-label">消息数</span>
              </div>
            </div>
            <div class="activity-item">
              <div class="activity-icon" style="background: var(--tc-warning-soft); color: var(--tc-warning);">
                <el-icon><Timer /></el-icon>
              </div>
              <div class="activity-info">
                <span class="activity-value">2.3s</span>
                <span class="activity-label">平均响应</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Module Status -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="status-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">模块状态</span>
              <el-tag type="success" size="small" effect="dark">全部正常</el-tag>
            </div>
          </template>
          <div class="module-list">
            <div v-for="mod in modules" :key="mod.name" class="module-item">
              <div class="module-icon">
                <span class="module-dot" :class="mod.status"></span>
              </div>
              <span class="module-name">{{ mod.name }}</span>
              <span class="module-count">{{ mod.count }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 7-Day Trend -->
    <el-card shadow="never" class="trend-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">7 天趋势</span>
          <div class="trend-legend">
            <span class="legend-item"><span class="legend-dot" style="background: #6366f1;"></span> 对话</span>
            <span class="legend-item"><span class="legend-dot" style="background: #10b981;"></span> 消息</span>
          </div>
        </div>
      </template>
      <div class="trend-chart">
        <div v-for="day in dailyTrend" :key="day.date" class="trend-column">
          <div class="trend-bars">
            <div class="trend-bar bar-conversations" :style="{ height: getBarHeight(day.conversations, 'conv') + 'px' }"></div>
            <div class="trend-bar bar-messages" :style="{ height: getBarHeight(day.messages, 'msg') + 'px' }"></div>
          </div>
          <span class="trend-date">{{ formatDate(day.date) }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { http } from '@/api/client'
import {
  Cpu, ChatDotRound, Collection, Document, TrendCharts, Setting,
  ChatLineRound, Timer, Top, Bottom
} from '@element-plus/icons-vue'

const userStore = useUserStore()

interface DashboardStats {
  modelCount: number
  agentCount: number
  knowledgeBaseCount: number
  documentCount: number
  conversationCount: number
  messageCount: number
  todayConversations: number
  todayMessages: number
  dailyTrend: Array<{ date: string; conversations: number; messages: number }>
}

interface ModuleInfo {
  name: string
  status: string
  count: number
}

const stats = ref<Partial<DashboardStats>>({})
const modules = ref<ModuleInfo[]>([])

const statsCards = computed(() => [
  { title: 'AI 模型', value: stats.value.modelCount || 0, icon: Cpu, color: '#6366f1', gradient: 'linear-gradient(135deg, #6366f1, #8b5cf6)', trend: 12 },
  { title: '智能体', value: stats.value.agentCount || 0, icon: ChatDotRound, color: '#ec4899', gradient: 'linear-gradient(135deg, #ec4899, #f472b6)', trend: 8 },
  { title: '知识库', value: stats.value.knowledgeBaseCount || 0, icon: Collection, color: '#06b6d4', gradient: 'linear-gradient(135deg, #06b6d4, #22d3ee)', trend: 5 },
  { title: '文档数', value: stats.value.documentCount || 0, icon: Document, color: '#10b981', gradient: 'linear-gradient(135deg, #10b981, #34d399)', trend: 15 },
])

const quickActions = [
  { label: '新建对话', path: '/ai/chat', icon: ChatDotRound, gradient: 'linear-gradient(135deg, #6366f1, #8b5cf6)' },
  { label: 'AI 模型', path: '/ai/models', icon: Cpu, gradient: 'linear-gradient(135deg, #ec4899, #f472b6)' },
  { label: '知识库', path: '/ai/knowledge', icon: Collection, gradient: 'linear-gradient(135deg, #06b6d4, #22d3ee)' },
  { label: '智能体', path: '/ai/agents', icon: TrendCharts, gradient: 'linear-gradient(135deg, #10b981, #34d399)' },
  { label: '工作流', path: '/ai/workflow', icon: Setting, gradient: 'linear-gradient(135deg, #f59e0b, #fbbf24)' },
  { label: '文档管理', path: '/ai/documents', icon: Document, gradient: 'linear-gradient(135deg, #8b5cf6, #a78bfa)' },
]

const dailyTrend = computed(() => stats.value.dailyTrend || [])

function getBarHeight(value: number, type: string) {
  const allValues = dailyTrend.value.map(d => type === 'conv' ? d.conversations : d.messages)
  const max = Math.max(...allValues, 1)
  return Math.max(4, (value / max) * 140)
}

function formatDate(dateStr: string) {
  const date = new Date(dateStr)
  const days = ['日', '一', '二', '三', '四', '五', '六']
  return `周${days[date.getDay()]}`
}

async function loadStats() {
  try {
    const [statsResult, overviewResult] = await Promise.all([
      http.get<any, DashboardStats>('/ai/dashboard/stats'),
      http.get<any, { modules: ModuleInfo[] }>('/ai/dashboard/overview'),
    ])
    stats.value = statsResult || {}
    modules.value = overviewResult?.modules || []
  } catch (error) {
    console.error('Failed to load dashboard stats:', error)
    stats.value = { modelCount: 0, agentCount: 0, knowledgeBaseCount: 0, documentCount: 0, todayConversations: 0, todayMessages: 0, dailyTrend: [] }
    modules.value = []
  }
}

onMounted(loadStats)
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

/* ── Welcome Banner ── */
.welcome-banner {
  background: var(--tc-primary-gradient);
  border-radius: var(--tc-radius-lg);
  padding: 32px 40px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
  color: #fff;
}

.welcome-content {
  position: relative;
  z-index: 2;
}

.welcome-content h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
}

.welcome-content p {
  margin: 0;
  opacity: 0.85;
  font-size: 14px;
}

.welcome-decoration {
  position: absolute;
  right: -20px;
  top: -20px;
  z-index: 1;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.deco-1 { width: 120px; height: 120px; right: 0; top: 0; }
.deco-2 { width: 80px; height: 80px; right: 60px; top: 40px; background: rgba(255, 255, 255, 0.08); }
.deco-3 { width: 60px; height: 60px; right: 20px; top: 80px; background: rgba(255, 255, 255, 0.06); }

/* ── Stats Grid ── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--tc-surface);
  border: 1px solid var(--tc-border-light);
  border-radius: var(--tc-radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s ease;
  box-shadow: var(--tc-shadow-xs);
}

.stat-card:hover {
  box-shadow: var(--tc-shadow-md);
  transform: translateY(-2px);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-label {
  display: block;
  color: var(--tc-text-secondary);
  font-size: 13px;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--tc-text);
  line-height: 1;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  color: var(--tc-success);
  font-size: 12px;
  font-weight: 600;
}

.stat-trend :deep(.el-icon) {
  color: var(--tc-success);
}

/* ── Content Grid ── */
.content-grid {
  margin-bottom: 20px;
}

.content-grid :deep(.el-card__header) {
  padding: 16px 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--tc-text);
}

/* ── Quick Actions ── */
.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border-radius: var(--tc-radius-md);
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
}

.action-item:hover {
  background: var(--tc-border-light);
  transform: translateY(-2px);
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.action-label {
  font-size: 12px;
  color: var(--tc-text-secondary);
  font-weight: 500;
}

/* ── Activity ── */
.activity-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.activity-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.activity-info {
  display: flex;
  flex-direction: column;
}

.activity-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--tc-text);
  line-height: 1.2;
}

.activity-label {
  font-size: 12px;
  color: var(--tc-text-secondary);
}

/* ── Module Status ── */
.module-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.module-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--tc-border-light);
}

.module-item:last-child {
  border-bottom: none;
}

.module-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.module-dot.active {
  background: var(--tc-success);
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.4);
}

.module-name {
  flex: 1;
  font-size: 13px;
  color: var(--tc-text);
}

.module-count {
  font-size: 13px;
  font-weight: 600;
  color: var(--tc-text-secondary);
}

/* ── Trend Chart ── */
.trend-card :deep(.el-card__header) {
  border-bottom: 1px solid var(--tc-border-light);
}

.trend-legend {
  display: flex;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--tc-text-secondary);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.trend-chart {
  display: flex;
  justify-content: space-around;
  align-items: flex-end;
  height: 200px;
  padding: 20px 0;
}

.trend-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 140px;
}

.trend-bar {
  width: 16px;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
}

.bar-conversations {
  background: linear-gradient(180deg, #6366f1, #818cf8);
}

.bar-messages {
  background: linear-gradient(180deg, #10b981, #34d399);
}

.trend-date {
  font-size: 12px;
  color: var(--tc-text-secondary);
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .welcome-banner {
    padding: 24px;
  }

  .welcome-content h1 {
    font-size: 20px;
  }

  .action-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
