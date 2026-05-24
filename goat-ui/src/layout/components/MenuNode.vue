<template>
  <el-sub-menu v-if="hasChildren" :index="route.path">
    <template #title>
      <el-icon v-if="menuIcon"><component :is="menuIcon" /></el-icon>
      <span>{{ route.meta?.title }}</span>
    </template>
    <MenuNode v-for="child in route.children" :key="String(child.name)" :route="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="route.path">
    <el-icon v-if="menuIcon"><component :is="menuIcon" /></el-icon>
    <span>{{ route.meta?.title }}</span>
  </el-menu-item>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import * as Icons from '@element-plus/icons-vue'

const props = defineProps<{
  route: any
}>()

const hasChildren = computed(() => Boolean(props.route.children?.length))
const menuIcon = computed(() => {
  const iconName = props.route.meta?.icon as keyof typeof Icons | undefined
  return iconName ? Icons[iconName] : null
})
</script>
