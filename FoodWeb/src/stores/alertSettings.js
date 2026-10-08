import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useStoreContext } from '@/stores/storeContext'

/** localStorage 持久化键名 */
const STORAGE_KEY = 'order_alert_settings'

/**
 * 提醒设置默认值
 * 轮询默认关闭：避免用户首次进入管理端就持续向后端发请求，
 * 由用户在店铺设置页按需开启。
 */
const DEFAULTS = {
  polling: false,
  notify: true,
  sound: true,
  intervalSeconds: 30,
}

/** 可选轮询频率 */
export const INTERVAL_OPTIONS = [
  { value: 10, label: '10 秒' },
  { value: 15, label: '15 秒' },
  { value: 30, label: '30 秒' },
  { value: 60, label: '1 分钟' },
  { value: 120, label: '2 分钟' },
]

const readStored = () => {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

/**
 * 新订单/新菜品提醒设置
 *
 * 通知与声音属于当前这台设备上的浏览器行为（同一个店员换浏览器需重新设置），
 * 因此只存 localStorage 不落库，并按门店隔离，便于一个浏览器管理多家门店时
 * 各店独立配置。
 */
export const useAlertSettings = defineStore('alertSettings', () => {
  const storeContext = useStoreContext()

  /** { [storeId]: 设置对象 } */
  const all = ref(readStored())

  const storeKey = computed(() => String(storeContext.currentStoreId ?? ''))

  /** 当前门店的设置（已合并默认值） */
  const current = computed(() => ({
    ...DEFAULTS,
    ...(all.value[storeKey.value] || {}),
  }))

  /**
   * 保存当前门店的设置（增量合并，立即写入 localStorage）
   * @param {object} patch - 需要修改的字段
   */
  function save(patch) {
    if (!storeContext.currentStoreId) return
    all.value[storeKey.value] = { ...current.value, ...patch }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(all.value))
  }

  /** 恢复当前门店的默认设置 */
  function reset() {
    save({ ...DEFAULTS })
  }

  return { current, save, reset }
})
