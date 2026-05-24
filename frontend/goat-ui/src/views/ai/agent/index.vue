<template>
  <div class="agent-workbench">
    <AiTablePage
      resource="agents"
      id-key="agentId"
      title="AI 智能体"
      :columns="columns"
      :form-fields="formFields"
      keyword-placeholder="请输入智能体名称、编码或能力"
    />

    <div class="layout-padding agent-run-shell">
      <div class="layout-padding-auto layout-padding-view">
        <el-row :gutter="12">
          <el-col :xs="24" :lg="8">
            <el-card shadow="never" class="agent-panel">
              <div class="table-toolbar">
                <div>
                  <div class="table-toolbar__title">智能体运行台</div>
                  <div class="agent-subtitle">选择已配置智能体，输入任务并调用真实运行接口。</div>
                </div>
                <el-button text type="primary" :loading="loadingAgents" @click="loadAgents">刷新</el-button>
              </div>

              <el-alert
                v-if="agentLoadError"
                class="agent-alert"
                type="warning"
                :title="agentLoadError"
                show-icon
                :closable="false"
              />

              <el-form label-position="top">
                <el-form-item label="选择智能体">
                  <el-select
                    v-model="runForm.agentId"
                    filterable
                    clearable
                    placeholder="请选择智能体"
                    class="agent-select"
                  >
                    <el-option
                      v-for="agent in agentOptions"
                      :key="agent.value"
                      :label="agent.label"
                      :value="agent.value"
                    >
                      <span>{{ agent.label }}</span>
                      <span class="agent-option-code">{{ agent.code }}</span>
                    </el-option>
                  </el-select>
                </el-form-item>

                <el-form-item label="任务">
                  <el-input
                    v-model="runForm.message"
                    type="textarea"
                    :rows="6"
                    maxlength="2000"
                    show-word-limit
                    placeholder="请输入要交给智能体执行的任务，例如：检查登录链路并总结健康状态。"
                  />
                </el-form-item>

                <div class="agent-switches">
                  <el-form-item label="使用 RAG">
                    <el-switch v-model="runForm.useRag" />
                  </el-form-item>
                  <el-form-item label="使用 ChatBI">
                    <el-switch v-model="runForm.useChatBi" />
                  </el-form-item>
                  <el-form-item label="TopK">
                    <el-input-number v-model="runForm.topK" :min="1" :max="20" controls-position="right" />
                  </el-form-item>
                </div>

                <el-button class="agent-run-button" type="primary" :loading="running" @click="runAgent">
                  运行智能体
                </el-button>
              </el-form>
            </el-card>
          </el-col>

          <el-col :xs="24" :lg="16">
            <el-card shadow="never" class="agent-panel agent-result-panel" v-loading="running">
              <div class="table-toolbar">
                <div class="table-toolbar__title">运行结果</div>
                <el-tag effect="plain">POST /api/ai/agents/{id}/run</el-tag>
              </div>

              <el-alert v-if="runError" class="agent-alert" type="error" :title="runError" show-icon />

              <el-empty v-if="!runResult" description="暂无运行结果，请选择智能体并提交任务。" />

              <template v-else>
                <section class="result-section">
                  <div class="section-title">最终回答</div>
                  <div class="answer-box">{{ finalAnswer }}</div>
                </section>

                <el-descriptions class="result-section" :column="3" border>
                  <el-descriptions-item label="模型">{{ modelText }}</el-descriptions-item>
                  <el-descriptions-item label="Finish Reason">{{ finishReasonText }}</el-descriptions-item>
                  <el-descriptions-item label="Token">{{ tokenText }}</el-descriptions-item>
                </el-descriptions>

                <section v-if="ragReferences.length" class="result-section">
                  <div class="section-title">RAG 引用</div>
                  <div v-for="(item, index) in ragReferences" :key="index" class="reference-card">
                    <div class="reference-title">{{ referenceTitle(item, index) }}</div>
                    <div class="reference-content">{{ referenceContent(item) }}</div>
                  </div>
                </section>

                <section v-if="chatBiSql" class="result-section">
                  <div class="section-title">ChatBI SQL 草案</div>
                  <pre class="json-block">{{ chatBiSql }}</pre>
                </section>

                <section v-if="toolResults.length" class="result-section">
                  <div class="section-title">Tool Results</div>
                  <pre class="json-block">{{ formatJson(toolResults) }}</pre>
                </section>

                <section v-if="planOrTraces.length" class="result-section">
                  <div class="section-title">Plan / Traces</div>
                  <el-timeline>
                    <el-timeline-item v-for="(item, index) in planOrTraces" :key="index" :timestamp="traceTitle(item)">
                      <pre class="trace-block">{{ formatJson(item) }}</pre>
                    </el-timeline-item>
                  </el-timeline>
                </section>

                <section v-if="metadataText" class="result-section">
                  <div class="section-title">Metadata</div>
                  <pre class="json-block">{{ metadataText }}</pre>
                </section>
              </template>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {fetchAiList, runAiAgent, type AiAgentRunResponse} from '@/api/ai'
import AiTablePage from '../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../components/types'

interface AgentOption {
  label: string
  value: string | number
  code: string
}

const columns: AiTableColumn[] = [
  { prop: 'agentName', label: '智能体名称', minWidth: 170 },
  { prop: 'agentCode', label: '智能体编码', minWidth: 140 },
  { prop: 'modelId', label: '模型ID', width: 100 },
  { prop: 'promptId', label: '提示词ID', width: 110 },
  { prop: 'toolIds', label: '工具能力', minWidth: 150 },
  { prop: 'knowledgeBaseIds', label: '知识库', minWidth: 130 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
]

const formFields: AiFormField[] = [
  { prop: 'agentName', label: '智能体名称', required: true },
  { prop: 'agentCode', label: '智能体编码', required: true },
  { prop: 'description', label: '描述', type: 'textarea', span: 24 },
  { prop: 'modelId', label: '模型ID', type: 'number', defaultValue: 1 },
  { prop: 'promptId', label: '提示词ID', type: 'number', defaultValue: 1 },
  { prop: 'toolIds', label: '工具ID', placeholder: '例如 1,2,3' },
  { prop: 'knowledgeBaseIds', label: '知识库ID', placeholder: '例如 1,2' },
  { prop: 'memoryConfig', label: '记忆配置 JSON', type: 'textarea', span: 24 },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    required: true,
    defaultValue: 'ENABLED',
    options: [
      { label: '启用', value: 'ENABLED' },
      { label: '停用', value: 'DISABLED' },
    ],
  },
  { prop: 'remark', label: '说明', type: 'textarea', span: 24 },
]

const runForm = reactive({
  agentId: null as string | number | null,
  message: '',
  useRag: true,
  useChatBi: false,
  topK: 3,
})

const agentOptions = ref<AgentOption[]>([])
const loadingAgents = ref(false)
const running = ref(false)
const agentLoadError = ref('')
const runError = ref('')
const runResult = ref<AiAgentRunResponse | null>(null)

const finalAnswer = computed(() => {
  const result = runResult.value
  return stringifyValue(
    result?.answer ||
      result?.summary ||
      result?.chat?.message?.content ||
      getByPath(result, ['result', 'answer']) ||
      getByPath(result, ['data', 'answer']) ||
      '接口已返回，但未识别到最终回答字段。',
  )
})

const modelText = computed(() => {
  const result = runResult.value
  return stringifyValue(result?.model || result?.modelCode || result?.chat?.modelCode || result?.chat?.provider || '-')
})

const finishReasonText = computed(() => {
  const result = runResult.value
  return stringifyValue(result?.finishReason || result?.chat?.finishReason || '-')
})

const tokenText = computed(() => {
  const usage = runResult.value?.usage || runResult.value?.chat?.usage
  if (!usage) {
    return '-'
  }
  return `输入 ${usage.promptTokens ?? 0} / 输出 ${usage.completionTokens ?? 0} / 总计 ${usage.totalTokens ?? 0}`
})

const metadata = computed(() => {
  const result = runResult.value
  return (result?.metadata || result?.chat?.metadata || {}) as Record<string, unknown>
})

const ragReferences = computed(() => {
  const result = runResult.value
  return normalizeArray(
    result?.ragReferences ||
      result?.references ||
      result?.citations ||
      getByPath(metadata.value, ['rag', 'references']) ||
      getByPath(metadata.value, ['rag', 'hits']) ||
      getByPath(metadata.value, ['references']),
  )
})

const toolResults = computed(() => {
  const result = runResult.value
  return normalizeArray(result?.toolResults || getByPath(metadata.value, ['toolResults']) || getByPath(metadata.value, ['tools']))
})

const planOrTraces = computed(() => {
  const result = runResult.value
  return normalizeArray(result?.traces || result?.plan || getByPath(metadata.value, ['traces']) || getByPath(metadata.value, ['plan']))
})

const chatBiSql = computed(() => {
  const candidates = [
    getByPath(metadata.value, ['chatBi', 'candidateSql']),
    getByPath(metadata.value, ['chatbi', 'candidateSql']),
    getByPath(metadata.value, ['chatBI', 'candidateSql']),
    getByPath(metadata.value, ['chatBi', 'sql']),
    getByPath(metadata.value, ['chatbi', 'sql']),
    metadata.value.candidateSql,
    metadata.value.sqlDraft,
    metadata.value.sql,
  ]
  return stringifyValue(candidates.find((item) => item !== undefined && item !== null && item !== ''))
})

const metadataText = computed(() => {
  if (!Object.keys(metadata.value).length) {
    return ''
  }
  return formatJson(metadata.value)
})

async function loadAgents() {
  loadingAgents.value = true
  agentLoadError.value = ''
  try {
    const result = await fetchAiList<Record<string, unknown>>('agents')
    agentOptions.value = result.map((item, index) => {
      const id = (item.agentId || item.id || item.value || index + 1) as string | number
      const code = stringifyValue(item.agentCode || item.code || id)
      return {
        label: stringifyValue(item.agentName || item.name || item.label || `智能体 ${index + 1}`),
        value: id,
        code,
      }
    })
    if (!runForm.agentId && agentOptions.value.length) {
      runForm.agentId = agentOptions.value[0].value
    }
  } catch (error) {
    agentOptions.value = []
    agentLoadError.value = '智能体列表加载失败，请检查后端服务和权限配置。'
  } finally {
    loadingAgents.value = false
  }
}

async function runAgent() {
  if (!runForm.agentId) {
    ElMessage.warning('请先选择智能体')
    return
  }
  const message = runForm.message.trim()
  if (!message) {
    ElMessage.warning('请输入任务内容')
    return
  }

  running.value = true
  runError.value = ''
  try {
    runResult.value = await runAiAgent(runForm.agentId, {
      message,
      options: {
        useRag: runForm.useRag,
        useChatBi: runForm.useChatBi,
        topK: runForm.topK,
      },
    })
  } catch (error) {
    runError.value = '智能体运行失败，请检查接口、权限或模型服务状态。'
  } finally {
    running.value = false
  }
}

function normalizeArray(value: unknown): unknown[] {
  if (Array.isArray(value)) {
    return value
  }
  if (value === undefined || value === null || value === '') {
    return []
  }
  return [value]
}

function getByPath(source: unknown, path: string[]) {
  return path.reduce<unknown>((current, key) => {
    if (current && typeof current === 'object' && key in current) {
      return (current as Record<string, unknown>)[key]
    }
    return undefined
  }, source)
}

function stringifyValue(value: unknown) {
  if (value === undefined || value === null) {
    return ''
  }
  if (typeof value === 'string') {
    return value
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  return formatJson(value)
}

function formatJson(value: unknown) {
  try {
    return JSON.stringify(value, null, 2)
  } catch (error) {
    return String(value)
  }
}

function referenceTitle(item: unknown, index: number) {
  if (!item || typeof item !== 'object') {
    return `引用 ${index + 1}`
  }
  const record = item as Record<string, unknown>
  return stringifyValue(record.title || record.documentName || record.knowledgeName || record.chunkId || `引用 ${index + 1}`)
}

function referenceContent(item: unknown) {
  if (!item || typeof item !== 'object') {
    return stringifyValue(item)
  }
  const record = item as Record<string, unknown>
  return stringifyValue(record.content || record.text || record.snippet || record.summary || item)
}

function traceTitle(item: unknown) {
  if (!item || typeof item !== 'object') {
    return ''
  }
  const record = item as Record<string, unknown>
  return stringifyValue(record.name || record.step || record.type || record.status)
}

onMounted(loadAgents)
</script>

<style scoped>
.agent-workbench :deep(.layout-padding:first-child) {
  padding-bottom: 0;
}

.agent-run-shell {
  padding-top: 12px;
}

.agent-panel {
  border-radius: 4px;
  min-height: 100%;
}

.agent-result-panel {
  min-height: 560px;
}

.agent-subtitle {
  color: var(--tc-subtle);
  font-size: 12px;
  margin-top: 4px;
}

.agent-alert {
  margin-bottom: 12px;
}

.agent-select,
.agent-switches :deep(.el-input-number) {
  width: 100%;
}

.agent-option-code {
  color: var(--tc-subtle);
  float: right;
  font-size: 12px;
  margin-left: 16px;
}

.agent-switches {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.agent-run-button {
  width: 100%;
}

.result-section {
  margin-top: 16px;
}

.section-title {
  color: #303133;
  font-weight: 600;
  margin-bottom: 8px;
}

.answer-box,
.reference-card,
.json-block,
.trace-block {
  background: #f7f9fc;
  border: 1px solid var(--tc-border);
  border-radius: 4px;
}

.answer-box {
  line-height: 1.8;
  min-height: 96px;
  padding: 12px;
  white-space: pre-wrap;
}

.reference-card {
  margin-bottom: 8px;
  padding: 10px 12px;
}

.reference-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.reference-content {
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
}

.json-block,
.trace-block {
  color: #303133;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  margin: 0;
  max-height: 320px;
  overflow: auto;
  padding: 12px;
  white-space: pre-wrap;
}

.trace-block {
  max-height: 180px;
}

@media (max-width: 1200px) {
  .agent-panel {
    margin-bottom: 12px;
  }
}

@media (max-width: 768px) {
  .agent-switches {
    grid-template-columns: 1fr;
  }
}
</style>
