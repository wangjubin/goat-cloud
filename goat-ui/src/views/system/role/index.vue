<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view">
      <el-form class="search-form" :model="query" inline @submit.prevent>
        <el-form-item label="角色编码">
          <el-input v-model="query.roleCode" placeholder="请输入角色编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="query.roleName" placeholder="请输入角色名称" clearable @keyup.enter="handleSearch" />
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
        <div class="table-toolbar__title">角色管理</div>
        <el-button type="primary" v-permission="'system:role:save'" @click="openCreateDialog">新增角色</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border>
        <el-table-column prop="roleId" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" min-width="150" show-overflow-tooltip />
        <el-table-column prop="roleName" label="角色名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="dataScope" label="数据范围" min-width="140">
          <template #default="{ row }">{{ dataScopeText(row.dataScope) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetailDialog(row.roleId)">详情</el-button>
            <el-button link type="primary" v-permission="'system:role:save'" @click="openEditDialog(row.roleId)">
              编辑
            </el-button>
            <el-button link type="primary" v-permission="'system:role:save'" @click="openPermissionDialog(row)">
              分配权限
            </el-button>
            <el-button link type="warning" v-permission="'system:role:save'" @click="handleStatusChange(row)">
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" v-permission="'system:role:save'" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="formDialogVisible" :title="formTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="请输入角色编码" maxlength="64" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" maxlength="100" />
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

    <el-dialog v-model="detailDialogVisible" title="角色详情" width="520px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="角色编码">{{ detail.roleCode }}</el-descriptions-item>
        <el-descriptions-item label="角色名称">{{ detail.roleName }}</el-descriptions-item>
        <el-descriptions-item label="数据范围">{{ dataScopeText(detail.dataScope) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="permissionDialogVisible" title="分配权限" width="720px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="角色">
          <span>{{ currentRole?.roleName }}（{{ currentRole?.roleCode }}）</span>
        </el-form-item>
        <el-form-item label="菜单权限">
          <div class="tree-panel">
            <div class="tree-actions">
              <el-button size="small" @click="toggleMenuTree(true)">全选</el-button>
              <el-button size="small" @click="toggleMenuTree(false)">清空</el-button>
            </div>
            <el-tree
              ref="menuTreeRef"
              :data="menuTree"
              :props="menuTreeProps"
              node-key="menuId"
              show-checkbox
              default-expand-all
            />
          </div>
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="permissionForm.dataScope" class="full-width">
            <el-option label="全部数据权限" value="ALL" />
            <el-option label="本部门数据权限" value="DEPT" />
            <el-option label="本部门及以下数据权限" value="DEPT_AND_CHILD" />
            <el-option label="仅本人数据权限" value="SELF" />
            <el-option label="自定义数据权限" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="permissionForm.dataScope === 'CUSTOM'" label="部门权限">
          <div class="tree-panel">
            <el-tree
              ref="deptTreeRef"
              :data="deptTree"
              :props="deptTreeProps"
              node-key="deptId"
              show-checkbox
              default-expand-all
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitPermissions">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, reactive, ref} from 'vue'
import type {FormInstance, FormRules} from 'element-plus'
import {ElMessage, ElMessageBox, type TreeInstance} from 'element-plus'
import {
  assignRolePermissions,
  changeRoleStatus,
  deleteRoles,
  fetchDeptTree,
  fetchManageMenus,
  fetchRoleDetail,
  fetchRolePermissions,
  fetchRoles,
  saveRole,
  type CommonStatus,
  type DataScope,
  type DeptTreeItem,
  type MenuTreeItem,
  type RoleItem,
} from '@/api/system'

interface RoleQuery {
  pageNum: number
  pageSize: number
  roleCode: string
  roleName: string
  status?: CommonStatus
}

interface RoleForm {
  roleId?: number
  roleCode: string
  roleName: string
  status: CommonStatus
  remark: string
}

const query = reactive<RoleQuery>({
  pageNum: 1,
  pageSize: 10,
  roleCode: '',
  roleName: '',
})

const emptyForm = (): RoleForm => ({
  roleCode: '',
  roleName: '',
  status: 'ENABLED',
  remark: '',
})

const records = ref<RoleItem[]>([])
const total = ref(0)
const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<RoleForm>(emptyForm())
const formDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const detail = ref<RoleItem>()
const currentRole = ref<RoleItem>()
const menuTree = ref<MenuTreeItem[]>([])
const deptTree = ref<DeptTreeItem[]>([])
const menuTreeRef = ref<TreeInstance>()
const deptTreeRef = ref<TreeInstance>()
const permissionForm = reactive({
  dataScope: 'SELF' as DataScope,
})

const isEdit = computed(() => Boolean(form.roleId))
const formTitle = computed(() => (isEdit.value ? '编辑角色' : '新增角色'))
const menuTreeProps = {label: 'menuName', children: 'children'}
const deptTreeProps = {label: 'deptName', children: 'children'}

const formRules: FormRules<RoleForm> = {
  roleCode: [{required: true, message: '请输入角色编码', trigger: 'blur'}],
  roleName: [{required: true, message: '请输入角色名称', trigger: 'blur'}],
  status: [{required: true, message: '请选择状态', trigger: 'change'}],
}

const dataScopeMap: Record<DataScope, string> = {
  ALL: '全部数据',
  DEPT: '本部门',
  DEPT_AND_CHILD: '本部门及以下',
  SELF: '仅本人',
  CUSTOM: '自定义',
}

function statusText(status?: CommonStatus) {
  return status === 'ENABLED' ? '正常' : '停用'
}

function dataScopeText(scope?: DataScope) {
  return scope ? dataScopeMap[scope] : '-'
}

function assignForm(data: Partial<RoleForm>) {
  Object.assign(form, emptyForm(), data)
}

async function loadData() {
  loading.value = true
  try {
    const result = await fetchRoles(query)
    records.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadPermissionOptions() {
  const [menus, depts] = await Promise.all([fetchManageMenus(), fetchDeptTree()])
  menuTree.value = menus
  deptTree.value = depts
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function resetQuery() {
  query.pageNum = 1
  query.roleCode = ''
  query.roleName = ''
  query.status = undefined
  loadData()
}

function openCreateDialog() {
  assignForm({})
  formDialogVisible.value = true
}

async function openEditDialog(roleId: number) {
  const data = await fetchRoleDetail(roleId)
  assignForm({
    roleId: data.roleId,
    roleCode: data.roleCode,
    roleName: data.roleName,
    status: data.status,
    remark: data.remark || '',
  })
  formDialogVisible.value = true
}

async function openDetailDialog(roleId: number) {
  detail.value = await fetchRoleDetail(roleId)
  detailDialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    await saveRole({...form})
    ElMessage.success(isEdit.value ? '角色已更新' : '角色已新增')
    formDialogVisible.value = false
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: RoleItem) {
  await ElMessageBox.confirm(`确认删除角色“${row.roleName}”吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteRoles([row.roleId])
  ElMessage.success('角色已删除')
  await loadData()
}

async function handleStatusChange(row: RoleItem) {
  const nextStatus: CommonStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  await ElMessageBox.confirm(`确认${statusText(nextStatus)}角色“${row.roleName}”吗？`, '状态确认', {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消',
  })
  await changeRoleStatus(row.roleId, nextStatus)
  ElMessage.success('角色状态已更新')
  await loadData()
}

async function openPermissionDialog(row: RoleItem) {
  currentRole.value = row
  const data = await fetchRolePermissions(row.roleId)
  permissionForm.dataScope = data.dataScope || 'SELF'
  permissionDialogVisible.value = true
  await nextTick()
  menuTreeRef.value?.setCheckedKeys(data.menuIds || [])
  deptTreeRef.value?.setCheckedKeys(data.deptIds || [])
}

function toggleMenuTree(checked: boolean) {
  if (!checked) {
    menuTreeRef.value?.setCheckedKeys([])
    return
  }
  menuTreeRef.value?.setCheckedKeys(flattenMenuIds(menuTree.value))
}

function flattenMenuIds(items: MenuTreeItem[]) {
  return items.flatMap((item) => [item.menuId, ...flattenMenuIds(item.children || [])])
}

async function submitPermissions() {
  if (!currentRole.value) return
  submitLoading.value = true
  try {
    await assignRolePermissions({
      roleId: currentRole.value.roleId,
      menuIds: [
        ...(menuTreeRef.value?.getCheckedKeys(false) as number[]),
        ...(menuTreeRef.value?.getHalfCheckedKeys() as number[]),
      ],
      dataScope: permissionForm.dataScope,
      deptIds: permissionForm.dataScope === 'CUSTOM' ? (deptTreeRef.value?.getCheckedKeys(false) as number[]) : [],
    })
    ElMessage.success('权限已分配')
    permissionDialogVisible.value = false
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadPermissionOptions(), loadData()])
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

.tree-panel {
  width: 100%;
  max-height: 320px;
  overflow: auto;
  padding: 12px;
  border: 1px solid var(--tc-border);
  border-radius: 4px;
}

.tree-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
