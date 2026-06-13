<template>
  <div class="prompt-page" v-loading="loading">
    <div class="prompt-header">
      <div class="prompt-header__title">
        <h2>提示词管理</h2>
        <span class="prompt-header__sub">提示词模板 · 变量定义 · 版本管理</span>
      </div>
      <div class="prompt-header__actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon>
          <span>刷新</span>
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          <span>新建提示词</span>
        </el-button>
      </div>
    </div>

    <div class="prompt-stats">
      <div class="prompt-stat">
        <span class="prompt-stat__label">提示词总数</span>
        <strong class="prompt-stat__value">{{ stats.total }}</strong>
      </div>
      <div class="prompt-stat prompt-stat--primary">
        <span class="prompt-stat__label">已启用</span>
        <strong class="prompt-stat__value">{{ stats.enabled }}</strong>
      </div>
      <div class="prompt-stat prompt-stat--muted">
        <span class="prompt-stat__label">已停用</span>
        <strong class="prompt-stat__value">{{ stats.disabled }}</strong>
      </div>
      <div class="prompt-stat prompt-stat--success">
        <span class="prompt-stat__label">使用场景</span>
        <strong class="prompt-stat__value">{{ typeCount }}</strong>
      </div>
    </div>

    <div class="prompt-filters">
      <el-input v-model="filters.keyword" placeholder="搜索提示词名称、编码或场景" clearable
        @keyup.enter="loadData" @clear="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filters.promptType" placeholder="适用场景" clearable @change="loadData">
        <el-option v-for="t in promptTypes" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable @change="loadData">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-alert v-if="errorMessage" class="prompt-alert" type="error" :title="errorMessage" show-icon />
    <el-empty v-if="!loading && records.length === 0" description="暂无提示词" />

    <div v-else class="prompt-grid">
      <div v-for="row in records" :key="row.promptId" class="prompt-card"
        :class="{ 'prompt-card--disabled': row.status !== 'ENABLED' }">
        <div class="prompt-card__header">
          <div class="prompt-card__title">
            <h3 class="prompt-card__name">{{ row.promptName }}</h3>
            <div class="prompt-card__tags">
              <el-tag size="small" effect="plain" :type="typeTagType(row.promptType)">
                {{ typeLabel(row.promptType) }}
              </el-tag>
              <el-tag size="small" effect="plain" type="info">v{{ row.version || '1.0.0' }}</el-tag>
              <el-tag size="small" effect="plain" :type="row.status === 'ENABLED' ? 'success' : 'info'">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </div>
          </div>
        </div>
        <code class="prompt-card__code">{{ row.promptCode }}</code>
        <div v-if="row.systemPrompt" class="prompt-card__system">
          <span class="prompt-card__label">系统提示词</span>
          <p>{{ row.systemPrompt }}</p>
        </div>
        <div v-if="row.userPrompt" class="prompt-card__user">
          <span class="prompt-card__label">用户提示词</span>
          <p>{{ row.userPrompt }}</p>
        </div>
        <div v-if="row.variables" class="prompt-card__vars">
          <span class="prompt-card__label">变量</span>
          <div class="prompt-card__var-list">
            <el-tag v-for="v in parseVars(row.variables)" :key="v" size="small" effect="plain">{{ v }}</el-tag>
          </div>
        </div>
        <div v-if="row.remark" class="prompt-card__remark">{{ row.remark }}</div>
        <div class="prompt-card__actions">
          <el-button text type="primary" @click="openEdit(row)">
            <el-icon><Edit /></el-icon><span>编辑</span>
          </el-button>
          <el-button text type="danger" @click="deleteRow(row)">
            <el-icon><Delete /></el-icon><span>删除</span>
          </el-button>
        </div>
      </div>
    </div>

    <el-pagination v-if="total > filters.pageSize" class="prompt-pagination" background
      layout="total, prev, pager, next" :total="total" :page-size="filters.pageSize"
      :current-page="filters.pageNum" @current-change="handlePageChange" />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑提示词' : '新建提示词'"
      width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="提示词名称" prop="promptName">
              <el-input v-model="form.promptName" placeholder="例如：通用助手" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提示词编码" prop="promptCode">
              <el-input v-model="form.promptCode" placeholder="例如：general-assistant" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用场景" prop="promptType">
              <el-select v-model="form.promptType" placeholder="请选择" style="width: 100%">
                <el-option v-for="t in promptTypes" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本" prop="version">
              <el-input v-model="form.version" placeholder="1.0.0" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="系统提示词">
              <el-input v-model="form.systemPrompt" type="textarea" :rows="5" placeholder="可选：系统角色设定" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="用户提示词">
              <el-input v-model="form.userPrompt" type="textarea" :rows="4" placeholder="可选：用户输入模板" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="变量">
              <el-input v-model="form.variables" placeholder="例如 question,context,citations" />
              <div class="prompt-form__hint">使用 <code v-pre>{{var}}</code> 占位，多个用英文逗号分隔</div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio-button value="ENABLED">启用</el-radio-button>
                <el-radio-button value="DISABLED">停用</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="说明">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {Delete, Edit, Plus, Refresh, Search} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {deleteAiResource, fetchAiDetail, fetchAiPage, saveAiResource} from '@/api/ai'

interface Prompt {
  promptId: number; promptName: string; promptCode: string; promptType: string;
  systemPrompt: string; userPrompt: string; variables: string; version: string;
  status: string; remark: string
}

const promptTypes = [
  {value: 'ASSISTANT', label: '智能助手'},
  {value: 'CHAT', label: '对话'},
  {value: 'RAG', label: 'RAG 检索'},
  {value: 'AGENT', label: '智能体'},
  {value: 'CODE', label: '代码生成'},
  {value: 'TRANSLATE', label: '翻译'},
  {value: 'SUMMARIZE', label: '摘要'},
]

const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const records = ref<Prompt[]>([])
const total = ref(0)
const filters = reactive({keyword: '', promptType: '', status: '', pageNum: 1, pageSize: 20})
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<Record<string, any>>({})

const stats = computed(() => {
  const s = {total: 0, enabled: 0, disabled: 0}
  for (const r of records.value) {
    s.total++
    if (r.status === 'ENABLED') s.enabled++
    else s.disabled++
  }
  return s
})
const typeCount = computed(() => new Set(records.value.map(r => r.promptType).filter(Boolean)).size)

const rules = computed<FormRules>(() => ({
  promptName: [{required: true, message: '请填写名称', trigger: 'blur'}],
  promptCode: [{required: true, message: '请填写编码', trigger: 'blur'}],
  promptType: [{required: true, message: '请选择场景', trigger: 'change'}],
  version: [{required: true, message: '请填写版本', trigger: 'blur'}],
}))

async function loadData() {
  loading.value = true; errorMessage.value = ''
  try {
    const payload: Record<string, unknown> = {pageNum: filters.pageNum, pageSize: filters.pageSize, keyword: filters.keyword}
    if (filters.promptType) payload.promptType = filters.promptType
    if (filters.status) payload.status = filters.status
    const result = await fetchAiPage<Prompt>('prompts', payload as any)
    records.value = result.records || []
    total.value = result.total || records.value.length
  } catch {
    errorMessage.value = '接口不可用，请检查后端服务和权限配置。'
    records.value = []; total.value = 0
  } finally { loading.value = false }
}

function resetFilters() { filters.keyword = ''; filters.promptType = ''; filters.status = ''; filters.pageNum = 1; loadData() }
function handlePageChange(p: number) { filters.pageNum = p; loadData() }
function resetForm() { Object.keys(form).forEach(k => delete form[k]); form.version = '1.0.0'; form.status = 'ENABLED' }
function openCreate() { editingId.value = null; resetForm(); dialogVisible.value = true }
async function openEdit(row: Prompt) {
  editingId.value = row.promptId; resetForm()
  try { Object.assign(form, await fetchAiDetail('prompts', row.promptId)) }
  catch { Object.assign(form, row) }
  dialogVisible.value = true
}
async function submitForm() {
  await formRef.value?.validate(); saving.value = true
  try {
    await saveAiResource('prompts', {...form})
    ElMessage.success('保存成功'); dialogVisible.value = false; await loadData()
  } finally { saving.value = false }
}
async function deleteRow(row: Prompt) {
  await ElMessageBox.confirm(`确认删除「${row.promptName}」吗？`, '删除确认', {type: 'warning'})
  await deleteAiResource('prompts', [row.promptId])
  ElMessage.success('删除成功'); await loadData()
}

function typeLabel(t: string) { return promptTypes.find(x => x.value === t)?.label || t }
function typeTagType(t: string) {
  return {ASSISTANT: 'primary', CHAT: 'primary', RAG: 'success', AGENT: 'warning', CODE: 'danger', TRANSLATE: 'info', SUMMARIZE: 'info'}[t] || 'info'
}
function parseVars(s: string): string[] {
  return s ? s.split(',').map(v => v.trim()).filter(Boolean) : []
}

onMounted(loadData)
</script>

<style scoped>
.prompt-page { padding: 20px; }
.prompt-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.prompt-header__title h2 { margin: 0 0 4px; font-size: 20px; font-weight: 700; color: var(--tc-text); }
.prompt-header__sub { font-size: 12px; color: var(--tc-text-secondary); }
.prompt-header__actions { display: flex; gap: 8px; }

.prompt-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.prompt-stat { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-left: 3px solid var(--tc-primary); border-radius: var(--tc-radius-md); padding: 14px 18px; display: flex; flex-direction: column; gap: 4px; }
.prompt-stat--primary { border-left-color: var(--tc-success); }
.prompt-stat--muted { border-left-color: var(--tc-text-secondary); }
.prompt-stat--success { border-left-color: #06b6d4; }
.prompt-stat__label { font-size: 12px; color: var(--tc-text-secondary); }
.prompt-stat__value { font-size: 22px; font-weight: 700; color: var(--tc-text); }

.prompt-filters { display: grid; grid-template-columns: 2fr 1fr 1fr auto; gap: 8px; margin-bottom: 16px; }
.prompt-filters :deep(.el-select), .prompt-filters :deep(.el-input) { width: 100%; }

.prompt-alert { margin-bottom: 12px; }

.prompt-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 14px; }
.prompt-card { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-radius: var(--tc-radius-md); padding: 16px; display: flex; flex-direction: column; gap: 10px; transition: all 0.2s ease; }
.prompt-card:hover { box-shadow: var(--tc-shadow-md); transform: translateY(-2px); }
.prompt-card--disabled { opacity: 0.6; }

.prompt-card__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }
.prompt-card__title { flex: 1; min-width: 0; }
.prompt-card__name { margin: 0 0 6px; font-size: 15px; font-weight: 700; color: var(--tc-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.prompt-card__tags { display: flex; gap: 6px; flex-wrap: wrap; }

.prompt-card__code { font-size: 11px; color: var(--tc-text-secondary); background: var(--tc-bg-soft, #fafafa); padding: 2px 6px; border-radius: 3px; align-self: flex-start; }

.prompt-card__system, .prompt-card__user { background: var(--tc-bg-soft, #fafafa); border-radius: var(--tc-radius-sm); padding: 8px 10px; }
.prompt-card__label { display: block; font-size: 11px; color: var(--tc-text-secondary); margin-bottom: 4px; }
.prompt-card__system p, .prompt-card__user p { margin: 0; font-size: 12px; line-height: 1.6; color: var(--tc-text); display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }

.prompt-card__vars { display: flex; align-items: center; gap: 6px; font-size: 12px; flex-wrap: wrap; }
.prompt-card__var-list { display: flex; gap: 4px; flex-wrap: wrap; }

.prompt-card__remark { font-size: 12px; color: var(--tc-text-secondary); font-style: italic; padding: 4px 8px; background: var(--tc-bg-soft, #fafafa); border-radius: 4px; }

.prompt-card__actions { display: flex; justify-content: flex-end; gap: 4px; }

.prompt-form__hint { font-size: 11px; color: var(--tc-text-secondary); margin-top: 4px; }
.prompt-form__hint code { background: var(--tc-bg-soft, #fafafa); padding: 1px 4px; border-radius: 3px; }

.prompt-pagination { justify-content: flex-end; margin-top: 16px; }

@media (max-width: 960px) {
  .prompt-filters { grid-template-columns: 1fr 1fr; }
  .prompt-stats { grid-template-columns: repeat(2, 1fr); }
  .prompt-grid { grid-template-columns: 1fr; }
}
</style>
