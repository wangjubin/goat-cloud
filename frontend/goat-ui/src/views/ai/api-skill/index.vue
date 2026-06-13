<template>
  <div class="skill-page" v-loading="loading">
    <div class="skill-header">
      <div class="skill-header__title">
        <h2>API Skills</h2>
        <span class="skill-header__sub">外部 API 工具化 · 自定义函数调用</span>
      </div>
      <div class="skill-header__actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon><span>刷新</span>
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon><span>新增 Skill</span>
        </el-button>
      </div>
    </div>

    <div class="skill-stats">
      <div class="skill-stat">
        <span class="skill-stat__label">Skill 总数</span>
        <strong class="skill-stat__value">{{ stats.total }}</strong>
      </div>
      <div class="skill-stat skill-stat--primary">
        <span class="skill-stat__label">已启用</span>
        <strong class="skill-stat__value">{{ stats.enabled }}</strong>
      </div>
      <div class="skill-stat skill-stat--success">
        <span class="skill-stat__label">GET</span>
        <strong class="skill-stat__value">{{ stats.get }}</strong>
      </div>
      <div class="skill-stat skill-stat--warning">
        <span class="skill-stat__label">POST</span>
        <strong class="skill-stat__value">{{ stats.post }}</strong>
      </div>
    </div>

    <div class="skill-filters">
      <el-input v-model="filters.keyword" placeholder="搜索 Skill 名称、编码或接口地址" clearable
        @keyup.enter="loadData" @clear="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filters.skillType" placeholder="类型" clearable @change="loadData">
        <el-option label="REST" value="REST" />
        <el-option label="GraphQL" value="GRAPHQL" />
        <el-option label="gRPC" value="GRPC" />
      </el-select>
      <el-select v-model="filters.authType" placeholder="鉴权" clearable @change="loadData">
        <el-option label="无" value="NONE" />
        <el-option label="Bearer" value="BEARER" />
        <el-option label="API Key" value="API_KEY" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable @change="loadData">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-alert v-if="errorMessage" class="skill-alert" type="error" :title="errorMessage" show-icon />
    <el-empty v-if="!loading && records.length === 0" description="暂无 API Skill" />

    <div v-else class="skill-grid">
      <div v-for="row in records" :key="row.apiSkillId" class="skill-card"
        :class="{ 'skill-card--disabled': row.status !== 'ENABLED' }">
        <div class="skill-card__header">
          <div class="skill-card__title">
            <span class="skill-card__method" :class="`skill-card__method--${(row.httpMethod || 'GET').toLowerCase()}`">
              {{ row.httpMethod || 'GET' }}
            </span>
            <div class="skill-card__title-text">
              <h3 class="skill-card__name">{{ row.skillName }}</h3>
              <code class="skill-card__code">{{ row.skillCode }}</code>
            </div>
          </div>
          <el-tag size="small" effect="plain" :type="row.status === 'ENABLED' ? 'success' : 'info'">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </div>

        <div class="skill-card__endpoint">
          <code :title="row.endpoint">{{ row.endpoint || '—' }}</code>
        </div>

        <div class="skill-card__meta">
          <el-tag size="small" effect="plain" type="info">{{ row.skillType || 'REST' }}</el-tag>
          <el-tag size="small" effect="plain" :type="authTagType(row.authType)">
            <el-icon style="margin-right:2px"><Lock v-if="row.authType && row.authType !== 'NONE'" /></el-icon>
            {{ authLabel(row.authType) }}
          </el-tag>
        </div>

        <div v-if="row.remark" class="skill-card__remark">{{ row.remark }}</div>

        <div class="skill-card__actions">
          <el-button text type="primary" @click="openEdit(row)">
            <el-icon><Edit /></el-icon><span>编辑</span>
          </el-button>
          <el-button text type="danger" @click="deleteRow(row)">
            <el-icon><Delete /></el-icon><span>删除</span>
          </el-button>
        </div>
      </div>
    </div>

    <el-pagination v-if="total > filters.pageSize" class="skill-pagination" background
      layout="total, prev, pager, next" :total="total" :page-size="filters.pageSize"
      :current-page="filters.pageNum" @current-change="handlePageChange" />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑 Skill' : '新增 Skill'"
      width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Skill 名称" prop="skillName">
              <el-input v-model="form.skillName" placeholder="例如：查询订单" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Skill 编码" prop="skillCode">
              <el-input v-model="form.skillCode" placeholder="例如：query-order" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Skill 类型" prop="skillType">
              <el-select v-model="form.skillType" placeholder="请选择" style="width:100%">
                <el-option label="REST" value="REST" />
                <el-option label="GraphQL" value="GRAPHQL" />
                <el-option label="gRPC" value="GRPC" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="HTTP 方法" prop="httpMethod">
              <el-select v-model="form.httpMethod" placeholder="请选择" style="width:100%">
                <el-option label="GET" value="GET" />
                <el-option label="POST" value="POST" />
                <el-option label="PUT" value="PUT" />
                <el-option label="DELETE" value="DELETE" />
                <el-option label="PATCH" value="PATCH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="鉴权方式" prop="authType">
              <el-select v-model="form.authType" placeholder="请选择" style="width:100%">
                <el-option label="无" value="NONE" />
                <el-option label="Bearer Token" value="BEARER" />
                <el-option label="API Key" value="API_KEY" />
                <el-option label="Basic Auth" value="BASIC" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="接口地址" prop="endpoint">
              <el-input v-model="form.endpoint" placeholder="https://api.example.com/v1/..." clearable />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="请求 Schema">
              <el-input v-model="form.requestSchema" type="textarea" :rows="4"
                placeholder='JSON Schema，例如 {"type":"object","properties":{...}}' />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="响应 Schema">
              <el-input v-model="form.responseSchema" type="textarea" :rows="4"
                placeholder='JSON Schema，例如 {"type":"object","properties":{...}}' />
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
import {Delete, Edit, Lock, Plus, Refresh, Search} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {deleteAiResource, fetchAiDetail, fetchAiPage, saveAiResource} from '@/api/ai'

interface ApiSkill {
  apiSkillId: number; skillCode: string; skillName: string; skillType: string;
  endpoint: string; httpMethod: string; authType: string;
  requestSchema: string; responseSchema: string; status: string; remark: string
}

const loading = ref(false); const saving = ref(false)
const errorMessage = ref(''); const records = ref<ApiSkill[]>([]); const total = ref(0)
const filters = reactive({keyword: '', skillType: '', authType: '', status: '', pageNum: 1, pageSize: 20})
const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>(); const form = reactive<Record<string, any>>({})

const stats = computed(() => {
  const s = {total: 0, enabled: 0, get: 0, post: 0}
  for (const r of records.value) {
    s.total++
    if (r.status === 'ENABLED') s.enabled++
    if (r.httpMethod === 'GET') s.get++
    if (r.httpMethod === 'POST') s.post++
  }
  return s
})

const rules = computed<FormRules>(() => ({
  skillName: [{required: true, message: '请填写名称', trigger: 'blur'}],
  skillCode: [{required: true, message: '请填写编码', trigger: 'blur'}],
  skillType: [{required: true, message: '请选择类型', trigger: 'change'}],
  httpMethod: [{required: true, message: '请选择方法', trigger: 'change'}],
  authType: [{required: true, message: '请选择鉴权', trigger: 'change'}],
  endpoint: [{required: true, message: '请填写接口地址', trigger: 'blur'}],
}))

async function loadData() {
  loading.value = true; errorMessage.value = ''
  try {
    const payload: Record<string, unknown> = {pageNum: filters.pageNum, pageSize: filters.pageSize, keyword: filters.keyword}
    if (filters.skillType) payload.skillType = filters.skillType
    if (filters.authType) payload.authType = filters.authType
    if (filters.status) payload.status = filters.status
    const result = await fetchAiPage<ApiSkill>('api-skills', payload as any)
    records.value = result.records || []
    total.value = result.total || records.value.length
  } catch {
    errorMessage.value = '接口不可用，请检查后端服务和权限配置。'
    records.value = []; total.value = 0
  } finally { loading.value = false }
}

function resetFilters() {
  filters.keyword = ''; filters.skillType = ''; filters.authType = ''; filters.status = ''; filters.pageNum = 1; loadData()
}
function handlePageChange(p: number) { filters.pageNum = p; loadData() }
function resetForm() {
  Object.keys(form).forEach(k => delete form[k])
  form.skillType = 'REST'; form.httpMethod = 'GET'; form.authType = 'NONE'; form.status = 'ENABLED'
}
function openCreate() { editingId.value = null; resetForm(); dialogVisible.value = true }
async function openEdit(row: ApiSkill) {
  editingId.value = row.apiSkillId; resetForm()
  try { Object.assign(form, await fetchAiDetail('api-skills', row.apiSkillId)) }
  catch { Object.assign(form, row) }
  dialogVisible.value = true
}
async function submitForm() {
  await formRef.value?.validate(); saving.value = true
  try {
    await saveAiResource('api-skills', {...form})
    ElMessage.success('保存成功'); dialogVisible.value = false; await loadData()
  } finally { saving.value = false }
}
async function deleteRow(row: ApiSkill) {
  await ElMessageBox.confirm(`确认删除「${row.skillName}」吗？`, '删除确认', {type: 'warning'})
  await deleteAiResource('api-skills', [row.apiSkillId])
  ElMessage.success('删除成功'); await loadData()
}

function authLabel(t: string) { return {NONE: '无鉴权', BEARER: 'Bearer', API_KEY: 'API Key', BASIC: 'Basic'}[t] || t || '无' }
function authTagType(t: string) { return {BEARER: 'warning', API_KEY: 'warning', BASIC: 'warning'}[t] || 'info' }

onMounted(loadData)
</script>

<style scoped>
.skill-page { padding: 20px; }
.skill-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.skill-header__title h2 { margin: 0 0 4px; font-size: 20px; font-weight: 700; color: var(--tc-text); }
.skill-header__sub { font-size: 12px; color: var(--tc-text-secondary); }
.skill-header__actions { display: flex; gap: 8px; }

.skill-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.skill-stat { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-left: 3px solid var(--tc-primary); border-radius: var(--tc-radius-md); padding: 14px 18px; display: flex; flex-direction: column; gap: 4px; }
.skill-stat--primary { border-left-color: var(--tc-success); }
.skill-stat--success { border-left-color: #10b981; }
.skill-stat--warning { border-left-color: #f59e0b; }
.skill-stat__label { font-size: 12px; color: var(--tc-text-secondary); }
.skill-stat__value { font-size: 22px; font-weight: 700; color: var(--tc-text); }

.skill-filters { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr auto; gap: 8px; margin-bottom: 16px; }
.skill-filters :deep(.el-select), .skill-filters :deep(.el-input) { width: 100%; }

.skill-alert { margin-bottom: 12px; }

.skill-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 14px; }
.skill-card { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-radius: var(--tc-radius-md); padding: 16px; display: flex; flex-direction: column; gap: 10px; transition: all 0.2s ease; }
.skill-card:hover { box-shadow: var(--tc-shadow-md); transform: translateY(-2px); }
.skill-card--disabled { opacity: 0.6; }

.skill-card__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }
.skill-card__title { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.skill-card__method {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  font-family: 'SFMono-Regular', Consolas, monospace;
  flex-shrink: 0;
}
.skill-card__method--get { background: #10b981; }
.skill-card__method--post { background: #f59e0b; }
.skill-card__method--put { background: #6366f1; }
.skill-card__method--delete { background: #ef4444; }
.skill-card__method--patch { background: #06b6d4; }
.skill-card__title-text { min-width: 0; flex: 1; }
.skill-card__name { margin: 0 0 2px; font-size: 15px; font-weight: 700; color: var(--tc-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.skill-card__code { font-size: 11px; color: var(--tc-text-secondary); background: var(--tc-bg-soft, #fafafa); padding: 1px 6px; border-radius: 3px; }

.skill-card__endpoint { padding: 8px 10px; background: var(--tc-bg-soft, #fafafa); border-radius: 4px; }
.skill-card__endpoint code { font-size: 11px; word-break: break-all; color: var(--tc-text); }

.skill-card__meta { display: flex; gap: 6px; flex-wrap: wrap; }

.skill-card__remark { font-size: 12px; color: var(--tc-text-secondary); font-style: italic; padding: 4px 8px; background: var(--tc-bg-soft, #fafafa); border-radius: 4px; }

.skill-card__actions { display: flex; justify-content: flex-end; gap: 4px; }

.skill-pagination { justify-content: flex-end; margin-top: 16px; }

@media (max-width: 960px) {
  .skill-filters { grid-template-columns: 1fr 1fr; }
  .skill-stats { grid-template-columns: repeat(2, 1fr); }
  .skill-grid { grid-template-columns: 1fr; }
}
</style>
