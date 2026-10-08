/**
 * 浏览器消息通知与提示音
 *
 * 两条浏览器限制决定了这里的用法：
 * - Notification 权限只能在用户手势（点击）中申请，因此授权由店铺设置页的开关触发，
 *   轮询侧只读取当前权限状态，不再主动弹授权框。
 * - 提示音受自动播放策略限制，页面尚无用户交互时 play() 会被拒绝，故播放失败只提示一次。
 */
import toast from '@/utils/toast'

const BASE = import.meta.env.BASE_URL || '/'

/** 提醒音源（public/sound 下的静态资源） */
const SOUNDS = {
  order: `${BASE}sound/new_order.mp3`,
  item: `${BASE}sound/new_item.wav`,
}

/** 当前浏览器是否支持消息通知 */
export const isNotificationSupported = () => typeof window !== 'undefined' && 'Notification' in window

/**
 * 通知权限状态：granted / denied / default / unsupported
 */
export const notificationPermission = () =>
  isNotificationSupported() ? Notification.permission : 'unsupported'

/** 是否已授权展示系统通知 */
export const canNotify = () => notificationPermission() === 'granted'

/**
 * 申请通知权限，必须在点击等用户手势的同步调用链中触发
 * @returns {Promise<string>} 申请后的权限状态
 */
export const requestNotificationPermission = async () => {
  if (!isNotificationSupported()) return 'unsupported'
  if (Notification.permission === 'granted') return 'granted'
  try {
    return await Notification.requestPermission()
  } catch {
    return notificationPermission()
  }
}

/**
 * 展示一条系统通知
 * @param {string} title - 标题
 * @param {string} body - 正文（\\n 换行，桌面端生效）
 * @returns {boolean} 是否成功展示（未授权或不支持时为 false，调用方据此降级为页内提示）
 */
export const showBrowserNotification = (title, body) => {
  if (!canNotify()) return false
  try {
    const notification = new Notification(title, {
      body,
      icon: `${BASE}logo.png`,
      // 同类通知只保留最新一条，避免连续来单时堆叠刷屏
      tag: title,
    })
    notification.onclick = () => {
      window.focus()
      notification.close()
    }
    return true
  } catch {
    // 部分移动端浏览器不支持构造函数式通知
    return false
  }
}

/** 提示音被自动播放策略拦截后只提醒一次 */
let autoplayTipShown = false

/**
 * 播放提示音
 * @param {'order'|'item'} kind - 提醒类型，决定音源
 */
export const playAlertSound = (kind = 'order') => {
  const src = SOUNDS[kind] || SOUNDS.order
  const audio = new Audio(src)
  audio.play().catch(() => {
    if (autoplayTipShown) return
    autoplayTipShown = true
    toast.warning('提示音未能播放：请先在页面上点一下（浏览器禁止未交互时自动播放声音）', 6000)
  })
}
