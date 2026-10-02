import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { httpGet, isSuccess, parsePage } from '@/utils/fetch'

/** localStorage 持久化键名 */
const STORAGE_KEY_CURRENT_STORE_ID = 'current_store_id'

/**
 * 门店上下文 Store（店主/店员端）
 *
 * 设计要点：
 * - 登录后加载当前用户管理的门店列表（依据门店角色确定范围）
 * - 多门店用户可切换"当前门店"，各管理页面均基于当前门店读写数据
 * - 选择结果持久化到 localStorage，刷新后自动恢复
 */
export const useStoreContext = defineStore('storeContext', () => {
  /** 当前用户管理的门店列表 */
  const stores = ref([])

  /** 当前选中门店 ID */
  const currentStoreId = ref(Number(localStorage.getItem(STORAGE_KEY_CURRENT_STORE_ID)) || null)

  /** 加载状态 */
  const loading = ref(false)

  /** 当前门店对象 */
  const currentStore = computed(() =>
    stores.value.find(s => s.id === currentStoreId.value) || null
  )

  /** 是否有可管理的门店 */
  const hasStore = computed(() => stores.value.length > 0)

  /**
   * 加载我管理的门店列表
   * 登录后或进入管理端布局时调用；自动选中第一个门店（或恢复上次选择）
   */
  async function loadMyStores() {
    loading.value = true
    try {
      const res = await httpGet('/store/manage/list?page=1&size=100')
      if (isSuccess(res) && res.data) {
        stores.value = parsePage(res.data).content
        // 恢复上次选择；若无有效选择则默认第一个门店
        const valid = stores.value.some(s => s.id === currentStoreId.value)
        if (!valid && stores.value.length > 0) {
          selectStore(stores.value[0].id)
        }
      }
    } catch (e) {
      console.error('加载门店列表失败:', e)
      stores.value = []
    } finally {
      loading.value = false
    }
    return stores.value
  }

  /**
   * 切换当前门店
   */
  function selectStore(storeId) {
    currentStoreId.value = storeId
    if (storeId) {
      localStorage.setItem(STORAGE_KEY_CURRENT_STORE_ID, String(storeId))
    } else {
      localStorage.removeItem(STORAGE_KEY_CURRENT_STORE_ID)
    }
  }

  /**
   * 清空门店上下文（登出时调用）
   */
  function clear() {
    stores.value = []
    currentStoreId.value = null
    localStorage.removeItem(STORAGE_KEY_CURRENT_STORE_ID)
  }

  return {
    stores,
    currentStoreId,
    currentStore,
    hasStore,
    loading,
    loadMyStores,
    selectStore,
    clear
  }
})
