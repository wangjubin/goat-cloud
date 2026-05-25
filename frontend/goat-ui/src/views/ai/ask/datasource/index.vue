<template>
  <AiTablePage
    resource="chatbi/datasources"
    id-key="datasourceId"
    title="问数数据源"
    :columns="columns"
    :form-fields="formFields"
    keyword-placeholder="请输入数据源名称、编码或连接地址"
  />
</template>

<script setup lang="ts">
import AiTablePage from '../../components/AiTablePage.vue'
import type {AiFormField, AiTableColumn} from '../../components/types'

const columns: AiTableColumn[] = [
  { prop: 'datasourceName', label: '数据源名称', minWidth: 160 },
  { prop: 'datasourceCode', label: '数据源编码', minWidth: 140 },
  { prop: 'datasourceType', label: '类型', width: 110 },
  { prop: 'jdbcUrl', label: '连接地址', minWidth: 240 },
  { prop: 'username', label: '账号', minWidth: 120 },
  { prop: 'status', label: '状态', width: 100, type: 'status' },
]

const formFields: AiFormField[] = [
  { prop: 'datasourceName', label: '数据源名称', required: true },
  { prop: 'datasourceCode', label: '数据源编码', required: true },
  { prop: 'datasourceType', label: '数据库类型', required: true, defaultValue: 'POSTGRESQL' },
  { prop: 'jdbcUrl', label: 'JDBC 地址', span: 24, required: true },
  { prop: 'username', label: '账号' },
  { prop: 'credentialRef', label: '凭据引用', placeholder: '例如 ENV:POSTGRES_PASSWORD' },
  { prop: 'modelId', label: 'NL2SQL 模型', type: 'selectAsync',
    apiPath: 'models', valueField: 'modelId', labelField: 'modelName',
    placeholder: '留空则使用默认模型' },
  { prop: 'driverClassName', label: 'JDBC 驱动', defaultValue: 'org.postgresql.Driver' },
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
