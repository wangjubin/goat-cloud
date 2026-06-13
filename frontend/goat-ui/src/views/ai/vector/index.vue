<template>
  <div class="vector-page" v-loading="loading">
    <!-- Header -->
    <div class="vector-header">
      <div class="vector-header__title">
        <h2>向量配置</h2>
        <span class="vector-header__sub">向量存储引擎 · 嵌入模型 · 索引参数</span>
      </div>
      <div class="vector-header__actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新增配置</span>
        </el-button>
      </div>
    </div>

    <!-- Stats -->
    <div class="vector-stats">
      <div class="vector-stat">
        <span class="vector-stat__label">配置总数</span>
        <strong class="vector-stat__value">{{ stats.total }}</strong>
      </div>
      <div class="vector-stat vector-stat--primary">
        <span class="vector-stat__label">已启用</span>
        <strong class="vector-stat__value">{{ stats.enabled }}</strong>
      </div>
      <div class="vector-stat vector-stat--muted">
        <span class="vector-stat__label">已停用</span>
        <strong class="vector-stat__value">{{ stats.disabled }}</strong>
      </div>
      <div class="vector-stat vector-stat--success">
        <span class="vector-stat__label">向量表</span>
        <strong class="vector-stat__value">{{ tableCount }}</strong>
      </div>
    </div>

    <!-- Filters -->
    <div class="vector-filters">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索配置名称、向量表或嵌入模型"
        clearable
        @keyup.enter="loadData"
        @clear="loadData"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="filters.status" placeholder="状态" clearable @change="loadData">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-alert v-if="errorMessage" class="vector-alert" type="error" :title="errorMessage" show-icon />

    <el-empty v-if="!loading && records.length === 0" description="暂无向量配置" />

    <div v-else class="vector-grid">
      <div
        v-for="row in records"
        :key="row.vectorConfigId"
        class="vector-card"
        :class="{ 'vector-card--disabled': row.status !== 'ENABLED' }"
      >
        <div class="vector-card__header">
          <div class="vector-card__title">
            <h3 class="vector-card__name">{{ row.configName || row.pgvectorTable }}</h3>
            <el-tag size="small" effect="plain" :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </div>
          <el-switch
            :model-value="row.status === 'ENABLED'"
            :loading="togglingId === row.vectorConfigId"
            @change="(v) => toggleStatus(row, v as boolean)"
          />
        </div>

        <div class="vector-card__engine">
          <div class="vector-card__engine-icon" :style="{background: engineColor(row.provider)}">
            <el-icon :size="18"><Coin /></el-icon>
          </div>
          <div class="vector-card__engine-info">
            <div class="vector-card__engine-name">{{ row.provider || '未指定' }}</div>
            <div class="vector-card__engine-table">
              <code>{{ row.pgvectorTable || '—' }}</code>
            </div>
          </div>
        </div>

        <div class="vector-card__metrics">
          <div class="vector-card__metric">
            <span class="vector-card__metric-label">维度</span>
            <strong class="vector-card__metric-value">{{ row.embeddingDimension || 0 }}</strong>
          </div>
          <div class="vector-card__metric">
            <span class="vector-card__metric-label">距离</span>
            <strong class="vector-card__metric-value">{{ row.distanceMetric || '—' }}</strong>
          </div>
          <div class="vector-card__metric">
            <span class="vector-card__metric-label">索引</span>
            <strong class="vector-card__metric-value">{{ row.indexType || '—' }}</strong>
          </div>
        </div>

        <div class="vector-card__details">
          <div class="vector-card__row">
            <span class="vector-card__key">嵌入模型</span>
            <code class="vector-card__code">{{ row.embeddingModel || '—' }}</code>
          </div>
          <div class="vector-card__row">
            <span class="vector-card__key">切片参数</span>
            <span class="vector-card__value">
              {{ row.chunkSize || 0 }} <span class="vector-card__sep">·</span> 重叠 {{ row.chunkOverlap || 0 }}
            </span>
          </div>
          <div v-if="row.remark" class="vector-card__row">
            <span class="vector-card__key">备注</span>
            <span class="vector-card__value vector-card__remark">{{ row.remark }}</span>
          </div>
        </div>

        <div class="vector-card__actions">
          <el-button text type="primary" @click="openEdit(row)">
            <el-icon><Edit /></el-icon>
            <span>编辑</span>
          </el-button>
          <el-button text type="danger" @click="deleteRow(row)">
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </el-button>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > filters.pageSize"
      class="vector-pagination"
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
      :title="editingId ? '编辑向量配置' : '新增向量配置'"
      width="720px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="配置名称" prop="configName">
              <el-input v-model="form.configName" placeholder="例如：默认向量库" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="存储引擎" prop="provider">
              <el-select v-model="form.provider" placeholder="请选择" style="width: 100%">
                <el-option label="PostgreSQL pgvector" value="POSTGRESQL" />
                <el-option label="Elasticsearch" value="ELASTICSEARCH" />
                <el-option label="Milvus" value="MILVUS" />
                <el-option label="Qdrant" value="QDRANT" />
                <el-option label="Chroma" value="CHROMA" />
                <el-option label="Weaviate" value="WEAVIATE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="嵌入模型" prop="embeddingModel">
              <el-input v-model="form.embeddingModel" placeholder="text-embedding-3-small" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="向量维度" prop="embeddingDimension">
              <el-input-number v-model="form.embeddingDimension" :min="1" :step="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="距离算法" prop="distanceMetric">
              <el-select v-model="form.distanceMetric" placeholder="请选择" style="width: 100%">
                <el-option label="余弦相似度 COSINE" value="COSINE" />
                <el-option label="欧氏距离 L2" value="L2" />
                <el-option label="内积 IP" value="IP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="索引类型" prop="indexType">
              <el-select v-model="form.indexType" placeholder="请选择" style="width: 100%">
                <el-option label="IVFFLAT" value="IVFFLAT" />
                <el-option label="HNSW" value="HNSW" />
                <el-option label="Flat" value="Flat" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="向量表" prop="pgvectorTable">
              <el-input v-model="form.pgvectorTable" placeholder="ai_document_chunk" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="切片长度" prop="chunkSize">
              <el-input-number v-model="form.chunkSize" :min="0" :step="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="切片重叠" prop="chunkOverlap">
              <el-input-number v-model="form.chunkOverlap" :min="0" :step="20" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
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
import {Coin, Delete, Edit, Plus, Refresh, Search} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {deleteAiResource, fetchAiDetail, fetchAiPage, saveAiResource} from '@/api/ai'

interface VectorConfig {
  vectorConfigId: number
  configName: string
  provider: string
  embeddingModel: string
  embeddingDimension: number
  distanceMetric: string
  pgvectorTable: string
  indexType: string
  chunkSize: number
  chunkOverlap: number
  status: string
  remark: string
}

const loading = ref(false)
const saving = ref(false)
const togglingId = ref<number | null>(null)
const errorMessage = ref('')
const records = ref<VectorConfig[]>([])
const total = ref(0)

const filters = reactive({keyword: '', status: '', pageNum: 1, pageSize: 20})
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({})

const stats = computed(() => {
  const s = {total: 0, enabled: 0, disabled: 0}
  for (const r of records.value) {
    s.total++
    if (r.status === 'ENABLED') s.enabled++
    else s.disabled++
  }
  return s
})

const tableCount = computed(() => {
  const set = new Set<string>()
  records.value.forEach((r) => r.pgvectorTable && set.add(r.pgvectorTable))
  return set.size
})

const rules = computed<FormRules>(() => ({
  configName: [{required: true, message: '请填写配置名称', trigger: 'blur'}],
  provider: [{required: true, message: '请选择存储引擎', trigger: 'change'}],
  embeddingModel: [{required: true, message: '请填写嵌入模型', trigger: 'blur'}],
  embeddingDimension: [{required: true, message: '请填写维度', trigger: 'blur'}],
  distanceMetric: [{required: true, message: '请选择距离算法', trigger: 'change'}],
  indexType: [{required: true, message: '请选择索引类型', trigger: 'change'}],
  pgvectorTable: [{required: true, message: '请填写向量表名', trigger: 'blur'}],
}))

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const payload: Record<string, unknown> = {
      pageNum: filters.pageNum,
      pageSize: filters.pageSize,
      keyword: filters.keyword,
    }
    if (filters.status) payload.status = filters.status
    const result = await fetchAiPage<VectorConfig>('vector-configs', payload as any)
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

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  filters.pageNum = 1
  loadData()
}

function handlePageChange(pageNum: number) {
  filters.pageNum = pageNum
  loadData()
}

function resetForm() {
  Object.keys(form).forEach((k) => delete form[k])
  form.provider = 'POSTGRESQL'
  form.embeddingModel = 'text-embedding'
  form.embeddingDimension = 1536
  form.distanceMetric = 'COSINE'
  form.indexType = 'IVFFLAT'
  form.pgvectorTable = 'ai_document_chunk'
  form.chunkSize = 800
  form.chunkOverlap = 120
  form.status = 'ENABLED'
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: VectorConfig) {
  editingId.value = row.vectorConfigId
  resetForm()
  try {
    const detail = (await fetchAiDetail('vector-configs', row.vectorConfigId)) as VectorConfig
    Object.assign(form, detail)
  } catch {
    Object.assign(form, row)
  }
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    await saveAiResource('vector-configs', {...form})
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function deleteRow(row: VectorConfig) {
  await ElMessageBox.confirm(`确认删除「${row.configName}」吗？`, '删除确认', {type: 'warning'})
  await deleteAiResource('vector-configs', [row.vectorConfigId])
  ElMessage.success('删除成功')
  await loadData()
}

async function toggleStatus(row: VectorConfig, enabled: boolean) {
  togglingId.value = row.vectorConfigId
  try {
    await saveAiResource('vector-configs', {...row, status: enabled ? 'ENABLED' : 'DISABLED'})
    ElMessage.success(enabled ? '已启用' : '已停用')
    await loadData()
  } finally {
    togglingId.value = null
  }
}

function engineColor(name: string) {
  const palette = [
    'linear-gradient(135deg,#6366f1,#8b5cf6)',
    'linear-gradient(135deg,#10b981,#34d399)',
    'linear-gradient(135deg,#f59e0b,#fbbf24)',
    'linear-gradient(135deg,#ec4899,#f472b6)',
    'linear-gradient(135deg,#06b6d4,#22d3ee)',
    'linear-gradient(135deg,#ef4444,#f87171)',
  ]
  const hash = Array.from(name || '').reduce((acc, c) => acc + c.charCodeAt(0), 0)
  return palette[hash % palette.length]
}

onMounted(loadData)
</script>

<style scoped>
.vector-page {
  padding: 20px;
}

.vector-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
}
.vector-header__title h2 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
  color: var(--tc-text);
}
.vector-header__sub {
  font-size: 12px;
  color: var(--tc-text-secondary);
}
.vector-header__actions {
  display: flex;
  gap: 8px;
}

.vector-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.vector-stat {
  background: var(--tc-surface);
  border: 1px solid var(--tc-border-light);
  border-left: 3px solid var(--tc-primary);
  border-radius: var(--tc-radius-md);
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.vector-stat--primary { border-left-color: var(--tc-success); }
.vector-stat--muted   { border-left-color: var(--tc-text-secondary); }
.vector-stat--success { border-left-color: #06b6d4; }
.vector-stat__label { font-size: 12px; color: var(--tc-text-secondary); }
.vector-stat__value { font-size: 22px; font-weight: 700; color: var(--tc-text); }

.vector-filters {
  display: grid;
  grid-template-columns: 2fr 1fr auto;
  gap: 8px;
  margin-bottom: 16px;
}
.vector-filters :deep(.el-select),
.vector-filters :deep(.el-input) { width: 100%; }

.vector-alert { margin-bottom: 12px; }

.vector-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 14px;
}

.vector-card {
  background: var(--tc-surface);
  border: 1px solid var(--tc-border-light);
  border-radius: var(--tc-radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: all 0.2s ease;
}
.vector-card:hover {
  box-shadow: var(--tc-shadow-md);
  transform: translateY(-2px);
}
.vector-card--disabled { opacity: 0.6; }

.vector-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}
.vector-card__title { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.vector-card__name {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--tc-text);
}

.vector-card__engine {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--tc-bg-soft, #fafafa);
  border-radius: var(--tc-radius-sm);
}
.vector-card__engine-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.vector-card__engine-info { min-width: 0; flex: 1; }
.vector-card__engine-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--tc-text);
}
.vector-card__engine-table {
  font-size: 11px;
  color: var(--tc-text-secondary);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.vector-card__engine-table code {
  background: var(--tc-surface);
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 11px;
}

.vector-card__metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 8px 0;
  border-top: 1px dashed var(--tc-border-light);
  border-bottom: 1px dashed var(--tc-border-light);
}
.vector-card__metric {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.vector-card__metric-label {
  font-size: 11px;
  color: var(--tc-text-secondary);
}
.vector-card__metric-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--tc-text);
}

.vector-card__details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.vector-card__row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  line-height: 1.6;
}
.vector-card__key {
  flex-shrink: 0;
  width: 76px;
  color: var(--tc-text-secondary);
}
.vector-card__value {
  color: var(--tc-text);
  flex: 1;
  min-width: 0;
  word-break: break-all;
}
.vector-card__code {
  background: var(--tc-bg-soft, #fafafa);
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
}
.vector-card__sep {
  margin: 0 4px;
  color: var(--tc-text-secondary);
}
.vector-card__remark {
  color: var(--tc-text-secondary);
  font-style: italic;
}

.vector-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.vector-pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 960px) {
  .vector-filters { grid-template-columns: 1fr 1fr; }
  .vector-stats { grid-template-columns: repeat(2, 1fr); }
  .vector-grid { grid-template-columns: 1fr; }
}
</style>
