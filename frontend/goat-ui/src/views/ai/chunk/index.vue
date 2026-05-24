<template>
  <AiTablePage
    resource="chunks"
    id-key="chunkId"
    title="切片管理"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入切片标题或内容"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../components/types'

const columns: AiTableColumn[] = [
  { prop: 'chunkIndex', label: '序号', width: 90 },
  { prop: 'title', label: '标题', minWidth: 180 },
  { prop: 'content', label: '内容预览', minWidth: 260 },
  { prop: 'tokenCount', label: 'Token', width: 90 },
  { prop: 'embeddingStatus', label: '向量状态', width: 110 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
]

const formFields: AiFormField[] = [
  { prop: 'knowledgeBaseId', label: '知识库ID', type: 'number', required: true, defaultValue: 1 },
  { prop: 'documentId', label: '文档ID', type: 'number', required: true, defaultValue: 1 },
  { prop: 'chunkIndex', label: '切片序号', type: 'number', required: true, defaultValue: 0 },
  { prop: 'title', label: '标题', required: true },
  { prop: 'content', label: '内容', type: 'textarea', span: 24, rows: 6, required: true },
  { prop: 'tokenCount', label: 'Token 数', type: 'number', defaultValue: 0 },
  { prop: 'embeddingStatus', label: '向量状态', defaultValue: 'PENDING' },
  { prop: 'embeddingVector', label: '向量数据', type: 'textarea', span: 24 },
  { prop: 'metadata', label: '元数据JSON', type: 'textarea', span: 24 },
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
