<template>
  <div class="bill-page" v-loading="loading">
    <div class="bill-header">
      <div class="bill-header__title">
        <h2>账单统计</h2>
        <span class="bill-header__sub">Token 消耗 · 费用趋势 · 调用明细</span>
      </div>
      <div class="bill-header__actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon><span>刷新</span>
        </el-button>
        <el-button @click="exportRecords">
          <el-icon><Download /></el-icon><span>导出</span>
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon><span>新增账单</span>
        </el-button>
      </div>
    </div>

    <div class="bill-stats">
      <div class="bill-stat bill-stat--primary">
        <span class="bill-stat__label">累计费用 ({{ stats.currency }})</span>
        <strong class="bill-stat__value">{{ formatCost(stats.totalCost) }}</strong>
        <span class="bill-stat__hint">共 {{ stats.records }} 条记录</span>
      </div>
      <div class="bill-stat bill-stat--success">
        <span class="bill-stat__label">累计 Token</span>
        <strong class="bill-stat__value">{{ formatNumber(stats.totalTokens) }}</strong>
        <span class="bill-stat__hint">输入 {{ formatNumber(stats.promptTokens) }} · 输出 {{ formatNumber(stats.completionTokens) }}</span>
      </div>
      <div class="bill-stat bill-stat--warning">
        <span class="bill-stat__label">调用次数</span>
        <strong class="bill-stat__value">{{ stats.calls }}</strong>
        <span class="bill-stat__hint">均次 {{ formatCost(stats.avgCost) }} / {{ formatNumber(stats.avgTokens) }} tok</span>
      </div>
      <div class="bill-stat bill-stat--info">
        <span class="bill-stat__label">成功率</span>
        <strong class="bill-stat__value">{{ successRate }}%</strong>
        <span class="bill-stat__hint">成功 {{ stats.success }} · 失败 {{ stats.failed }}</span>
      </div>
    </div>

    <div v-if="topProviders.length" class="bill-top">
      <span class="bill-top__label">TOP 厂商</span>
      <div class="bill-top__list">
        <div v-for="p in topProviders" :key="p.name" class="bill-top__item">
          <span class="bill-top__name">{{ p.name }}</span>
          <div class="bill-top__bar">
            <div class="bill-top__bar-fill" :style="{width: p.percent + '%', background: p.color}"></div>
          </div>
          <span class="bill-top__value">{{ formatCost(p.cost) }}</span>
        </div>
      </div>
    </div>

    <div class="bill-filters">
      <el-input v-model="filters.keyword" placeholder="搜索会话、模型或调用方" clearable
        @keyup.enter="loadData" @clear="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filters.provider" placeholder="厂商" clearable @change="loadData">
        <el-option v-for="p in providerOptions" :key="p" :label="p" :value="p" />
      </el-select>
      <el-select v-model="filters.bizType" placeholder="调用方" clearable @change="loadData">
        <el-option label="AI 助手" value="AI_ASSISTANT" />
        <el-option label="RAG 检索" value="RAG" />
        <el-option label="Agent" value="AGENT" />
        <el-option label="Embedding" value="EMBEDDING" />
        <el-option label="Workflow" value="WORKFLOW" />
        <el-option label="代码生成" value="CODE" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable @change="loadData">
        <el-option label="成功" value="SUCCESS" />
        <el-option label="失败" value="FAILED" />
      </el-select>
      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="loadData"
      />
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-alert v-if="errorMessage" class="bill-alert" type="error" :title="errorMessage" show-icon />
    <el-empty v-if="!loading && records.length === 0" description="暂无账单记录" />

    <div v-else class="bill-list">
      <el-table :data="records" stripe style="width: 100%">
        <el-table-column label="会话" min-width="180">
          <template #default="{row}">
            <code class="bill-list__id" :title="row.conversationId">{{ shortId(row.conversationId) }}</code>
          </template>
        </el-table-column>
        <el-table-column label="厂商" width="130">
          <template #default="{row}">
            <span class="bill-list__provider" :style="{background: providerColor(row.provider)}">
              {{ row.provider || '—' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="模型" min-width="160">
          <template #default="{row}">
            <code class="bill-list__model">{{ row.modelCode || '—' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="调用方" width="120">
          <template #default="{row}">
            <el-tag size="small" effect="plain" :type="bizTagType(row.bizType)">
              {{ bizLabel(row.bizType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Token 消耗" min-width="220">
          <template #default="{row}">
            <div class="bill-list__tokens">
              <div class="bill-list__tokens-bar">
                <div class="bill-list__tokens-input" :style="{flex: row.promptTokens || 0}"></div>
                <div class="bill-list__tokens-output" :style="{flex: row.completionTokens || 0}"></div>
              </div>
              <div class="bill-list__tokens-legend">
                <span><i class="bill-list__dot bill-list__dot--input"></i>入 {{ formatNumber(row.promptTokens) }}</span>
                <span><i class="bill-list__dot bill-list__dot--output"></i>出 {{ formatNumber(row.completionTokens) }}</span>
                <strong>{{ formatNumber(row.totalTokens) }}</strong>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="费用" width="120" align="right">
          <template #default="{row}">
            <strong class="bill-list__cost">{{ formatCost(row.costAmount) }}</strong>
            <span class="bill-list__currency">{{ row.currency || 'CNY' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="请求时间" width="170">
          <template #default="{row}">
            <span class="bill-list__time">{{ formatTime(row.requestTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{row}">
            <el-tag size="small" effect="plain" :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
              <el-icon style="margin-right:2px">
                <CircleCheck v-if="row.status === 'SUCCESS'" /><CircleClose v-else />
              </el-icon>
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{row}">
            <el-button text type="primary" @click="openEdit(row)">详情</el-button>
            <el-button text type="danger" @click="deleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination
      v-if="total > filters.pageSize"
      class="bill-pagination"
      background
      layout="total, prev, pager, next, jumper"
      :total="total"
      :page-size="filters.pageSize"
      :current-page="filters.pageNum"
      @current-change="handlePageChange"
    />

    <el-dialog v-model="dialogVisible" :title="editingId ? '账单详情' : '新增账单'"
      width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="会话 ID" prop="conversationId">
              <el-input v-model="form.conversationId" placeholder="会话唯一标识" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="厂商" prop="provider">
              <el-select v-model="form.provider" allow-create filterable placeholder="请选择或输入" style="width:100%">
                <el-option v-for="p in providerOptions" :key="p" :label="p" :value="p" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型编码" prop="modelCode">
              <el-input v-model="form.modelCode" placeholder="例如 gpt-4o-mini" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调用方" prop="bizType">
              <el-select v-model="form.bizType" placeholder="请选择" style="width:100%">
                <el-option label="AI 助手" value="AI_ASSISTANT" />
                <el-option label="RAG 检索" value="RAG" />
                <el-option label="Agent" value="AGENT" />
                <el-option label="Embedding" value="EMBEDDING" />
                <el-option label="Workflow" value="WORKFLOW" />
                <el-option label="代码生成" value="CODE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="输入 Token" prop="promptTokens">
              <el-input-number v-model="form.promptTokens" :min="0" :step="1" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="输出 Token" prop="completionTokens">
              <el-input-number v-model="form.completionTokens" :min="0" :step="1" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总 Token" prop="totalTokens">
              <el-input-number v-model="form.totalTokens" :min="0" :step="1" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用" prop="costAmount">
              <el-input-number v-model="form.costAmount" :min="0" :precision="4" :step="0.0001" controls-position="right" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="币种">
              <el-select v-model="form.currency" placeholder="请选择" style="width:100%">
                <el-option label="人民币 CNY" value="CNY" />
                <el-option label="美元 USD" value="USD" />
                <el-option label="欧元 EUR" value="EUR" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="请求时间" prop="requestTime">
              <el-date-picker v-model="form.requestTime" type="datetime" placeholder="选择时间"
                value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio-button value="SUCCESS">成功</el-radio-button>
                <el-radio-button value="FAILED">失败</el-radio-button>
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
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {CircleCheck, CircleClose, Delete, Download, Edit, Plus, Refresh, Search} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {deleteAiResource, fetchAiPage, saveAiResource} from '@/api/ai'

interface Billing {
  billingId: number; conversationId: string; provider: string; modelCode: string;
  bizType: string; promptTokens: number; completionTokens: number; totalTokens: number;
  costAmount: number; currency: string; requestTime: string; status: string; remark: string
}

const providerOptions = ['OpenAI', 'DeepSeek', 'Anthropic', 'Moonshot', '通义千问', '百度千帆', '智谱', '腾讯混元', 'Ollama', 'OpenAI Compatible']

const loading = ref(false); const saving = ref(false)
const errorMessage = ref(''); const records = ref<Billing[]>([]); const total = ref(0)
const filters = reactive({keyword: '', provider: '', bizType: '', status: '', dateRange: [] as string[], pageNum: 1, pageSize: 20})
const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>(); const form = reactive<Record<string, any>>({})

const stats = computed(() => {
  const s = {totalCost: 0, totalTokens: 0, promptTokens: 0, completionTokens: 0, calls: 0, success: 0, failed: 0, records: 0, currency: 'CNY', avgCost: 0, avgTokens: 0}
  s.records = records.value.length
  for (const r of records.value) {
    s.totalCost += Number(r.costAmount) || 0
    s.totalTokens += Number(r.totalTokens) || 0
    s.promptTokens += Number(r.promptTokens) || 0
    s.completionTokens += Number(r.completionTokens) || 0
    s.calls++
    if (r.status === 'SUCCESS') s.success++; else s.failed++
    if (r.currency) s.currency = r.currency
  }
  s.avgCost = s.calls > 0 ? s.totalCost / s.calls : 0
  s.avgTokens = s.calls > 0 ? s.totalTokens / s.calls : 0
  return s
})

const successRate = computed(() => {
  if (stats.value.calls === 0) return '0.0'
  return ((stats.value.success / stats.value.calls) * 100).toFixed(1)
})

const topProviders = computed(() => {
  const map = new Map<string, number>()
  for (const r of records.value) {
    const k = r.provider || '未知'
    map.set(k, (map.get(k) || 0) + (Number(r.costAmount) || 0))
  }
  const list = Array.from(map.entries()).map(([name, cost]) => ({name, cost})).sort((a, b) => b.cost - a.cost).slice(0, 5)
  const max = list[0]?.cost || 1
  return list.map((p, i) => ({...p, percent: Math.round((p.cost / max) * 100), color: providerColor(p.name)}))
})

const rules = computed<FormRules>(() => ({
  conversationId: [{required: true, message: '请填写会话 ID', trigger: 'blur'}],
  provider: [{required: true, message: '请填写厂商', trigger: 'change'}],
  modelCode: [{required: true, message: '请填写模型编码', trigger: 'blur'}],
  bizType: [{required: true, message: '请选择调用方', trigger: 'change'}],
  costAmount: [{required: true, message: '请填写费用', trigger: 'blur'}],
  status: [{required: true, message: '请选择状态', trigger: 'change'}],
}))

async function loadData() {
  loading.value = true; errorMessage.value = ''
  try {
    const payload: Record<string, unknown> = {pageNum: filters.pageNum, pageSize: filters.pageSize, keyword: filters.keyword}
    if (filters.provider) payload.provider = filters.provider
    if (filters.bizType) payload.bizType = filters.bizType
    if (filters.status) payload.status = filters.status
    if (filters.dateRange && filters.dateRange.length === 2) {
      payload.startDate = filters.dateRange[0]
      payload.endDate = filters.dateRange[1]
    }
    const result = await fetchAiPage<Billing>('billing', payload as any)
    records.value = result.records || []
    total.value = result.total || records.value.length
  } catch {
    errorMessage.value = '接口不可用，请检查后端服务和权限配置。'
    records.value = []; total.value = 0
  } finally { loading.value = false }
}

function resetFilters() {
  filters.keyword = ''; filters.provider = ''; filters.bizType = ''; filters.status = ''; filters.dateRange = []; filters.pageNum = 1; loadData()
}
function handlePageChange(p: number) { filters.pageNum = p; loadData() }
function resetForm() {
  Object.keys(form).forEach(k => delete form[k])
  form.provider = 'OpenAI Compatible'; form.bizType = 'AI_ASSISTANT'
  form.promptTokens = 0; form.completionTokens = 0; form.totalTokens = 0
  form.costAmount = 0; form.currency = 'CNY'; form.status = 'SUCCESS'
}
function openCreate() { editingId.value = null; resetForm(); dialogVisible.value = true }
function openEdit(row: Billing) {
  editingId.value = row.billingId; resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}
async function submitForm() {
  await formRef.value?.validate(); saving.value = true
  try {
    const payload = {...form}
    if (editingId.value) payload.billingId = editingId.value
    await saveAiResource('billing', payload)
    ElMessage.success('保存成功'); dialogVisible.value = false; await loadData()
  } finally { saving.value = false }
}
async function deleteRow(row: Billing) {
  await ElMessageBox.confirm(`确认删除该账单记录吗？`, '删除确认', {type: 'warning'})
  await deleteAiResource('billing', [row.billingId])
  ElMessage.success('删除成功'); await loadData()
}

function exportRecords() {
  if (!records.value.length) { ElMessage.warning('暂无可导出数据'); return }
  const header = ['会话ID', '厂商', '模型', '调用方', '输入Token', '输出Token', '总Token', '费用', '币种', '状态', '请求时间']
  const rows = records.value.map(r => [
    r.conversationId, r.provider, r.modelCode, r.bizType,
    r.promptTokens, r.completionTokens, r.totalTokens,
    r.costAmount, r.currency, r.status, r.requestTime,
  ])
  const csv = [header, ...rows].map(line => line.map(c => `"${(c ?? '').toString().replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + csv], {type: 'text/csv;charset=utf-8'})
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `billing-${new Date().toISOString().slice(0, 10)}.csv`
  a.click(); URL.revokeObjectURL(url)
  ElMessage.success('导出完成')
}

function formatCost(v: number | string | undefined) {
  const n = Number(v) || 0
  if (n === 0) return '0.00'
  if (n < 0.01) return n.toFixed(4)
  return n.toFixed(2)
}
function formatNumber(v: number | string | undefined) {
  const n = Number(v) || 0
  return n.toLocaleString('zh-CN')
}
function shortId(id: string) {
  if (!id) return '—'
  return id.length > 16 ? id.slice(0, 8) + '…' + id.slice(-4) : id
}
function formatTime(t: string) {
  if (!t) return '—'
  try { return new Date(t).toLocaleString('zh-CN', {hour12: false}) } catch { return t }
}
function bizLabel(t: string) {
  return {AI_ASSISTANT: 'AI 助手', RAG: 'RAG', AGENT: 'Agent', EMBEDDING: 'Embedding', WORKFLOW: 'Workflow', CODE: '代码'}[t] || t || '—'
}
function bizTagType(t: string) {
  return {AI_ASSISTANT: 'primary', RAG: 'success', AGENT: 'warning', EMBEDDING: 'info', WORKFLOW: 'danger', CODE: 'info'}[t] || 'info'
}
function providerColor(p: string) {
  const palette: Record<string, string> = {
    OpenAI: 'linear-gradient(135deg,#10a37f,#10b981)',
    DeepSeek: 'linear-gradient(135deg,#4f46e5,#7c3aed)',
    Anthropic: 'linear-gradient(135deg,#d97706,#f59e0b)',
    Moonshot: 'linear-gradient(135deg,#1e293b,#475569)',
    通义千问: 'linear-gradient(135deg,#ff6a00,#fb923c)',
    百度千帆: 'linear-gradient(135deg,#2563eb,#3b82f6)',
    智谱: 'linear-gradient(135deg,#3b82f6,#06b6d4)',
    腾讯混元: 'linear-gradient(135deg,#0ea5e9,#22d3ee)',
    Ollama: 'linear-gradient(135deg,#374151,#6b7280)',
  }
  return palette[p] || 'linear-gradient(135deg,#6366f1,#8b5cf6)'
}

onMounted(loadData)
</script>

<style scoped>
.bill-page { padding: 20px; }
.bill-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.bill-header__title h2 { margin: 0 0 4px; font-size: 20px; font-weight: 700; color: var(--tc-text); }
.bill-header__sub { font-size: 12px; color: var(--tc-text-secondary); }
.bill-header__actions { display: flex; gap: 8px; }

.bill-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.bill-stat { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-left: 3px solid var(--tc-primary); border-radius: var(--tc-radius-md); padding: 14px 18px; display: flex; flex-direction: column; gap: 4px; }
.bill-stat--primary { border-left-color: var(--tc-primary); }
.bill-stat--success { border-left-color: var(--tc-success); }
.bill-stat--warning { border-left-color: #f59e0b; }
.bill-stat--info { border-left-color: #06b6d4; }
.bill-stat__label { font-size: 12px; color: var(--tc-text-secondary); }
.bill-stat__value { font-size: 24px; font-weight: 700; color: var(--tc-text); font-family: 'SFMono-Regular', Consolas, monospace; }
.bill-stat__hint { font-size: 11px; color: var(--tc-text-secondary); }

.bill-top { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-radius: var(--tc-radius-md); padding: 12px 16px; margin-bottom: 16px; }
.bill-top__label { display: block; font-size: 12px; color: var(--tc-text-secondary); margin-bottom: 8px; font-weight: 600; }
.bill-top__list { display: flex; flex-direction: column; gap: 6px; }
.bill-top__item { display: flex; align-items: center; gap: 12px; font-size: 12px; }
.bill-top__name { width: 100px; color: var(--tc-text-secondary); flex-shrink: 0; }
.bill-top__bar { flex: 1; height: 8px; background: var(--tc-bg-soft, #f5f5f5); border-radius: 4px; overflow: hidden; }
.bill-top__bar-fill { height: 100%; border-radius: 4px; transition: width 0.4s ease; }
.bill-top__value { width: 80px; text-align: right; font-weight: 600; font-family: 'SFMono-Regular', Consolas, monospace; }

.bill-filters { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr 1.4fr auto; gap: 8px; margin-bottom: 16px; }
.bill-filters :deep(.el-select), .bill-filters :deep(.el-input), .bill-filters :deep(.el-date-editor) { width: 100%; }

.bill-alert { margin-bottom: 12px; }

.bill-list { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-radius: var(--tc-radius-md); padding: 4px; }
.bill-list__id { font-size: 11px; color: var(--tc-text-secondary); background: var(--tc-bg-soft, #fafafa); padding: 2px 6px; border-radius: 3px; font-family: 'SFMono-Regular', Consolas, monospace; }
.bill-list__provider { display: inline-block; padding: 3px 10px; border-radius: 4px; color: #fff; font-size: 11px; font-weight: 600; }
.bill-list__model { font-size: 12px; color: var(--tc-text); background: var(--tc-bg-soft, #fafafa); padding: 2px 8px; border-radius: 3px; font-family: 'SFMono-Regular', Consolas, monospace; }
.bill-list__tokens { display: flex; flex-direction: column; gap: 4px; }
.bill-list__tokens-bar { display: flex; height: 6px; background: var(--tc-bg-soft, #fafafa); border-radius: 3px; overflow: hidden; }
.bill-list__tokens-input { background: #6366f1; }
.bill-list__tokens-output { background: #f59e0b; }
.bill-list__tokens-legend { display: flex; gap: 8px; font-size: 11px; color: var(--tc-text-secondary); align-items: center; }
.bill-list__tokens-legend strong { color: var(--tc-text); margin-left: auto; font-family: 'SFMono-Regular', Consolas, monospace; }
.bill-list__dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 3px; vertical-align: middle; }
.bill-list__dot--input { background: #6366f1; }
.bill-list__dot--output { background: #f59e0b; }
.bill-list__cost { font-size: 14px; color: var(--tc-text); font-family: 'SFMono-Regular', Consolas, monospace; }
.bill-list__currency { font-size: 11px; color: var(--tc-text-secondary); margin-left: 4px; }
.bill-list__time { font-size: 12px; color: var(--tc-text-secondary); font-family: 'SFMono-Regular', Consolas, monospace; }

.bill-pagination { justify-content: flex-end; margin-top: 16px; }

@media (max-width: 1100px) {
  .bill-filters { grid-template-columns: 1fr 1fr 1fr; }
}
@media (max-width: 960px) {
  .bill-stats { grid-template-columns: repeat(2, 1fr); }
  .bill-filters { grid-template-columns: 1fr 1fr; }
}
</style>
