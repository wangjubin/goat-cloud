<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view">
      <div class="toolbar">
        <div>
          <h3>工作台</h3>
          <p>权限、组织、菜单和 Redis 会话控制已经接入，可以继续扩展业务模块。</p>
        </div>
      </div>
      <el-row :gutter="12">
        <el-col :xs="24" :sm="8">
          <el-card shadow="never" class="metric-card">
            <span>Redis Session</span>
            <strong>Enabled</strong>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-card shadow="never" class="metric-card">
            <span>Database</span>
            <strong>PostgreSQL</strong>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-card shadow="never" class="metric-card">
            <span>Auth Model</span>
            <strong>Access + Refresh</strong>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped>
.metric-card {
  border-radius: 4px;
}

.metric-card span {
  display: block;
  color: var(--tc-subtle);
  font-size: 13px;
  margin-bottom: 10px;
}

.metric-card strong {
  font-size: 22px;
  color: #303133;
}
</style>
