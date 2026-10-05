import { defineStore } from 'pinia'
import { ref } from 'vue'
import { httpGet, isSuccess } from '@/utils/fetch'
import { useAuthStore } from '@/stores/auth'

/** 未读数轮询间隔（毫秒） */
const POLL_INTERVAL = 60000

/**
 * 站内通知状态管理
 *
 * 只维护未读角标与轮询，通知列表由各页面自行拉取。
 * 轮询在布局挂载时启动，登出或组件卸载时停止。
 */
export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  let timer = null
  /**
   * 订阅计数：铃铛同时存在于前台导航与后台布局，路由切换时
   * 新组件挂载与旧组件卸载的先后顺序不确定，用计数避免互相把定时器清掉。
   */
  let subscribers = 0

  /** 拉取未读/待处理通知数量 */
  async function fetchUnreadCount() {
    const authStore = useAuthStore()
    if (!authStore.isLoggedIn) {
      unreadCount.value = 0
      return
    }
    try {
      const res = await httpGet('/notification/unread-count')
      if (isSuccess(res)) {
        unreadCount.value = Number(res.data) || 0
      }
    } catch {
      // 角标拉取失败不打扰用户
    }
  }

  function startPolling() {
    subscribers += 1
    if (timer) return
    fetchUnreadCount()
    timer = setInterval(() => {
      if (!document.hidden) fetchUnreadCount()
    }, POLL_INTERVAL)
  }

  function stopPolling() {
    subscribers = Math.max(0, subscribers - 1)
    if (subscribers > 0) return
    if (timer) clearInterval(timer)
    timer = null
  }

  /** 登出时调用：无视订阅计数，彻底停止并清零 */
  function reset() {
    subscribers = 0
    if (timer) clearInterval(timer)
    timer = null
    unreadCount.value = 0
  }

  return { unreadCount, fetchUnreadCount, startPolling, stopPolling, reset }
})
