<template>
  <AiTablePage
    resource="workflows"
    id-key="workflowId"
    title="流程编排"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入流程名称、编码或触发方式"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../components/types'

const columns: AiTableColumn[] = [
  { prop: 'workflowName', label: '流程名称', minWidth: 170 },
  { prop: 'workflowCode', label: '流程编码', minWidth: 140 },
  { prop: 'triggerType', label: '触发方式', width: 120 },
  { prop: 'version', label: '版本', width: 90 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
  { prop: 'remark', label: '说明', minWidth: 220 },
]

const formFields: AiFormField[] = [
  { prop: 'workflowName', label: '流程名称', required: true },
  { prop: 'workflowCode', label: '流程编码', required: true },
  { prop: 'description', label: '描述', type: 'textarea', span: 24 },
  {
    prop: 'triggerType',
    label: '触发方式',
    type: 'select',
    required: true,
    defaultValue: 'MANUAL',
    options: [
      { label: '手动触发', value: 'MANUAL' },
      { label: '定时触发', value: 'SCHEDULED' },
      { label: '事件触发', value: 'EVENT' },
    ],
  },
  { prop: 'version', label: '版本', defaultValue: '1.0.0' },
  { prop: 'graphJson', label: '流程图 JSON', type: 'textarea', span: 24, rows: 6 },
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
