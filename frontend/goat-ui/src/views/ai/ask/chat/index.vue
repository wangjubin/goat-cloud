<template>
  <div class="chatbi-container">
    <!-- 左侧：数据源选择 -->
    <div class="chatbi-sidebar">
      <div class="sidebar-header">
        <h3>智能问数</h3>
      </div>
      <div class="sidebar-content">
        <el-form label-position="top" size="small">
          <el-form-item label="数据源">
            <el-select v-model="datasourceId" placeholder="选择数据源" clearable style="width: 100%">
              <el-option
                v-for="ds in datasources"
                :key="ds.datasourceId"
                :label="ds.datasourceName"
                :value="ds.datasourceId"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="sidebar-tips">
          <h4>使用提示</h4>
          <ul>
            <li>选择数据源后提问效果更佳</li>
            <li>支持自然语言查询数据</li>
            <li>可生成图表展示结果</li>
            <li>支持 SQL 确认和修正</li>
          </ul>
        </div>
        <div class="sidebar-history">
          <h4 @click="loadHistory" style="cursor:pointer">对话历史 <el-icon :size="12"><Refresh /></el-icon></h4>
          <div v-for="h in history" :key="h.sessionId" class="history-item" @click="viewHistory(h)">
            <span class="history-question">{{ h.question }}</span>
            <el-tag size="small" :type="h.status === 'COMPLETED' ? 'success' : h.status === 'FAILED' ? 'danger' : 'warning'">{{ h.status }}</el-tag>
          </div>
          <div v-if="history.length === 0" class="history-empty">暂无历史记录</div>
        </div>
      </div>
    </div>

    <!-- 右侧：对话区 -->
    <div class="chatbi-main">
      <div class="chatbi-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="empty-state">
          <el-icon :size="48"><ChatDotRound /></el-icon>
          <h3>智能问数助手</h3>
          <p>请输入自然语言问题，我将帮您生成 SQL 查询并可视化展示结果</p>
          <div class="quick-questions">
            <el-tag
              v-for="q in quickQuestions"
              :key="q"
              @click="askQuestion(q)"
              class="quick-tag"
              effect="plain"
            >{{ q }}</el-tag>
          </div>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx" :class="['chat-message', msg.role]">
          <div class="message-avatar">
            <el-avatar :size="32" :icon="msg.role === 'user' ? User : Monitor" />
          </div>
          <div class="message-content">
            <!-- 用户消息 -->
            <template v-if="msg.role === 'user'">
              <div class="message-text">{{ msg.content }}</div>
            </template>

            <!-- 助手消息 -->
            <template v-else>
              <!-- 意图识别结果 -->
              <div v-if="msg.intent" class="result-section">
                <el-tag type="info" size="small">意图: {{ msg.intent }}</el-tag>
              </div>

              <!-- SQL 展示 -->
              <div v-if="msg.sql" class="result-section sql-section">
                <div class="section-header">
                  <span>生成的 SQL</span>
                  <el-button size="small" text @click="copySql(msg.sql)">复制</el-button>
                </div>
                <pre class="sql-code"><code>{{ msg.sql }}</code></pre>
              </div>

              <!-- SQL 结果表格 -->
              <div v-if="msg.tableData && msg.tableData.length > 0" class="result-section">
                <div class="section-header">
                  <span>查询结果 ({{ msg.tableData.length }} 行)</span>
                </div>
                <el-table :data="msg.tableData.slice(0, 20)" size="small" border stripe max-height="300">
                  <el-table-column
                    v-for="col in msg.columns"
                    :key="col"
                    :prop="col"
                    :label="col"
                    show-overflow-tooltip
                  />
                </el-table>
              </div>

              <!-- ECharts 图表 -->
              <div v-if="msg.chartOption" class="result-section">
                <div class="section-header">
                  <span>数据可视化</span>
                </div>
                <div :ref="(el) => setChartRef(idx, el)" class="chart-container"></div>
              </div>

              <!-- 人工反馈 -->
              <div v-if="msg.feedbackOptions" class="result-section feedback-section">
                <div class="section-header"><span>请确认</span></div>
                <el-button-group>
                  <el-button
                    v-for="opt in msg.feedbackOptions"
                    :key="opt.value"
                    :type="opt.value === 'APPROVE' ? 'primary' : 'default'"
                    size="small"
                    @click="handleFeedback(msg.runId, opt.value)"
                  >{{ opt.label }}</el-button>
                </el-button-group>
              </div>

              <!-- 错误信息 -->
              <div v-if="msg.error" class="result-section error-section">
                <el-alert :title="msg.error" type="error" :closable="false" />
              </div>

              <!-- 加载中 -->
              <div v-if="msg.loading" class="result-section">
                <el-skeleton :rows="3" animated />
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="chatbi-input">
        <el-input
          v-model="inputText"
          placeholder="输入问题，如：查询最近7天的用户注册数"
          @keydown.enter="sendMessage"
          :disabled="loading"
          size="large"
        >
          <template #append>
            <el-button :icon="Promotion" @click="sendMessage" :loading="loading" />
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { User, Monitor, ChatDotRound, Promotion, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { chatBiStreamFetch, resumeChatBiSession } from '@/api/ai'
import { fetchAiList } from '@/api/ai'
import http from '@/api/http'
import * as echarts from 'echarts'

interface ChatMessage {
  role: 'user' | 'assistant'
  content?: string
  intent?: string
  sql?: string
  tableData?: Record<string, unknown>[]
  columns?: string[]
  chartOption?: Record<string, unknown>
  feedbackOptions?: Array<{ value: string; label: string }>
  runId?: string
  error?: string
  loading?: boolean
}

const inputText = ref('')
const loading = ref(false)
const messages = ref<ChatMessage[]>([])
const messagesRef = ref<HTMLElement>()
const datasourceId = ref<number>()
const datasources = ref<Array<{ datasourceId: number; datasourceName: string }>>([])
const chartRefs = ref<Map<number, HTMLElement>>(new Map())
const chartInstances = ref<echarts.ECharts[]>([])
const resizeObservers: ResizeObserver[] = []
let abortController: AbortController | null = null

const quickQuestions = [
  '查询用户总数',
  '最近7天的注册用户数',
  '各部门人数统计',
  '查询系统菜单数量',
]

interface HistoryItem {
  sessionId: number
  question: string
  status: string
  startedAt: string
}
const history = ref<HistoryItem[]>([])

onMounted(async () => {
  try {
    const res = await fetchAiList<Record<string, unknown>>('chatbi/datasources')
    datasources.value = (res || []) as any
  } catch {
    // 数据源加载失败不影响使用
  }
  loadHistory()
})

async function loadHistory() {
  try {
    const res = await http.get<any>('/ai/chatbi/history/sessions')
    history.value = (res || []).map((s: any) => ({
      sessionId: s.sessionId,
      question: s.contextJson ? tryExtractQuestion(s.contextJson) : '(未知)',
      status: s.status,
      startedAt: s.startedAt,
    }))
  } catch { /* ignore */ }
}

function tryExtractQuestion(ctx: string): string {
  try {
    const obj = JSON.parse(ctx)
    return obj.input?.question || obj.question || '(未知)'
  } catch { return '(未知)' }
}

function viewHistory(h: HistoryItem) {
  ElMessage.info('查看对话 #' + h.sessionId)
}

onBeforeUnmount(() => {
  abortController?.abort()
  resizeObservers.forEach(ro => ro.disconnect())
  resizeObservers.length = 0
  chartInstances.value.forEach(c => c.dispose())
  chartInstances.value = []
  chartRefs.value.clear()
})

function setChartRef(idx: number, el: any) {
  if (el) chartRefs.value.set(idx, el as HTMLElement)
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function askQuestion(q: string) {
  inputText.value = q
  sendMessage()
}

async function sendMessage() {
  const question = inputText.value.trim()
  if (!question || loading.value) return

  loading.value = true
  inputText.value = ''

  // 添加用户消息
  messages.value.push({ role: 'user', content: question })
  // 添加空的助手消息用于流式更新
  const assistantMsg: ChatMessage = { role: 'assistant', loading: true }
  messages.value.push(assistantMsg)
  scrollToBottom()

  try {
    abortController = new AbortController()
    await chatBiStreamFetch(
      {
        question,
        datasourceId: datasourceId.value,
      },
      (event, data) => handleStreamEvent(assistantMsg, event, data),
      (error) => {
        assistantMsg.loading = false
        assistantMsg.error = `连接失败: ${error}`
        loading.value = false
      },
      abortController.signal
    )
  } catch (e: any) {
    assistantMsg.loading = false
    assistantMsg.error = e.message || '请求失败'
  } finally {
    assistantMsg.loading = false
    loading.value = false
    scrollToBottom()
  }
}

function handleStreamEvent(msg: ChatMessage, event: string, data: Record<string, unknown>) {
  switch (event) {
    case 'node_start':
      msg.loading = true
      break

    case 'node_complete': {
      const nodeType = data.nodeType as string
      const output = data.output as Record<string, unknown> | undefined

      if (nodeType === 'INTENT_RECOGNITION' && output) {
        msg.intent = output.intent as string
      }

      if (nodeType === 'NL2SQL' && output) {
        msg.sql = output.generatedSql as string
      }

      if (nodeType === 'SQL_EXECUTION' && output) {
        msg.tableData = output.rows as Record<string, unknown>[]
        msg.columns = output.columns as string[]
      }

      if (nodeType === 'REPORT_GENERATION' && output) {
        msg.chartOption = output.echartsOption as Record<string, unknown>
        nextTick(() => renderChart(msg))
      }
      break
    }

    case 'interrupt': {
      const nodeCode = data.nodeCode as string
      if (nodeCode === 'human_feedback') {
        msg.feedbackOptions = [
          { value: 'APPROVE', label: '确认，继续' },
          { value: 'MODIFY_SQL', label: '修改SQL' },
          { value: 'REJECT', label: '重新生成' },
          { value: 'APPROVE_WITH_CHART', label: '确认并生成图表' },
        ]
      }
      msg.runId = data.runId as string
      break
    }

    case 'complete':
      msg.loading = false
      break

    case 'node_error':
      msg.loading = false
      msg.error = data.error as string
      break

    case 'error':
      msg.loading = false
      msg.error = data.message as string
      break
  }

  scrollToBottom()
}

async function handleFeedback(runId: string | undefined, action: string) {
  if (!runId) return

  const msg = messages.value[messages.value.length - 1]
  if (msg) {
    msg.feedbackOptions = undefined
  }

  if (action === 'APPROVE' || action === 'APPROVE_WITH_CHART') {
    try {
      const res = await resumeChatBiSession(runId, { action, approved: true })
      // 添加结果消息
      const resultMsg: ChatMessage = { role: 'assistant', content: '已确认' }
      if (action === 'APPROVE_WITH_CHART' && res) {
        // 后续流程会生成图表
      }
      messages.value.push(resultMsg)
    } catch (e: any) {
      ElMessage.error('操作失败: ' + e.message)
    }
  }
}

function renderChart(msg: ChatMessage) {
  const idx = messages.value.indexOf(msg)
  if (idx < 0) return

  nextTick(() => {
    const el = chartRefs.value.get(idx)
    if (!el || !msg.chartOption) return

    const chart = echarts.init(el)
    chartInstances.value.push(chart)
    chart.setOption(msg.chartOption as any)
    chart.resize()

    // 监听窗口变化
    const ro = new ResizeObserver(() => chart.resize())
    ro.observe(el)
  })
}

function copySql(sql: string) {
  navigator.clipboard.writeText(sql).then(() => {
    ElMessage.success('SQL 已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}
</script>

<style scoped>
.chatbi-container {
  display: flex;
  height: calc(100vh - 120px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.chatbi-sidebar {
  width: 240px;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.sidebar-content {
  padding: 16px;
  flex: 1;
}

.sidebar-tips {
  margin-top: 24px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.sidebar-tips h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #606266;
}

.sidebar-tips ul {
  margin: 0;
  padding-left: 16px;
  font-size: 12px;
  color: #909399;
  line-height: 1.8;
}

.sidebar-history {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.sidebar-history h4 {
  margin: 0 0 8px;
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 4px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  margin-bottom: 4px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #606266;
}

.history-item:hover {
  background: #f5f7fa;
}

.history-question {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140px;
}

.history-empty {
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
  padding: 12px;
}

.chatbi-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chatbi-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-state h3 {
  margin: 16px 0 8px;
  color: #606266;
}

.empty-state p {
  font-size: 14px;
  margin-bottom: 20px;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.quick-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.quick-tag:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 80%;
}

.chat-message.user .message-text {
  background: var(--el-color-primary);
  color: #fff;
  padding: 10px 16px;
  border-radius: 12px 12px 2px 12px;
  font-size: 14px;
  line-height: 1.6;
}

.chat-message.assistant .message-content {
  flex: 1;
}

.result-section {
  margin-bottom: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}

.sql-section {
  background: #1e1e1e;
  border-radius: 6px;
  overflow: hidden;
}

.sql-section .section-header {
  background: #2d2d2d;
  padding: 8px 12px;
  color: #ccc;
  margin-bottom: 0;
}

.sql-section .section-header .el-button {
  color: #909399;
}

.sql-code {
  padding: 12px;
  margin: 0;
  overflow-x: auto;
}

.sql-code code {
  color: #d4d4d4;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.chart-container {
  width: 100%;
  height: 350px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.feedback-section {
  padding: 12px;
  background: #f0f9eb;
  border-radius: 6px;
}

.error-section {
  padding: 12px;
}

.chatbi-input {
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
}
</style>
