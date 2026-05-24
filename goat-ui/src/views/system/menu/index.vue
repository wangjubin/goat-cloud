<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view">
      <el-form class="search-form" :model="query" inline @submit.prevent>
        <el-form-item label="菜单名称">
          <el-input v-model="query.menuName" placeholder="请输入菜单名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="query.permissionCode" placeholder="请输入权限标识" clearable @keyup.enter="handleSearch" />
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
        <div class="table-toolbar__title">菜单管理</div>
        <el-button type="primary" v-permission="'system:menu:save'" @click="openCreateDialog()">新增菜单</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="filteredRecords"
        border
        row-key="menuId"
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="menuType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="menuTypeTag(row.menuType)" effect="plain">{{ menuTypeText(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="routePath" label="路由地址" min-width="150" show-overflow-tooltip />
        <el-table-column prop="componentPath" label="组件路径" min-width="170" show-overflow-tooltip />
        <el-table-column prop="permissionCode" label="权限标识" min-width="170" show-overflow-tooltip />
        <el-table-column prop="icon" label="图标" width="110" show-overflow-tooltip />
        <el-table-column prop="sortNo" label="排序" width="80" />
        <el-table-column prop="visible" label="显示" width="90">
          <template #default="{ row }">{{ row.visible ? '显示' : '隐藏' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetailDialog(row.menuId)">详情</el-button>
            <el-button
              link
              type="primary"
              v-permission="'system:menu:save'"
              :disabled="row.menuType === 'BUTTON'"
              @click="openCreateDialog(row)"
            >
              新增子级
            </el-button>
            <el-button link type="primary" v-permission="'system:menu:save'" @click="openEditDialog(row.menuId)">
              编辑
            </el-button>
            <el-button link type="danger" v-permission="'system:menu:save'" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formDialogVisible" :title="formTitle" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="parentTreeProps"
            check-strictly
            placeholder="请选择上级菜单"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio-button label="DIRECTORY">目录</el-radio-button>
            <el-radio-button label="MENU">菜单</el-radio-button>
            <el-radio-button label="BUTTON">按钮</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" maxlength="100" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'BUTTON'" label="路由地址" prop="routePath">
          <el-input v-model="form.routePath" placeholder="例如：/system/users" maxlength="255" />
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'BUTTON'" label="组件路径" prop="componentPath">
          <el-input v-model="form.componentPath" placeholder="例如：system/user/index 或 Layout" maxlength="255" />
        </el-form-item>
        <el-form-item label="权限标识" prop="permissionCode">
          <el-input v-model="form.permissionCode" placeholder="例如：system:user:view" maxlength="128" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="请输入 Element Plus 图标名" maxlength="64" />
        </el-form-item>
        <el-form-item label="排序" prop="sortNo">
          <el-input-number v-model="form.sortNo" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="菜单状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button label="ENABLED">正常</el-radio-button>
            <el-radio-button label="DISABLED">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.menuType !== 'BUTTON'" label="路由属性">
          <el-checkbox v-model="form.visible">侧边栏显示</el-checkbox>
          <el-checkbox v-model="form.keepAlive">页面缓存</el-checkbox>
          <el-checkbox v-model="form.externalLink">外部链接</el-checkbox>
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

    <el-dialog v-model="detailDialogVisible" title="菜单详情" width="620px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="菜单名称">{{ detail.menuName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ menuTypeText(detail.menuType) }}</el-descriptions-item>
        <el-descriptions-item label="上级ID">{{ detail.parentId }}</el-descriptions-item>
        <el-descriptions-item label="排序">{{ detail.sortNo ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="路由地址">{{ detail.routePath || '-' }}</el-descriptions-item>
        <el-descriptions-item label="组件路径">{{ detail.componentPath || '-' }}</el-descriptions-item>
        <el-descriptions-item label="权限标识">{{ detail.permissionCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="图标">{{ detail.icon || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="侧边栏显示">{{ detail.visible ? '显示' : '隐藏' }}</el-descriptions-item>
        <el-descriptions-item label="页面缓存">{{ detail.keepAlive ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="外部链接">{{ detail.externalLink ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import type {FormInstance, FormRules} from 'element-plus'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  deleteMenu,
  fetchManageMenus,
  fetchMenuDetail,
  saveMenu,
  type CommonStatus,
  type MenuTreeItem,
  type MenuType,
} from '@/api/system'

interface MenuQuery {
  menuName: string
  permissionCode: string
  status?: CommonStatus
}

interface MenuForm {
  menuId?: number
  parentId: number
  menuName: string
  menuType: MenuType
  routePath: string
  componentPath: string
  permissionCode: string
  icon: string
  sortNo: number
  visible: boolean
  keepAlive: boolean
  externalLink: boolean
  status: CommonStatus
  remark: string
}

const rootMenu = {
  menuId: 0,
  parentId: -1,
  menuName: '顶级菜单',
  menuType: 'DIRECTORY' as MenuType,
  status: 'ENABLED' as CommonStatus,
}

const query = reactive<MenuQuery>({
  menuName: '',
  permissionCode: '',
})

const emptyForm = (): MenuForm => ({
  parentId: 0,
  menuName: '',
  menuType: 'MENU',
  routePath: '',
  componentPath: '',
  permissionCode: '',
  icon: '',
  sortNo: 0,
  visible: true,
  keepAlive: false,
  externalLink: false,
  status: 'ENABLED',
  remark: '',
})

const records = ref<MenuTreeItem[]>([])
const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<MenuForm>(emptyForm())
const formDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const detail = ref<MenuTreeItem>()

const parentTreeProps = {label: 'menuName', value: 'menuId', children: 'children'}
const isEdit = computed(() => Boolean(form.menuId))
const formTitle = computed(() => (isEdit.value ? '编辑菜单' : '新增菜单'))
const parentOptions = computed(() => [
  {
    ...rootMenu,
    children: removeMenuFromTree(records.value, form.menuId),
  },
])
const filteredRecords = computed(() => filterMenus(records.value, query))

const formRules: FormRules<MenuForm> = {
  parentId: [{required: true, message: '请选择上级菜单', trigger: 'change'}],
  menuName: [{required: true, message: '请输入菜单名称', trigger: 'blur'}],
  menuType: [{required: true, message: '请选择菜单类型', trigger: 'change'}],
  status: [{required: true, message: '请选择菜单状态', trigger: 'change'}],
}

function statusText(status?: CommonStatus) {
  return status === 'ENABLED' ? '正常' : '停用'
}

function menuTypeText(type?: MenuType) {
  const textMap: Record<MenuType, string> = {
    DIRECTORY: '目录',
    MENU: '菜单',
    BUTTON: '按钮',
  }
  return type ? textMap[type] : '-'
}

function menuTypeTag(type?: MenuType) {
  if (type === 'DIRECTORY') return 'warning'
  if (type === 'BUTTON') return 'success'
  return ''
}

function assignForm(data: Partial<MenuForm>) {
  Object.assign(form, emptyForm(), data)
}

async function loadData() {
  loading.value = true
  try {
    records.value = await fetchManageMenus()
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  // 列表为树形全量数据，筛选在前端实时计算。
}

function resetQuery() {
  query.menuName = ''
  query.permissionCode = ''
  query.status = undefined
}

function openCreateDialog(parent?: MenuTreeItem) {
  assignForm({
    parentId: parent?.menuId ?? 0,
    menuType: parent?.menuType === 'MENU' ? 'BUTTON' : 'MENU',
  })
  formDialogVisible.value = true
}

async function openEditDialog(menuId: number) {
  const data = await fetchMenuDetail(menuId)
  assignForm({
    menuId: data.menuId,
    parentId: data.parentId,
    menuName: data.menuName,
    menuType: data.menuType,
    routePath: data.routePath || '',
    componentPath: data.componentPath || '',
    permissionCode: data.permissionCode || '',
    icon: data.icon || '',
    sortNo: data.sortNo || 0,
    visible: data.visible ?? true,
    keepAlive: data.keepAlive ?? false,
    externalLink: data.externalLink ?? false,
    status: data.status,
    remark: data.remark || '',
  })
  formDialogVisible.value = true
}

async function openDetailDialog(menuId: number) {
  detail.value = await fetchMenuDetail(menuId)
  detailDialogVisible.value = true
}

async function submitForm() {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    const payload = normalizeMenuPayload(form)
    await saveMenu(payload)
    ElMessage.success(isEdit.value ? '菜单已更新' : '菜单已新增')
    formDialogVisible.value = false
    await loadData()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: MenuTreeItem) {
  await ElMessageBox.confirm(`确认删除菜单“${row.menuName}”吗？请先删除它的子菜单或按钮。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteMenu(row.menuId)
  ElMessage.success('菜单已删除')
  await loadData()
}

function normalizeMenuPayload(data: MenuForm) {
  const payload = {...data}
  if (payload.menuType === 'BUTTON') {
    payload.routePath = ''
    payload.componentPath = ''
    payload.icon = ''
    payload.visible = true
    payload.keepAlive = false
    payload.externalLink = false
  }
  return payload
}

function filterMenus(items: MenuTreeItem[], params: MenuQuery): MenuTreeItem[] {
  const name = params.menuName.trim().toLowerCase()
  const permission = params.permissionCode.trim().toLowerCase()
  return items
    .map((item) => {
      const children = filterMenus(item.children || [], params)
      const matchedName = !name || item.menuName.toLowerCase().includes(name)
      const matchedPermission = !permission || (item.permissionCode || '').toLowerCase().includes(permission)
      const matchedStatus = !params.status || item.status === params.status
      if ((matchedName && matchedPermission && matchedStatus) || children.length > 0) {
        return {...item, children}
      }
      return undefined
    })
    .filter((item): item is MenuTreeItem => Boolean(item))
}

function removeMenuFromTree(items: MenuTreeItem[], menuId?: number): MenuTreeItem[] {
  return items
    .filter((item) => item.menuId !== menuId && item.menuType !== 'BUTTON')
    .map((item) => ({
      ...item,
      children: removeMenuFromTree(item.children || [], menuId),
    }))
}

onMounted(loadData)
</script>
