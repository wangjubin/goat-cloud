<template>
  <AiCardPage
    resource="overview"
    title="AI 问数总览"
    description="统一管理问数数据源、数据表、数据集与业务术语，为自然语言取数提供元数据基础。"
    :fallback-cards="fallbackCards"
  >
    <div class="table-toolbar ask-toolbar">
      <div class="table-toolbar__title">问数链路</div>
    </div>
    <el-row :gutter="12">
      <el-col v-for="item in steps" :key="item.title" :xs="24" :sm="12" :lg="6">
        <el-card shadow="never" class="ask-step">
          <b>{{ item.title }}</b>
          <span>{{ item.description }}</span>
        </el-card>
      </el-col>
    </el-row>
  </AiCardPage>
</template>

<script setup lang="ts">
import AiCardPage from '../components/AiCardPage.vue'
import type {AiMetricCard} from '../components/types'

const fallbackCards: AiMetricCard[] = [
  { title: '数据源', value: 4, description: '已接入数据库、接口或文件源' },
  { title: '数据表', value: 28, description: '完成字段扫描和语义标注' },
  { title: '数据集', value: 9, description: '可被问数助手直接使用' },
  { title: '业务术语', value: 36, description: '沉淀指标口径和别名词库' },
]

const steps = [
  { title: '数据源接入', description: '配置连接信息和权限边界。' },
  { title: '表结构同步', description: '同步字段、类型、主外键和样例值。' },
  { title: '数据集编排', description: '把多表关系与指标口径组织成问数主题。' },
  { title: '自然语言问数', description: '结合术语、提示词和模型生成查询结果。' },
]
</script>

<style scoped>
.ask-toolbar {
  margin-top: 12px;
}

.ask-step {
  border-radius: 4px;
  margin-bottom: 12px;
}

.ask-step b {
  display: block;
  color: #303133;
  font-size: 15px;
  margin-bottom: 8px;
}

.ask-step span {
  color: var(--tc-subtle);
  font-size: 13px;
  line-height: 1.6;
}
</style>
