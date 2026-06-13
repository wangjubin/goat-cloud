<template>
  <div class="wf-page" v-loading="loading">
    <div class="wf-header">
      <div class="wf-header__title">
        <h2>流程编排</h2>
        <span class="wf-header__sub">可视化编排 · 多步骤流程 · 触发调度</span>
      </div>
      <div class="wf-header__actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon><span>刷新</span>
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon><span>新建流程</span>
        </el-button>
      </div>
    </div>

    <div class="wf-stats">
      <div class="wf-stat">
        <span class="wf-stat__label">流程总数</span>
        <strong class="wf-stat__value">{{ stats.total }}</strong>
      </div>
      <div class="wf-stat wf-stat--primary">
        <span class="wf-stat__label">已启用</span>
        <strong class="wf-stat__value">{{ stats.enabled }}</strong>
      </div>
      <div class="wf-stat wf-stat--warning">
        <span class="wf-stat__label">手动触发</span>
        <strong class="wf-stat__value">{{ stats.manual }}</strong>
      </div>
      <div class="wf-stat wf-stat--success">
        <span class="wf-stat__label">定时/事件</span>
        <strong class="wf-stat__value">{{ stats.auto }}</strong>
      </div>
    </div>

    <div class="wf-filters">
      <el-input v-model="filters.keyword" placeholder="搜索流程名称、编码或触发方式" clearable
        @keyup.enter="loadData" @clear="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filters.triggerType" placeholder="触发方式" clearable @change="loadData">
        <el-option label="手动触发" value="MANUAL" />
        <el-option label="定时触发" value="SCHEDULED" />
        <el-option label="事件触发" value="EVENT" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable @change="loadData">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-alert v-if="errorMessage" class="wf-alert" type="error" :title="errorMessage" show-icon />
    <el-empty v-if="!loading && records.length === 0" description="暂无流程" />

    <div v-else class="wf-grid">
      <div v-for="row in records" :key="row.workflowId" class="wf-card"
        :class="{ 'wf-card--disabled': row.status !== 'ENABLED' }">
        <div class="wf-card__header">
          <div class="wf-card__title">
            <div class="wf-card__icon" :style="{background: triggerGradient(row.triggerType)}">
              <el-icon :size="22"><Share /></el-icon>
            </div>
            <div class="wf-card__title-text">
              <h3 class="wf-card__name">{{ row.workflowName }}</h3>
              <code class="wf-card__code">{{ row.workflowCode }}</code>
            </div>
          </div>
          <el-tag size="small" effect="plain" :type="row.status === 'ENABLED' ? 'success' : 'info'">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </div>

        <div v-if="row.description" class="wf-card__desc">{{ row.description }}</div>

        <div class="wf-card__meta">
          <el-tag size="small" effect="plain" :type="triggerTagType(row.triggerType)">
            <el-icon style="margin-right:2px"><Pointer v-if="row.triggerType === 'MANUAL'" /><Clock v-else-if="row.triggerType === 'SCHEDULED'" /><Bell v-else /></el-icon>
            {{ triggerLabel(row.triggerType) }}
          </el-tag>
          <el-tag size="small" effect="plain" type="info">v{{ row.version || '1.0.0' }}</el-tag>
          <el-tag size="small" effect="plain" type="info">{{ nodeCount(row.graphJson) }} 节点</el-tag>
        </div>

        <div v-if="row.remark" class="wf-card__remark">{{ row.remark }}</div>

        <div class="wf-card__actions">
          <el-button text type="primary" @click="openEdit(row)">
            <el-icon><Edit /></el-icon><span>编辑</span>
          </el-button>
          <el-button text type="danger" @click="deleteRow(row)">
            <el-icon><Delete /></el-icon><span>删除</span>
          </el-button>
        </div>
      </div>
    </div>

    <el-pagination v-if="total > filters.pageSize" class="wf-pagination" background
      layout="total, prev, pager, next" :total="total" :page-size="filters.pageSize"
      :current-page="filters.pageNum" @current-change="handlePageChange" />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑流程' : '新建流程'"
      width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="流程名称" prop="workflowName">
              <el-input v-model="form.workflowName" placeholder="例如：每日数据同步" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="流程编码" prop="workflowCode">
              <el-input v-model="form.workflowCode" placeholder="例如：daily-sync" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="触发方式" prop="triggerType">
              <el-select v-model="form.triggerType" placeholder="请选择" style="width:100%">
                <el-option label="手动触发" value="MANUAL" />
                <el-option label="定时触发" value="SCHEDULED" />
                <el-option label="事件触发" value="EVENT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本" prop="version">
              <el-input v-model="form.version" placeholder="1.0.0" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选：流程用途说明" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="流程图 JSON">
              <el-input v-model="form.graphJson" type="textarea" :rows="6"
                placeholder='{"nodes":[...], "edges":[...]}' />
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
            <el-form-item label="备注">
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
import {Bell, Clock, Delete, Edit, Plus, Pointer, Refresh, Search, Share} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {deleteAiResource, fetchAiDetail, fetchAiPage, saveAiResource} from '@/api/ai'

interface Workflow {
  workflowId: number; workflowCode: string; workflowName: string;
  description: string; triggerType: string; graphJson: string; version: string;
  status: string; remark: string
}

const loading = ref(false); const saving = ref(false)
const errorMessage = ref(''); const records = ref<Workflow[]>([]); const total = ref(0)
const filters = reactive({keyword: '', triggerType: '', status: '', pageNum: 1, pageSize: 20})
const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>(); const form = reactive<Record<string, any>>({})

const stats = computed(() => {
  const s = {total: 0, enabled: 0, manual: 0, auto: 0}
  for (const r of records.value) {
    s.total++
    if (r.status === 'ENABLED') s.enabled++
    if (r.triggerType === 'MANUAL') s.manual++
    else if (r.triggerType === 'SCHEDULED' || r.triggerType === 'EVENT') s.auto++
  }
  return s
})

const rules = computed<FormRules>(() => ({
  workflowName: [{required: true, message: '请填写名称', trigger: 'blur'}],
  workflowCode: [{required: true, message: '请填写编码', trigger: 'blur'}],
  triggerType: [{required: true, message: '请选择触发方式', trigger: 'change'}],
  version: [{required: true, message: '请填写版本', trigger: 'blur'}],
}))

async function loadData() {
  loading.value = true; errorMessage.value = ''
  try {
    const payload: Record<string, unknown> = {pageNum: filters.pageNum, pageSize: filters.pageSize, keyword: filters.keyword}
    if (filters.triggerType) payload.triggerType = filters.triggerType
    if (filters.status) payload.status = filters.status
    const result = await fetchAiPage<Workflow>('workflows', payload as any)
    records.value = result.records || []
    total.value = result.total || records.value.length
  } catch {
    errorMessage.value = '接口不可用，请检查后端服务和权限配置。'
    records.value = []; total.value = 0
  } finally { loading.value = false }
}

function resetFilters() { filters.keyword = ''; filters.triggerType = ''; filters.status = ''; filters.pageNum = 1; loadData() }
function handlePageChange(p: number) { filters.pageNum = p; loadData() }
function resetForm() { Object.keys(form).forEach(k => delete form[k]); form.triggerType = 'MANUAL'; form.version = '1.0.0'; form.status = 'ENABLED' }
function openCreate() { editingId.value = null; resetForm(); dialogVisible.value = true }
async function openEdit(row: Workflow) {
  editingId.value = row.workflowId; resetForm()
  try { Object.assign(form, await fetchAiDetail('workflows', row.workflowId)) }
  catch { Object.assign(form, row) }
  dialogVisible.value = true
}
async function submitForm() {
  await formRef.value?.validate(); saving.value = true
  try {
    await saveAiResource('workflows', {...form})
    ElMessage.success('保存成功'); dialogVisible.value = false; await loadData()
  } finally { saving.value = false }
}
async function deleteRow(row: Workflow) {
  await ElMessageBox.confirm(`确认删除「${row.workflowName}」吗？`, '删除确认', {type: 'warning'})
  await deleteAiResource('workflows', [row.workflowId])
  ElMessage.success('删除成功'); await loadData()
}

function triggerLabel(t: string) { return {MANUAL: '手动触发', SCHEDULED: '定时触发', EVENT: '事件触发'}[t] || t || '手动触发' }
function triggerTagType(t: string) { return {MANUAL: 'warning', SCHEDULED: 'success', EVENT: 'primary'}[t] || 'info' }
function triggerGradient(t: string) {
  return {MANUAL: 'linear-gradient(135deg,#f59e0b,#fbbf24)', SCHEDULED: 'linear-gradient(135deg,#10b981,#34d399)', EVENT: 'linear-gradient(135deg,#6366f1,#8b5cf6)'}[t] || 'linear-gradient(135deg,#6b7280,#9ca3af)'
}
function nodeCount(json: string): number {
  if (!json) return 0
  try { const p = JSON.parse(json); return Array.isArray(p.nodes) ? p.nodes.length : 0 } catch { return 0 }
}

onMounted(loadData)
</script>

<style scoped>
.wf-page { padding: 20px; }
.wf-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.wf-header__title h2 { margin: 0 0 4px; font-size: 20px; font-weight: 700; color: var(--tc-text); }
.wf-header__sub { font-size: 12px; color: var(--tc-text-secondary); }
.wf-header__actions { display: flex; gap: 8px; }

.wf-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.wf-stat { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-left: 3px solid var(--tc-primary); border-radius: var(--tc-radius-md); padding: 14px 18px; display: flex; flex-direction: column; gap: 4px; }
.wf-stat--primary { border-left-color: var(--tc-success); }
.wf-stat--warning { border-left-color: #f59e0b; }
.wf-stat--success { border-left-color: #10b981; }
.wf-stat__label { font-size: 12px; color: var(--tc-text-secondary); }
.wf-stat__value { font-size: 22px; font-weight: 700; color: var(--tc-text); }

.wf-filters { display: grid; grid-template-columns: 2fr 1fr 1fr auto; gap: 8px; margin-bottom: 16px; }
.wf-filters :deep(.el-select), .wf-filters :deep(.el-input) { width: 100%; }

.wf-alert { margin-bottom: 12px; }

.wf-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 14px; }
.wf-card { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-radius: var(--tc-radius-md); padding: 16px; display: flex; flex-direction: column; gap: 10px; transition: all 0.2s ease; }
.wf-card:hover { box-shadow: var(--tc-shadow-md); transform: translateY(-2px); }
.wf-card--disabled { opacity: 0.6; }

.wf-card__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }
.wf-card__title { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.wf-card__icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.wf-card__title-text { min-width: 0; flex: 1; }
.wf-card__name { margin: 0 0 2px; font-size: 15px; font-weight: 700; color: var(--tc-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wf-card__code { font-size: 11px; color: var(--tc-text-secondary); background: var(--tc-bg-soft, #fafafa); padding: 1px 6px; border-radius: 3px; }

.wf-card__desc { font-size: 12px; color: var(--tc-text-secondary); line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.wf-card__meta { display: flex; gap: 6px; flex-wrap: wrap; }

.wf-card__remark { font-size: 12px; color: var(--tc-text-secondary); font-style: italic; padding: 4px 8px; background: var(--tc-bg-soft, #fafafa); border-radius: 4px; }

.wf-card__actions { display: flex; justify-content: flex-end; gap: 4px; }

.wf-pagination { justify-content: flex-end; margin-top: 16px; }

@media (max-width: 960px) {
  .wf-filters { grid-template-columns: 1fr 1fr; }
  .wf-stats { grid-template-columns: repeat(2, 1fr); }
  .wf-grid { grid-template-columns: 1fr; }
}
</style>
