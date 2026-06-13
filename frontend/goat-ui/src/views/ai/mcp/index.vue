<template>
  <div class="mcp-page" v-loading="loading">
    <div class="mcp-header">
      <div class="mcp-header__title">
        <h2>AI MCP</h2>
        <span class="mcp-header__sub">Model Context Protocol · 工具接入 · 服务管理</span>
      </div>
      <div class="mcp-header__actions">
        <el-button @click="loadData">
          <el-icon><Refresh /></el-icon><span>刷新</span>
        </el-button>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon><span>新增 MCP 服务</span>
        </el-button>
      </div>
    </div>

    <div class="mcp-stats">
      <div class="mcp-stat">
        <span class="mcp-stat__label">服务总数</span>
        <strong class="mcp-stat__value">{{ stats.total }}</strong>
      </div>
      <div class="mcp-stat mcp-stat--primary">
        <span class="mcp-stat__label">已启用</span>
        <strong class="mcp-stat__value">{{ stats.enabled }}</strong>
      </div>
      <div class="mcp-stat mcp-stat--success">
        <span class="mcp-stat__label">健康</span>
        <strong class="mcp-stat__value">{{ stats.healthy }}</strong>
      </div>
      <div class="mcp-stat mcp-stat--danger">
        <span class="mcp-stat__label">异常</span>
        <strong class="mcp-stat__value">{{ stats.unhealthy }}</strong>
      </div>
    </div>

    <div class="mcp-filters">
      <el-input v-model="filters.keyword" placeholder="搜索服务名称、编码或地址" clearable
        @keyup.enter="loadData" @clear="loadData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filters.transportType" placeholder="传输方式" clearable @change="loadData">
        <el-option label="stdio" value="stdio" />
        <el-option label="SSE" value="SSE" />
        <el-option label="HTTP" value="HTTP" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable @change="loadData">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-alert v-if="errorMessage" class="mcp-alert" type="error" :title="errorMessage" show-icon />
    <el-empty v-if="!loading && records.length === 0" description="暂无 MCP 服务" />

    <div v-else class="mcp-grid">
      <div v-for="row in records" :key="row.serverId" class="mcp-card"
        :class="{ 'mcp-card--disabled': row.status !== 'ENABLED' }">
        <div class="mcp-card__header">
          <div class="mcp-card__title">
            <div class="mcp-card__icon" :style="{background: transportColor(row.transportType)}">
              <el-icon :size="22"><Tools /></el-icon>
            </div>
            <div class="mcp-card__title-text">
              <h3 class="mcp-card__name">{{ row.serverName || row.serverCode }}</h3>
              <code class="mcp-card__code">{{ row.serverCode }}</code>
            </div>
          </div>
          <el-tag size="small" effect="plain" :type="healthType(row.healthStatus)">
            <el-icon style="margin-right:2px"><CircleCheck v-if="row.healthStatus === 'UP'" /><WarningFilled v-else /></el-icon>
            {{ healthLabel(row.healthStatus) }}
          </el-tag>
        </div>

        <div class="mcp-card__transport">
          <el-tag size="small" effect="plain">{{ row.transportType || 'stdio' }}</el-tag>
          <el-tag size="small" effect="plain" :type="row.status === 'ENABLED' ? 'success' : 'info'">
            {{ row.status === 'ENABLED' ? '已启用' : '已停用' }}
          </el-tag>
        </div>

        <div v-if="row.endpoint" class="mcp-card__endpoint">
          <el-icon><Connection /></el-icon>
          <code :title="row.endpoint">{{ row.endpoint }}</code>
        </div>

        <div v-if="row.capabilitiesJson" class="mcp-card__caps">
          <span class="mcp-card__label">能力</span>
          <div class="mcp-card__cap-list">
            <el-tag v-for="c in parseCaps(row.capabilitiesJson)" :key="c" size="small" effect="plain">{{ c }}</el-tag>
          </div>
        </div>

        <div v-if="row.lastHealthCheck" class="mcp-card__check">
          <span class="mcp-card__label">最近检测</span>
          <span class="mcp-card__time">{{ formatTime(row.lastHealthCheck) }}</span>
        </div>

        <div class="mcp-card__actions">
          <el-button text type="primary" @click="openEdit(row)">
            <el-icon><Edit /></el-icon><span>编辑</span>
          </el-button>
          <el-button text type="danger" @click="deleteRow(row)">
            <el-icon><Delete /></el-icon><span>删除</span>
          </el-button>
        </div>
      </div>
    </div>

    <el-pagination v-if="total > filters.pageSize" class="mcp-pagination" background
      layout="total, prev, pager, next" :total="total" :page-size="filters.pageSize"
      :current-page="filters.pageNum" @current-change="handlePageChange" />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑 MCP 服务' : '新增 MCP 服务'"
      width="720px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="服务名称" prop="serverName">
              <el-input v-model="form.serverName" placeholder="例如：天气服务" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务编码" prop="serverCode">
              <el-input v-model="form.serverCode" placeholder="例如：weather-mcp" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="传输方式" prop="transportType">
              <el-select v-model="form.transportType" placeholder="请选择" style="width:100%">
                <el-option label="stdio (本地进程)" value="stdio" />
                <el-option label="SSE (服务器推送)" value="SSE" />
                <el-option label="HTTP" value="HTTP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="健康状态">
              <el-select v-model="form.healthStatus" placeholder="请选择" style="width:100%">
                <el-option label="健康 UP" value="UP" />
                <el-option label="异常 DOWN" value="DOWN" />
                <el-option label="未知 UNKNOWN" value="UNKNOWN" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="服务地址">
              <el-input v-model="form.endpoint" placeholder="stdio: 命令/SSE/HTTP 地址" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="鉴权配置">
              <el-input v-model="form.authConfigJson" type="textarea" :rows="2"
                placeholder='例如 {"type":"bearer","token":"..."}' />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="能力清单">
              <el-input v-model="form.capabilitiesJson" type="textarea" :rows="2"
                placeholder='例如 ["tools","resources","prompts"]' />
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
import {CircleCheck, Connection, Delete, Edit, Plus, Refresh, Search, Tools, WarningFilled} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'
import {deleteAiResource, fetchAiDetail, fetchAiPage, saveAiResource} from '@/api/ai'

interface McpServer {
  serverId: number; serverCode: string; serverName: string; transportType: string;
  endpoint: string; authConfigJson: string; capabilitiesJson: string;
  healthStatus: string; lastHealthCheck: string; status: string
}

const loading = ref(false); const saving = ref(false)
const errorMessage = ref(''); const records = ref<McpServer[]>([]); const total = ref(0)
const filters = reactive({keyword: '', transportType: '', status: '', pageNum: 1, pageSize: 20})
const dialogVisible = ref(false); const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>(); const form = reactive<Record<string, any>>({})

const stats = computed(() => {
  const s = {total: 0, enabled: 0, healthy: 0, unhealthy: 0}
  for (const r of records.value) {
    s.total++
    if (r.status === 'ENABLED') s.enabled++
    if (r.healthStatus === 'UP') s.healthy++
    else if (r.healthStatus === 'DOWN') s.unhealthy++
  }
  return s
})

const rules = computed<FormRules>(() => ({
  serverName: [{required: true, message: '请填写服务名称', trigger: 'blur'}],
  serverCode: [{required: true, message: '请填写服务编码', trigger: 'blur'}],
  transportType: [{required: true, message: '请选择传输方式', trigger: 'change'}],
}))

async function loadData() {
  loading.value = true; errorMessage.value = ''
  try {
    const payload: Record<string, unknown> = {pageNum: filters.pageNum, pageSize: filters.pageSize, keyword: filters.keyword}
    if (filters.transportType) payload.transportType = filters.transportType
    if (filters.status) payload.status = filters.status
    const result = await fetchAiPage<McpServer>('mcp-servers', payload as any)
    records.value = result.records || []
    total.value = result.total || records.value.length
  } catch {
    errorMessage.value = '接口不可用，请检查后端服务和权限配置。'
    records.value = []; total.value = 0
  } finally { loading.value = false }
}

function resetFilters() { filters.keyword = ''; filters.transportType = ''; filters.status = ''; filters.pageNum = 1; loadData() }
function handlePageChange(p: number) { filters.pageNum = p; loadData() }
function resetForm() { Object.keys(form).forEach(k => delete form[k]); form.transportType = 'stdio'; form.healthStatus = 'UNKNOWN'; form.status = 'ENABLED' }
function openCreate() { editingId.value = null; resetForm(); dialogVisible.value = true }
async function openEdit(row: McpServer) {
  editingId.value = row.serverId; resetForm()
  try { Object.assign(form, await fetchAiDetail('mcp-servers', row.serverId)) }
  catch { Object.assign(form, row) }
  dialogVisible.value = true
}
async function submitForm() {
  await formRef.value?.validate(); saving.value = true
  try {
    await saveAiResource('mcp-servers', {...form})
    ElMessage.success('保存成功'); dialogVisible.value = false; await loadData()
  } finally { saving.value = false }
}
async function deleteRow(row: McpServer) {
  await ElMessageBox.confirm(`确认删除「${row.serverName || row.serverCode}」吗？`, '删除确认', {type: 'warning'})
  await deleteAiResource('mcp-servers', [row.serverId])
  ElMessage.success('删除成功'); await loadData()
}

function transportColor(t: string) {
  return {stdio: 'linear-gradient(135deg,#6366f1,#8b5cf6)', SSE: 'linear-gradient(135deg,#10b981,#34d399)', HTTP: 'linear-gradient(135deg,#f59e0b,#fbbf24)'}[t] || 'linear-gradient(135deg,#6b7280,#9ca3af)'
}
function healthLabel(s: string) { return {UP: '健康', DOWN: '异常', UNKNOWN: '未知'}[s] || s || '未知' }
function healthType(s: string) { return {UP: 'success', DOWN: 'danger', UNKNOWN: 'info'}[s] || 'info' }
function parseCaps(json: string): string[] {
  if (!json) return []
  try { const p = JSON.parse(json); return Array.isArray(p) ? p : [] } catch { return json.split(',').map(s => s.trim()).filter(Boolean) }
}
function formatTime(t: string) {
  if (!t) return ''
  try { return new Date(t).toLocaleString('zh-CN') } catch { return t }
}

onMounted(loadData)
</script>

<style scoped>
.mcp-page { padding: 20px; }
.mcp-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 16px; }
.mcp-header__title h2 { margin: 0 0 4px; font-size: 20px; font-weight: 700; color: var(--tc-text); }
.mcp-header__sub { font-size: 12px; color: var(--tc-text-secondary); }
.mcp-header__actions { display: flex; gap: 8px; }

.mcp-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.mcp-stat { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-left: 3px solid var(--tc-primary); border-radius: var(--tc-radius-md); padding: 14px 18px; display: flex; flex-direction: column; gap: 4px; }
.mcp-stat--primary { border-left-color: var(--tc-success); }
.mcp-stat--success { border-left-color: #10b981; }
.mcp-stat--danger { border-left-color: var(--tc-danger); }
.mcp-stat__label { font-size: 12px; color: var(--tc-text-secondary); }
.mcp-stat__value { font-size: 22px; font-weight: 700; color: var(--tc-text); }

.mcp-filters { display: grid; grid-template-columns: 2fr 1fr 1fr auto; gap: 8px; margin-bottom: 16px; }
.mcp-filters :deep(.el-select), .mcp-filters :deep(.el-input) { width: 100%; }

.mcp-alert { margin-bottom: 12px; }

.mcp-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 14px; }
.mcp-card { background: var(--tc-surface); border: 1px solid var(--tc-border-light); border-radius: var(--tc-radius-md); padding: 16px; display: flex; flex-direction: column; gap: 10px; transition: all 0.2s ease; }
.mcp-card:hover { box-shadow: var(--tc-shadow-md); transform: translateY(-2px); }
.mcp-card--disabled { opacity: 0.6; }

.mcp-card__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; }
.mcp-card__title { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.mcp-card__icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.mcp-card__title-text { min-width: 0; flex: 1; }
.mcp-card__name { margin: 0 0 2px; font-size: 15px; font-weight: 700; color: var(--tc-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mcp-card__code { font-size: 11px; color: var(--tc-text-secondary); background: var(--tc-bg-soft, #fafafa); padding: 1px 6px; border-radius: 3px; }

.mcp-card__transport { display: flex; gap: 6px; flex-wrap: wrap; }

.mcp-card__endpoint { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--tc-text-secondary); padding: 6px 10px; background: var(--tc-bg-soft, #fafafa); border-radius: 4px; }
.mcp-card__endpoint code { flex: 1; font-size: 11px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.mcp-card__caps { font-size: 12px; }
.mcp-card__label { display: block; font-size: 11px; color: var(--tc-text-secondary); margin-bottom: 4px; }
.mcp-card__cap-list { display: flex; gap: 4px; flex-wrap: wrap; }

.mcp-card__check { display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--tc-text-secondary); }
.mcp-card__time { font-family: 'SFMono-Regular', Consolas, monospace; }

.mcp-card__actions { display: flex; justify-content: flex-end; gap: 4px; }

.mcp-pagination { justify-content: flex-end; margin-top: 16px; }

@media (max-width: 960px) {
  .mcp-filters { grid-template-columns: 1fr 1fr; }
  .mcp-stats { grid-template-columns: repeat(2, 1fr); }
  .mcp-grid { grid-template-columns: 1fr; }
}
</style>
