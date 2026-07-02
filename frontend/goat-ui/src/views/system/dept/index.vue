<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view">
      <el-form class="search-form" :model="query" inline @submit.prevent>
        <el-form-item label="组织名称">
          <el-input v-model="query.deptName" placeholder="请输入组织名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="请选择状态" clearable>
            <el-option label="正常" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <div class="table-toolbar__title">组织管理</div>
        <el-button type="primary" v-permission="'system:dept:save'" @click="openCreateDialog">新增组织</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border row-key="deptId" :tree-props="{ children: 'children' }">
        <el-table-column prop="deptName" label="组织名称" min-width="180" />
        <el-table-column prop="deptCode" label="组织编码" min-width="140" />
        <el-table-column prop="leader" label="负责人" min-width="120" />
        <el-table-column prop="phone" label="联系电话" min-width="140" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
              {{ row.status === 'ENABLED' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" v-permission="'system:dept:save'" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="primary" v-permission="'system:dept:save'" @click="openCreateDialog(row.deptId)">新增子级</el-button>
            <el-button link type="danger" v-permission="'system:dept:delete'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
          <el-form-item label="上级组织">
            <el-tree-select
              v-model="form.parentId"
              :data="records"
              :props="{ label: 'deptName', value: 'deptId', children: 'children' }"
              check-strictly
              clearable
              placeholder="请选择上级组织"
            />
          </el-form-item>
          <el-form-item label="组织名称" prop="deptName">
            <el-input v-model="form.deptName" placeholder="请输入组织名称" />
          </el-form-item>
          <el-form-item label="组织编码" prop="deptCode">
            <el-input v-model="form.deptCode" placeholder="请输入组织编码" />
          </el-form-item>
          <el-form-item label="负责人">
            <el-input v-model="form.leader" placeholder="请输入负责人" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio value="ENABLED">正常</el-radio>
              <el-radio value="DISABLED">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import type {FormInstance, FormRules} from 'element-plus'
import {fetchDeptTree} from '@/api/system'
import {http} from '@/api/client'

interface DeptItem {
  deptId: number
  deptName: string
  deptCode: string
  leader?: string
  phone?: string
  status: string
  parentId?: number
  children?: DeptItem[]
}

const loading = ref(false)
const submitting = ref(false)
const records = ref<DeptItem[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const query = reactive({
  deptName: '',
  status: '',
})

const form = reactive({
  parentId: 0,
  deptName: '',
  deptCode: '',
  leader: '',
  phone: '',
  status: 'ENABLED',
})

const rules: FormRules = {
  deptName: [{required: true, message: '请输入组织名称', trigger: 'blur'}],
  deptCode: [{required: true, message: '请输入组织编码', trigger: 'blur'}],
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {}
    if (query.deptName) params.deptName = query.deptName
    if (query.status) params.status = query.status
    records.value = await fetchDeptTree(params)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function resetQuery() {
  query.deptName = ''
  query.status = ''
  loadData()
}

function resetForm() {
  form.parentId = 0
  form.deptName = ''
  form.deptCode = ''
  form.leader = ''
  form.phone = ''
  form.status = 'ENABLED'
  editingId.value = null
}

function openCreateDialog(parentId?: number) {
  resetForm()
  if (parentId) {
    form.parentId = parentId
  }
  dialogTitle.value = '新增组织'
  dialogVisible.value = true
}

function openEditDialog(row: DeptItem) {
  resetForm()
  editingId.value = row.deptId
  form.parentId = row.parentId || 0
  form.deptName = row.deptName
  form.deptCode = row.deptCode
  form.leader = row.leader || ''
  form.phone = row.phone || ''
  form.status = row.status
  dialogTitle.value = '编辑组织'
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload: Record<string, unknown> = {
      deptName: form.deptName,
      deptCode: form.deptCode,
      leader: form.leader,
      phone: form.phone,
      status: form.status,
      parentId: form.parentId || 0,
    }
    if (editingId.value) {
      payload.deptId = editingId.value
    }
    await http.post('/system/dept/save', payload)
    ElMessage.success(editingId.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } catch (e: unknown) {
    const message = (e as {message?: string})?.message || '操作失败'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: DeptItem) {
  await ElMessageBox.confirm(`确定要删除组织「${row.deptName}」吗？`, '提示', {
    type: 'warning',
  })
  await http.post('/system/dept/delete', {ids: [row.deptId]})
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
