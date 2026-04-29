// utils/toast.js
import { h, render } from 'vue'
import ToastComponent from '@/components/Toast.vue'

const createToast = (message, type = 'info', duration = 3000) => {
  // 1. 创建挂载容器
  const container = document.createElement('div')
  document.body.appendChild(container)

  // 2. 创建 VNode
  const vnode = h(ToastComponent, {
    message,
    type,
    duration,
    // 动画结束后销毁 DOM
    onVnodeUnmounted: () => {
      document.body.removeChild(container)
    }
  })

  // 3. 渲染
  render(vnode, container)

  // 4. 定时移除（匹配组件内的 visible 逻辑）
  setTimeout(() => {
    render(null, container)
  }, duration + 500) // 略长于 duration 以便留出动画时间
}

const toast = {
  info: (msg, dur) => createToast(msg, 'info', dur),
  success: (msg, dur) => createToast(msg, 'success', dur),
  warning: (msg, dur) => createToast(msg, 'warning', dur),
  error: (msg, dur) => createToast(msg, 'error', dur)
}

// 改为默认导出
export default toast;