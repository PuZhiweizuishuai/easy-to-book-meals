/**
 * 订单领域展示工具（全端统一口径）
 *
 * 集中管理此前散落在 OrderManagement / Dashboard / MyOrders / StoreMenuPage
 * 中重复实现的订单状态、订单类型文案与徽章样式，避免各页维护不一致。
 */

/** 订单状态码（与后端 OrderStatus 枚举一致） */
export const ORDER_STATUS = {
  PENDING: 0,     // 待接单
  CONFIRMED: 1,   // 已接单
  PREPARING: 2,   // 制作中
  SERVED: 3,      // 已出餐
  COMPLETED: 4,   // 已完成
  CANCELLED: 5    // 已取消
}

const STATUS_TEXT_MAP = {
  [ORDER_STATUS.PENDING]: '待接单',
  [ORDER_STATUS.CONFIRMED]: '已接单',
  [ORDER_STATUS.PREPARING]: '制作中',
  [ORDER_STATUS.SERVED]: '已出餐',
  [ORDER_STATUS.COMPLETED]: '已完成',
  [ORDER_STATUS.CANCELLED]: '已取消'
}

const STATUS_BADGE_MAP = {
  [ORDER_STATUS.PENDING]: 'badge-warning',
  [ORDER_STATUS.CONFIRMED]: 'badge-info',
  [ORDER_STATUS.PREPARING]: 'badge-accent',
  [ORDER_STATUS.SERVED]: 'badge-secondary',
  [ORDER_STATUS.COMPLETED]: 'badge-success',
  [ORDER_STATUS.CANCELLED]: 'badge-error'
}

/** 订单状态文案 */
export const orderStatusText = (status) => STATUS_TEXT_MAP[status] ?? '未知'

/** 订单状态对应的 DaisyUI badge 样式类 */
export const orderStatusBadge = (status) => STATUS_BADGE_MAP[status] ?? 'badge-ghost'

/** 订单类型码（与后端 OrderType 枚举一致）：0 堂食 / 1 外送 / 2 线上 */
export const ORDER_TYPE = {
  DINE_IN: 0,
  DELIVERY: 1,
  ONLINE: 2
}

const ORDER_TYPE_TEXT_MAP = {
  [ORDER_TYPE.DINE_IN]: '堂食',
  [ORDER_TYPE.DELIVERY]: '外送',
  [ORDER_TYPE.ONLINE]: '线上'
}

const ORDER_TYPE_BADGE_MAP = {
  [ORDER_TYPE.DINE_IN]: 'badge-success',
  [ORDER_TYPE.DELIVERY]: 'badge-warning',
  [ORDER_TYPE.ONLINE]: 'badge-info'
}

/** 订单类型文案 */
export const orderTypeText = (type) => ORDER_TYPE_TEXT_MAP[type] ?? '堂食'

/** 订单类型对应的 DaisyUI badge 样式类 */
export const orderTypeBadge = (type) => ORDER_TYPE_BADGE_MAP[type] ?? 'badge-ghost'

/**
 * 统计订单内菜品总件数（退菜菜品不计入）
 * @param {object} order - 订单对象（含 items）
 * @returns {number}
 */
export const orderItemCount = (order) => {
  return (order?.items || []).reduce((sum, item) => sum + (item.quantity || 0), 0)
}
