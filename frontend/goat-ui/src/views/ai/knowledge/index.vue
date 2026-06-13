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
import {computed, onMounted, ref} from 'vue'
import AiTablePage from '../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../components/types'
import {fetchAiList} from '@/api/ai'

const columns: AiTableColumn[] = [
  { prop: 'knowledgeBaseName', label: '知识库名称', minWidth: 170 },
  { prop: 'knowledgeBaseCode', label: '知识库编码', minWidth: 150 },
  { prop: 'embeddingModel', label: '向量模型', minWidth: 150 },
  { prop: 'documentCount', label: '文档数', width: 90 },
  { prop: 'chunkCount', label: '切片数', width: 90 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
  { prop: 'remark', label: '说明', minWidth: 200 },
]

interface SelectOption {
  label: string
  value: string | number
}

const vectorConfigOptions = ref<SelectOption[]>([])
const embeddingModelOptions = ref<SelectOption[]>([])

const formFields = computed<AiFormField[]>(() => [
  { prop: 'knowledgeBaseName', label: '知识库名称', required: true },
  { prop: 'knowledgeBaseCode', label: '知识库编码', required: true },
  { prop: 'description', label: '描述', type: 'textarea', span: 24 },
  {
    prop: 'vectorConfigId',
    label: '向量配置',
    type: 'select',
    options: vectorConfigOptions.value,
    placeholder: '请选择向量配置',
  },
  {
    prop: 'embeddingModel',
    label: '向量模型',
    type: 'select',
    options: embeddingModelOptions.value,
    placeholder: '请选择向量模型',
  },
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
])

onMounted(() => {
  loadVectorConfigOptions()
  loadEmbeddingModelOptions()
})

async function loadVectorConfigOptions() {
  try {
    const result = await fetchAiList<Record<string, unknown>>('vector-configs')
    vectorConfigOptions.value = result.map((item) => ({
      label: `${item.configName || item.name} (${item.provider || ''})`,
      value: item.vectorConfigId || item.id,
    }))
  } catch (error) {
    console.error('Failed to load vector configs:', error)
  }
}

async function loadEmbeddingModelOptions() {
  try {
    const result = await fetchAiList<Record<string, unknown>>('models')
    embeddingModelOptions.value = result
      .filter((item) => item.modelType === 'EMBEDDING' || item.modelType === 'embedding')
      .map((item) => ({
        label: `${item.modelName || item.name} (${item.modelCode || item.code || ''})`,
        value: item.modelCode || item.code || item.modelName || item.name,
      }))
    if (embeddingModelOptions.value.length === 0) {
      embeddingModelOptions.value = [
        { label: 'text-embedding-3-small', value: 'text-embedding-3-small' },
        { label: 'text-embedding-3-large', value: 'text-embedding-3-large' },
        { label: 'text-embedding-ada-002', value: 'text-embedding-ada-002' },
      ]
    }
  } catch (error) {
    console.error('Failed to load embedding models:', error)
    embeddingModelOptions.value = [
      { label: 'text-embedding-3-small', value: 'text-embedding-3-small' },
      { label: 'text-embedding-3-large', value: 'text-embedding-3-large' },
    ]
  }
}
</script>
