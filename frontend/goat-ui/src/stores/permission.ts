import {defineStore} from 'pinia'
import {h, markRaw} from 'vue'
import {RouterView} from 'vue-router'
import type {RouteRecordRaw} from 'vue-router'
import router from '@/router'
import type {RouteRecordPayload} from '@/types/auth'

const viewModules = import.meta.glob('@/views/**/*.vue')
const EmptyRouterView = markRaw({ render: () => h(RouterView) })

function resolveComponent(component: string) {
  if (component === 'Layout') {
    return EmptyRouterView
  }
  return viewModules[`/src/views/${component}.vue`] || (() => import('@/views/error/NotFoundView.vue'))
}

function mapRoute(item: RouteRecordPayload): RouteRecordRaw {
  return {
    path: item.path,
    name: item.name,
    component: markRaw(resolveComponent(item.component)),
    meta: {
      menuId: item.menuId,
      title: item.name,
      icon: item.icon,
      visible: item.visible !== false,
      keepAlive: item.keepAlive,
      externalLink: item.externalLink,
    },
    children: item.children?.map(mapRoute) || [],
  }
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    routes: [] as RouteRecordRaw[],
    built: false,
    removeRouteHandlers: [] as Array<() => void>,
  }),
  actions: {
    async buildRoutes(payload: RouteRecordPayload[]) {
      this.resetDynamicRoutes()
      this.routes = payload.map(mapRoute)
      this.removeRouteHandlers = this.routes.map((route) => router.addRoute('Root', route))
      this.built = true
    },
    reset() {
      this.routes = []
      this.built = false
      this.resetDynamicRoutes()
    },
    resetDynamicRoutes() {
      this.removeRouteHandlers.forEach((removeRoute) => removeRoute())
      this.removeRouteHandlers = []
    },
  },
})
