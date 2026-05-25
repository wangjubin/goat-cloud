<template>
  <div class="node-config-editor">
    <el-form label-width="120px" size="small">
      <!-- NL2SQL config -->
      <template v-if="nodeType === 'NL2SQL'">
        <el-form-item label="模型">
          <el-select v-model="config.modelId" placeholder="选择模型" clearable filterable @focus="loadModels">
            <el-option v-for="m in models" :key="m.modelId" :label="m.modelName" :value="m.modelId" />
          </el-select>
        </el-form-item>
        <el-form-item label="Temperature">
          <el-input-number v-model="config.temperature" :min="0" :max="2" :step="0.1" :precision="1" />
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="config.maxTokens" :min="100" :max="32000" :step="100" />
        </el-form-item>
      </template>

      <!-- RAG_SEARCH config -->
      <template v-else-if="nodeType === 'RAG_SEARCH'">
        <el-form-item label="知识库">
          <el-select v-model="config.knowledgeBaseId" placeholder="选择知识库" clearable filterable @focus="loadKnowledgeBases">
            <el-option v-for="kb in knowledgeBases" :key="kb.knowledgeBaseId" :label="kb.knowledgeBaseName" :value="kb.knowledgeBaseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="Top K">
          <el-input-number v-model="config.topK" :min="1" :max="50" />
        </el-form-item>
      </template>

      <!-- CHATBI config -->
      <template v-else-if="nodeType === 'CHATBI'">
        <el-form-item label="数据源">
          <el-select v-model="config.datasourceId" placeholder="选择数据源" clearable filterable @focus="loadDatasources">
            <el-option v-for="ds in datasources" :key="ds.datasourceId" :label="ds.datasourceName" :value="ds.datasourceId" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据集">
          <el-select v-model="config.datasetId" placeholder="选择数据集" clearable filterable @focus="loadDatasets">
            <el-option v-for="d in datasets" :key="d.datasetId" :label="d.datasetName" :value="d.datasetId" />
          </el-select>
        </el-form-item>
        <el-form-item label="Limit">
          <el-input-number v-model="config.limit" :min="1" :max="10000" />
        </el-form-item>
      </template>

      <!-- AGENT config -->
      <template v-else-if="nodeType === 'AGENT'">
        <el-form-item label="智能体">
          <el-select v-model="config.agentId" placeholder="选择智能体" clearable filterable @focus="loadAgents">
            <el-option v-for="a in agents" :key="a.agentId" :label="a.agentName" :value="a.agentId" />
          </el-select>
        </el-form-item>
      </template>

      <!-- CHAT config -->
      <template v-else-if="nodeType === 'CHAT'">
        <el-form-item label="模型">
          <el-select v-model="config.modelId" placeholder="选择模型" clearable filterable @focus="loadModels">
            <el-option v-for="m in models" :key="m.modelId" :label="m.modelName" :value="m.modelId" />
          </el-select>
        </el-form-item>
        <el-form-item label="System Prompt">
          <el-input v-model="config.systemPrompt" type="textarea" :rows="3" placeholder="系统提示词" />
        </el-form-item>
        <el-form-item label="Temperature">
          <el-input-number v-model="config.temperature" :min="0" :max="2" :step="0.1" :precision="1" />
        </el-form-item>
      </template>

      <!-- SQL_EXECUTION config -->
      <template v-else-if="nodeType === 'SQL_EXECUTION'">
        <el-form-item label="Max Rows">
          <el-input-number v-model="config.maxRows" :min="1" :max="10000" />
        </el-form-item>
        <el-form-item label="Timeout (ms)">
          <el-input-number v-model="config.timeout" :min="1000" :max="300000" :step="1000" />
        </el-form-item>
      </template>

      <!-- GATEWAY config -->
      <template v-else-if="nodeType === 'GATEWAY'">
        <el-form-item label="模式">
          <el-select v-model="config.mode" placeholder="选择模式">
            <el-option label="XOR (排他)" value="XOR" />
            <el-option label="AND (并行)" value="AND" />
            <el-option label="OR (任意)" value="OR" />
          </el-select>
        </el-form-item>
      </template>

      <!-- Fallback: raw JSON textarea -->
      <template v-else>
        <el-form-item label="配置 JSON">
          <el-input :model-value="modelValue" type="textarea" :rows="4" @update:model-value="emit('update:modelValue', $event)" />
        </el-form-item>
      </template>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { fetchAiPage } from '@/api/ai'

const props = defineProps<{
  modelValue: string
  nodeType?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const config = reactive<Record<string, any>>({})

const models = ref<any[]>([])
const knowledgeBases = ref<any[]>([])
const datasources = ref<any[]>([])
const datasets = ref<any[]>([])
const agents = ref<any[]>([])

function parseConfig(json: string) {
  Object.keys(config).forEach(k => delete config[k])
  if (!json || json.trim() === '') return
  try {
    const parsed = JSON.parse(json)
    Object.assign(config, parsed)
  } catch {}
}

function syncConfig() {
  emit('update:modelValue', JSON.stringify(config))
}

onMounted(() => parseConfig(props.modelValue))
watch(() => props.modelValue, (v) => parseConfig(v))
watch(config, () => syncConfig(), { deep: true })

async function loadModels() {
  if (models.value.length) return
  try { models.value = (await fetchAiPage('models', { pageNum: 1, pageSize: 100 })).records || [] } catch {}
}
async function loadKnowledgeBases() {
  if (knowledgeBases.value.length) return
  try { knowledgeBases.value = (await fetchAiPage('knowledge-bases', { pageNum: 1, pageSize: 100 })).records || [] } catch {}
}
async function loadDatasources() {
  if (datasources.value.length) return
  try { datasources.value = (await fetchAiPage('chatbi/datasources', { pageNum: 1, pageSize: 100 })).records || [] } catch {}
}
async function loadDatasets() {
  if (datasets.value.length) return
  try { datasets.value = (await fetchAiPage('chatbi/datasets', { pageNum: 1, pageSize: 100 })).records || [] } catch {}
}
async function loadAgents() {
  if (agents.value.length) return
  try { agents.value = (await fetchAiPage('agents', { pageNum: 1, pageSize: 100 })).records || [] } catch {}
}
</script>