<template>
  <AiTablePage
    resource="models"
    id-key="modelId"
    title="模型配置"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入模型名称、编码或厂商"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../components/types'

const columns: AiTableColumn[] = [
  { prop: 'modelName', label: '模型名称', minWidth: 160 },
  { prop: 'modelCode', label: '模型编码', minWidth: 150 },
  { prop: 'provider', label: '厂商', minWidth: 120 },
  { prop: 'modelType', label: '类型', width: 110 },
  { prop: 'contextWindow', label: '上下文', width: 110 },
  { prop: 'defaultModel', label: '默认', width: 90, type: 'boolean' },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
  { prop: 'remark', label: '备注', minWidth: 180 },
]

const formFields: AiFormField[] = [
  { prop: 'modelName', label: '模型名称', required: true },
  { prop: 'modelCode', label: '模型编码', required: true },
  { prop: 'provider', label: '厂商', required: true, defaultValue: 'OpenAI Compatible' },
  {
    prop: 'modelType',
    label: '模型类型',
    type: 'select',
    required: true,
    defaultValue: 'CHAT',
    options: [
      { label: '对话模型', value: 'CHAT' },
      { label: '向量模型', value: 'EMBEDDING' },
      { label: '重排序模型', value: 'RERANK' },
    ],
  },
  { prop: 'endpoint', label: '接口地址', span: 24 },
  { prop: 'apiKeyRef', label: '密钥引用', placeholder: '例如 ENV:OPENAI_API_KEY' },
  { prop: 'contextWindow', label: '上下文窗口', type: 'number', defaultValue: 8192 },
  { prop: 'defaultModel', label: '默认模型', type: 'switch', defaultValue: false },
  { prop: 'sortOrder', label: '排序', type: 'number', defaultValue: 0 },
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
