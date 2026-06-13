<template>
  <div class="doc-page" v-loading="loading">
    <div class="doc-header">
      <div class="doc-header__title">
        <h2>文档管理</h2>
        <span class="doc-header__sub">文件上传 · 自动解析 · 切片索引</span>
      </div>
      <div class="doc-header__actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon><span>刷新</span>
        </el-button>
        <el-button type="primary" @click="openUpload">
          <el-icon><Upload /></el-icon><span>上传文档</span>
        </el-button>
      </div>
    </div>

    <div class="doc-stats">
      <div class="doc-stat">
        <span class="doc-stat__label">文档总数</span>
        <strong class="doc-stat__value">{{ stats.total }}</strong>
      </div>
      <div class="doc-stat doc-stat--primary">
        <span class="doc-stat__label">已解析</span>
        <strong class="doc-stat__value">{{ stats.parsed }}</strong>
      </div>
      <div class="doc-stat doc-stat--warning">
        <span class="doc-stat__label">解析中</span>
        <strong class="doc-stat__value">{{ stats.parsing }}</strong>
      </div>
      <div class="doc-stat doc-stat--danger">
        <span class="doc-stat__label">解析失败</span>
        <strong class="doc-stat__value">{{ stats.failed }}</strong>
      </div>
    </div>

    <div class="doc-filters">
      <el-input v-model="query.keyword" placeholder="搜索文档名称、来源或类型" clearable
        @keyup.enter="loadData" @clear="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.knowledgeBaseId" placeholder="知识库" clearable filterable @change="loadData">
        <el-option v-for="kb in knowledgeBases" :key="kb.knowledgeBaseId"
          :label="kb.knowledgeBaseName" :value="kb.knowledgeBaseId" />
      </el-select>
      <el-select v-model="query.documentType" placeholder="文档类型" clearable @change="loadData">
        <el-option label="PDF" value="PDF" />
        <el-option label="DOCX" value="DOCX" />
        <el-option label="TXT" value="TXT" />
        <el-option label="Markdown" value="MARKDOWN" />
        <el-option label="HTML" value="HTML" />
      </el-select>
      <el-select v-model="query.parseStatus" placeholder="解析状态" clearable @change="loadData">
        <el-option label="待处理" value="PENDING" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="成功" value="SUCCESS" />
        <el-option label="失败" value="FAILED" />
      </el-select>
      <el-select v-model="query.status" placeholder="启用状态" clearable @change="loadData">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <el-alert v-if="errorMessage" class="doc-alert" type="error" :title="errorMessage" show-icon />
    <el-empty v-if="!loading && records.length === 0" description="暂无文档，请上传或选择其他知识库" />

    <div v-else class="doc-grid">
      <div v-for="row in records" :key="row.documentId" class="doc-card"
        :class="{ 'doc-card--disabled': row.status !== 'ENABLED' }">
        <div class="doc-card__icon" :style="{background: typeGradient(row.documentType)}">
          <span class="doc-card__type">{{ fileTypeLabel(row.documentType) }}</span>
          <el-icon class="doc-card__type-icon" :size="26"><Document /></el-icon>
        </div>

        <div class="doc-card__body">
          <div class="doc-card__header">
            <h3 class="doc-card__name" :title="row.documentName">{{ row.documentName }}</h3>
            <el-tag size="small" effect="plain" :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </div>

          <div class="doc-card__meta">
            <span class="doc-card__meta-item">
              <el-icon><Collection /></el-icon>
              <span>{{ row.knowledgeBaseName || knowledgeBaseName(row.knowledgeBaseId) }}</span>
            </span>
            <span class="doc-card__meta-item">
              <el-icon><Files /></el-icon>
              <span>{{ formatFileSize(row.fileSize) }}</span>
            </span>
            <span class="doc-card__meta-item" v-if="row.chunkCount">
              <el-icon><Operation /></el-icon>
              <span>{{ row.chunkCount }} 切片</span>
            </span>
          </div>

          <div v-if="row.sourceUri" class="doc-card__source" :title="row.sourceUri">
            <el-icon><Link /></el-icon>
            <code>{{ row.sourceUri }}</code>
          </div>

          <div v-if="row.parseMessage" class="doc-card__msg doc-card__msg--error">
            <el-icon><WarningFilled /></el-icon>
            <span>{{ row.parseMessage }}</span>
          </div>

          <div class="doc-card__status">
            <div class="doc-card__status-item">
              <span class="doc-card__status-label">解析</span>
              <el-tag size="small" effect="plain" :type="parseStatusType(row.parseStatus)">
                <el-icon style="margin-right:2px" v-if="row.parseStatus === 'PROCESSING'"><Loading /></el-icon>
                {{ formatParseStatus(row.parseStatus) }}
              </el-tag>
            </div>
            <div class="doc-card__status-item">
              <span class="doc-card__status-label">切片</span>
              <el-tag size="small" effect="plain" :type="chunkStatusType(row.chunkStatus)">
                {{ formatChunkStatus(row.chunkStatus) }}
              </el-tag>
            </div>
            <span class="doc-card__time" v-if="row.createTime">{{ formatTime(row.createTime) }}</span>
          </div>

          <div class="doc-card__actions">
            <el-button text type="primary" @click="openEdit(row)">
              <el-icon><Edit /></el-icon><span>编辑</span>
            </el-button>
            <el-button text type="danger" @click="deleteRow(row)">
              <el-icon><Delete /></el-icon><span>删除</span>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-pagination v-if="total > query.pageSize" class="doc-pagination" background
      layout="total, prev, pager, next" :total="total" :page-size="query.pageSize"
      :current-page="query.pageNum" @current-change="handlePageChange" />

    <el-dialog v-model="uploadVisible" title="上传文档" width="520px" destroy-on-close>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="100px">
        <el-form-item label="知识库" prop="knowledgeBaseId">
          <el-select v-model="uploadForm.knowledgeBaseId" placeholder="请选择知识库" filterable clearable
            style="width:100%">
            <el-option v-for="kb in knowledgeBases" :key="kb.knowledgeBaseId"
              :label="kb.knowledgeBaseName" :value="kb.knowledgeBaseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件" prop="file">
          <el-upload ref="uploadRef" :auto-upload="false" :limit="1"
            :on-change="handleFileChange" :on-remove="handleFileRemove"
            accept=".pdf,.docx,.txt,.md,.html" drag>
            <el-icon class="el-upload__icon" :size="42"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              <em>点击或拖拽文件到此处</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、DOCX、TXT、Markdown、HTML 格式</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">上传并解析</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑文档' : '新增文档'"
      width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="知识库" prop="knowledgeBaseId">
              <el-select v-model="form.knowledgeBaseId" placeholder="请选择知识库" filterable clearable style="width:100%">
                <el-option v-for="kb in knowledgeBases" :key="kb.knowledgeBaseId"
                  :label="kb.knowledgeBaseName" :value="kb.knowledgeBaseId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文档名称" prop="documentName">
              <el-input v-model="form.documentName" placeholder="例如：产品白皮书" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="文档类型" prop="documentType">
              <el-select v-model="form.documentType" placeholder="请选择" style="width:100%">
                <el-option label="PDF" value="PDF" />
                <el-option label="DOCX" value="DOCX" />
                <el-option label="TXT" value="TXT" />
                <el-option label="Markdown" value="MARKDOWN" />
                <el-option label="HTML" value="HTML" />
              </el-select>
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
            <el-form-item label="来源地址">
              <el-input v-model="form.sourceUri" placeholder="可选：URL 或路径" clearable />
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
import {Collection, Delete, Document, Edit, Files, Link, Loading, Operation, Refresh, Search, Upload, UploadFilled, WarningFilled} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {deleteAiResource, fetchAiDetail, fetchAiPage, saveAiResource, uploadAiDocument} from '@/api/ai'

interface DocumentRow {
  documentId: number | string
  documentName: string
  knowledgeBaseId: number | string
  knowledgeBaseName?: string
  documentType: string
  fileSize: number
  chunkCount?: number
  parseStatus: string
  parseMessage?: string
  chunkStatus: string
  sourceUri: string
  status: string
  remark: string
  createTime?: string
}

const query = reactive({pageNum: 1, pageSize: 12, keyword: '', knowledgeBaseId: '', documentType: '', parseStatus: '', status: ''})
const records = ref<DocumentRow[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const errorMessage = ref('')
const dialogVisible = ref(false)
const uploadVisible = ref(false)
const editingId = ref<number | string | null>(null)
const formRef = ref<FormInstance>()
const uploadFormRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({})
const uploadForm = reactive<Record<string, any>>({knowledgeBaseId: null, file: null})
const knowledgeBases = ref<Array<{knowledgeBaseId: number | string; knowledgeBaseName: string}>>([])

const stats = computed(() => {
  const s = {total: 0, parsed: 0, parsing: 0, failed: 0}
  for (const r of records.value) {
    s.total++
    if (r.parseStatus === 'SUCCESS') s.parsed++
    else if (r.parseStatus === 'PROCESSING') s.parsing++
    else if (r.parseStatus === 'FAILED') s.failed++
  }
  return s
})

const uploadRules = computed<FormRules>(() => ({
  knowledgeBaseId: [{required: true, message: '请选择知识库', trigger: 'change'}],
}))

const rules = computed<FormRules>(() => ({
  knowledgeBaseId: [{required: true, message: '请选择知识库', trigger: 'change'}],
  documentName: [{required: true, message: '请输入文档名称', trigger: 'blur'}],
  documentType: [{required: true, message: '请选择文档类型', trigger: 'change'}],
  status: [{required: true, message: '请选择状态', trigger: 'change'}],
}))

async function loadData() {
  loading.value = true; errorMessage.value = ''
  try {
    const payload: Record<string, unknown> = {...query}
    if (!payload.knowledgeBaseId) delete payload.knowledgeBaseId
    if (!payload.documentType) delete payload.documentType
    if (!payload.parseStatus) delete payload.parseStatus
    if (!payload.status) delete payload.status
    const result = await fetchAiPage<DocumentRow>('documents', payload as any)
    records.value = result.records || []
    total.value = result.total || records.value.length
  } catch {
    errorMessage.value = '接口不可用，请检查后端服务和权限配置。'
    records.value = []; total.value = 0
  } finally { loading.value = false }
}

async function loadKnowledgeBases() {
  try {
    const result = await fetchAiPage<any>('knowledge-bases', {pageNum: 1, pageSize: 100, keyword: ''} as any)
    knowledgeBases.value = (result.records || []).map((kb: any) => ({
      knowledgeBaseId: kb.knowledgeBaseId,
      knowledgeBaseName: kb.knowledgeBaseName,
    }))
  } catch (error) {
    console.error('Failed to load knowledge bases', error)
  }
}

function knowledgeBaseName(id: number | string | undefined): string {
  if (!id) return '—'
  const kb = knowledgeBases.value.find(k => String(k.knowledgeBaseId) === String(id))
  return kb?.knowledgeBaseName || `KB#${id}`
}

function resetQuery() {
  query.pageNum = 1; query.keyword = ''; query.knowledgeBaseId = ''
  query.documentType = ''; query.parseStatus = ''; query.status = ''
  loadData()
}
function handlePageChange(p: number) { query.pageNum = p; loadData() }

function resetForm() {
  Object.keys(form).forEach(k => delete form[k])
  form.status = 'ENABLED'; form.parseStatus = 'PENDING'; form.chunkStatus = 'PENDING'
}

function openUpload() {
  uploadForm.knowledgeBaseId = null; uploadForm.file = null
  uploadVisible.value = true
}

function handleFileChange(file: any) { uploadForm.file = file.raw }
function handleFileRemove() { uploadForm.file = null }

async function submitUpload() {
  try { await uploadFormRef.value?.validate() } catch { return }
  if (!uploadForm.file) { ElMessage.warning('请选择文件'); return }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('knowledgeBaseId', String(uploadForm.knowledgeBaseId))
    fd.append('file', uploadForm.file as File)
    const result = await uploadAiDocument(fd)
    ElMessage.success(result?.message || '上传成功，解析任务已启动')
    uploadVisible.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '上传失败')
  } finally { uploading.value = false }
}

async function openEdit(row: DocumentRow) {
  editingId.value = row.documentId; resetForm()
  try {
    const detail: any = await fetchAiDetail('documents', row.documentId)
    Object.assign(form, detail)
  } catch { Object.assign(form, row) }
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate(); saving.value = true
  try {
    await saveAiResource('documents', {...form})
    ElMessage.success('保存成功'); dialogVisible.value = false; await loadData()
  } finally { saving.value = false }
}

async function deleteRow(row: DocumentRow) {
  await ElMessageBox.confirm(
    `确认删除文档「${row.documentName}」吗？该操作不会删除已生成的切片数据。`,
    '删除确认', {type: 'warning'},
  )
  await deleteAiResource('documents', [row.documentId])
  ElMessage.success('删除成功'); await loadData()
}

function fileTypeLabel(t: string) { return {PDF: 'PDF', DOCX: 'DOCX', TXT: 'TXT', MARKDOWN: 'MD', HTML: 'HTML'}[t] || (t || 'FILE').slice(0, 4) }
function formatFileSize(bytes: number | undefined) {
  if (!bytes) return '—'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}
function formatTime(t: string) {
  if (!t) return ''
  try { return new Date(t).toLocaleDateString('zh-CN') } catch { return t }
}
function formatStatus(v: string) { return {ENABLED: '启用', DISABLED: '停用'}[v] || v || '—' }
function formatParseStatus(v: string) { return {PENDING: '待处理', PROCESSING: '处理中', SUCCESS: '成功', FAILED: '失败'}[v] || v || '—' }
function formatChunkStatus(v: string) { return {PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '已完成', FAILED: '失败'}[v] || v || '—' }
function parseStatusType(v: string) { return {SUCCESS: 'success', FAILED: 'danger', PROCESSING: 'warning'}[v] || 'info' }
function chunkStatusType(v: string) { return {COMPLETED: 'success', FAILED: 'danger', PROCESSING: 'warning'}[v] || 'info' }
function typeGradient(t: string) {
  return {
    PDF: 'linear-gradient(135deg,#ef4444,#f87171)',
    DOCX: 'linear-gradient(135deg,#2563eb,#3b82f6)',
    TXT: 'linear-gradient(135deg,#6b7280,#9ca3af)',
    MARKDOWN: 'linear-gradient(135deg,#10b981,#34d399)',
    HTML: 'linear-gradient(135deg,#f59e0b,#fbbf24)',
  }[t] || 'linear-gradient(135deg,#6366f1,#8b5cf6)'
}

onMounted(async () => {
  await loadKnowledgeBases()
  await loadData()
})
</script>

<style scoped>
.doc-page { padding: 20px; }
.doc-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.doc-header__title h2 { margin: 0 0 4px; font-size: 20px; font-weight: 700; color: var(--tc-text); }
.doc-header__sub { font-size: 12px; color: var(--tc-text-secondary); }
.doc-header__actions { display: flex; gap: 8px; }

.doc-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.doc-stat { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-left: 3px solid var(--tc-primary); border-radius: var(--tc-radius-md); padding: 14px 18px; display: flex; flex-direction: column; gap: 4px; }
.doc-stat--primary { border-left-color: var(--tc-success); }
.doc-stat--warning { border-left-color: #f59e0b; }
.doc-stat--danger { border-left-color: var(--tc-danger); }
.doc-stat__label { font-size: 12px; color: var(--tc-text-secondary); }
.doc-stat__value { font-size: 22px; font-weight: 700; color: var(--tc-text); }

.doc-filters { display: grid; grid-template-columns: 2fr 1.2fr 1fr 1fr 1fr auto; gap: 8px; margin-bottom: 16px; }
.doc-filters :deep(.el-select), .doc-filters :deep(.el-input) { width: 100%; }

.doc-alert { margin-bottom: 12px; }

.doc-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(420px, 1fr)); gap: 14px; }
.doc-card { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-radius: var(--tc-radius-md); display: flex; gap: 14px; padding: 14px; transition: all 0.2s ease; }
.doc-card:hover { box-shadow: var(--tc-shadow-md); transform: translateY(-2px); }
.doc-card--disabled { opacity: 0.6; }

.doc-card__icon { width: 72px; height: 88px; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; position: relative; }
.doc-card__type { font-size: 11px; font-weight: 700; letter-spacing: 0.5px; margin-top: 4px; font-family: 'SFMono-Regular', Consolas, monospace; }
.doc-card__type-icon { opacity: 0.85; }

.doc-card__body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px; }
.doc-card__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }
.doc-card__name { margin: 0; font-size: 14px; font-weight: 700; color: var(--tc-text); flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.doc-card__meta { display: flex; gap: 12px; flex-wrap: wrap; font-size: 11px; color: var(--tc-text-secondary); }
.doc-card__meta-item { display: flex; align-items: center; gap: 3px; }
.doc-card__meta-item :deep(svg) { font-size: 12px; }

.doc-card__source { display: flex; align-items: center; gap: 4px; font-size: 11px; color: var(--tc-text-secondary); padding: 3px 8px; background: var(--tc-bg-soft, #fafafa); border-radius: 3px; }
.doc-card__source code { font-size: 10px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0; }
.doc-card__source :deep(svg) { font-size: 12px; flex-shrink: 0; }

.doc-card__msg { display: flex; align-items: center; gap: 4px; font-size: 11px; padding: 4px 8px; border-radius: 3px; }
.doc-card__msg--error { color: var(--tc-danger); background: rgba(239, 68, 68, 0.08); }

.doc-card__status { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.doc-card__status-item { display: flex; align-items: center; gap: 4px; font-size: 11px; }
.doc-card__status-label { color: var(--tc-text-secondary); }
.doc-card__time { font-size: 10px; color: var(--tc-text-secondary); margin-left: auto; font-family: 'SFMono-Regular', Consolas, monospace; }

.doc-card__actions { display: flex; justify-content: flex-end; gap: 4px; margin-top: auto; padding-top: 4px; }

.doc-pagination { justify-content: flex-end; margin-top: 16px; }

:deep(.el-upload-dragger) { padding: 18px 12px; }
:deep(.el-upload__text em) { color: var(--tc-primary); font-style: normal; }

@media (max-width: 960px) {
  .doc-filters { grid-template-columns: 1fr 1fr; }
  .doc-stats { grid-template-columns: repeat(2, 1fr); }
  .doc-grid { grid-template-columns: 1fr; }
}
</style>
