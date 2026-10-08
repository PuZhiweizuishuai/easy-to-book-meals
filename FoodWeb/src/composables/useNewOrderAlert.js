import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useAlertSettings } from '@/stores/alertSettings'
import { useStoreContext } from '@/stores/storeContext'
import { showBrowserNotification, playAlertSound, canNotify } from '@/utils/browserNotify'
import toast from '@/utils/toast'

/** 已见条目上限，超出后丢弃最早的，避免长时间挂机内存持续增长 */
const SEEN_LIMIT = 800

/** 单条通知正文最多列出的明细行数 */
const MAX_BODY_LINES = 3

/**
 * 新订单 / 新菜品轮询提醒
 *
 * 使用方式：页面传入一个 fetcher，每轮返回当前快照条目 `[{ key, text }]`
 * （key 为订单 id / 菜品明细 id，text 为通知正文里的一行描述）。
 * 首轮只建立基线不提醒，之后每轮把未见过的 key 汇总成一条系统通知并播放提示音。
 *
 * 开关与频率取自「店铺设置」页保存的提醒设置，切换门店或修改设置会即时生效。
 *
 * @param {object} options
 * @param {'order'|'item'} options.kind - 提醒类型，决定提示音
 * @param {string} options.title - 通知标题
 * @param {() => Promise<Array<{key: any, text: string}>|null>} options.fetcher - 拉取快照，失败返回 null
 * @param {(items: Array<{key: any, text: string}>) => void} [options.onFresh] - 发现新条目后的回调（用于刷新页面列表）
 * @returns {{ polling: import('vue').Ref<boolean> }} polling - 是否正在轮询
 */
export function useNewOrderAlert({ kind, title, fetcher, onFresh }) {
  const alertSettings = useAlertSettings()
  const storeContext = useStoreContext()

  /** 是否处于轮询中（页面可据此让出自己的定时器，避免重复请求） */
  const polling = ref(false)

  let timer = null
  /** 已提醒过的条目 key */
  const seen = new Set()
  let baselined = false
  /** 轮询代次：停止/切换门店后丢弃在途请求的结果，避免污染新基线 */
  let generation = 0

  const enabled = computed(
    () => alertSettings.current.polling && !!storeContext.currentStoreId
  )

  const unitLabel = kind === 'item' ? '菜品' : '订单'
  const unitWord = kind === 'item' ? '道' : '笔'

  const markSeen = (key) => {
    if (key == null || seen.has(key)) return
    seen.add(key)
    // Set 的迭代顺序即插入顺序，超限时丢弃最早的
    if (seen.size > SEEN_LIMIT) {
      seen.delete(seen.values().next().value)
    }
  }

  /** 发一条系统通知 + 提示音，设置与权限决定具体形式 */
  const alert = (fresh) => {
    const lines = [
      `${fresh.length} ${unitWord}新${unitLabel}`,
      ...fresh.slice(0, MAX_BODY_LINES).map(i => i.text),
    ]
    if (fresh.length > MAX_BODY_LINES) {
      lines.push(`……等共 ${fresh.length} ${unitWord}`)
    }
    const body = lines.join('\n')

    if (alertSettings.current.notify) {
      const shown = showBrowserNotification(title, body)
      // 权限被拒绝或浏览器不支持时，至少在本页给出可见提示
      if (!shown && !canNotify()) toast.warning(`${title}：${body}`, 8000)
    }
    if (alertSettings.current.sound) playAlertSound(kind)
  }

  const tick = async () => {
    const currentGeneration = generation
    let items
    try {
      items = await fetcher()
    } catch {
      return
    }
    if (currentGeneration !== generation) return
    if (!Array.isArray(items)) return

    if (!baselined) {
      baselined = true
      items.forEach(i => markSeen(i.key))
      return
    }

    const fresh = items.filter(i => i.key != null && !seen.has(i.key))
    items.forEach(i => markSeen(i.key))
    if (fresh.length === 0) return

    alert(fresh)
    onFresh?.(fresh)
  }

  const clearTimer = () => {
    if (timer) clearInterval(timer)
    timer = null
  }

  const stop = () => {
    generation += 1
    clearTimer()
    polling.value = false
    baselined = false
    seen.clear()
  }

  const start = () => {
    stop()
    if (!enabled.value) return
    polling.value = true
    tick()
    timer = setInterval(tick, alertSettings.current.intervalSeconds * 1000)
  }

  /** 仅重建定时器，保留基线（改频率时不必重新拉一轮） */
  const retime = () => {
    if (!polling.value) return
    clearTimer()
    timer = setInterval(tick, alertSettings.current.intervalSeconds * 1000)
  }

  watch(enabled, (on) => {
    if (on) start()
    else stop()
  })

  watch(() => storeContext.currentStoreId, () => {
    if (enabled.value) start()
    else stop()
  })

  watch(() => alertSettings.current.intervalSeconds, retime)

  onMounted(() => {
    if (enabled.value) start()
  })

  onBeforeUnmount(stop)

  return { polling }
}
