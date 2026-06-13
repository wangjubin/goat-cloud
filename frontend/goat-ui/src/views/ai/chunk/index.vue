<template>
  <div class="chunk-page" v-loading="loading">
    <!-- Header -->
    <div class="chunk-header">
      <div class="chunk-header__title">
        <h2>切片管理</h2>
        <span class="chunk-header__sub">RAG 文档切片 · 向量化与检索的最小单元</span>
      </div>
      <div class="chunk-header__actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新建切片</span>
        </el-button>
      </div>
    </div>

    <!-- Stats -->
    <div class="chunk-stats">
      <div class="chunk-stat">
        <span class="chunk-stat__label">总计</span>
        <strong class="chunk-stat__value">{{ stats.total }}</strong>
      </div>
      <div class="chunk-stat chunk-stat--success">
        <span class="chunk-stat__label">已向量化</span>
        <strong class="chunk-stat__value">{{ stats.success }}</strong>
      </div>
      <div class="chunk-stat chunk-stat--warning">
        <span class="chunk-stat__label">待处理</span>
        <strong class="chunk-stat__value">{{ stats.pending }}</strong>
      </div>
      <div class="chunk-stat chunk-stat--danger">
        <span class="chunk-stat__label">失败</span>
        <strong class="chunk-stat__value">{{ stats.failed }}</strong>
      </div>
    </div>

    <!-- Filters -->
    <div class="chunk-filters">
      <el-select
        v-model="filters.knowledgeBaseId"
        placeholder="选择知识库"
        clearable
        filterable
        class="chunk-filters__kb"
        @change="onKnowledgeBaseChange"
      >
        <el-option
          v-for="kb in knowledgeBases"
          :key="kb.knowledgeBaseId"
          :label="kb.knowledgeBaseName"
          :value="kb.knowledgeBaseId"
        />
      </el-select>
      <el-select
        v-model="filters.documentId"
        placeholder="选择文档"
        clearable
        filterable
        :disabled="!filters.knowledgeBaseId"
        class="chunk-filters__doc"
      >
        <el-option
          v-for="doc in filteredDocuments"
          :key="doc.documentId"
          :label="doc.documentName"
          :value="doc.documentId"
        />
      </el-select>
      <el-select v-model="filters.embeddingStatus" placeholder="向量状态" clearable class="chunk-filters__status">
        <el-option label="待处理" value="PENDING" />
        <el-option label="成功" value="SUCCESS" />
        <el-option label="失败" value="FAILED" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        placeholder="搜索切片标题或内容"
        clearable
        class="chunk-filters__keyword"
        @keyup.enter="loadData"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-alert v-if="errorMessage" class="chunk-alert" type="error" :title="errorMessage" show-icon />

    <!-- Empty -->
    <el-empty v-if="!loading && groupedChunks.length === 0" description="暂无切片数据" />

    <!-- Grouped list -->
    <div v-else class="chunk-groups">
      <div
        v-for="group in groupedChunks"
        :key="group.documentId"
        class="chunk-group"
      >
        <div class="chunk-group__header">
          <div class="chunk-group__title">
            <el-icon class="chunk-group__icon"><Document /></el-icon>
            <span class="chunk-group__name">{{ group.documentName || '未命名文档' }}</span>
            <el-tag size="small" effect="plain" type="info">{{ group.knowledgeBaseName }}</el-tag>
            <span class="chunk-group__count">{{ group.chunks.length }} 个切片</span>
          </div>
        </div>

        <div class="chunk-group__body">
          <div
            v-for="chunk in group.chunks"
            :key="chunk.chunkId"
            class="chunk-item"
            :class="{ 'chunk-item--disabled': chunk.status === 'DISABLED' }"
          >
            <div class="chunk-item__index">#{{ chunk.chunkIndex ?? '-' }}</div>
            <div class="chunk-item__main">
              <div class="chunk-item__title">
                <span class="chunk-item__name">{{ chunk.title || '（无标题）' }}</span>
                <div class="chunk-item__badges">
                  <el-tag
                    size="small"
                    effect="plain"
                    :type="embeddingTagType(chunk.embeddingStatus)"
                  >
                    <el-icon style="margin-right: 2px"><MagicStick /></el-icon>
                    {{ embeddingLabel(chunk.embeddingStatus) }}
                  </el-tag>
                  <el-tag size="small" effect="plain" :type="chunk.tokenCount > 0 ? '' : 'info'">
                    {{ chunk.tokenCount || 0 }} tokens
                  </el-tag>
                  <el-tag
                    v-if="chunk.status === 'DISABLED'"
                    size="small"
                    effect="plain"
                    type="info"
                  >已停用</el-tag>
                </div>
              </div>
              <div class="chunk-item__content">{{ chunk.content || '（无内容）' }}</div>
              <div v-if="chunk.remark" class="chunk-item__remark">{{ chunk.remark }}</div>
            </div>
            <div class="chunk-item__actions">
              <el-button text type="primary" @click="openEdit(chunk)">编辑</el-button>
              <el-button text type="danger" @click="deleteRow(chunk)">删除</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <el-pagination
      v-if="total > filters.pageSize"
      class="chunk-pagination"
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
      :title="editingId ? '编辑切片' : '新建切片'"
      width="780px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="所属知识库" prop="knowledgeBaseId">
              <el-select
                v-model="form.knowledgeBaseId"
                placeholder="请选择知识库"
                filterable
                style="width: 100%"
                @change="onFormKnowledgeBaseChange"
              >
                <el-option
                  v-for="kb in knowledgeBases"
                  :key="kb.knowledgeBaseId"
                  :label="kb.knowledgeBaseName"
                  :value="kb.knowledgeBaseId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属文档" prop="documentId">
              <el-select
                v-model="form.documentId"
                placeholder="请先选择知识库"
                filterable
                :disabled="!form.knowledgeBaseId"
                style="width: 100%"
              >
                <el-option
                  v-for="doc in formDocumentOptions"
                  :key="doc.documentId"
                  :label="doc.documentName"
                  :value="doc.documentId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="切片序号" prop="chunkIndex">
              <el-input-number v-model="form.chunkIndex" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Token 数" prop="tokenCount">
              <el-input-number v-model="form.tokenCount" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="向量状态" prop="embeddingStatus">
              <el-select v-model="form.embeddingStatus" placeholder="请选择" style="width: 100%">
                <el-option label="待处理" value="PENDING" />
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAILED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入切片标题" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="内容" prop="content">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="8"
                placeholder="请输入切片内容"
                show-word-limit
              />
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
          <el-col :span="12">
            <el-form-item label="向量预览">
              <el-input
                :model-value="embeddingPreview"
                readonly
                placeholder="—"
                :title="form.embeddingVector || ''"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="元数据 JSON">
              <el-input
                v-model="form.metadata"
                type="textarea"
                :rows="3"
                placeholder='例如 {"source":"pdf","page":3}'
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
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {Document, MagicStick, Plus, Refresh, Search} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {
  deleteAiResource,
  fetchAiDetail,
  fetchAiPage,
  saveAiResource,
} from '@/api/ai'

interface KnowledgeBase {
  knowledgeBaseId: number
  knowledgeBaseName: string
}
interface DocumentItem {
  documentId: number
  documentName: string
  knowledgeBaseId: number
}
interface Chunk {
  chunkId: number
  knowledgeBaseId: number
  documentId: number
  chunkIndex: number
  title: string
  content: string
  tokenCount: number
  embeddingStatus: string
  embeddingVector?: string
  metadata?: string
  status: string
  remark?: string
}

const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const records = ref<Chunk[]>([])
const total = ref(0)
const knowledgeBases = ref<KnowledgeBase[]>([])
const documents = ref<DocumentItem[]>([])

const filters = reactive({
  knowledgeBaseId: null as number | null,
  documentId: null as number | null,
  embeddingStatus: '' as string,
  keyword: '',
  pageNum: 1,
  pageSize: 10,
})

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({})

// Resolved names for the current page (for grouped display)
const kbNameMap = computed(() => {
  const m: Record<number, string> = {}
  knowledgeBases.value.forEach((kb) => (m[kb.knowledgeBaseId] = kb.knowledgeBaseName))
  return m
})
const docNameMap = computed(() => {
  const m: Record<number, string> = {}
  documents.value.forEach((d) => (m[d.documentId] = d.documentName))
  return m
})

const filteredDocuments = computed(() => {
  if (!filters.knowledgeBaseId) return documents.value
  return documents.value.filter((d) => d.knowledgeBaseId === filters.knowledgeBaseId)
})

const formDocumentOptions = computed(() => {
  if (!form.knowledgeBaseId) return []
  return documents.value.filter((d) => d.knowledgeBaseId === form.knowledgeBaseId)
})

const groupedChunks = computed(() => {
  const groups = new Map<number, { documentId: number; documentName: string; knowledgeBaseName: string; chunks: Chunk[] }>()
  for (const chunk of records.value) {
    if (!groups.has(chunk.documentId)) {
      groups.set(chunk.documentId, {
        documentId: chunk.documentId,
        documentName: docNameMap.value[chunk.documentId] || `文档 #${chunk.documentId}`,
        knowledgeBaseName: kbNameMap.value[chunk.knowledgeBaseId] || `知识库 #${chunk.knowledgeBaseId}`,
        chunks: [],
      })
    }
    groups.get(chunk.documentId)!.chunks.push(chunk)
  }
  // Sort chunks inside each group by index
  groups.forEach((g) => g.chunks.sort((a, b) => (a.chunkIndex ?? 0) - (b.chunkIndex ?? 0)))
  return Array.from(groups.values()).sort((a, b) => a.documentId - b.documentId)
})

const stats = computed(() => {
  const s = { total: records.value.length, success: 0, pending: 0, failed: 0 }
  for (const c of records.value) {
    if (c.embeddingStatus === 'SUCCESS') s.success++
    else if (c.embeddingStatus === 'PENDING') s.pending++
    else if (c.embeddingStatus === 'FAILED') s.failed++
  }
  return s
})

const embeddingPreview = computed(() => {
  const v = String(form.embeddingVector || '')
  if (!v) return ''
  return v.length > 60 ? v.slice(0, 60) + '…' : v
})

const rules = computed<FormRules>(() => ({
  knowledgeBaseId: [{required: true, message: '请选择知识库', trigger: 'change'}],
  documentId: [{required: true, message: '请选择文档', trigger: 'change'}],
  title: [{required: true, message: '请填写标题', trigger: 'blur'}],
  content: [{required: true, message: '请填写内容', trigger: 'blur'}],
  chunkIndex: [{required: true, message: '请填写切片序号', trigger: 'blur'}],
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
    }
    if (filters.knowledgeBaseId) payload.knowledgeBaseId = filters.knowledgeBaseId
    if (filters.documentId) payload.documentId = filters.documentId
    if (filters.embeddingStatus) payload.embeddingStatus = filters.embeddingStatus

    const result = await fetchAiPage<Chunk>('chunks', payload as any)
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

async function loadKnowledgeBases() {
  try {
    const result = await fetchAiPage<KnowledgeBase>('knowledge-bases', {
      pageNum: 1,
      pageSize: 100,
      keyword: '',
    } as any)
    knowledgeBases.value = (result.records || []).map((kb) => ({
      knowledgeBaseId: kb.knowledgeBaseId,
      knowledgeBaseName: kb.knowledgeBaseName,
    }))
  } catch (error) {
    console.error('Failed to load knowledge bases:', error)
    knowledgeBases.value = []
  }
}

async function loadDocuments(knowledgeBaseId?: number | null) {
  try {
    const payload: Record<string, unknown> = {pageNum: 1, pageSize: 200, keyword: ''}
    if (knowledgeBaseId) payload.knowledgeBaseId = knowledgeBaseId
    const result = await fetchAiPage<DocumentItem>('documents', payload as any)
    documents.value = (result.records || []).map((d) => ({
      documentId: d.documentId,
      documentName: d.documentName,
      knowledgeBaseId: d.knowledgeBaseId,
    }))
  } catch (error) {
    console.error('Failed to load documents:', error)
    documents.value = []
  }
}

function onKnowledgeBaseChange(val: number | null) {
  filters.documentId = null
  if (val) {
    loadDocuments(val)
  }
}

function onFormKnowledgeBaseChange() {
  // When KB changes, clear the document selection (form)
}

function resetFilters() {
  filters.knowledgeBaseId = null
  filters.documentId = null
  filters.embeddingStatus = ''
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
  form.chunkIndex = 0
  form.tokenCount = 0
  form.embeddingStatus = 'PENDING'
  form.status = 'ENABLED'
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: Chunk) {
  editingId.value = row.chunkId
  resetForm()
  try {
    const detail = (await fetchAiDetail('chunks', row.chunkId)) as Chunk
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
    await saveAiResource('chunks', {...form})
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function deleteRow(row: Chunk) {
  await ElMessageBox.confirm(`确认删除切片「${row.title || row.chunkId}」吗？`, '删除确认', {type: 'warning'})
  await deleteAiResource('chunks', [row.chunkId])
  ElMessage.success('删除成功')
  await loadData()
}

function embeddingLabel(s?: string) {
  if (s === 'SUCCESS') return '已向量化'
  if (s === 'FAILED') return '向量化失败'
  return '待处理'
}
function embeddingTagType(s?: string) {
  if (s === 'SUCCESS') return 'success'
  if (s === 'FAILED') return 'danger'
  return 'warning'
}

onMounted(async () => {
  await Promise.all([loadKnowledgeBases(), loadDocuments()])
  await loadData()
})
</script>

<style scoped>
.chunk-page {
  padding: 20px;
}

.chunk-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 16px;
}
.chunk-header__title h2 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
  color: var(--tc-text);
}
.chunk-header__sub {
  font-size: 12px;
  color: var(--tc-text-secondary);
}
.chunk-header__actions {
  display: flex;
  gap: 8px;
}

.chunk-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.chunk-stat {
  background: var(--tc-surface);
  border: 1px solid var(--tc-border-light);
  border-left: 3px solid var(--tc-primary);
  border-radius: var(--tc-radius-md);
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.chunk-stat--success { border-left-color: var(--tc-success); }
.chunk-stat--warning { border-left-color: var(--tc-warning); }
.chunk-stat--danger  { border-left-color: var(--tc-danger); }
.chunk-stat__label {
  font-size: 12px;
  color: var(--tc-text-secondary);
}
.chunk-stat__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--tc-text);
}

.chunk-filters {
  display: grid;
  grid-template-columns: 1.2fr 1.2fr 1fr 2fr auto auto;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}
.chunk-filters :deep(.el-select),
.chunk-filters :deep(.el-input) {
  width: 100%;
}

.chunk-alert {
  margin-bottom: 12px;
}

.chunk-groups {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chunk-group {
  background: var(--tc-surface);
  border: 1px solid var(--tc-border-light);
  border-radius: var(--tc-radius-md);
  overflow: hidden;
}
.chunk-group__header {
  padding: 12px 16px;
  background: var(--tc-bg-soft, #fafafa);
  border-bottom: 1px solid var(--tc-border-light);
}
.chunk-group__title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.chunk-group__icon {
  color: var(--tc-primary);
}
.chunk-group__name {
  font-weight: 600;
  color: var(--tc-text);
  font-size: 14px;
}
.chunk-group__count {
  font-size: 12px;
  color: var(--tc-text-secondary);
  margin-left: auto;
}
.chunk-group__body {
  display: flex;
  flex-direction: column;
}

.chunk-item {
  display: grid;
  grid-template-columns: 56px 1fr auto;
  gap: 12px;
  padding: 14px 16px;
  border-top: 1px solid var(--tc-border-light);
  transition: background 0.15s ease;
}
.chunk-item:first-child { border-top: none; }
.chunk-item:hover {
  background: var(--tc-bg-soft, #fafafa);
}
.chunk-item--disabled {
  opacity: 0.55;
}

.chunk-item__index {
  font-weight: 700;
  font-size: 14px;
  color: var(--tc-primary);
  background: var(--tc-primary-soft, rgba(99, 102, 241, 0.1));
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  margin-top: 2px;
}

.chunk-item__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.chunk-item__title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.chunk-item__name {
  font-weight: 600;
  font-size: 14px;
  color: var(--tc-text);
}
.chunk-item__badges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.chunk-item__content {
  font-size: 13px;
  line-height: 1.6;
  color: var(--tc-text);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: pre-wrap;
}
.chunk-item__remark {
  font-size: 12px;
  color: var(--tc-text-secondary);
  font-style: italic;
}

.chunk-item__actions {
  display: flex;
  align-items: flex-start;
  gap: 4px;
}

.chunk-pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 960px) {
  .chunk-filters {
    grid-template-columns: 1fr 1fr;
  }
  .chunk-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .chunk-item {
    grid-template-columns: 48px 1fr;
  }
  .chunk-item__actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}
</style>
