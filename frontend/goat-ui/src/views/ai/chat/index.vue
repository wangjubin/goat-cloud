<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view ai-chat-page">
      <el-row :gutter="12">
        <!-- 对话历史侧边栏 -->
        <el-col :xs="24" :lg="6">
          <el-card shadow="never" class="chat-side-card">
            <div class="table-toolbar">
              <div class="table-toolbar__title">对话历史</div>
              <el-button text type="primary" @click="startNewChat">新对话</el-button>
            </div>

            <div class="conversation-list" v-loading="loadingConversations">
              <el-empty v-if="conversations.length === 0" description="暂无对话记录" :image-size="60" />
              <div
                v-for="conv in conversations"
                :key="conv.conversationId"
                class="conversation-item"
                :class="{ active: currentConversationId === conv.conversationId }"
                @click="loadConversation(conv.conversationId)"
              >
                <div class="conv-title">{{ conv.title || '新对话' }}</div>
                <div class="conv-time">{{ formatTime(conv.createTime) }}</div>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 配置面板 -->
        <el-col :xs="24" :lg="5">
          <el-card shadow="never" class="chat-side-card">
            <div class="table-toolbar">
              <div class="table-toolbar__title">助手配置</div>
            </div>
            <el-form label-position="top">
              <el-form-item label="模型">
                <el-select v-model="form.modelId" placeholder="请选择模型" clearable>
                  <el-option v-for="item in modelOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="知识库">
                <el-select v-model="form.knowledgeId" placeholder="请选择知识库" clearable>
                  <el-option
                    v-for="item in knowledgeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <!-- 聊天主区域 -->
        <el-col :xs="24" :lg="13">
          <el-card shadow="never" class="chat-main-card" v-loading="loading">
            <div class="table-toolbar">
              <div class="table-toolbar__title">AI 助手聊天</div>
              <el-tag effect="plain">/api/ai/chat</el-tag>
            </div>

            <el-alert v-if="errorMessage" class="chat-error" type="error" :title="errorMessage" show-icon />

            <div class="message-list">
              <el-empty v-if="messages.length === 0" description="暂无会话，输入问题开始体验 AI 助手。" />
              <div v-for="(message, index) in messages" :key="index" class="message-item" :class="message.role">
                <div class="message-role">{{ message.role === 'user' ? '用户' : 'AI 助手' }}</div>
                <div v-if="message.role === 'user'" class="message-content">{{ message.content }}</div>
                <div v-else class="message-content markdown-body" v-html="renderMarkdown(message.content)"></div>
              </div>
            </div>

            <div class="chat-input">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="4"
                placeholder="请输入你想咨询的问题，例如：帮我总结知识库中的使用手册。"
                @keydown.ctrl.enter.prevent="sendMessage"
              />
              <div class="chat-input__actions">
                <span>Ctrl + Enter 发送</span>
                <el-button type="primary" :loading="loading" @click="sendMessage">发送</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import {fetchAiList, sendAiChat, type AiChatMessage} from '@/api/ai'
import {fetchConversations, fetchConversationHistory, type AiConversation} from '@/api/ai'
import { renderMarkdown } from '@/utils/markdown'

interface OptionItem {
  label: string
  value: string
}

const form = reactive({
  modelId: '',
  knowledgeId: '',
  content: '',
})

const messages = ref<AiChatMessage[]>([])
const modelOptions = ref<OptionItem[]>([])
const knowledgeOptions = ref<OptionItem[]>([])
const loading = ref(false)
const errorMessage = ref('')

// 对话历史
const conversations = ref<AiConversation[]>([])
const currentConversationId = ref<string>('')
const loadingConversations = ref(false)

async function loadOptions() {
  try {
    const [models, knowledge] = await Promise.all([
      fetchAiList<Record<string, unknown>>('models'),
      fetchAiList<Record<string, unknown>>('knowledge-bases'),
    ])
    modelOptions.value = normalizeOptions(models, 'modelName')
    knowledgeOptions.value = normalizeOptions(knowledge, 'knowledgeBaseName')
  } catch (error) {
    modelOptions.value = [{ label: '通用对话模型', value: 'default-chat' }]
    knowledgeOptions.value = [{ label: '产品制度知识库', value: 'default-knowledge' }]
  }
}

async function loadConversations() {
  loadingConversations.value = true
  try {
    conversations.value = await fetchConversations({ pageNum: 1, pageSize: 50 })
  } catch (error) {
    console.error('Failed to load conversations:', error)
    conversations.value = []
  } finally {
    loadingConversations.value = false
  }
}

async function loadConversation(conversationId: string) {
  currentConversationId.value = conversationId
  loading.value = true
  try {
    const history = await fetchConversationHistory(conversationId, 50)
    messages.value = history.map((record) => ({
      role: record.role as 'user' | 'assistant',
      content: record.content,
    }))
  } catch (error) {
    console.error('Failed to load conversation history:', error)
  } finally {
    loading.value = false
  }
}

function startNewChat() {
  currentConversationId.value = ''
  messages.value = []
  errorMessage.value = ''
}

async function sendMessage() {
  const content = form.content.trim()
  if (!content || loading.value) {
    return
  }

  const userMessage: AiChatMessage = { role: 'user', content }
  messages.value.push(userMessage)
  form.content = ''
  loading.value = true
  errorMessage.value = ''

  try {
    const result = await sendAiChat({
      modelId: form.modelId,
      knowledgeId: form.knowledgeId,
      messages: messages.value,
      conversationId: currentConversationId.value || undefined,
    })
    messages.value.push(result.message)
    if (result.conversationId && !currentConversationId.value) {
      currentConversationId.value = result.conversationId
      await loadConversations()
    }
  } catch (error) {
    errorMessage.value = '接口暂不可用，已追加本地示例回复。'
    messages.value.push({
      role: 'assistant',
      content: '我已收到你的问题。当前是基础框架版，后端接口接通后会返回真实模型响应。',
    })
  } finally {
    loading.value = false
  }
}

function normalizeOptions(items: Record<string, unknown>[], nameKey: string) {
  return items.map((item, index) => ({
    label: String(item[nameKey] || item.name || item.label || `选项 ${index + 1}`),
    value: String(item.id || item.modelId || item.knowledgeBaseId || item.value || index + 1),
  }))
}

function formatTime(timeStr: string) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return `${date.getMonth() + 1}/${date.getDate()}`
}

onMounted(() => {
  loadOptions()
  loadConversations()
})
</script>

<style scoped>
.ai-chat-page {
  min-height: calc(100vh - var(--tc-top-height) - var(--tc-tags-height) - 24px);
}

.chat-side-card,
.chat-main-card {
  border-radius: 4px;
  min-height: 100%;
}

.chat-side-card :deep(.el-select) {
  width: 100%;
}

.chat-error {
  margin-bottom: 12px;
}

.conversation-list {
  max-height: 400px;
  overflow-y: auto;
}

.conversation-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-bottom: 4px;
}

.conversation-item:hover {
  background-color: #f5f7fa;
}

.conversation-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.conv-title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

.message-list {
  background: #f7f9fc;
  border: 1px solid var(--tc-border);
  border-radius: 4px;
  min-height: 360px;
  max-height: 500px;
  overflow-y: auto;
  padding: 16px;
  margin-bottom: 12px;
}

.message-item {
  margin-bottom: 16px;
}

.message-item:last-child {
  margin-bottom: 0;
}

.message-role {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 6px;
}

.message-item.user .message-role {
  color: #409eff;
}

.message-item.assistant .message-role {
  color: #67c23a;
}

.message-content {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-input__actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.chat-input__actions span {
  color: #909399;
  font-size: 12px;
}
</style>
