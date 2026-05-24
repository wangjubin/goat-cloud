<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view">
      <div class="search-form">
        <el-input v-model="query.keyword" placeholder="请输入文档名称、来源或类型" clearable @keyup.enter="loadData" />
        <el-select v-model="query.status" placeholder="状态" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <div class="table-toolbar">
        <div class="table-toolbar__title">文档管理</div>
        <div class="ai-toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openUpload">上传文档</el-button>
        </div>
      </div>

      <el-alert v-if="errorMessage" class="ai-page-alert" type="error" :title="errorMessage" show-icon />

      <el-table v-loading="loading" :data="records" border>
        <el-table-column prop="documentName" label="文档名称" minWidth="190" show-overflow-tooltip />
        <el-table-column prop="knowledgeBaseName" label="知识库" minWidth="150" show-overflow-tooltip />
        <el-table-column prop="documentType" label="类型" width="100" />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="parseStatus" label="解析状态" width="110">
          <template #default="{ row }">
            <el-tag :type="parseStatusType(row.parseStatus)" effect="plain">
              {{ formatParseStatus(row.parseStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkStatus" label="切片状态" width="110">
          <template #default="{ row }">
            <el-tag :type="chunkStatusType(row.chunkStatus)" effect="plain">
              {{ formatChunkStatus(row.chunkStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" @click="deleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > query.pageSize"
        class="ai-pagination"
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNum"
        @current-change="handlePageChange"
      />

      <el-dialog v-model="uploadVisible" title="上传文档" width="500px" destroy-on-close>
        <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="100px">
          <el-form-item label="知识库" prop="knowledgeBaseId">
            <el-select
              v-model="uploadForm.knowledgeBaseId"
              placeholder="请选择知识库"
              filterable
              clearable
            >
              <el-option
                v-for="kb in knowledgeBases"
                :key="kb.knowledgeBaseId"
                :label="kb.knowledgeBaseName"
                :value="kb.knowledgeBaseId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="选择文件" prop="file">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              accept=".pdf,.docx,.txt,.md,.html"
            >
              <el-button type="primary">选择文件</el-button>
              <template #tip>
                <div class="el-upload__tip">支持 PDF、DOCX、TXT、Markdown、HTML 格式</div>
              </template>
            </el-upload>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="uploadVisible = false">取消</el-button>
          <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" destroy-on-close>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="知识库" prop="knowledgeBaseId">
                <el-select v-model="form.knowledgeBaseId" placeholder="请选择知识库" filterable clearable>
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
              <el-form-item label="文档名称" prop="documentName">
                <el-input v-model="form.documentName" placeholder="请输入文档名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="文档类型" prop="documentType">
                <el-select v-model="form.documentType" placeholder="请选择类型">
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
                <el-select v-model="form.status" placeholder="请选择状态">
                  <el-option label="启用" value="ENABLED" />
                  <el-option label="停用" value="DISABLED" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="来源地址" prop="sourceUri">
                <el-input v-model="form.sourceUri" placeholder="请输入来源地址" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注" prop="remark">
                <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
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
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {deleteAiResource, fetchAiDetail, fetchAiPage, saveAiResource} from '@/api/ai'

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
})

const records = ref<Record<string, unknown>[]>([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const errorMessage = ref('')
const dialogVisible = ref(false)
const uploadVisible = ref(false)
const editingId = ref<string | number | null>(null)
const formRef = ref<FormInstance>()
const uploadFormRef = ref<FormInstance>()
const uploadRef = ref()
const form = reactive<Record<string, unknown>>({})
const uploadForm = reactive<Record<string, unknown>>({
  knowledgeBaseId: null,
  file: null,
})
const knowledgeBases = ref<Record<string, unknown>[]>([])

const dialogTitle = computed(() => `${editingId.value ? '编辑' : '新增'}文档`)

const uploadRules = computed<FormRules>(() => ({
  knowledgeBaseId: [{ required: true, message: '请选择知识库', trigger: 'change' }],
}))

const rules = computed<FormRules>(() => ({
  knowledgeBaseId: [{ required: true, message: '请选择知识库', trigger: 'change' }],
  documentName: [{ required: true, message: '请输入文档名称', trigger: 'blur' }],
  documentType: [{ required: true, message: '请选择文档类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}))

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchAiPage('documents', query)
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
    const result = await fetchAiPage('knowledge-bases', { pageNum: 1, pageSize: 100, keyword: '' })
    knowledgeBases.value = result.records || []
  } catch (error) {
    console.error('Failed to load knowledge bases:', error)
  }
}

function resetQuery() {
  query.pageNum = 1
  query.keyword = ''
  query.status = ''
  loadData()
}

function handlePageChange(pageNum: number) {
  query.pageNum = pageNum
  loadData()
}

function openUpload() {
  uploadForm.knowledgeBaseId = null
  uploadForm.file = null
  uploadVisible.value = true
}

function handleFileChange(file: any) {
  uploadForm.file = file.raw
}

function handleFileRemove() {
  uploadForm.file = null
}

async function submitUpload() {
  await uploadFormRef.value?.validate()
  if (!uploadForm.file) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('knowledgeBaseId', String(uploadForm.knowledgeBaseId))
    formData.append('file', uploadForm.file as File)
    const token = localStorage.getItem('access_token') || ''
    await fetch('/api/ai/documents/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      body: formData,
    })
    ElMessage.success('上传成功')
    uploadVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: Record<string, unknown>) {
  const id = row.documentId as string | number
  editingId.value = id
  resetForm()
  try {
    const detail = await fetchAiDetail('documents', id)
    Object.assign(form, detail || row)
  } catch (error) {
    Object.assign(form, row)
  }
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    await saveAiResource('documents', {...form})
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function deleteRow(row: Record<string, unknown>) {
  await ElMessageBox.confirm('确认删除这条数据吗？', '删除确认', {type: 'warning'})
  await deleteAiResource('documents', [row.documentId as string | number])
  ElMessage.success('删除成功')
  await loadData()
}

function resetForm() {
  Object.keys(form).forEach((key) => delete form[key])
  form.status = 'ENABLED'
  form.parseStatus = 'PENDING'
  form.chunkStatus = 'PENDING'
}

function formatStatus(value: unknown) {
  const status = String(value || '')
  const statusMap: Record<string, string> = {
    ENABLED: '启用',
    DISABLED: '停用',
  }
  return statusMap[status] || status || '-'
}

function formatParseStatus(value: unknown) {
  const status = String(value || '')
  const statusMap: Record<string, string> = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    SUCCESS: '成功',
    FAILED: '失败',
  }
  return statusMap[status] || status || '-'
}

function formatChunkStatus(value: unknown) {
  const status = String(value || '')
  const statusMap: Record<string, string> = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    FAILED: '失败',
  }
  return statusMap[status] || status || '-'
}

function parseStatusType(value: unknown): string {
  const status = String(value || '')
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PROCESSING') return 'warning'
  return 'info'
}

function chunkStatusType(value: unknown): string {
  const status = String(value || '')
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PROCESSING') return 'warning'
  return 'info'
}

function statusTagType(value: unknown): string {
  const status = String(value || '')
  return status === 'ENABLED' ? 'success' : 'info'
}

function formatFileSize(bytes: number | undefined): string {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

onMounted(() => {
  loadData()
  loadKnowledgeBases()
})
</script>

<style scoped>
.ai-page-alert {
  margin-bottom: 12px;
}

.ai-toolbar-actions {
  display: flex;
  gap: 8px;
}

.ai-pagination {
  justify-content: flex-end;
  margin-top: 12px;
}

:deep(.el-select) {
  width: 100%;
}
</style>
