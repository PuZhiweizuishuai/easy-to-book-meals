/**
 * 加菜功能 composable（顾客端「我的订单」与管理端「订单管理」共用）
 *
 * 此前两端各维护一份约 200 行的菜品选择 / 规格选择 / 待加清单 / 提交逻辑，
 * 现统一收敛到此处；差异点通过配置回调注入：
 *
 * @param {object}   options
 * @param {(order: object) => Promise<Array>} options.loadDishes
 *        加载门店可选菜品（管理端走 /dishes/list 并过滤启用；顾客端走 /store/{id}/dishes）
 * @param {(order: object, items: Array) => Promise<object>} options.submitItems
 *        提交加菜请求，返回后端统一响应体（成功 status===200）
 * @param {() => void} [options.onSuccess] 加菜成功后的回调（如刷新订单列表）
 * @param {string} [options.title] 弹窗标题
 *
 * 返回 reactive API 对象，供 AddItemsDialog 直接绑定：
 *   const addItems = useAddItems({...}); <AddItemsDialog :api="addItems" />
 */
import { reactive, ref } from 'vue'
import { isSuccess } from '@/utils/fetch'
import toast from '@/utils/toast'
import { formatSpecNames } from '@/utils/format'
import { orderItemCount } from '@/utils/order'

export function useAddItems(options = {}) {
  const { loadDishes, submitItems, onSuccess, title = '订单加菜' } = options

  /** 当前加菜目标订单（null 表示弹窗关闭） */
  const order = ref(null)
  const dishes = ref([])
  const dishLoading = ref(false)
  /** 规格选择弹窗当前菜品（SKU 加载与规格匹配由 SkuPickerModal 组件内部完成） */
  const skuPickerDish = ref(null)
  /** 待加菜品清单 */
  const pendingItems = ref([])
  const submitting = ref(false)

  /** 打开加菜弹窗：重置状态并加载菜品 */
  const open = async (targetOrder) => {
    order.value = targetOrder
    pendingItems.value = []
    skuPickerDish.value = null
    dishes.value = []
    dishLoading.value = true
    try {
      dishes.value = (await loadDishes(targetOrder)) || []
    } catch (e) {
      if (e?.type === 'UNAUTHORIZED') return
      toast.error('加载菜品失败')
    } finally {
      dishLoading.value = false
    }
  }

  const close = () => {
    order.value = null
    pendingItems.value = []
    skuPickerDish.value = null
  }

  /** 打开规格选择弹窗（SKU 数据由 SkuPickerModal 组件自行加载） */
  const openSkuPicker = (dish) => {
    skuPickerDish.value = dish
  }

  const closeSkuPicker = () => {
    skuPickerDish.value = null
  }

  /**
   * 规格选择完成回调（由 SkuPickerModal 的 confirm 事件触发）
   * @param {{sku: object, quantity: number, dealPrice: number, specNamesText: string}} payload
   */
  const addPendingFromPicker = ({ sku, quantity = 1, dealPrice, specNamesText }) => {
    if (!skuPickerDish.value) return
    const existing = pendingItems.value.find(i => i.skuId === sku.id)
    if (existing) {
      existing.quantity = Math.min(existing.quantity + quantity, 99)
    } else {
      pendingItems.value.push({
        skuId: sku.id,
        dishId: skuPickerDish.value.id,
        dishName: skuPickerDish.value.name,
        specNamesText: specNamesText || formatSpecNames(sku.specNames) || '标准',
        unitPrice: Number(sku.unitPrice),
        dealPrice: dealPrice ?? Number(sku.unitPrice),
        quantity
      })
    }
    toast.success('已加入加菜清单')
  }

  const changePendingQty = (index, delta) => {
    const item = pendingItems.value[index]
    if (!item) return
    item.quantity += delta
    if (item.quantity <= 0) {
      pendingItems.value.splice(index, 1)
    }
  }

  const removePending = (index) => {
    pendingItems.value.splice(index, 1)
  }

  /** 提交加菜 */
  const submit = async () => {
    if (!order.value || pendingItems.value.length === 0 || typeof submitItems !== 'function') return
    submitting.value = true
    try {
      const items = pendingItems.value.map(i => ({ skuId: i.skuId, quantity: i.quantity }))
      const res = await submitItems(order.value, items)
      if (isSuccess(res)) {
        toast.success('加菜成功')
        close()
        onSuccess?.()
      } else {
        toast.error(res?.message || '加菜失败')
      }
    } catch (e) {
      // 登录失效时 fetch 封装已统一跳转，不再重复提示
      if (e?.type === 'UNAUTHORIZED') return
      toast.error(e?.message || '加菜失败')
    } finally {
      submitting.value = false
    }
  }

  // reactive 包装：模板中 api.xxx 自动解包 ref，无需 .value
  return reactive({
    title,
    order,
    dishes,
    dishLoading,
    skuPickerDish,
    pendingItems,
    submitting,
    itemCount: orderItemCount,
    specNamesText: formatSpecNames,
    open,
    close,
    openSkuPicker,
    closeSkuPicker,
    addPendingFromPicker,
    changePendingQty,
    removePending,
    submit
  })
}
