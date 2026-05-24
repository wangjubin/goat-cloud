<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view">
      <el-form class="search-form" :model="query" inline @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="请输入用户名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="query.nickname" placeholder="请输入昵称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="query.deptId"
            :data="deptTree"
            :props="deptTreeProps"
            check-strictly
            clearable
            placeholder="请选择部门"
          />
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
        <div class="table-toolbar__title">用户管理</div>
        <el-button type="primary" v-permission="'system:user:create'" @click="openCreateDialog">新增用户</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border>
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="130" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="130" show-overflow-tooltip />
        <el-table-column prop="deptName" label="部门" min-width="150" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="130" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetailDialog(row.userId)">详情</el-button>
            <el-button link type="primary" v-permission="'system:user:update'" @click="openEditDialog(row.userId)">
              编辑
            </el-button>
            <el-button link type="primary" v-permission="'system:user:update'" @click="openAssignRoleDialog(row)">
              分配角色
            </el-button>
            <el-button
              link
              type="warning"
              v-permission="'system:user:update'"
              :disabled="row.superAdmin"
              @click="handleStatusChange(row)"
            >
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button
              link
              type="warning"
              v-permission="'system:user:update'"
              :disabled="row.superAdmin"
              @click="handleResetPassword(row)"
            >
              重置密码
            </el-button>
            <el-button
              link
              type="danger"
              v-permission="'system:user:delete'"
              :disabled="row.superAdmin"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="formDialogVisible" :title="formTitle" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" maxlength="64" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="100" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTree"
            :props="deptTreeProps"
            check-strictly
            placeholder="请选择部门"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="32" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="128" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button label="ENABLED">正常</el-radio-button>
            <el-radio-button label="DISABLED">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="用户详情" width="560px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detail.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="超级管理员">{{ detail.superAdmin ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="角色ID">{{ detail.roleIds?.join(', ') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="520px">
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ currentUser?.nickname }}（{{ currentUser?.username }}）</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="checkedRoleIds" multiple filterable placeholder="请选择角色" class="full-width">
            <el-option
              v-for="role in roleOptions"
              :key="role.roleId"
              :label="`${role.roleName}（${role.roleCode}）`"
              :value="role.roleId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitAssignRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import type {FormInstance, FormRules} from 'element-plus'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  assignUserRoles,
  changeUserStatus,
  createUser,
  deleteUsers,
  fetchDeptTree,
  fetchRoles,
  fetchUserDetail,
  fetchUsers,
  resetUserPassword,
  updateUser,
  type CommonStatus,
  type DeptTreeItem,
  type RoleItem,
  type UserDetail,
  type UserItem,
} from '@/api/system'

interface UserQuery {
  pageNum: number
  pageSize: number
  username: string
  nickname: string
  deptId?: number
  status?: CommonStatus
}

interface UserForm {
  userId?: number
  username: string
  nickname: string
  deptId?: number
  phone: string
  email: string
  status: CommonStatus
  remark: string
}

const query = reactive<UserQuery>({
  pageNum: 1,
  pageSize: 10,
  username: '',
  nickname: '',
})

const emptyForm = (): UserForm => ({
  username: '',
  nickname: '',
  deptId: undefined,
  phone: '',
  email: '',
  status: 'ENABLED',
  remark: '',
})

const records = ref<UserItem[]>([])
const total = ref(0)
const loading = ref(false)
const submitLoading = ref(false)
const deptTree = ref<DeptTreeItem[]>([])
const roleOptions = ref<RoleItem[]>([])

const formRef = ref<FormInstance>()
const form = reactive<UserForm>(emptyForm())
const formDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const detail = ref<UserDetail>()
const currentUser = ref<UserItem>()
const checkedRoleIds = ref<number[]>([])

const isEdit = computed(() => Boolean(form.userId))
const formTitle = computed(() => (isEdit.value ? '编辑用户' : '新增用户'))
const deptTreeProps = {label: 'deptName', value: 'deptId', children: 'children'}

const formRules: FormRules<UserForm> = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  nickname: [{required: true, message: '请输入昵称', trigger: 'blur'}],
  deptId: [{required: true, message: '请选择部门', trigger: 'change'}],
  email: [{type: 'email', message: '邮箱格式不正确', trigger: 'blur'}],
  status: [{required: true, message: '请选择状态', trigger: 'change'}],
}

function statusText(status?: CommonStatus) {
  return status === 'ENABLED' ? '正常' : '停用'
}

function assignForm(data: Partial<UserForm>) {
  Object.assign(form, emptyForm(), data)
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchUsers(query)
    records.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [depts, roles] = await Promise.all([
    fetchDeptTree(),
    fetchRoles({pageNum: 1, pageSize: 200, status: 'ENABLED'}),
  ])
  deptTree.value = depts
  roleOptions.value = roles.records
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function resetQuery() {
  query.pageNum = 1
  query.username = ''
  query.nickname = ''
  query.deptId = undefined
  query.status = undefined
  loadData()
}

function openCreateDialog() {
  assignForm({})
  formDialogVisible.value = true
}

async function openEditDialog(userId: number) {
  const data = await fetchUserDetail(userId)
  assignForm({
    userId: data.userId,
    username: data.username,
    nickname: data.nickname,
    deptId: data.deptId,
    phone: data.phone || '',
    email: data.email || '',
    status: data.status,
    remark: data.remark || '',
  })
  formDialogVisible.value = true
}

async function openDetailDialog(userId: number) {
  detail.value = await fetchUserDetail(userId)
  detailDialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    const payload = {...form}
    if (isEdit.value) {
      await updateUser(payload)
      ElMessage.success('用户已更新')
    } else {
      await createUser(payload)
      ElMessage.success('用户已新增，初始密码使用系统默认配置')
    }
    formDialogVisible.value = false
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: UserItem) {
  await ElMessageBox.confirm(`确认删除用户“${row.nickname || row.username}”吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteUsers([row.userId])
  ElMessage.success('用户已删除')
  await loadData()
}

async function handleStatusChange(row: UserItem) {
  const nextStatus: CommonStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  await ElMessageBox.confirm(`确认${statusText(nextStatus)}用户“${row.nickname || row.username}”吗？`, '状态确认', {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消',
  })
  await changeUserStatus(row.userId, nextStatus)
  ElMessage.success('用户状态已更新')
  await loadData()
}

async function handleResetPassword(row: UserItem) {
  await ElMessageBox.confirm(`确认重置用户“${row.nickname || row.username}”的密码吗？`, '重置密码', {
    type: 'warning',
    confirmButtonText: '重置',
    cancelButtonText: '取消',
  })
  await resetUserPassword(row.userId)
  ElMessage.success('密码已重置为系统默认初始密码')
}

async function openAssignRoleDialog(row: UserItem) {
  currentUser.value = row
  const data = await fetchUserDetail(row.userId)
  checkedRoleIds.value = data.roleIds || []
  roleDialogVisible.value = true
}

async function submitAssignRoles() {
  if (!currentUser.value) return
  submitLoading.value = true
  try {
    await assignUserRoles(currentUser.value.userId, checkedRoleIds.value)
    ElMessage.success('角色已分配')
    roleDialogVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

onMounted(async () => {
  await loadOptions()
  await loadData()
})
</script>

<style scoped>
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.full-width {
  width: 100%;
}
</style>
