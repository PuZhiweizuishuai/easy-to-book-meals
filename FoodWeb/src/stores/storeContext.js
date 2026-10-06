import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { httpGet, isSuccess, parsePage } from '@/utils/fetch'

/** localStorage 持久化键名 */
const STORAGE_KEY_CURRENT_STORE_ID = 'current_store_id'
/** sessionStorage 键名：超管从系统后台“进入管理”指定门店的标记 */
const STORAGE_KEY_ADMIN_ENTERED = 'admin_entered_store'

/**
 * 门店上下文 Store（店主/店员端）
 *
 * 设计要点：
 * - 登录后加载与当前用户相关的门店列表（门店角色 ∪ 自己创建的门店，含待审核）
 * - 多门店用户可切换“当前门店”，各管理页面均基于当前门店读写数据
 * - 选择结果持久化到 localStorage，刷新后自动恢复
 * - 超级管理员可从系统后台“进入管理”指定门店（enterStore），
 *   该手动上下文保存在 sessionStorage，不被列表加载覆盖，关标签页即失效
 */
export const useStoreContext = defineStore('storeContext', () => {
  /** 与当前用户相关的门店列表 */
  const stores = ref([])

  /** 当前选中门店 ID */
  const currentStoreId = ref(Number(localStorage.getItem(STORAGE_KEY_CURRENT_STORE_ID)) || null)

  /** 超管从系统后台手动进入指定门店的标记 */
  const adminEntered = ref(sessionStorage.getItem(STORAGE_KEY_ADMIN_ENTERED) === '1')

  /** 加载状态 */
  const loading = ref(false)

  /** 当前门店对象 */
  const currentStore = computed(() =>
    stores.value.find(s => s.id === currentStoreId.value) || null
  )

  /** 是否有相关门店 */
  const hasStore = computed(() => stores.value.length > 0)

  /** 当前门店是否具备门店管理角色（待审核门店的创建者为 false） */
  const canManageCurrent = computed(() => !!currentStore.value?.hasManageRole)

  /**
   * 加载与当前用户相关的门店列表
   * 登录后或进入管理端布局时调用；自动选中第一个可管理门店（或恢复上次选择）
   * @param {boolean} force - 为 true 时忽略超管手动进入标记，强制拉取列表
   */
  async function loadMyStores(force = false) {
    loading.value = true
    try {
      // 超管从系统后台进入指定门店时保持手动上下文，避免空列表将其覆盖
      if (adminEntered.value && !force) {
        if (currentStoreId.value && !stores.value.some(s => s.id === currentStoreId.value)) {
          const res = await httpGet(`/store/${currentStoreId.value}`)
          stores.value = isSuccess(res) && res.data ? [res.data] : []
        }
        return stores.value
      }

      const res = await httpGet('/store/manage/list?page=1&size=100')
      if (isSuccess(res) && res.data) {
        stores.value = parsePage(res.data).content
        // 恢复上次选择：仅当该门店仍具备管理角色时保留；
        // 否则优先选中第一个可管理门店（避免默认落在待审核门店上）
        const prev = stores.value.find(s => s.id === currentStoreId.value)
        const preferred = stores.value.find(s => s.hasManageRole) || stores.value[0]
        if (preferred && (!prev || (!prev.hasManageRole && preferred.hasManageRole))) {
          selectStore(preferred.id)
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
   * 超管从系统后台直接进入指定门店的管理上下文
   * 手动上下文标记存入 sessionStorage，刷新后仍恢复，退出登录时清除
   */
  function enterStore(store) {
    if (!store) return
    stores.value = [store]
    selectStore(store.id)
    adminEntered.value = true
    sessionStorage.setItem(STORAGE_KEY_ADMIN_ENTERED, '1')
  }

  /**
   * 退出超管手动进入的门店上下文，恢复常规列表加载
   */
  function exitAdminEnter() {
    adminEntered.value = false
    sessionStorage.removeItem(STORAGE_KEY_ADMIN_ENTERED)
  }

  /**
   * 清空门店上下文（登出时调用）
   */
  function clear() {
    stores.value = []
    currentStoreId.value = null
    adminEntered.value = false
    localStorage.removeItem(STORAGE_KEY_CURRENT_STORE_ID)
    sessionStorage.removeItem(STORAGE_KEY_ADMIN_ENTERED)
  }

  return {
    stores,
    currentStoreId,
    currentStore,
    hasStore,
    canManageCurrent,
    loading,
    adminEntered,
    loadMyStores,
    selectStore,
    enterStore,
    exitAdminEnter,
    clear
  }
})
