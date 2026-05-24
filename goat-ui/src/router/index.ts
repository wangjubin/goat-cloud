import {createRouter, createWebHistory, type RouteRecordRaw} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import {usePermissionStore} from '@/stores/permission'

const baseRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    name: 'Root',
    component: () => import('@/layout/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [],
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/ForbiddenView.vue'),
    meta: { public: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFoundView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: baseRoutes,
})

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  const permissionStore = usePermissionStore()
  const isPublic = to.meta.public === true

  if (!authStore.accessToken) {
    if (isPublic) {
      next()
      return
    }
    next('/login')
    return
  }

  if (!permissionStore.built) {
    try {
      await authStore.restoreProfile()
      next({ path: to.path, query: to.query, hash: to.hash, replace: true })
      return
    } catch (error) {
      authStore.logoutLocal()
      next('/login')
      return
    }
  }

  next()
})

export default router
