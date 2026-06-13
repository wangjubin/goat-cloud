<template>
  <div class="model-page" v-loading="loading">
    <!-- Header -->
    <div class="model-header">
      <div class="model-header__title">
        <h2>模型管理</h2>
        <span class="model-header__sub">AI 模型配置 · 厂商接入 · 默认模型管理</span>
      </div>
      <div class="model-header__actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新增模型</span>
        </el-button>
      </div>
    </div>

    <!-- Tabs by model type -->
    <el-tabs v-model="activeType" class="model-tabs" @tab-change="loadData">
      <el-tab-pane
        v-for="tab in typeTabs"
        :key="tab.value"
        :name="tab.value"
      >
        <template #label>
          <span class="model-tab__label">
            <el-icon><component :is="tab.icon" /></el-icon>
            <span>{{ tab.label }}</span>
            <el-badge :value="typeCount(tab.value)" :max="99" class="model-tab__badge" type="primary" />
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- Stats -->
    <div class="model-stats">
      <div class="model-stat">
        <span class="model-stat__label">该类型模型</span>
        <strong class="model-stat__value">{{ stats.total }}</strong>
      </div>
      <div class="model-stat model-stat--primary">
        <span class="model-stat__label">默认模型</span>
        <strong class="model-stat__value">{{ stats.default }}</strong>
      </div>
      <div class="model-stat model-stat--success">
        <span class="model-stat__label">已启用</span>
        <strong class="model-stat__value">{{ stats.enabled }}</strong>
      </div>
      <div class="model-stat model-stat--muted">
        <span class="model-stat__label">已停用</span>
        <strong class="model-stat__value">{{ stats.disabled }}</strong>
      </div>
    </div>

    <!-- Filters -->
    <div class="model-filters">
      <el-select
        v-model="filters.provider"
        placeholder="选择厂商"
        clearable
        filterable
        class="model-filters__provider"
        @change="loadData"
      >
        <el-option
          v-for="p in providerOptions"
          :key="p.value"
          :label="p.label"
          :value="p.value"
        />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable class="model-filters__status" @change="loadData">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="搜索模型名称或编码"
        clearable
        class="model-filters__keyword"
        @keyup.enter="loadData"
        @clear="loadData"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-alert v-if="errorMessage" class="model-alert" type="error" :title="errorMessage" show-icon />

    <!-- Empty -->
    <el-empty v-if="!loading && filteredRecords.length === 0" description="暂无模型数据" />

    <!-- Grouped cards by provider -->
    <div v-else class="model-groups">
      <div v-for="group in groupedRecords" :key="group.provider" class="model-group">
        <div class="model-group__header">
          <div class="model-group__title">
            <span class="model-group__icon" :style="{background: providerColor(group.provider)}">
              {{ providerInitial(group.provider) }}
            </span>
            <span class="model-group__name">{{ group.provider }}</span>
            <el-tag size="small" effect="plain" type="info">{{ group.models.length }} 个模型</el-tag>
          </div>
        </div>

        <div class="model-group__body">
          <div
            v-for="model in group.models"
            :key="model.modelId"
            class="model-card"
            :class="{ 'model-card--disabled': model.status !== 'ENABLED', 'model-card--default': model.defaultModel }"
          >
            <div class="model-card__header">
              <div class="model-card__title">
                <h3 class="model-card__name">{{ model.modelName || model.modelCode }}</h3>
                <div class="model-card__tags">
                  <el-tag v-if="model.defaultModel" type="warning" effect="dark" size="small">
                    <el-icon style="margin-right: 2px"><Star /></el-icon>默认
                  </el-tag>
                  <el-tag :type="typeTagType(model.modelType)" effect="plain" size="small">
                    {{ typeLabel(model.modelType) }}
                  </el-tag>
                  <el-tag
                    :type="model.status === 'ENABLED' ? 'success' : 'info'"
                    effect="plain"
                    size="small"
                  >
                    {{ model.status === 'ENABLED' ? '启用中' : '已停用' }}
                  </el-tag>
                </div>
              </div>
              <el-switch
                :model-value="model.status === 'ENABLED'"
                :loading="togglingId === model.modelId"
                @change="(v) => toggleStatus(model, v as boolean)"
              />
            </div>

            <div class="model-card__body">
              <div class="model-card__row">
                <span class="model-card__key">模型标识</span>
                <code class="model-card__value model-card__code">{{ model.modelCode }}</code>
              </div>
              <div class="model-card__row">
                <span class="model-card__key">上下文窗口</span>
                <span class="model-card__value">{{ formatContext(model.contextWindow) }}</span>
              </div>
              <div v-if="model.endpoint" class="model-card__row">
                <span class="model-card__key">接口地址</span>
                <span class="model-card__value model-card__endpoint" :title="model.endpoint">
                  {{ model.endpoint }}
                </span>
              </div>
              <div v-if="model.apiKeyRef || model.apiKeyEncrypted" class="model-card__row">
                <span class="model-card__key">API Key</span>
                <span class="model-card__value model-card__apikey">
                  <code>{{ maskApiKey(model.apiKeyRef) || maskApiKey(model.apiKeyEncrypted) }}</code>
                  <el-icon v-if="apiKeyVisible[model.modelId]" class="model-card__eye" @click="toggleApiKey(model.modelId)">
                    <View />
                  </el-icon>
                  <el-icon v-else class="model-card__eye" @click="toggleApiKey(model.modelId)">
                    <Hide />
                  </el-icon>
                </span>
              </div>
              <div v-if="model.capabilityTags" class="model-card__row">
                <span class="model-card__key">能力标签</span>
                <span class="model-card__value model-card__caps">
                  <el-tag
                    v-for="cap in parseCaps(model.capabilityTags)"
                    :key="cap"
                    size="small"
                    effect="plain"
                  >{{ cap }}</el-tag>
                </span>
              </div>
              <div v-if="model.remark" class="model-card__row">
                <span class="model-card__key">备注</span>
                <span class="model-card__value model-card__remark">{{ model.remark }}</span>
              </div>
            </div>

            <div class="model-card__actions">
              <el-button
                v-if="!model.defaultModel"
                text
                type="warning"
                @click="setAsDefault(model)"
              >
                <el-icon><Star /></el-icon>
                <span>设为默认</span>
              </el-button>
              <el-button text type="primary" @click="openEdit(model)">
                <el-icon><Edit /></el-icon>
                <span>编辑</span>
              </el-button>
              <el-button text type="danger" @click="deleteRow(model)">
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > filters.pageSize"
      class="model-pagination"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="filters.pageSize"
      :current-page="filters.pageNum"
      @current-change="handlePageChange"
    />

    <!-- Edit dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑模型' : '新增模型'"
      width="820px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="厂商" prop="provider">
              <el-select
                v-model="form.provider"
                placeholder="请选择厂商"
                filterable
                allow-create
                style="width: 100%"
                @change="onProviderChange"
              >
                <el-option
                  v-for="p in providerPresets"
                  :key="p.value"
                  :label="p.label"
                  :value="p.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型类型" prop="modelType">
              <el-select v-model="form.modelType" placeholder="请选择类型" style="width: 100%">
                <el-option
                  v-for="t in typeTabs"
                  :key="t.value"
                  :label="t.label"
                  :value="t.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型名称" prop="modelName">
              <el-input v-model="form.modelName" placeholder="例如：通用大模型" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型标识" prop="modelCode">
              <el-input v-model="form.modelCode" placeholder="例如：gpt-4 / qwen-max" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="接口地址" prop="endpoint">
              <el-input
                v-model="form.endpoint"
                placeholder="例如：https://api.openai.com/v1"
                clearable
              >
                <template #append>
                  <el-dropdown @command="applyPresetEndpoint">
                    <el-button>
                      常用预设
                      <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item
                          v-for="p in providerPresets"
                          :key="p.value"
                          :command="p.endpoint"
                        >
                          {{ p.label }} — {{ p.endpoint }}
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="API Key" prop="apiKeyRef">
              <el-input
                v-model="form.apiKeyRef"
                :type="showApiKey ? 'text' : 'password'"
                placeholder="直接粘贴明文 sk-… · 或 ENV:NAME / PROP:path / ${name}"
                clearable
              >
                <template #append>
                  <el-button @click="showApiKey = !showApiKey">
                    <el-icon v-if="showApiKey"><View /></el-icon>
                    <el-icon v-else><Hide /></el-icon>
                  </el-button>
                </template>
              </el-input>
              <div class="model-form__hint">
                <el-tag v-if="apiKeyRefMode === 'plain'" size="small" effect="plain" type="warning">
                  明文模式 · 提交时自动按 VALUE: 存储
                </el-tag>
                <el-tag v-else-if="apiKeyRefMode === 'env'" size="small" effect="plain" type="success">
                  环境变量
                </el-tag>
                <el-tag v-else-if="apiKeyRefMode === 'prop'" size="small" effect="plain" type="primary">
                  Spring 配置
                </el-tag>
                <el-tag v-else-if="apiKeyRefMode === 'placeholder'" size="small" effect="plain" type="info">
                  ${} 占位符
                </el-tag>
                <el-tag v-else-if="apiKeyRefMode === 'value'" size="small" effect="plain" type="info">
                  VALUE: 字面值
                </el-tag>
                <span v-else class="model-form__hint-text">支持直接粘贴明文 Key,自动按字面值存储</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上下文窗口" prop="contextWindow">
              <el-input-number
                v-model="form.contextWindow"
                :min="0"
                :step="1024"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="能力标签">
              <el-select
                v-model="form.capabilityTags"
                multiple
                filterable
                allow-create
                placeholder="选择或输入能力标签"
                style="width: 100%"
              >
                <el-option
                  v-for="cap in capabilityPresets"
                  :key="cap"
                  :label="cap"
                  :value="cap"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="默认模型">
              <el-switch v-model="form.defaultModel" />
              <span class="model-form__hint">同类型仅一个默认</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
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
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {
  ArrowDown, ChatDotRound, Connection, Cpu, DataAnalysis, Delete, Edit,
  Hide, Plus, Refresh, Search, Star, View,
} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {
  deleteAiResource, fetchAiDetail, fetchAiPage, saveAiResource,
} from '@/api/ai'

interface ModelItem {
  modelId: number
  modelName: string
  provider: string
  modelCode: string
  modelType: string
  endpoint: string
  apiKeyRef: string
  apiKeyEncrypted: string
  capabilityTags: string
  contextWindow: number
  defaultModel: boolean
  status: string
  sortOrder: number
  remark: string
}

const typeTabs = [
  { value: 'CHAT', label: '对话模型', icon: ChatDotRound },
  { value: 'EMBEDDING', label: '向量模型', icon: Connection },
  { value: 'RERANK', label: '重排模型', icon: DataAnalysis },
]

// Common provider presets — auto-fill endpoint when selected
const providerPresets = [
  { value: 'OpenAI', label: 'OpenAI', endpoint: 'https://api.openai.com/v1' },
  { value: 'DeepSeek', label: 'DeepSeek', endpoint: 'https://api.deepseek.com/v1' },
  { value: 'Anthropic', label: 'Anthropic', endpoint: 'https://api.anthropic.com/v1' },
  { value: 'Moonshot', label: 'Moonshot (Kimi)', endpoint: 'https://api.moonshot.cn/v1' },
  { value: '阿里通义', label: '阿里通义 (Qwen)', endpoint: 'https://dashscope.aliyuncs.com/compatible-mode/v1' },
  { value: '百度千帆', label: '百度千帆', endpoint: 'https://qianfan.baidubce.com/v2' },
  { value: '智谱', label: '智谱 GLM', endpoint: 'https://open.bigmodel.cn/api/paas/v4' },
  { value: '腾讯混元', label: '腾讯混元', endpoint: 'https://api.hunyuan.tencent.com/v1' },
  { value: 'Ollama', label: 'Ollama (本地)', endpoint: 'http://localhost:11434/v1' },
  { value: 'OpenAI Compatible', label: 'OpenAI 兼容', endpoint: '' },
]

const capabilityPresets = [
  '文本生成', '多轮对话', '函数调用', '长上下文', '流式输出', '视觉理解',
  '文本向量化', '代码生成', 'JSON 输出', '深度思考',
]

const loading = ref(false)
const saving = ref(false)
const togglingId = ref<number | null>(null)
const errorMessage = ref('')
const records = ref<ModelItem[]>([])
const total = ref(0)
const activeType = ref('CHAT')

const filters = reactive({
  provider: '' as string,
  status: '' as string,
  keyword: '',
  pageNum: 1,
  pageSize: 20,
})

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({})
const showApiKey = ref(false)
const apiKeyVisible = reactive<Record<number, boolean>>({})

/**
 * 解析 apiKeyRef 的输入模式,用于在表单中显示提示标签。
 *  - empty:    空
 *  - plain:    明文(没有任何前缀,提交时自动补 VALUE:)
 *  - env:      ENV:NAME
 *  - prop:     PROP:path
 *  - placeholder: ${name}
 *  - value:    VALUE:xxx
 */
const apiKeyRefMode = computed<'empty' | 'plain' | 'env' | 'prop' | 'placeholder' | 'value'>(() => {
  const raw = (form.apiKeyRef || '').trim()
  if (!raw) return 'empty'
  if (raw.startsWith('ENV:')) return 'env'
  if (raw.startsWith('PROP:')) return 'prop'
  if (raw.startsWith('${') && raw.endsWith('}')) return 'placeholder'
  if (raw.startsWith('VALUE:')) return 'value'
  return 'plain'
})

/**
 * 把用户输入规范化成后端能解析的形式:
 *  - 已经带前缀的(ENV:/PROP:/${}/VALUE:)原样返回
 *  - 没有任何前缀的明文,自动补 VALUE: 前缀
 *  - 空字符串返回空
 */
function normalizeApiKeyRef(raw: string | undefined | null): string {
  if (!raw) return ''
  const v = raw.trim()
  if (!v) return ''
  if (v.startsWith('ENV:') || v.startsWith('PROP:') || v.startsWith('VALUE:')) return v
  if (v.startsWith('${') && v.endsWith('}')) return v
  return `VALUE:${v}`
}

const providerOptions = computed(() => {
  const set = new Set<string>()
  records.value.forEach((m) => m.provider && set.add(m.provider))
  providerPresets.forEach((p) => set.add(p.value))
  return Array.from(set).map((v) => ({label: v, value: v}))
})

const filteredRecords = computed(() => records.value)

const groupedRecords = computed(() => {
  const groups = new Map<string, { provider: string; models: ModelItem[] }>()
  for (const m of filteredRecords.value) {
    if (!groups.has(m.provider)) groups.set(m.provider, {provider: m.provider, models: []})
    groups.get(m.provider)!.models.push(m)
  }
  groups.forEach((g) => g.models.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)))
  return Array.from(groups.values()).sort((a, b) => a.provider.localeCompare(b.provider))
})

const stats = computed(() => {
  const s = {total: 0, default: 0, enabled: 0, disabled: 0}
  for (const m of records.value) {
    s.total++
    if (m.defaultModel) s.default++
    if (m.status === 'ENABLED') s.enabled++
    else s.disabled++
  }
  return s
})

const rules = computed<FormRules>(() => ({
  provider: [{required: true, message: '请选择厂商', trigger: 'change'}],
  modelType: [{required: true, message: '请选择类型', trigger: 'change'}],
  modelName: [{required: true, message: '请填写模型名称', trigger: 'blur'}],
  modelCode: [{required: true, message: '请填写模型标识', trigger: 'blur'}],
  status: [{required: true, message: '请选择状态', trigger: 'change'}],
}))

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const payload: Record<string, unknown> = {
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
      keyword: filters.keyword,
      modelType: activeType.value,
    }
    if (filters.provider) payload.provider = filters.provider
    if (filters.status) payload.status = filters.status

    const result = await fetchAiPage<ModelItem>('models', payload as any)
    records.value = result.records || []
    total.value = result.total || records.value.length
  } catch (error) {
    errorMessage.value = '接口不可用，请检查后端服务和权限配置。'
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function typeCount(type: string): number {
  return type === activeType.value ? records.value.length : 0
}

function resetFilters() {
  filters.provider = ''
  filters.status = ''
  filters.keyword = ''
  filters.pageNum = 1
  loadData()
}

function handlePageChange(pageNum: number) {
  filters.pageNum = pageNum
  loadData()
}

function resetForm() {
  Object.keys(form).forEach((k) => delete form[k])
  form.modelType = activeType.value
  form.contextWindow = 8192
  form.defaultModel = false
  form.sortOrder = 0
  form.status = 'ENABLED'
  form.provider = 'OpenAI Compatible'
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: ModelItem) {
  editingId.value = row.modelId
  resetForm()
  try {
    const detail = (await fetchAiDetail('models', row.modelId)) as ModelItem
    Object.assign(form, detail)
  } catch {
    Object.assign(form, row)
  }
  // capabilityTags might be JSON array string or comma-separated; normalize
  if (typeof form.capabilityTags === 'string' && form.capabilityTags) {
    try {
      const parsed = JSON.parse(form.capabilityTags)
      if (Array.isArray(parsed)) form.capabilityTags = parsed
    } catch {
      form.capabilityTags = form.capabilityTags.split(',').map((s: string) => s.trim()).filter(Boolean)
    }
  }
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload: Record<string, unknown> = {...form}
    if (Array.isArray(payload.capabilityTags)) {
      payload.capabilityTags = JSON.stringify(payload.capabilityTags)
    }
    // 用户直接粘贴明文(无前缀)时,自动补 VALUE: 前缀,后端会按字面值使用
    if (typeof payload.apiKeyRef === 'string') {
      payload.apiKeyRef = normalizeApiKeyRef(payload.apiKeyRef)
    }
    await saveAiResource('models', payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function deleteRow(row: ModelItem) {
  await ElMessageBox.confirm(
    `确认删除模型「${row.modelName || row.modelCode}」吗？`,
    '删除确认',
    {type: 'warning'},
  )
  await deleteAiResource('models', [row.modelId])
  ElMessage.success('删除成功')
  await loadData()
}

async function setAsDefault(row: ModelItem) {
  saving.value = true
  try {
    await saveAiResource('models', {...row, defaultModel: true})
    ElMessage.success(`已将「${row.modelName || row.modelCode}」设为${typeLabel(row.modelType)}默认`)
    await loadData()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: ModelItem, enabled: boolean) {
  togglingId.value = row.modelId
  try {
    const newStatus = enabled ? 'ENABLED' : 'DISABLED'
    await saveAiResource('models', {...row, status: newStatus})
    ElMessage.success(enabled ? '已启用' : '已停用')
    await loadData()
  } finally {
    togglingId.value = null
  }
}

function onProviderChange(val: string) {
  const preset = providerPresets.find((p) => p.value === val)
  if (preset?.endpoint && !form.endpoint) {
    form.endpoint = preset.endpoint
  }
}

function applyPresetEndpoint(endpoint: string) {
  if (endpoint) form.endpoint = endpoint
}

function providerInitial(name: string): string {
  if (!name) return '?'
  return name.trim()[0].toUpperCase()
}

function providerColor(name: string): string {
  const palette = [
    'linear-gradient(135deg,#6366f1,#8b5cf6)',
    'linear-gradient(135deg,#10b981,#34d399)',
    'linear-gradient(135deg,#f59e0b,#fbbf24)',
    'linear-gradient(135deg,#ec4899,#f472b6)',
    'linear-gradient(135deg,#06b6d4,#22d3ee)',
    'linear-gradient(135deg,#ef4444,#f87171)',
    'linear-gradient(135deg,#8b5cf6,#a78bfa)',
  ]
  const hash = Array.from(name).reduce((acc, c) => acc + c.charCodeAt(0), 0)
  return palette[hash % palette.length]
}

function typeLabel(t: string) {
  return typeTabs.find((x) => x.value === t)?.label || t
}

function typeTagType(t: string) {
  if (t === 'CHAT') return 'primary'
  if (t === 'EMBEDDING') return 'success'
  if (t === 'RERANK') return 'warning'
  return 'info'
}

function formatContext(n?: number) {
  if (!n) return '-'
  if (n >= 1000000) return (n / 1000000).toFixed(1).replace(/\.0$/, '') + 'M'
  if (n >= 1000) return (n / 1000).toFixed(0) + 'K'
  return String(n)
}

function maskApiKey(key: string) {
  if (!key) return ''
  // Don't mask ENV: references
  if (key.startsWith('ENV:')) return key
  if (key.length <= 8) return '***'
  return key.slice(0, 3) + '***' + key.slice(-4)
}

function toggleApiKey(id: number) {
  apiKeyVisible[id] = !apiKeyVisible[id]
}

function parseCaps(tags: string | string[] | null | undefined): string[] {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  try {
    const parsed = JSON.parse(tags)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return tags.split(',').map((s) => s.trim()).filter(Boolean)
  }
}

onMounted(loadData)
</script>

<style scoped>
.model-page {
  padding: 20px;
}

.model-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 12px;
}
.model-header__title h2 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
  color: var(--tc-text);
}
.model-header__sub {
  font-size: 12px;
  color: var(--tc-text-secondary);
}
.model-header__actions {
  display: flex;
  gap: 8px;
}

.model-tabs {
  margin-bottom: 8px;
}
.model-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}
.model-tab__label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.model-tab__badge {
  margin-left: 4px;
}

.model-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin: 12px 0 16px;
}
.model-stat {
  background: var(--tc-surface);
  border: 1px solid var(--tc-border-light);
  border-left: 3px solid var(--tc-primary);
  border-radius: var(--tc-radius-md);
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.model-stat--primary { border-left-color: var(--tc-warning); }
.model-stat--success { border-left-color: var(--tc-success); }
.model-stat--muted   { border-left-color: var(--tc-text-secondary); }
.model-stat__label {
  font-size: 12px;
  color: var(--tc-text-secondary);
}
.model-stat__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--tc-text);
}

.model-filters {
  display: grid;
  grid-template-columns: 1.2fr 1fr 2fr auto;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}
.model-filters :deep(.el-select),
.model-filters :deep(.el-input) {
  width: 100%;
}

.model-alert {
  margin-bottom: 12px;
}

.model-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.model-group {
  background: var(--tc-surface);
  border: 1px solid var(--tc-border-light);
  border-radius: var(--tc-radius-md);
  overflow: hidden;
}
.model-group__header {
  padding: 12px 16px;
  background: var(--tc-bg-soft, #fafafa);
  border-bottom: 1px solid var(--tc-border-light);
}
.model-group__title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.model-group__icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
}
.model-group__name {
  font-weight: 600;
  color: var(--tc-text);
  font-size: 14px;
}
.model-group__body {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 12px;
  padding: 12px;
}

.model-card {
  background: var(--tc-bg-soft, #fafafa);
  border: 1px solid var(--tc-border-light);
  border-radius: var(--tc-radius-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: all 0.2s ease;
}
.model-card:hover {
  box-shadow: var(--tc-shadow-md);
  transform: translateY(-2px);
}
.model-card--default {
  border-color: var(--tc-warning);
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.04), transparent);
}
.model-card--disabled {
  opacity: 0.55;
}

.model-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}
.model-card__title {
  flex: 1;
  min-width: 0;
}
.model-card__name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: var(--tc-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.model-card__tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.model-card__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 0;
  border-top: 1px dashed var(--tc-border-light);
  border-bottom: 1px dashed var(--tc-border-light);
}
.model-card__row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  line-height: 1.6;
}
.model-card__key {
  flex-shrink: 0;
  width: 76px;
  color: var(--tc-text-secondary);
}
.model-card__value {
  color: var(--tc-text);
  word-break: break-all;
  flex: 1;
  min-width: 0;
}
.model-card__code {
  background: var(--tc-surface);
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid var(--tc-border-light);
  font-size: 12px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}
.model-card__endpoint {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.model-card__apikey {
  display: flex;
  align-items: center;
  gap: 4px;
}
.model-card__apikey code {
  background: var(--tc-surface);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}
.model-card__eye {
  cursor: pointer;
  color: var(--tc-text-secondary);
}
.model-card__eye:hover {
  color: var(--tc-primary);
}
.model-card__caps {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.model-card__remark {
  color: var(--tc-text-secondary);
  font-style: italic;
}

.model-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.model-form__hint {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 6px;
  font-size: 11px;
  color: var(--tc-text-secondary);
}
.model-form__hint-text { color: var(--tc-text-secondary); }

.model-pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 960px) {
  .model-filters {
    grid-template-columns: 1fr 1fr;
  }
  .model-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .model-group__body {
    grid-template-columns: 1fr;
  }
}
</style>
