<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view" v-loading="loading">
      <div class="toolbar">
        <div>
          <h3>{{ title }}</h3>
          <p>{{ description }}</p>
        </div>
        <el-button type="primary" @click="loadData">刷新</el-button>
      </div>

      <el-alert v-if="errorMessage" class="ai-page-alert" type="error" :title="errorMessage" show-icon />
      <el-empty v-if="!loading && cards.length === 0" description="暂无数据" />

      <el-row v-else :gutter="12">
        <el-col v-for="card in cards" :key="card.title" :xs="24" :sm="12" :lg="6">
          <el-card shadow="never" class="ai-card">
            <span>{{ card.title }}</span>
            <strong>{{ card.value }}</strong>
            <p>{{ card.description }}</p>
          </el-card>
        </el-col>
      </el-row>

      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {fetchAiList, fetchAiOverview} from '@/api/ai'
import type {AiMetricCard} from './types'

const props = withDefaults(
  defineProps<{
    resource: string
    title: string
    description: string
    fallbackCards?: AiMetricCard[]
  }>(),
  {
    fallbackCards: () => [],
  },
)

const cards = ref<AiMetricCard[]>([])
const loading = ref(false)
const errorMessage = ref('')

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    if (props.resource === 'overview') {
      const result = await fetchAiOverview()
      cards.value = normalizeOverviewCards(result)
    } else {
      const result = await fetchAiList<AiMetricCard>(props.resource)
      cards.value = result || []
    }
  } catch (error) {
    errorMessage.value = '接口暂不可用，已展示本地示例数据。'
    cards.value = props.fallbackCards
  } finally {
    loading.value = false
  }
}

function normalizeOverviewCards(result: Record<string, unknown>) {
  const modules = Array.isArray(result.modules) ? result.modules : []
  return modules.slice(0, 4).map((item) => {
    const module = item as Record<string, unknown>
    return {
      title: String(module.name || module.code || 'AI 模块'),
      value: Number(module.count || 0),
      description: String(module.description || module.basePath || '-'),
    }
  })
}

onMounted(loadData)
</script>

<style scoped>
.ai-page-alert {
  margin-bottom: 12px;
}

.ai-card {
  border-radius: 4px;
  margin-bottom: 12px;
}

.ai-card span {
  display: block;
  color: var(--tc-subtle);
  font-size: 13px;
  margin-bottom: 10px;
}

.ai-card strong {
  display: block;
  color: #303133;
  font-size: 24px;
  line-height: 1;
  margin-bottom: 10px;
}

.ai-card p {
  color: var(--tc-subtle);
  font-size: 13px;
  line-height: 1.5;
  margin: 0;
}
</style>
