/**
 * 全局确认弹窗 composable（替代浏览器原生 confirm()）
 *
 * 用法：
 *   const { confirm } = useConfirm()
 *   const ok = await confirm({ title: '确认删除', message: '...', tone: 'error' })
 *   if (!ok) return
 *
 * 状态为模块级单例：只需在 App.vue 挂载一次 <ConfirmDialog />，
 * 任意页面/组件调用 confirm() 都会弹出同一个弹窗。
 */
import { reactive } from 'vue'

const DEFAULT_OPTIONS = {
  title: '请确认',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
  /** 确认按钮语义色调：primary 常规 / error 危险操作（删除、退菜）/ warning 警示 */
  tone: 'primary'
}

const confirmState = reactive({
  open: false,
  ...DEFAULT_OPTIONS,
  /** 用户选择后的 Promise 解决函数 */
  resolve: null
})

function close(result) {
  confirmState.open = false
  const resolve = confirmState.resolve
  confirmState.resolve = null
  if (typeof resolve === 'function') resolve(result)
}

export function useConfirm() {
  /**
   * 弹出确认框，返回 Promise<boolean>：确认=true，取消/点遮罩/Esc=false
   * @param {string|object} options - 直接传字符串作为 message，或传完整配置对象
   * @returns {Promise<boolean>}
   */
  const confirm = (options) => {
    const normalized = typeof options === 'string' ? { message: options } : (options || {})
    Object.assign(confirmState, DEFAULT_OPTIONS, normalized, { open: true })
    return new Promise((resolve) => {
      confirmState.resolve = resolve
    })
  }

  const handleConfirm = () => close(true)
  const handleCancel = () => close(false)

  return {
    confirmState,
    confirm,
    handleConfirm,
    handleCancel
  }
}
