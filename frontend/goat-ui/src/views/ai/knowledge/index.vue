<template>
  <div class="kb-page" v-loading="loading">
    <!-- Header -->
    <div class="kb-header">
      <div class="kb-header__title">
        <h2>RAG 知识库</h2>
        <span class="kb-header__sub">文档检索增强 · 向量索引 · 智能问答</span>
      </div>
      <div class="kb-header__actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新建知识库</span>
        </el-button>
      </div>
    </div>

    <!-- Stats -->
    <div class="kb-stats">
      <div class="kb-stat">
        <span class="kb-stat__label">知识库</span>
        <strong class="kb-stat__value">{{ stats.total }}</strong>
      </div>
      <div class="kb-stat kb-stat--primary">
        <span class="kb-stat__label">文档总数</span>
        <strong class="kb-stat__value">{{ stats.documents }}</strong>
      </div>
      <div class="kb-stat kb-stat--success">
        <span class="kb-stat__label">切片总数</span>
        <strong class="kb-stat__value">{{ stats.chunks }}</strong>
      </div>
      <div class="kb-stat kb-stat--muted">
        <span class="kb-stat__label">已启用</span>
        <strong class="kb-stat__value">{{ stats.enabled }}</strong>
      </div>
    </div>

    <!-- Filters -->
    <div class="kb-filters">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索知识库名称或编码"
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

    <el-alert v-if="errorMessage" class="kb-alert" type="error" :title="errorMessage" show-icon />

    <el-empty v-if="!loading && records.length === 0" description="暂无知识库" />

    <div v-else class="kb-grid">
      <div
        v-for="kb in records"
        :key="kb.knowledgeBaseId"
        class="kb-card"
        :class="{ 'kb-card--disabled': kb.status !== 'ENABLED' }"
      >
        <div class="kb-card__header">
          <div class="kb-card__title">
            <div class="kb-card__icon" :style="{background: gradientByCode(kb.knowledgeBaseCode)}">
              <el-icon :size="22"><Collection /></el-icon>
            </div>
            <div class="kb-card__title-text">
              <h3 class="kb-card__name">{{ kb.knowledgeBaseName }}</h3>
              <code class="kb-card__code">{{ kb.knowledgeBaseCode }}</code>
            </div>
          </div>
          <el-tag size="small" effect="plain" :type="kb.status === 'ENABLED' ? 'success' : 'info'">
            {{ kb.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </div>

        <div v-if="kb.description" class="kb-card__desc">{{ kb.description }}</div>

        <div class="kb-card__stats">
          <div class="kb-card__stat">
            <div class="kb-card__stat-value">{{ kb.documentCount || 0 }}</div>
            <div class="kb-card__stat-label">文档</div>
          </div>
          <div class="kb-card__stat-divider"></div>
          <div class="kb-card__stat">
            <div class="kb-card__stat-value">{{ kb.chunkCount || 0 }}</div>
            <div class="kb-card__stat-label">切片</div>
          </div>
          <div class="kb-card__stat-divider"></div>
          <div class="kb-card__stat">
            <div class="kb-card__stat-value">{{ kb.embeddingDimension || 0 }}</div>
            <div class="kb-card__stat-label">维度</div>
          </div>
        </div>

        <div class="kb-card__config">
          <div class="kb-card__row">
            <span class="kb-card__key">嵌入模型</span>
            <code class="kb-card__value kb-card__code-inline">{{ kb.embeddingModel || '—' }}</code>
          </div>
          <div v-if="kb.retrievalConfig" class="kb-card__row">
            <span class="kb-card__key">检索配置</span>
            <span class="kb-card__value">{{ kb.retrievalConfig }}</span>
          </div>
        </div>

        <div v-if="kb.remark" class="kb-card__remark">{{ kb.remark }}</div>

        <div class="kb-card__actions">
          <el-button text type="primary" @click="openEdit(kb)">
            <el-icon><Edit /></el-icon>
            <span>编辑</span>
          </el-button>
          <el-button text type="danger" @click="deleteRow(kb)">
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </el-button>
        </div>
      </div>
    </div>

    <el-pagination
      v-if="total > filters.pageSize"
      class="kb-pagination"
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
      :title="editingId ? '编辑知识库' : '新建知识库'"
      width="720px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="知识库名称" prop="knowledgeBaseName">
              <el-input v-model="form.knowledgeBaseName" placeholder="例如：产品手册" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="知识库编码" prop="knowledgeBaseCode">
              <el-input v-model="form.knowledgeBaseCode" placeholder="例如：product-manual" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选：知识库用途说明" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="向量配置" prop="vectorConfigId">
              <el-select
                v-model="form.vectorConfigId"
                placeholder="请选择向量配置"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="vc in vectorConfigs"
                  :key="vc.vectorConfigId"
                  :label="vc.configName"
                  :value="vc.vectorConfigId"
                />
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
              <el-input-number v-model="form.embeddingDimension" :min="0" :step="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio-button value="ENABLED">启用</el-radio-button>
                <el-radio-button value="DISABLED">停用</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="检索配置">
              <el-input
                v-model="form.retrievalConfig"
                type="textarea"
                :rows="2"
                placeholder='例如 {"topK":5,"threshold":0.7}'
              />
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
import {Collection, Delete, Edit, Plus, Refresh, Search} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {deleteAiResource, fetchAiDetail, fetchAiList, fetchAiPage, saveAiResource} from '@/api/ai'

interface KnowledgeBase {
  knowledgeBaseId: number
  knowledgeBaseName: string
  knowledgeBaseCode: string
  description: string
  vectorConfigId: number
  embeddingModel: string
  embeddingDimension: number
  documentCount: number
  chunkCount: number
  retrievalConfig: string
  status: string
  remark: string
}
interface VectorConfigItem {
  vectorConfigId: number
  configName: string
}

const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const records = ref<KnowledgeBase[]>([])
const total = ref(0)
const vectorConfigs = ref<VectorConfigItem[]>([])

const filters = reactive({keyword: '', status: '', pageNum: 1, pageSize: 20})
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({})

const stats = computed(() => {
  const s = {total: 0, documents: 0, chunks: 0, enabled: 0}
  for (const r of records.value) {
    s.total++
    s.documents += r.documentCount || 0
    s.chunks += r.chunkCount || 0
    if (r.status === 'ENABLED') s.enabled++
  }
  return s
})

const rules = computed<FormRules>(() => ({
  knowledgeBaseName: [{required: true, message: '请填写名称', trigger: 'blur'}],
  knowledgeBaseCode: [{required: true, message: '请填写编码', trigger: 'blur'}],
  vectorConfigId: [{required: true, message: '请选择向量配置', trigger: 'change'}],
  embeddingModel: [{required: true, message: '请填写嵌入模型', trigger: 'blur'}],
  embeddingDimension: [{required: true, message: '请填写维度', trigger: 'blur'}],
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
    const result = await fetchAiPage<KnowledgeBase>('knowledge-bases', payload as any)
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

async function loadVectorConfigs() {
  try {
    const list = await fetchAiList<any>('vector-configs')
    vectorConfigs.value = (list || []).map((v) => ({
      vectorConfigId: v.vectorConfigId,
      configName: v.configName,
    }))
  } catch (error) {
    console.error('Failed to load vector configs', error)
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
  form.embeddingDimension = 1536
  form.status = 'ENABLED'
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: KnowledgeBase) {
  editingId.value = row.knowledgeBaseId
  resetForm()
  try {
    const detail = (await fetchAiDetail('knowledge-bases', row.knowledgeBaseId)) as KnowledgeBase
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
    await saveAiResource('knowledge-bases', {...form})
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function deleteRow(row: KnowledgeBase) {
  await ElMessageBox.confirm(
    `确认删除知识库「${row.knowledgeBaseName}」吗？该知识库下的文档、切片数据不会被自动删除。`,
    '删除确认',
    {type: 'warning'},
  )
  await deleteAiResource('knowledge-bases', [row.knowledgeBaseId])
  ElMessage.success('删除成功')
  await loadData()
}

function gradientByCode(code: string) {
  const palette = [
    'linear-gradient(135deg,#6366f1,#8b5cf6)',
    'linear-gradient(135deg,#10b981,#34d399)',
    'linear-gradient(135deg,#f59e0b,#fbbf24)',
    'linear-gradient(135deg,#ec4899,#f472b6)',
    'linear-gradient(135deg,#06b6d4,#22d3ee)',
    'linear-gradient(135deg,#ef4444,#f87171)',
    'linear-gradient(135deg,#8b5cf6,#a78bfa)',
  ]
  const hash = Array.from(code || '').reduce((acc, c) => acc + c.charCodeAt(0), 0)
  return palette[hash % palette.length]
}

onMounted(async () => {
  await loadVectorConfigs()
  await loadData()
})
</script>

<style scoped>
.kb-page { padding: 20px; }

.kb-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
}
.kb-header__title h2 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
  color: var(--tc-text);
}
.kb-header__sub {
  font-size: 12px;
  color: var(--tc-text-secondary);
}
.kb-header__actions { display: flex; gap: 8px; }

.kb-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.kb-stat {
  background: var(--tc-surface);
  border: 1px solid var(--tc-border-light);
  border-left: 3px solid var(--tc-primary);
  border-radius: var(--tc-radius-md);
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kb-stat--primary { border-left-color: var(--tc-primary); }
.kb-stat--success { border-left-color: var(--tc-success); }
.kb-stat--muted   { border-left-color: var(--tc-text-secondary); }
.kb-stat__label { font-size: 12px; color: var(--tc-text-secondary); }
.kb-stat__value { font-size: 22px; font-weight: 700; color: var(--tc-text); }

.kb-filters {
  display: grid;
  grid-template-columns: 2fr 1fr auto;
  gap: 8px;
  margin-bottom: 16px;
}
.kb-filters :deep(.el-select),
.kb-filters :deep(.el-input) { width: 100%; }

.kb-alert { margin-bottom: 12px; }

.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 14px;
}

.kb-card {
  background: var(--tc-surface);
  border: 1px solid var(--tc-border-light);
  border-radius: var(--tc-radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: all 0.2s ease;
}
.kb-card:hover {
  box-shadow: var(--tc-shadow-md);
  transform: translateY(-2px);
}
.kb-card--disabled { opacity: 0.6; }

.kb-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}
.kb-card__title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}
.kb-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.kb-card__title-text { min-width: 0; flex: 1; }
.kb-card__name {
  margin: 0 0 2px;
  font-size: 15px;
  font-weight: 700;
  color: var(--tc-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kb-card__code {
  font-size: 11px;
  color: var(--tc-text-secondary);
  background: var(--tc-bg-soft, #fafafa);
  padding: 1px 6px;
  border-radius: 3px;
}

.kb-card__desc {
  font-size: 12px;
  color: var(--tc-text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kb-card__stats {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 10px 0;
  background: var(--tc-bg-soft, #fafafa);
  border-radius: var(--tc-radius-sm);
}
.kb-card__stat {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.kb-card__stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--tc-text);
}
.kb-card__stat-label {
  font-size: 11px;
  color: var(--tc-text-secondary);
}
.kb-card__stat-divider {
  width: 1px;
  height: 24px;
  background: var(--tc-border-light);
}

.kb-card__config {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kb-card__row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  line-height: 1.6;
}
.kb-card__key {
  flex-shrink: 0;
  width: 76px;
  color: var(--tc-text-secondary);
}
.kb-card__value {
  color: var(--tc-text);
  flex: 1;
  min-width: 0;
  word-break: break-all;
}
.kb-card__code-inline {
  background: var(--tc-bg-soft, #fafafa);
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
}

.kb-card__remark {
  font-size: 12px;
  color: var(--tc-text-secondary);
  font-style: italic;
  line-height: 1.5;
  padding: 6px 8px;
  background: var(--tc-bg-soft, #fafafa);
  border-radius: 4px;
}

.kb-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.kb-pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 960px) {
  .kb-filters { grid-template-columns: 1fr 1fr; }
  .kb-stats { grid-template-columns: repeat(2, 1fr); }
  .kb-grid { grid-template-columns: 1fr; }
}
</style>
