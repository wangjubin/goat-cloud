<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view" v-loading="loading">
      <div class="search-form">
        <el-input v-model="query.keyword" :placeholder="keywordPlaceholder" clearable @keyup.enter="loadData" />
        <el-select v-model="query.status" placeholder="状态" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="待处理" value="PENDING" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-alert v-if="errorMessage" class="ai-page-alert" type="error" :title="errorMessage" show-icon />

      <div class="table-toolbar">
        <div class="table-toolbar__title">{{ title }}</div>
        <div class="ai-toolbar-actions">
          <el-button @click="loadData">刷新</el-button>
          <el-button type="primary" @click="openCreate">新增</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && records.length === 0" :description="emptyText" />
      <el-table v-else :data="records" border>
        <el-table-column
          v-for="column in columns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :min-width="column.minWidth"
          :width="column.width"
          :show-overflow-tooltip="column.tooltip !== false"
        >
          <template #default="{ row }">
            <el-tag v-if="column.type === 'status'" :type="statusTagType(row[column.prop])" effect="plain">
              {{ formatStatus(row[column.prop]) }}
            </el-tag>
            <el-tag v-else-if="column.type === 'boolean'" :type="row[column.prop] ? 'success' : 'info'" effect="plain">
              {{ row[column.prop] ? '是' : '否' }}
            </el-tag>
            <span v-else>{{ formatCell(row[column.prop]) }}</span>
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

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" destroy-on-close>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
          <el-row :gutter="12">
            <el-col v-for="field in formFields" :key="field.prop" :span="field.span || 12">
              <el-form-item :label="field.label" :prop="field.prop">
                <el-input
                  v-if="!field.type || field.type === 'input'"
                  v-model="form[field.prop]"
                  :placeholder="field.placeholder || `请输入${field.label}`"
                  clearable
                />
                <el-input
                  v-else-if="field.type === 'textarea'"
                  v-model="form[field.prop]"
                  type="textarea"
                  :rows="field.rows || 4"
                  :placeholder="field.placeholder || `请输入${field.label}`"
                />
                <el-input-number
                  v-else-if="field.type === 'number'"
                  v-model="form[field.prop]"
                  :min="0"
                  controls-position="right"
                  class="ai-number"
                />
                <el-select
                  v-else-if="field.type === 'select'"
                  v-model="form[field.prop]"
                  :placeholder="field.placeholder || `请选择${field.label}`"
                  clearable
                >
                  <el-option
                    v-for="option in field.options || []"
                    :key="String(option.value)"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-select
                  v-else-if="field.type === 'selectAsync'"
                  v-model="form[field.prop]"
                  :placeholder="field.placeholder || `请选择${field.label}`"
                  filterable
                  clearable
                >
                  <el-option
                    v-for="option in (asyncOptions[field.prop] || [])"
                    :key="String(option[field.valueField || 'id'])"
                    :label="option[field.labelField || 'name']"
                    :value="option[field.valueField || 'id']"
                  />
                </el-select>
                <el-switch v-else-if="field.type === 'switch'" v-model="form[field.prop]" />
                <el-date-picker
                  v-else-if="field.type === 'datetime'"
                  v-model="form[field.prop]"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  placeholder="请选择时间"
                />
                <div v-else-if="field.type === 'upload'">
                  <el-button type="primary" @click="triggerUpload(field.prop)">选择文件</el-button>
                  <span v-if="form[field.prop]" class="upload-file-name">{{ getFileName(form[field.prop]) }}</span>
                </div>
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
import type {AiFormField, AiTableColumn} from './types'

const props = withDefaults(
  defineProps<{
    resource: string
    idKey: string
    title: string
    columns: AiTableColumn[]
    formFields: AiFormField[]
    keywordPlaceholder?: string
    emptyText?: string
  }>(),
  {
    keywordPlaceholder: '请输入关键字',
    emptyText: '暂无数据',
  },
)

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
const errorMessage = ref('')
const dialogVisible = ref(false)
const editingId = ref<string | number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, unknown>>({})

const asyncOptions = reactive<Record<string, any[]>>({})
const uploadField = ref<string | null>(null)
const rules = computed<FormRules>(() => {
  const result: FormRules = {}
  props.formFields.forEach((field) => {
    if (field.required) {
      result[field.prop] = [{ required: true, message: `请填写${field.label}`, trigger: 'blur' }]
    }
  })
  return result
})

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await fetchAiPage(props.resource, query)
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

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: Record<string, unknown>) {
  const id = row[props.idKey] as string | number
  editingId.value = id
  resetForm()
  try {
    const detail = await fetchAiDetail(props.resource, id)
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
    await saveAiResource(props.resource, {...form})
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function deleteRow(row: Record<string, unknown>) {
  const id = row[props.idKey] as string | number
  await ElMessageBox.confirm('确认删除这条数据吗？', '删除确认', {type: 'warning'})
  await deleteAiResource(props.resource, [id])
  ElMessage.success('删除成功')
  await loadData()
}

function resetForm() {
  Object.keys(form).forEach((key) => delete form[key])
  props.formFields.forEach((field) => {
    form[field.prop] = field.defaultValue ?? defaultValue(field)
  })
}

function defaultValue(field: AiFormField) {
  if (field.type === 'number') {
    return 0
  }
  if (field.type === 'switch') {
    return false
  }
  if (field.prop === 'status') {
    return 'ENABLED'
  }
  return ''
}

function formatCell(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  return String(value)
}

async function loadAsyncOptions(field: AiFormField) {
  if (!field.apiPath || asyncOptions[field.prop]?.length) return
  try {
    const res = await fetchAiPage(field.apiPath, { pageNum: 1, pageSize: 100, keyword: '' })
    asyncOptions[field.prop] = res.records || []
  } catch (e) {
    console.error('Failed to load async options:', e)
  }
}

function triggerUpload(prop: string) {
  uploadField.value = prop
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '*'
  input.onchange = (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (file) {
      form[prop] = file
    }
  }
  input.click()
}

function getFileName(value: unknown): string {
  if (!value) return ''
  return value instanceof File ? value.name : String(value)
}

function formatStatus(value: unknown): string {
  const status = String(value || '')
  const statusMap: Record<string, string> = {
    ENABLED: '启用',
    DISABLED: '停用',
    DRAFT: '草稿',
    RUNNING: '运行中',
    SUCCESS: '成功',
    FAILED: '失败',
    PENDING: '待处理',
    READY: '就绪',
  }
  return statusMap[status] || status || '-'
}

function statusTagType(value: unknown) {
  const status = String(value || '')
  if (['ENABLED', 'SUCCESS', 'RUNNING', 'READY'].includes(status)) {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  if (['DRAFT', 'PENDING'].includes(status)) {
    return 'warning'
  }
  return 'info'
}

onMounted(loadData)
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

.ai-number,
:deep(.el-select),
:deep(.el-date-editor) {
  width: 100%;
}
</style>
