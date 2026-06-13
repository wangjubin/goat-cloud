<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view">
      <div class="toolbar">
        <div>
          <h3>工作台</h3>
          <p>Goat Cloud AI 平台数据概览</p>
        </div>
        <el-button @click="loadStats" :loading="loading">刷新</el-button>
      </div>

      <!-- 核心指标卡片 -->
      <el-row :gutter="16" class="metric-row">
        <el-col :xs="12" :sm="6" v-for="card in metricCards" :key="card.title">
          <el-card shadow="never" class="metric-card">
            <div class="metric-icon" :style="{ background: card.color }">
              <el-icon :size="24"><component :is="card.icon" /></el-icon>
            </div>
            <div class="metric-info">
              <span class="metric-label">{{ card.title }}</span>
              <strong class="metric-value">{{ card.value }}</strong>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 今日数据 + 模块状态 -->
      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :xs="24" :lg="14">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>今日数据</span>
              </div>
            </template>
            <el-row :gutter="16">
              <el-col :span="12">
                <div class="today-stat">
                  <span class="today-label">今日对话</span>
                  <strong class="today-value">{{ stats.todayConversations || 0 }}</strong>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="today-stat">
                  <span class="today-label">今日消息</span>
                  <strong class="today-value">{{ stats.todayMessages || 0 }}</strong>
                </div>
              </el-col>
            </el-row>
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="10">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>模块状态</span>
              </div>
            </template>
            <div class="module-list">
              <div v-for="module in modules" :key="module.name" class="module-item">
                <span class="module-name">{{ module.name }}</span>
                <el-tag :type="module.status === 'active' ? 'success' : 'info'" size="small">
                  {{ module.status === 'active' ? '运行中' : '未启用' }}
                </el-tag>
                <span class="module-count">{{ module.count }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 最近 7 天趋势 -->
      <el-card shadow="never" style="margin-top: 16px">
        <template #header>
          <div class="card-header">
            <span>最近 7 天趋势</span>
          </div>
        </template>
        <div class="trend-chart">
          <div v-for="day in dailyTrend" :key="day.date" class="trend-bar">
            <div class="trend-bar-fill" :style="{ height: getBarHeight(day.conversations) + 'px' }"></div>
            <div class="trend-bar-label">{{ formatDate(day.date) }}</div>
            <div class="trend-bar-value">{{ day.conversations }}</div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {Cpu, ChatDotRound, Collection, Document, TrendCharts, SetUp} from '@element-plus/icons-vue'
import {http} from '@/api/client'

interface DashboardStats {
  modelCount: number
  agentCount: number
  knowledgeBaseCount: number
  documentCount: number
  conversationCount: number
  messageCount: number
  todayConversations: number
  todayMessages: number
  dailyTrend: Array<{date: string; conversations: number; messages: number}>
}

interface ModuleInfo {
  name: string
  status: string
  count: number
}

const loading = ref(false)
const stats = ref<Partial<DashboardStats>>({})
const modules = ref<ModuleInfo[]>([])

const metricCards = computed(() => [
  { title: 'AI 模型', value: stats.value.modelCount || 0, icon: Cpu, color: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { title: '智能体', value: stats.value.agentCount || 0, icon: ChatDotRound, color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { title: '知识库', value: stats.value.knowledgeBaseCount || 0, icon: Collection, color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { title: '文档数', value: stats.value.documentCount || 0, icon: Document, color: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' },
])

const dailyTrend = computed(() => stats.value.dailyTrend || [])

function getBarHeight(conversations: number) {
  const max = Math.max(...dailyTrend.value.map(d => d.conversations), 1)
  return Math.max(20, (conversations / max) * 120)
}

function formatDate(dateStr: string) {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

async function loadStats() {
  loading.value = true
  try {
    const [statsResult, overviewResult] = await Promise.all([
      http.get<any, DashboardStats>('/ai/dashboard/stats'),
      http.get<any, { modules: ModuleInfo[] }>('/ai/dashboard/overview'),
    ])
    stats.value = statsResult || {}
    modules.value = overviewResult?.modules || []
  } catch (error) {
    console.error('Failed to load dashboard stats:', error)
    // 使用默认数据
    stats.value = {
      modelCount: 0,
      agentCount: 0,
      knowledgeBaseCount: 0,
      documentCount: 0,
      conversationCount: 0,
      messageCount: 0,
      todayConversations: 0,
      todayMessages: 0,
      dailyTrend: [],
    }
    modules.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar h3 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.toolbar p {
  margin: 4px 0 0;
  color: #909399;
  font-size: 13px;
}

.metric-row {
  margin-bottom: 0;
}

.metric-card {
  border-radius: 4px;
}

.metric-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.metric-info {
  display: flex;
  flex-direction: column;
}

.metric-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 4px;
}

.metric-value {
  font-size: 24px;
  color: #303133;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.today-stat {
  text-align: center;
  padding: 20px;
  background: #f7f9fc;
  border-radius: 8px;
}

.today-label {
  display: block;
  color: #909399;
  font-size: 13px;
  margin-bottom: 8px;
}

.today-value {
  font-size: 32px;
  color: #303133;
  font-weight: 600;
}

.module-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.module-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.module-item:last-child {
  border-bottom: none;
}

.module-name {
  color: #303133;
  font-size: 14px;
}

.module-count {
  color: #909399;
  font-size: 14px;
  font-weight: 500;
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 180px;
  padding: 20px 0;
}

.trend-bar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.trend-bar-fill {
  width: 32px;
  background: linear-gradient(180deg, #409eff 0%, #79bbff 100%);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
}

.trend-bar-label {
  color: #909399;
  font-size: 12px;
}

.trend-bar-value {
  color: #303133;
  font-size: 12px;
  font-weight: 500;
}
</style>
