<template>
  <AiTablePage
    resource="vector-configs"
    id-key="vectorConfigId"
    title="向量配置"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入配置名称、向量表或模型"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../components/types'

const columns: AiTableColumn[] = [
  { prop: 'configName', label: '配置名称', minWidth: 160 },
  { prop: 'provider', label: '存储引擎', minWidth: 120 },
  { prop: 'embeddingModel', label: '向量模型', minWidth: 150 },
  { prop: 'embeddingDimension', label: '维度', width: 90 },
  { prop: 'pgvectorTable', label: '向量表', minWidth: 160 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
  { prop: 'remark', label: '备注', minWidth: 180 },
]

const formFields: AiFormField[] = [
  { prop: 'configName', label: '配置名称', required: true },
  { prop: 'provider', label: '存储引擎', required: true, defaultValue: 'POSTGRESQL' },
  { prop: 'embeddingModel', label: '向量模型', required: true, defaultValue: 'text-embedding' },
  { prop: 'embeddingDimension', label: '向量维度', type: 'number', required: true, defaultValue: 1536 },
  { prop: 'distanceMetric', label: '距离算法', required: true, defaultValue: 'COSINE' },
  { prop: 'pgvectorTable', label: '向量表', required: true, defaultValue: 'ai_document_chunk' },
  { prop: 'indexType', label: '索引类型', required: true, defaultValue: 'IVFFLAT' },
  { prop: 'chunkSize', label: '切片长度', type: 'number', defaultValue: 800 },
  { prop: 'chunkOverlap', label: '切片重叠', type: 'number', defaultValue: 120 },
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
