<template>
  <div>
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-base-content">提醒设置</h2>
        <p class="text-sm text-base-content/60 mt-1">
          {{ storeContext.currentStore ? `${storeContext.currentStore.storeName} · ` : '' }}新订单与新菜品的轮询提醒
        </p>
      </div>
      <button class="btn btn-ghost btn-sm bg-base-100 border border-base-200" @click="handleTestAlert">
        发送测试提醒
      </button>
    </div>

    <div v-if="!storeContext.currentStore" class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body text-center py-16">
        <p class="text-base-content/50 text-lg">请先在顶部选择要管理的门店</p>
      </div>
    </div>

    <div v-else class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body">
        <p class="text-sm text-base-content/50">
          开启后，在「数据看板」「订单管理」「后厨出餐」页面按设定频率轮询新订单与新菜品，
          发现后立即弹出浏览器系统通知并播放提示音。设置仅保存在当前浏览器（换设备或清除浏览器数据后需重新设置）。
        </p>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-4">
          <div class="space-y-3">
            <label class="flex items-center justify-between gap-3 bg-base-200 rounded-lg px-4 py-3 cursor-pointer">
              <span class="min-w-0">
                <span class="block text-sm font-medium">开启轮询</span>
                <span class="block text-xs text-base-content/50">关闭后上述页面不再自动查询新订单/新菜品</span>
              </span>
              <input type="checkbox" class="toggle toggle-primary shrink-0"
                     :checked="alertSettings.current.polling" @change="handlePollingToggle($event)" />
            </label>

            <label class="flex items-center justify-between gap-3 bg-base-200 rounded-lg px-4 py-3"
                   :class="notifySupported ? 'cursor-pointer' : 'opacity-60'">
              <span class="min-w-0">
                <span class="block text-sm font-medium">浏览器消息通知</span>
                <span class="block text-xs text-base-content/50">关闭后只在页面内提示，不弹系统通知</span>
              </span>
              <input type="checkbox" class="toggle toggle-primary shrink-0"
                     :checked="alertSettings.current.notify" :disabled="!notifySupported"
                     @change="handleNotifyToggle($event)" />
            </label>

            <div class="flex flex-wrap items-center justify-between gap-3 bg-base-200 rounded-lg px-4 py-3">
              <label class="flex items-center justify-between gap-3 flex-1 min-w-[220px] cursor-pointer">
                <span class="min-w-0">
                  <span class="block text-sm font-medium">声音提醒</span>
                  <span class="block text-xs text-base-content/50">新订单播 new_order.mp3，新菜品播 new_item.wav</span>
                </span>
                <input type="checkbox" class="toggle toggle-primary shrink-0"
                       :checked="alertSettings.current.sound" @change="handleSoundToggle($event)" />
              </label>
              <div class="flex gap-1 shrink-0">
                <button class="btn btn-ghost btn-xs" @click="handlePreviewSound('order')">试听订单音</button>
                <button class="btn btn-ghost btn-xs" @click="handlePreviewSound('item')">试听菜品音</button>
              </div>
            </div>
          </div>

          <div class="space-y-3">
            <div class="bg-base-200 rounded-lg px-4 py-3">
              <span class="block text-sm font-medium">轮询频率</span>
              <select class="select select-bordered select-sm mt-2 w-full max-w-xs"
                      :value="alertSettings.current.intervalSeconds" @change="handleIntervalChange">
                <option v-for="opt in INTERVAL_OPTIONS" :key="opt.value" :value="opt.value">
                  {{ opt.label }}一次
                </option>
              </select>
              <p class="text-xs text-base-content/40 mt-2">
                频率越高提醒越及时，请求量也越大；后台标签页会被浏览器限速，实际间隔可能更长。
              </p>
            </div>

            <div class="rounded-lg border px-4 py-3 text-xs" :class="permissionTone">
              <div class="flex items-center gap-2">
                <span class="font-medium">浏览器通知权限</span>
                <span :class="['badge badge-sm', permissionBadge]">{{ permissionText }}</span>
              </div>
              <p v-if="permission === 'denied'" class="mt-2 text-base-content/50">
                权限已被拒绝：请在浏览器地址栏左侧的「站点设置 → 通知」中改为「允许」，刷新页面后再开启。
              </p>
              <p v-else-if="permission === 'default'" class="mt-2 text-base-content/50">
                开启「浏览器消息通知」时会弹出授权框，请选择「允许」。
              </p>
              <p v-else-if="permission === 'unsupported'" class="mt-2 text-base-content/50">
                当前浏览器不支持消息通知，提醒将以页面内提示 + 提示音的形式呈现。
              </p>
              <p v-else class="mt-2 text-base-content/50">已授权，新订单/新菜品会弹出系统通知。</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import toast from '@/utils/toast'
import { useStoreContext } from '@/stores/storeContext'
import { useAlertSettings, INTERVAL_OPTIONS } from '@/stores/alertSettings'
import {
  isNotificationSupported,
  notificationPermission,
  requestNotificationPermission,
  showBrowserNotification,
  playAlertSound,
} from '@/utils/browserNotify'

const storeContext = useStoreContext()

/** ============ 新订单提醒设置（存 localStorage，按门店隔离） ============ */
const alertSettings = useAlertSettings()
const notifySupported = isNotificationSupported()
/** 通知权限状态，授权结果实时刷新到界面 */
const permission = ref(notificationPermission())

const handlePollingToggle = (e) => {
  const next = e.target.checked
  alertSettings.save({ polling: next })
  if (next && !notifySupported) {
    toast.warning('当前浏览器不支持消息通知，提醒将以页面提示 + 提示音呈现')
  }
}

/**
 * 消息通知开关：权限必须在点击手势中申请，所以这里同步发起授权再落库；
 * 授权未通过时把开关显示回滚，避免界面与实际设置不一致。
 */
const handleNotifyToggle = async (e) => {
  const next = e.target.checked
  if (next) {
    const result = await requestNotificationPermission()
    permission.value = result
    if (result !== 'granted') {
      e.target.checked = false
      toast.error(result === 'denied'
        ? '浏览器已拒绝通知权限，请在站点设置中改为「允许」'
        : '未获得通知权限')
      return
    }
  }
  alertSettings.save({ notify: next })
}

const handleSoundToggle = (e) => {
  alertSettings.save({ sound: e.target.checked })
}

const handleIntervalChange = (e) => {
  alertSettings.save({ intervalSeconds: Number(e.target.value) || 30 })
}

const handlePreviewSound = (kind) => {
  playAlertSound(kind)
}

const handleTestAlert = () => {
  if (alertSettings.current.notify) {
    const shown = showBrowserNotification('新订单提醒', '这是一条测试通知，收到即表示浏览器通知已生效')
    if (!shown) toast.warning('未获得浏览器通知权限，无法展示系统通知')
  }
  if (alertSettings.current.sound) playAlertSound('order')
}

const permissionText = computed(() => ({
  granted: '已授权',
  denied: '已拒绝',
  default: '未申请',
  unsupported: '不支持',
})[permission.value] || '未知')

const permissionBadge = computed(() => ({
  granted: 'badge-success',
  denied: 'badge-error',
  default: 'badge-warning',
  unsupported: 'badge-ghost',
})[permission.value] || 'badge-ghost')

const permissionTone = computed(() =>
  permission.value === 'granted' ? 'border-success/40' : 'border-base-300'
)
</script>
