<template>
  <div class="layout-padding">
    <div class="layout-padding-auto layout-padding-view error-view">
      <h2>403</h2>
      <p>你当前没有访问这个页面的权限。</p>
      <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.error-view {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  text-align: center;
}

.error-view h2 {
  margin: 0;
  font-size: 48px;
  color: var(--tc-primary);
}

.error-view p {
  margin: 0 0 8px;
  color: var(--tc-subtle);
}
</style>
