<template>
  <div>
    <div style="margin-bottom: 12px; display: flex; align-items: center; gap: 8px; padding: 0 12px;">
      <el-select v-model="importDatasourceId" placeholder="选择数据源" clearable style="width: 220px" @focus="loadDatasources">
        <el-option v-for="ds in datasources" :key="ds.datasourceId" :label="ds.datasourceName" :value="ds.datasourceId" />
      </el-select>
      <el-input v-model="importSchema" placeholder="Schema" style="width: 120px" />
      <el-button type="primary" :loading="importing" :disabled="!importDatasourceId" @click="handleImport">
        从数据源导入
      </el-button>
    </div>
    <AiTablePage
      ref="tablePageRef"
      resource="chatbi/tables"
      id-key="tableId"
      title="问数数据表"
      :columns="columns"
      :form-fields="formFields"
      keyword-placeholder="请输入表名、业务名称或字段说明"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import AiTablePage from '../../components/AiTablePage.vue'
import type { AiFormField, AiTableColumn } from '../../components/types'
import { fetchAiPage, importDatasourceTables } from '@/api/ai'

const tablePageRef = ref()

const columns: AiTableColumn[] = [
  { prop: 'tableName', label: '物理表名', minWidth: 170 },
  { prop: 'tableComment', label: '业务名称', minWidth: 150 },
  { prop: 'schemaName', label: 'Schema', width: 110 },
  { prop: 'datasourceId', label: '数据源', width: 110 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
  { prop: 'remark', label: '说明', minWidth: 200 },
]

const formFields: AiFormField[] = [
  { prop: 'datasourceId', label: '数据源', type: 'selectAsync', required: true,
    apiPath: 'chatbi/datasources', valueField: 'datasourceId', labelField: 'datasourceName' },
  { prop: 'schemaName', label: 'Schema', defaultValue: 'public' },
  { prop: 'tableName', label: '物理表名', required: true },
  { prop: 'tableComment', label: '业务名称' },
  { prop: 'columnsJson', label: '字段定义', type: 'columnEditor', span: 24 },
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

// Import toolbar state
const importDatasourceId = ref<number | null>(null)
const importSchema = ref('public')
const importing = ref(false)
const datasources = ref<any[]>([])

async function loadDatasources() {
  if (datasources.value.length) return
  try {
    const res = await fetchAiPage('chatbi/datasources', { pageNum: 1, pageSize: 100, keyword: '' })
    datasources.value = res.records || []
  } catch (e) {
    console.error('Failed to load datasources', e)
  }
}

async function handleImport() {
  if (!importDatasourceId.value) return
  importing.value = true
  try {
    const res = await importDatasourceTables(importDatasourceId.value, importSchema.value || 'public')
    const count = Array.isArray(res) ? res.length : 0
    ElMessage.success(`导入成功，共 ${count} 张表`)
    tablePageRef.value?.loadData?.()
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败')
  } finally {
    importing.value = false
  }
}
</script>
