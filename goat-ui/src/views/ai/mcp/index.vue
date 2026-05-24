<template>
  <AiTablePage
    resource="mcp-tools"
    id-key="mcpToolId"
    title="AI MCP"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入工具名称、编码或服务名"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../components/types'

const columns: AiTableColumn[] = [
  { prop: 'toolName', label: '工具名称', minWidth: 160 },
  { prop: 'toolCode', label: '工具编码', minWidth: 140 },
  { prop: 'serverName', label: '服务名称', minWidth: 130 },
  { prop: 'transportType', label: '传输方式', width: 110 },
  { prop: 'endpoint', label: '服务地址', minWidth: 220 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
]

const formFields: AiFormField[] = [
  { prop: 'toolName', label: '工具名称', required: true },
  { prop: 'toolCode', label: '工具编码', required: true },
  { prop: 'serverName', label: '服务名称' },
  {
    prop: 'transportType',
    label: '传输方式',
    type: 'select',
    required: true,
    defaultValue: 'stdio',
    options: [
      { label: 'stdio', value: 'stdio' },
      { label: 'SSE', value: 'SSE' },
      { label: 'HTTP', value: 'HTTP' },
    ],
  },
  { prop: 'endpoint', label: '服务地址', span: 24 },
  { prop: 'inputSchema', label: '输入 Schema', type: 'textarea', span: 24 },
  { prop: 'outputSchema', label: '输出 Schema', type: 'textarea', span: 24 },
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
