<template>
  <AiTablePage
    resource="chatbi/datasets"
    id-key="datasetId"
    title="问数数据集"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入数据集名称或编码"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../../components/types'

const columns: AiTableColumn[] = [
  { prop: 'datasetName', label: '数据集名称', minWidth: 170 },
  { prop: 'datasetCode', label: '数据集编码', minWidth: 140 },
  { prop: 'datasourceId', label: '数据源', width: 110 },
  { prop: 'tableIds', label: '关联表', minWidth: 130 },
  { prop: 'defaultFilters', label: '默认过滤', minWidth: 160 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
]

const formFields: AiFormField[] = [
  { prop: 'datasetName', label: '数据集名称', required: true },
  { prop: 'datasetCode', label: '数据集编码', required: true },
  { prop: 'datasourceId', label: '数据源', type: 'selectAsync',
    apiPath: 'chatbi/datasources', valueField: 'datasourceId', labelField: 'datasourceName' },
  { prop: 'tableIds', label: '关联表ID', placeholder: '例如 1,2,3' },
  { prop: 'semanticModel', label: '语义模型 JSON', type: 'textarea', span: 24, rows: 5 },
  { prop: 'defaultFilters', label: '默认过滤条件', type: 'textarea', span: 24 },
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
