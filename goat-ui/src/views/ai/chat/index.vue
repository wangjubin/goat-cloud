<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view ai-chat-page">
      <el-row :gutter="12">
        <el-col :xs="24" :lg="7">
          <el-card shadow="never" class="chat-side-card">
            <div class="table-toolbar">
              <div class="table-toolbar__title">助手配置</div>
              <el-button text type="primary" @click="resetChat">清空</el-button>
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
            <el-alert
              class="chat-tip"
              type="info"
              title="基础框架版先接入通用大模型交互入口，后续可扩展流式输出、工具调用和上下文管理。"
              show-icon
              :closable="false"
            />
          </el-card>
        </el-col>

        <el-col :xs="24" :lg="17">
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
                <div class="message-content">{{ message.content }}</div>
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
    })
    messages.value.push(result.message)
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

function resetChat() {
  messages.value = []
  errorMessage.value = ''
}

function normalizeOptions(items: Record<string, unknown>[], nameKey: string) {
  return items.map((item, index) => ({
    label: String(item[nameKey] || item.name || item.label || `选项 ${index + 1}`),
    value: String(item.id || item.modelId || item.knowledgeBaseId || item.value || index + 1),
  }))
}

onMounted(loadOptions)
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

.chat-tip,
.chat-error {
  margin-bottom: 12px;
}

.message-list {
  background: #f7f9fc;
  border: 1px solid var(--tc-border);
  border-radius: 4px;
  min-height: 360px;
  max-height: 520px;
  overflow-y: auto;
  padding: 12px;
}

.message-item {
  margin-bottom: 12px;
}

.message-item.user {
  text-align: right;
}

.message-role {
  color: var(--tc-subtle);
  font-size: 12px;
  margin-bottom: 5px;
}

.message-content {
  display: inline-block;
  max-width: 82%;
  border: 1px solid var(--tc-border);
  border-radius: 4px;
  background: #fff;
  color: #303133;
  line-height: 1.7;
  padding: 9px 12px;
  text-align: left;
  white-space: pre-wrap;
}

.message-item.user .message-content {
  background: var(--tc-primary);
  border-color: var(--tc-primary);
  color: #fff;
}

.chat-input {
  margin-top: 12px;
}

.chat-input__actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--tc-subtle);
  font-size: 12px;
  margin-top: 8px;
}

@media (max-width: 1200px) {
  .chat-side-card {
    margin-bottom: 12px;
  }
}
</style>
