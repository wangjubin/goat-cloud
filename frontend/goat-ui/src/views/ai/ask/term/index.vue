<template>
  <AiTablePage
    resource="chatbi/terms"
    id-key="termId"
    title="问数术语"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入术语、编码、别名或表达式"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../../components/types'

const columns: AiTableColumn[] = [
  { prop: 'termName', label: '术语名称', minWidth: 150 },
  { prop: 'termCode', label: '术语编码', minWidth: 140 },
  { prop: 'synonyms', label: '别名', minWidth: 180 },
  { prop: 'definition', label: '定义', minWidth: 260 },
  { prop: 'datasetId', label: '数据集ID', width: 110 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
]

const formFields: AiFormField[] = [
  { prop: 'termName', label: '术语名称', required: true },
  { prop: 'termCode', label: '术语编码', required: true },
  { prop: 'synonyms', label: '别名', span: 24 },
  { prop: 'definition', label: '定义', type: 'textarea', span: 24 },
  { prop: 'expression', label: '计算表达式', type: 'textarea', span: 24 },
  { prop: 'datasetId', label: '数据集ID', type: 'number', defaultValue: 1 },
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
  { prop: 'remark', label: '备注', type: 'textarea', span: 24 },
]
</script>
