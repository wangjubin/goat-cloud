<template>
  <AiTablePage
    resource="chatbi/tables"
    id-key="tableId"
    title="问数数据表"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入表名、业务名称或字段说明"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../../components/types'

const columns: AiTableColumn[] = [
  { prop: 'tableName', label: '物理表名', minWidth: 170 },
  { prop: 'tableComment', label: '业务名称', minWidth: 150 },
  { prop: 'schemaName', label: 'Schema', width: 110 },
  { prop: 'datasourceId', label: '数据源ID', width: 110 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
  { prop: 'remark', label: '说明', minWidth: 200 },
]

const formFields: AiFormField[] = [
  { prop: 'datasourceId', label: '数据源ID', type: 'number', required: true, defaultValue: 1 },
  { prop: 'schemaName', label: 'Schema', defaultValue: 'public' },
  { prop: 'tableName', label: '物理表名', required: true },
  { prop: 'tableComment', label: '业务名称' },
  { prop: 'columnsJson', label: '字段 JSON', type: 'textarea', span: 24, rows: 5 },
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
</script>
