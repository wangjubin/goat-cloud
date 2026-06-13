<template>
  <div class="agent-page" v-loading="loading">
    <div class="agent-header">
      <div class="agent-header__title">
        <h2>AI 智能体</h2>
        <span class="agent-header__sub">智能体编排 · 工具组合 · 运行调试</span>
      </div>
      <div class="agent-header__actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon><span>刷新</span>
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon><span>新建智能体</span>
        </el-button>
      </div>
    </div>

    <div class="agent-stats">
      <div class="agent-stat">
        <span class="agent-stat__label">智能体总数</span>
        <strong class="agent-stat__value">{{ stats.total }}</strong>
      </div>
      <div class="agent-stat agent-stat--primary">
        <span class="agent-stat__label">已启用</span>
        <strong class="agent-stat__value">{{ stats.enabled }}</strong>
      </div>
      <div class="agent-stat agent-stat--success">
        <span class="agent-stat__label">已绑定模型</span>
        <strong class="agent-stat__value">{{ stats.withModel }}</strong>
      </div>
      <div class="agent-stat agent-stat--warning">
        <span class="agent-stat__label">已绑定工具</span>
        <strong class="agent-stat__value">{{ stats.withTool }}</strong>
      </div>
    </div>

    <div class="agent-filters">
      <el-input v-model="query.keyword" placeholder="搜索智能体名称、编码或能力" clearable
        @keyup.enter="loadData" @clear="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.agentType" placeholder="类型" clearable @change="loadData">
        <el-option label="对话助手" value="ASSISTANT" />
        <el-option label="RAG 检索" value="RAG" />
        <el-option label="工作流" value="WORKFLOW" />
        <el-option label="数据分析" value="CHATBI" />
        <el-option label="代码生成" value="CODE" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable @change="loadData">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <el-alert v-if="errorMessage" class="agent-alert" type="error" :title="errorMessage" show-icon />
    <el-empty v-if="!loading && records.length === 0" description="暂无智能体" />

    <div v-else class="agent-grid">
      <div v-for="row in records" :key="row.agentId" class="agent-card"
        :class="{ 'agent-card--disabled': row.status !== 'ENABLED' }">
        <div class="agent-card__header">
          <div class="agent-card__title">
            <div class="agent-card__icon" :style="{background: typeGradient(row.agentType)}">
              <el-icon :size="22"><Avatar /></el-icon>
            </div>
            <div class="agent-card__title-text">
              <h3 class="agent-card__name">{{ row.agentName }}</h3>
              <code class="agent-card__code">{{ row.agentCode }}</code>
            </div>
          </div>
          <el-tag size="small" effect="plain" :type="row.status === 'ENABLED' ? 'success' : 'info'">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </div>

        <div v-if="row.description" class="agent-card__desc">{{ row.description }}</div>

        <div class="agent-card__binding">
          <div class="agent-card__binding-row" v-if="row.modelName || row.modelCode">
            <el-icon><Connection /></el-icon>
            <span class="agent-card__binding-key">模型</span>
            <span class="agent-card__binding-val">{{ row.modelName || row.modelCode }}</span>
          </div>
          <div class="agent-card__binding-row" v-if="row.promptName">
            <el-icon><Document /></el-icon>
            <span class="agent-card__binding-key">提示词</span>
            <span class="agent-card__binding-val">{{ row.promptName }}</span>
          </div>
          <div class="agent-card__binding-row" v-if="countTools(row)">
            <el-icon><Tools /></el-icon>
            <span class="agent-card__binding-key">工具</span>
            <span class="agent-card__binding-val">{{ countTools(row) }} 项</span>
          </div>
          <div class="agent-card__binding-row" v-if="countKb(row)">
            <el-icon><Collection /></el-icon>
            <span class="agent-card__binding-key">知识库</span>
            <span class="agent-card__binding-val">{{ countKb(row) }} 个</span>
          </div>
        </div>

        <div class="agent-card__caps">
          <el-tag v-if="row.agentType" size="small" effect="plain" :type="typeTagType(row.agentType)">
            {{ typeLabel(row.agentType) }}
          </el-tag>
          <el-tag v-if="row.enableRag" size="small" effect="plain" type="success">RAG</el-tag>
          <el-tag v-if="row.enableChatBi" size="small" effect="plain" type="warning">ChatBI</el-tag>
          <el-tag v-if="row.enableMemory" size="small" effect="plain" type="info">记忆</el-tag>
        </div>

        <div v-if="row.remark" class="agent-card__remark">{{ row.remark }}</div>

        <div class="agent-card__actions">
          <el-button text type="primary" @click="openRun(row)">
            <el-icon><VideoPlay /></el-icon><span>运行</span>
          </el-button>
          <el-button text type="primary" @click="openEdit(row)">
            <el-icon><Edit /></el-icon><span>编辑</span>
          </el-button>
          <el-button text type="danger" @click="deleteRow(row)">
            <el-icon><Delete /></el-icon><span>删除</span>
          </el-button>
        </div>
      </div>
    </div>

    <el-pagination v-if="total > query.pageSize" class="agent-pagination" background
      layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
      :current-page="query.pageNum" @current-change="handlePageChange" />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑智能体' : '新建智能体'"
      width="780px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="智能体名称" prop="agentName">
              <el-input v-model="form.agentName" placeholder="例如：通用助手" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="智能体编码" prop="agentCode">
              <el-input v-model="form.agentCode" placeholder="例如：general-assistant" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="智能体类型" prop="agentType">
              <el-select v-model="form.agentType" placeholder="请选择" style="width:100%">
                <el-option label="对话助手" value="ASSISTANT" />
                <el-option label="RAG 检索" value="RAG" />
                <el-option label="工作流" value="WORKFLOW" />
                <el-option label="数据分析" value="CHATBI" />
                <el-option label="代码生成" value="CODE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对话模型" prop="modelId">
              <el-select v-model="form.modelId" placeholder="请选择" filterable clearable style="width:100%">
                <el-option v-for="m in modelOptions" :key="m.value" :label="m.label" :value="m.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提示词模板" prop="promptId">
              <el-select v-model="form.promptId" placeholder="请选择" filterable clearable style="width:100%">
                <el-option v-for="p in promptOptions" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="能力开关">
              <div class="agent-form__switches">
                <el-checkbox v-model="form.enableRag">RAG 检索</el-checkbox>
                <el-checkbox v-model="form.enableChatBi">ChatBI</el-checkbox>
                <el-checkbox v-model="form.enableMemory">长期记忆</el-checkbox>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="工具能力">
              <el-select v-model="form.toolIds" multiple collapse-tags collapse-tags-tooltip
                placeholder="选择 API Skills / MCP 工具" style="width:100%">
                <el-option-group label="API Skills">
                  <el-option v-for="t in toolOptions.filter(t => t.kind === 'api')" :key="t.value"
                    :label="t.label" :value="t.value" />
                </el-option-group>
                <el-option-group label="MCP 工具">
                  <el-option v-for="t in toolOptions.filter(t => t.kind === 'mcp')" :key="t.value"
                    :label="t.label" :value="t.value" />
                </el-option-group>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="知识库">
              <el-select v-model="form.knowledgeBaseIds" multiple collapse-tags collapse-tags-tooltip
                placeholder="选择知识库" style="width:100%">
                <el-option v-for="kb in knowledgeBaseOptions" :key="kb.value"
                  :label="kb.label" :value="kb.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选：智能体用途说明" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="记忆配置">
              <el-input v-model="form.memoryConfig" type="textarea" :rows="2"
                placeholder='JSON，例如 {"window":10,"summary":true}' />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio-button value="ENABLED">启用</el-radio-button>
                <el-radio-button value="DISABLED">停用</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="runVisible" :title="`运行智能体 - ${runningAgent?.agentName || ''}`"
      size="900px" direction="rtl" destroy-on-close>
      <div class="agent-run" v-loading="running">
        <div class="agent-run__form">
          <el-form label-position="top">
            <el-form-item label="任务内容">
              <el-input v-model="runForm.message" type="textarea" :rows="6" maxlength="2000"
                show-word-limit placeholder="请输入要交给智能体执行的任务" />
            </el-form-item>
            <div class="agent-run__switches">
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
            <el-button class="agent-run__button" type="primary" :loading="running" @click="runAgent">
              <el-icon><Promotion /></el-icon><span>提交运行</span>
            </el-button>
          </el-form>
        </div>

        <el-alert v-if="runError" class="agent-run__alert" type="error" :title="runError" show-icon />

        <el-empty v-if="!runResult" description="暂无运行结果" />

        <template v-else>
          <div class="agent-run__result">
            <div class="agent-run__result-header">
              <h4>最终回答</h4>
              <el-tag effect="plain" size="small">POST /api/ai/agents/{id}/run</el-tag>
            </div>
            <div class="agent-run__answer">{{ finalAnswer }}</div>
          </div>

          <el-descriptions class="agent-run__meta" :column="3" border>
            <el-descriptions-item label="模型">{{ modelText }}</el-descriptions-item>
            <el-descriptions-item label="Finish">{{ finishReasonText }}</el-descriptions-item>
            <el-descriptions-item label="Token">{{ tokenText }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="ragReferences.length" class="agent-run__section">
            <h4>RAG 引用 ({{ ragReferences.length }})</h4>
            <div v-for="(item, index) in ragReferences" :key="index" class="agent-run__ref">
              <div class="agent-run__ref-title">{{ referenceTitle(item, index) }}</div>
              <div class="agent-run__ref-content">{{ referenceContent(item) }}</div>
            </div>
          </div>

          <div v-if="chatBiSql" class="agent-run__section">
            <h4>ChatBI SQL 草案</h4>
            <pre class="agent-run__code">{{ chatBiSql }}</pre>
          </div>

          <div v-if="toolResults.length" class="agent-run__section">
            <h4>Tool Results</h4>
            <pre class="agent-run__code">{{ formatJson(toolResults) }}</pre>
          </div>

          <div v-if="planOrTraces.length" class="agent-run__section">
            <h4>Plan / Traces</h4>
            <el-timeline>
              <el-timeline-item v-for="(item, index) in planOrTraces" :key="index" :timestamp="traceTitle(item)">
                <pre class="agent-run__code agent-run__code--small">{{ formatJson(item) }}</pre>
              </el-timeline-item>
            </el-timeline>
          </div>

          <div v-if="metadataText" class="agent-run__section">
            <h4>Metadata</h4>
            <pre class="agent-run__code">{{ metadataText }}</pre>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {
  Avatar, Collection, Connection, Delete, Document, Edit, Plus, Promotion,
  Refresh, Search, Tools, VideoPlay,
} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {deleteAiResource, fetchAiDetail, fetchAiList, fetchAiPage, runAiAgent, saveAiResource, type AiAgentRunResponse} from '@/api/ai'

interface Agent {
  agentId: number | string
  agentName: string
  agentCode: string
  agentType: string
  description: string
  modelId: number | string
  modelName?: string
  modelCode?: string
  promptId: number | string
  promptName?: string
  toolIds: string | number[]
  knowledgeBaseIds: string | number[]
  enableRag: boolean
  enableChatBi: boolean
  enableMemory: boolean
  memoryConfig: string
  status: string
  remark: string
}

const query = reactive({pageNum: 1, pageSize: 12, keyword: '', agentType: '', status: ''})
const records = ref<Agent[]>([])
const total = ref(0)
const loading = ref(false); const saving = ref(false); const running = ref(false)
const errorMessage = ref(''); const runError = ref('')
const dialogVisible = ref(false); const runVisible = ref(false)
const editingId = ref<number | string | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({})
const runningAgent = ref<Agent | null>(null)
const runResult = ref<AiAgentRunResponse | null>(null)
const runForm = reactive({message: '', useRag: true, useChatBi: false, topK: 3})

const modelOptions = ref<Array<{label: string; value: number | string}>>([])
const promptOptions = ref<Array<{label: string; value: number | string}>>([])
const toolOptions = ref<Array<{label: string; value: number | string; kind: 'api' | 'mcp'}>>([])
const knowledgeBaseOptions = ref<Array<{label: string; value: number | string}>>([])

const stats = computed(() => {
  const s = {total: 0, enabled: 0, withModel: 0, withTool: 0}
  for (const r of records.value) {
    s.total++
    if (r.status === 'ENABLED') s.enabled++
    if (r.modelId) s.withModel++
    if (countTools(r) > 0) s.withTool++
  }
  return s
})

const rules = computed<FormRules>(() => ({
  agentName: [{required: true, message: '请填写智能体名称', trigger: 'blur'}],
  agentCode: [{required: true, message: '请填写智能体编码', trigger: 'blur'}],
  agentType: [{required: true, message: '请选择类型', trigger: 'change'}],
  status: [{required: true, message: '请选择状态', trigger: 'change'}],
}))

const finalAnswer = computed(() => {
  const r = runResult.value
  return stringifyValue(
    r?.answer || r?.summary || r?.chat?.message?.content ||
    getByPath(r, ['result', 'answer']) || getByPath(r, ['data', 'answer']) ||
    '接口已返回，但未识别到最终回答字段。',
  )
})
const modelText = computed(() => stringifyValue(runResult.value?.model || runResult.value?.modelCode || runResult.value?.chat?.modelCode || runResult.value?.chat?.provider || '—'))
const finishReasonText = computed(() => stringifyValue(runResult.value?.finishReason || runResult.value?.chat?.finishReason || '—'))
const tokenText = computed(() => {
  const u = runResult.value?.usage || runResult.value?.chat?.usage
  if (!u) return '—'
  return `入 ${u.promptTokens ?? 0} / 出 ${u.completionTokens ?? 0} / 总 ${u.totalTokens ?? 0}`
})
const metadata = computed(() => (runResult.value?.metadata || runResult.value?.chat?.metadata || {}) as Record<string, unknown>)
const ragReferences = computed(() => normalizeArray(runResult.value?.ragReferences || runResult.value?.references || runResult.value?.citations || getByPath(metadata.value, ['rag', 'references']) || getByPath(metadata.value, ['rag', 'hits'])))
const toolResults = computed(() => normalizeArray(runResult.value?.toolResults || getByPath(metadata.value, ['toolResults']) || getByPath(metadata.value, ['tools'])))
const planOrTraces = computed(() => normalizeArray(runResult.value?.traces || runResult.value?.plan || getByPath(metadata.value, ['traces']) || getByPath(metadata.value, ['plan'])))
const chatBiSql = computed(() => {
  const m = metadata.value
  const cands = [getByPath(m, ['chatBi', 'candidateSql']), getByPath(m, ['chatbi', 'sql']), m.candidateSql, m.sqlDraft, m.sql]
  return stringifyValue(cands.find(i => i !== undefined && i !== null && i !== ''))
})
const metadataText = computed(() => Object.keys(metadata.value).length ? formatJson(metadata.value) : '')

async function loadData() {
  loading.value = true; errorMessage.value = ''
  try {
    const payload: Record<string, unknown> = {...query}
    if (!payload.agentType) delete payload.agentType
    if (!payload.status) delete payload.status
    const result = await fetchAiPage<Agent>('agents', payload as any)
    records.value = (result.records || []).map(r => ({
      ...r,
      enableRag: r.enableRag ?? false,
      enableChatBi: r.enableChatBi ?? false,
      enableMemory: r.enableMemory ?? false,
    }))
    total.value = result.total || records.value.length
  } catch {
    errorMessage.value = '接口不可用，请检查后端服务和权限配置。'
    records.value = []; total.value = 0
  } finally { loading.value = false }
}

async function loadOptions() {
  try {
    const models = await fetchAiList<any>('models')
    modelOptions.value = models.map(m => ({label: `${m.modelName || m.name} (${m.modelCode || m.code || ''})`, value: m.modelId || m.id}))
  } catch { /* ignore */ }
  try {
    const prompts = await fetchAiList<any>('prompts')
    promptOptions.value = prompts.map(p => ({label: `${p.promptName || p.name} (${p.promptCode || p.code || ''})`, value: p.promptId || p.id}))
  } catch { /* ignore */ }
  try {
    const [apis, mcps] = await Promise.all([
      fetchAiList<any>('api-skills').catch(() => []),
      fetchAiList<any>('mcp-tools').catch(() => []),
    ])
    toolOptions.value = [
      ...apis.map((t: any) => ({label: `[API] ${t.skillName || t.name}`, value: t.apiSkillId || t.id, kind: 'api' as const})),
      ...mcps.map((t: any) => ({label: `[MCP] ${t.toolName || t.name}`, value: t.mcpToolId || t.id, kind: 'mcp' as const})),
    ]
  } catch { /* ignore */ }
  try {
    const kbs = await fetchAiList<any>('knowledge-bases')
    knowledgeBaseOptions.value = kbs.map((k: any) => ({label: k.knowledgeBaseName || k.name, value: k.knowledgeBaseId || k.id}))
  } catch { /* ignore */ }
}

function resetQuery() { query.pageNum = 1; query.keyword = ''; query.agentType = ''; query.status = ''; loadData() }
function handlePageChange(p: number) { query.pageNum = p; loadData() }

function resetForm() {
  Object.keys(form).forEach(k => delete form[k])
  form.status = 'ENABLED'
  form.enableRag = false; form.enableChatBi = false; form.enableMemory = false
  form.toolIds = []; form.knowledgeBaseIds = []
}

function openCreate() { editingId.value = null; resetForm(); dialogVisible.value = true }

async function openEdit(row: Agent) {
  editingId.value = row.agentId; resetForm()
  try {
    const detail: any = await fetchAiDetail('agents', row.agentId)
    const d = {toolIds: [], knowledgeBaseIds: [], ...detail}
    if (typeof d.toolIds === 'string') d.toolIds = d.toolIds ? d.toolIds.split(',').map((s: string) => s.trim()).filter(Boolean) : []
    if (typeof d.knowledgeBaseIds === 'string') d.knowledgeBaseIds = d.knowledgeBaseIds ? d.knowledgeBaseIds.split(',').map((s: string) => s.trim()).filter(Boolean) : []
    Object.assign(form, d)
  } catch { Object.assign(form, row) }
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate(); saving.value = true
  try {
    const payload: Record<string, any> = {...form}
    if (Array.isArray(payload.toolIds)) payload.toolIds = payload.toolIds.join(',')
    if (Array.isArray(payload.knowledgeBaseIds)) payload.knowledgeBaseIds = payload.knowledgeBaseIds.join(',')
    await saveAiResource('agents', payload)
    ElMessage.success('保存成功'); dialogVisible.value = false; await loadData()
  } finally { saving.value = false }
}

async function deleteRow(row: Agent) {
  await ElMessageBox.confirm(`确认删除智能体「${row.agentName}」吗？`, '删除确认', {type: 'warning'})
  await deleteAiResource('agents', [row.agentId])
  ElMessage.success('删除成功'); await loadData()
}

function openRun(row: Agent) {
  runningAgent.value = row
  runForm.message = ''
  runResult.value = null; runError.value = ''
  runVisible.value = true
  loadAgentsForRun()
}

const runAgentOptions = ref<Array<{label: string; value: number | string}>>([])
async function loadAgentsForRun() {
  try {
    const list = await fetchAiList<any>('agents')
    runAgentOptions.value = list.map((a, i) => ({
      label: a.agentName || a.name || `智能体 ${i + 1}`,
      value: a.agentId || a.id || i + 1,
    }))
  } catch { runAgentOptions.value = [] }
}

async function runAgent() {
  if (!runningAgent.value) { ElMessage.warning('请先选择智能体'); return }
  if (!runForm.message.trim()) { ElMessage.warning('请输入任务内容'); return }
  running.value = true; runError.value = ''
  try {
    runResult.value = await runAiAgent(runningAgent.value.agentId, {
      message: runForm.message.trim(),
      options: {useRag: runForm.useRag, useChatBi: runForm.useChatBi, topK: runForm.topK},
    })
  } catch { runError.value = '智能体运行失败，请检查接口、权限或模型服务状态。' }
  finally { running.value = false }
}

function countTools(row: Agent): number {
  if (Array.isArray(row.toolIds)) return row.toolIds.length
  if (typeof row.toolIds === 'string') return row.toolIds ? row.toolIds.split(',').filter(Boolean).length : 0
  return 0
}
function countKb(row: Agent): number {
  if (Array.isArray(row.knowledgeBaseIds)) return row.knowledgeBaseIds.length
  if (typeof row.knowledgeBaseIds === 'string') return row.knowledgeBaseIds ? row.knowledgeBaseIds.split(',').filter(Boolean).length : 0
  return 0
}

function typeLabel(t: string) { return {ASSISTANT: '对话助手', RAG: 'RAG 检索', WORKFLOW: '工作流', CHATBI: '数据分析', CODE: '代码生成'}[t] || t || '通用' }
function typeTagType(t: string) { return {ASSISTANT: 'primary', RAG: 'success', WORKFLOW: 'warning', CHATBI: 'danger', CODE: 'info'}[t] || 'info' }
function typeGradient(t: string) {
  return {
    ASSISTANT: 'linear-gradient(135deg,#6366f1,#8b5cf6)',
    RAG: 'linear-gradient(135deg,#10b981,#34d399)',
    WORKFLOW: 'linear-gradient(135deg,#f59e0b,#fbbf24)',
    CHATBI: 'linear-gradient(135deg,#ec4899,#f472b6)',
    CODE: 'linear-gradient(135deg,#06b6d4,#22d3ee)',
  }[t] || 'linear-gradient(135deg,#6b7280,#9ca3af)'
}

function normalizeArray(v: unknown): unknown[] {
  if (Array.isArray(v)) return v
  if (v === undefined || v === null || v === '') return []
  return [v]
}
function getByPath(src: unknown, path: string[]): unknown {
  return path.reduce<unknown>((cur, k) => (cur && typeof cur === 'object' && k in (cur as any)) ? (cur as any)[k] : undefined, src)
}
function stringifyValue(v: unknown): string {
  if (v === undefined || v === null) return ''
  if (typeof v === 'string') return v
  if (typeof v === 'number' || typeof v === 'boolean') return String(v)
  return formatJson(v)
}
function formatJson(v: unknown): string {
  try { return JSON.stringify(v, null, 2) } catch { return String(v) }
}
function referenceTitle(item: unknown, i: number): string {
  if (!item || typeof item !== 'object') return `引用 ${i + 1}`
  const r = item as Record<string, unknown>
  return stringifyValue(r.title || r.documentName || r.knowledgeName || r.chunkId || `引用 ${i + 1}`)
}
function referenceContent(item: unknown): string {
  if (!item || typeof item !== 'object') return stringifyValue(item)
  const r = item as Record<string, unknown>
  return stringifyValue(r.content || r.text || r.snippet || r.summary || item)
}
function traceTitle(item: unknown): string {
  if (!item || typeof item !== 'object') return ''
  const r = item as Record<string, unknown>
  return stringifyValue(r.name || r.step || r.type || r.status)
}

onMounted(async () => {
  await loadOptions()
  await loadData()
})
</script>

<style scoped>
.agent-page { padding: 20px; }
.agent-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.agent-header__title h2 { margin: 0 0 4px; font-size: 20px; font-weight: 700; color: var(--tc-text); }
.agent-header__sub { font-size: 12px; color: var(--tc-text-secondary); }
.agent-header__actions { display: flex; gap: 8px; }

.agent-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.agent-stat { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-left: 3px solid var(--tc-primary); border-radius: var(--tc-radius-md); padding: 14px 18px; display: flex; flex-direction: column; gap: 4px; }
.agent-stat--primary { border-left-color: var(--tc-success); }
.agent-stat--success { border-left-color: #10b981; }
.agent-stat--warning { border-left-color: #f59e0b; }
.agent-stat__label { font-size: 12px; color: var(--tc-text-secondary); }
.agent-stat__value { font-size: 22px; font-weight: 700; color: var(--tc-text); }

.agent-filters { display: grid; grid-template-columns: 2fr 1fr 1fr auto; gap: 8px; margin-bottom: 16px; }
.agent-filters :deep(.el-select), .agent-filters :deep(.el-input) { width: 100%; }

.agent-alert { margin-bottom: 12px; }

.agent-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 14px; }
.agent-card { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-radius: var(--tc-radius-md); padding: 16px; display: flex; flex-direction: column; gap: 10px; transition: all 0.2s ease; }
.agent-card:hover { box-shadow: var(--tc-shadow-md); transform: translateY(-2px); }
.agent-card--disabled { opacity: 0.6; }

.agent-card__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }
.agent-card__title { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.agent-card__icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.agent-card__title-text { min-width: 0; flex: 1; }
.agent-card__name { margin: 0 0 2px; font-size: 15px; font-weight: 700; color: var(--tc-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.agent-card__code { font-size: 11px; color: var(--tc-text-secondary); background: var(--tc-bg-soft, #fafafa); padding: 1px 6px; border-radius: 3px; }

.agent-card__desc { font-size: 12px; color: var(--tc-text-secondary); line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.agent-card__binding { display: flex; flex-direction: column; gap: 4px; padding: 8px 10px; background: var(--tc-bg-soft, #fafafa); border-radius: 4px; }
.agent-card__binding-row { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.agent-card__binding-row :deep(svg) { color: var(--tc-text-secondary); font-size: 13px; }
.agent-card__binding-key { color: var(--tc-text-secondary); flex-shrink: 0; width: 50px; }
.agent-card__binding-val { color: var(--tc-text); flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.agent-card__caps { display: flex; gap: 4px; flex-wrap: wrap; }

.agent-card__remark { font-size: 12px; color: var(--tc-text-secondary); font-style: italic; padding: 4px 8px; background: var(--tc-bg-soft, #fafafa); border-radius: 4px; }

.agent-card__actions { display: flex; justify-content: flex-end; gap: 4px; }

.agent-form__switches { display: flex; gap: 16px; padding-top: 4px; }

.agent-pagination { justify-content: flex-end; margin-top: 16px; }

/* Run drawer */
.agent-run { display: flex; flex-direction: column; gap: 14px; padding: 4px; }
.agent-run__form { padding: 12px; background: var(--tc-bg-soft, #fafafa); border-radius: 6px; }
.agent-run__switches { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 12px; }
.agent-run__button { width: 100%; }
.agent-run__alert { margin-bottom: 8px; }
.agent-run__result { background: linear-gradient(135deg, #f0f4ff 0%, #faf5ff 100%); border: 1px solid #e0e7ff; border-radius: 6px; padding: 14px; }
.agent-run__result-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.agent-run__result-header h4 { margin: 0; font-size: 13px; color: var(--tc-text); }
.agent-run__answer { font-size: 13px; line-height: 1.7; white-space: pre-wrap; color: var(--tc-text); }
.agent-run__meta { font-size: 12px; }
.agent-run__section h4 { margin: 0 0 8px; font-size: 13px; color: var(--tc-text); }
.agent-run__ref { padding: 10px 12px; background: var(--tc-bg-soft, #fafafa); border-radius: 4px; margin-bottom: 6px; }
.agent-run__ref-title { font-size: 12px; font-weight: 600; margin-bottom: 4px; color: var(--tc-text); }
.agent-run__ref-content { font-size: 12px; color: var(--tc-text-secondary); line-height: 1.6; white-space: pre-wrap; }
.agent-run__code { font-family: 'SFMono-Regular', Consolas, monospace; font-size: 12px; line-height: 1.6; padding: 12px; background: var(--tc-bg-soft, #fafafa); border: 1px solid var(--tc-border-light); border-radius: 4px; max-height: 320px; overflow: auto; white-space: pre-wrap; margin: 0; }
.agent-run__code--small { max-height: 180px; }

@media (max-width: 960px) {
  .agent-filters { grid-template-columns: 1fr 1fr; }
  .agent-stats { grid-template-columns: repeat(2, 1fr); }
  .agent-grid { grid-template-columns: 1fr; }
}
</style>
