<template>
  <AiTablePage
    resource="knowledge-bases"
    id-key="knowledgeBaseId"
    title="RAG 知识库"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入知识库名称或编码"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../components/types'

const columns: AiTableColumn[] = [
  { prop: 'knowledgeBaseName', label: '知识库名称', minWidth: 170 },
  { prop: 'knowledgeBaseCode', label: '知识库编码', minWidth: 150 },
  { prop: 'embeddingModel', label: '向量模型', minWidth: 150 },
  { prop: 'documentCount', label: '文档数', width: 90 },
  { prop: 'chunkCount', label: '切片数', width: 90 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
  { prop: 'remark', label: '说明', minWidth: 200 },
]

const formFields: AiFormField[] = [
  { prop: 'knowledgeBaseName', label: '知识库名称', required: true },
  { prop: 'knowledgeBaseCode', label: '知识库编码', required: true },
  { prop: 'description', label: '描述', type: 'textarea', span: 24 },
  { prop: 'vectorConfigId', label: '向量配置ID', type: 'number', defaultValue: 1 },
  { prop: 'embeddingModel', label: '向量模型', defaultValue: 'text-embedding' },
  { prop: 'embeddingDimension', label: '向量维度', type: 'number', defaultValue: 1536 },
  { prop: 'documentCount', label: '文档数', type: 'number', defaultValue: 0 },
  { prop: 'chunkCount', label: '切片数', type: 'number', defaultValue: 0 },
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
