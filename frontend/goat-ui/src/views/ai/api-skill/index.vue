<template>
  <AiTablePage
    resource="api-skills"
    id-key="apiSkillId"
    title="API Skills"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入 Skill 名称、编码或接口"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../components/types'

const columns: AiTableColumn[] = [
  { prop: 'skillName', label: 'Skill 名称', minWidth: 160 },
  { prop: 'skillCode', label: 'Skill 编码', minWidth: 140 },
  { prop: 'skillType', label: '类型', width: 100 },
  { prop: 'httpMethod', label: '方法', width: 90 },
  { prop: 'endpoint', label: '接口地址', minWidth: 220 },
  { prop: 'authType', label: '鉴权', width: 110 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
]

const formFields: AiFormField[] = [
  { prop: 'skillName', label: 'Skill 名称', required: true },
  { prop: 'skillCode', label: 'Skill 编码', required: true },
  { prop: 'skillType', label: 'Skill 类型', required: true, defaultValue: 'REST' },
  {
    prop: 'httpMethod',
    label: 'HTTP 方法',
    type: 'select',
    required: true,
    defaultValue: 'GET',
    options: [
      { label: 'GET', value: 'GET' },
      { label: 'POST', value: 'POST' },
      { label: 'PUT', value: 'PUT' },
      { label: 'DELETE', value: 'DELETE' },
    ],
  },
  { prop: 'endpoint', label: '接口地址', span: 24 },
  {
    prop: 'authType',
    label: '鉴权方式',
    type: 'select',
    required: true,
    defaultValue: 'NONE',
    options: [
      { label: '无', value: 'NONE' },
      { label: 'Bearer Token', value: 'BEARER' },
      { label: 'API Key', value: 'API_KEY' },
    ],
  },
  { prop: 'requestSchema', label: '请求 Schema', type: 'textarea', span: 24 },
  { prop: 'responseSchema', label: '响应 Schema', type: 'textarea', span: 24 },
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
