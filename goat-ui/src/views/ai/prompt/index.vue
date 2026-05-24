<template>
  <AiTablePage
    resource="prompts"
    id-key="promptId"
    title="提示词管理"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入提示词名称、编码或场景"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../components/types'

const columns: AiTableColumn[] = [
  { prop: 'promptName', label: '提示词名称', minWidth: 160 },
  { prop: 'promptCode', label: '提示词编码', minWidth: 150 },
  { prop: 'promptType', label: '适用场景', minWidth: 130 },
  { prop: 'version', label: '版本', width: 90 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
  { prop: 'remark', label: '说明', minWidth: 220 },
]

const formFields: AiFormField[] = [
  { prop: 'promptName', label: '提示词名称', required: true },
  { prop: 'promptCode', label: '提示词编码', required: true },
  { prop: 'promptType', label: '适用场景', required: true, defaultValue: 'ASSISTANT' },
  { prop: 'version', label: '版本', required: true, defaultValue: '1.0.0' },
  { prop: 'systemPrompt', label: '系统提示词', type: 'textarea', span: 24, rows: 5 },
  { prop: 'userPrompt', label: '用户提示词', type: 'textarea', span: 24, rows: 4 },
  { prop: 'variables', label: '变量', placeholder: '例如 question,context,citations', span: 24 },
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
