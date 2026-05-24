import type {Pinia} from 'pinia'
import {useUserStore} from '@/stores/user'

export function createPermissionDirective(pinia: Pinia) {
  return {
    mounted(el: HTMLElement, binding: { value?: string | string[] }) {
      const userStore = useUserStore(pinia)
      const required = binding.value
      if (!required) return
      const permissions = userStore.permissions
      const allow = Array.isArray(required)
        ? required.some((item) => permissions.includes(item))
        : permissions.includes(required)
      if (!allow) {
        el.parentNode?.removeChild(el)
      }
    },
  }
}
