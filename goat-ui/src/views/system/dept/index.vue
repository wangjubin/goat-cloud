<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view">
      <div class="search-form">
        <el-input placeholder="请输入组织名称" clearable />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="loadData">重置</el-button>
      </div>
      <div class="table-toolbar">
        <div class="table-toolbar__title">组织管理</div>
        <el-button type="primary" v-permission="'system:dept:save'">新增组织</el-button>
      </div>
      <el-table :data="records" border row-key="deptId" default-expand-all>
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
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {fetchDeptTree} from '@/api/system'

const records = ref<any[]>([])

async function loadData() {
  records.value = await fetchDeptTree()
}

onMounted(loadData)
</script>
