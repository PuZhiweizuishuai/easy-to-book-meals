import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 购物车 Store（顾客端点餐）
 *
 * 设计要点：
 * - 购物车按门店隔离：加入其它门店的菜品时清空重建
 * - 条目记录 SKU 快照（名称、规格、单价），结算时提交 skuId + quantity
 * - 购物车不持久化到 localStorage，刷新即清空（点餐是短会话行为）
 * - 扫码点餐场景下 tableQrCodeKey 不为空，结算走免登录端点
 */
export const useCartStore = defineStore('cart', () => {
  /** 购物车所属门店 ID */
  const storeId = ref(null)

  /** 门店名称（展示用） */
  const storeName = ref('')

  /** 扫码绑定的餐桌（qrCodeKey，下单时提交） */
  const tableQrCodeKey = ref(null)

  /** 购物车条目 */
  const items = ref([])

  /** 商品总数量 */
  const totalCount = computed(() =>
    items.value.reduce((sum, i) => sum + i.quantity, 0)
  )

  /** 应付总金额（按成交价计算） */
  const totalPrice = computed(() =>
    items.value.reduce((sum, i) => sum + Number(i.dealPrice ?? i.unitPrice ?? 0) * i.quantity, 0)
  )

  /** 是否为扫码堂食点餐（有餐桌二维码即为堂食免登录场景） */
  const isDineInScan = computed(() => !!tableQrCodeKey.value)

  /**
   * 绑定门店（进入门店点餐页时调用）
   * @param {number} id 门店ID
   * @param {string} name 门店名称
   * @param {string|null} qrCodeKey 扫码餐桌标识
   */
  function bindStore(id, name, qrCodeKey = null) {
    if (storeId.value !== id) {
      // 切换门店时清空购物车
      storeId.value = id
      storeName.value = name || ''
      items.value = []
    }
    tableQrCodeKey.value = qrCodeKey || null
  }

  /**
   * 加入购物车（同 SKU 数量累加）
   */
  function addItem(item) {
    if (storeId.value === null) {
      return
    }
    const existing = items.value.find(i => i.skuId === item.skuId)
    if (existing) {
      existing.quantity = Math.min(existing.quantity + item.quantity, 99)
    } else {
      items.value.push({ ...item, quantity: item.quantity || 1 })
    }
  }

  /**
   * 修改数量（减到 0 时移除）
   */
  function changeQuantity(skuId, delta) {
    const item = items.value.find(i => i.skuId === skuId)
    if (!item) return
    item.quantity += delta
    if (item.quantity <= 0) {
      removeItem(skuId)
    }
  }

  /**
   * 移除条目
   */
  function removeItem(skuId) {
    items.value = items.value.filter(i => i.skuId !== skuId)
  }

  /**
   * 清空购物车
   */
  function clear() {
    items.value = []
  }

  /**
   * 生成下单明细（提交后端格式）
   */
  function toOrderItems() {
    return items.value.map(i => ({ skuId: i.skuId, quantity: i.quantity }))
  }

  return {
    storeId,
    storeName,
    tableQrCodeKey,
    items,
    totalCount,
    totalPrice,
    isDineInScan,
    bindStore,
    addItem,
    changeQuantity,
    removeItem,
    clear,
    toOrderItems
  }
})
