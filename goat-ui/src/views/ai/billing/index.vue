<template>
  <AiTablePage
    resource="billing"
    id-key="billingId"
    title="账单统计"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入会话、模型或调用方"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../components/types'

const columns: AiTableColumn[] = [
  { prop: 'conversationId', label: '会话ID', minWidth: 160 },
  { prop: 'provider', label: '厂商', minWidth: 120 },
  { prop: 'modelCode', label: '模型', minWidth: 130 },
  { prop: 'bizType', label: '调用方', minWidth: 120 },
  { prop: 'totalTokens', label: 'Token', width: 100 },
  { prop: 'costAmount', label: '费用', width: 100 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
]

const formFields: AiFormField[] = [
  { prop: 'conversationId', label: '会话ID' },
  { prop: 'provider', label: '厂商', defaultValue: 'OpenAI Compatible' },
  { prop: 'modelCode', label: '模型编码', required: true },
  { prop: 'bizType', label: '调用方', required: true, defaultValue: 'AI_ASSISTANT' },
  { prop: 'promptTokens', label: '输入 Token', type: 'number', defaultValue: 0 },
  { prop: 'completionTokens', label: '输出 Token', type: 'number', defaultValue: 0 },
  { prop: 'totalTokens', label: '总 Token', type: 'number', defaultValue: 0 },
  { prop: 'costAmount', label: '费用', type: 'number', defaultValue: 0 },
  { prop: 'currency', label: '币种', defaultValue: 'CNY' },
  { prop: 'requestTime', label: '请求时间', type: 'datetime' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    required: true,
    defaultValue: 'SUCCESS',
    options: [
      { label: '成功', value: 'SUCCESS' },
      { label: '失败', value: 'FAILED' },
    ],
  },
  { prop: 'remark', label: '备注', type: 'textarea', span: 24 },
]
</script>
