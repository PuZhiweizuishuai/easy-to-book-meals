<template>
  <div>
    <!-- 顶部返回 + 标题 -->
    <div class="flex items-center gap-3 mb-6">
      <button class="btn btn-ghost btn-circle btn-sm" @click="goBack">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <div class="flex-1">
        <h2 class="text-2xl font-bold text-base-content">餐桌详情</h2>
        <p class="text-sm text-base-content/60 mt-1" v-if="table">
          {{ table.tableNumber }} · {{ table.area || '未分区' }} · {{ table.capacity }}人桌
        </p>
      </div>
      <button class="btn btn-ghost btn-sm bg-base-100 border border-base-200" @click="fetchTable" :disabled="loading">
        刷新
      </button>
    </div>

    <div v-if="loading" class="flex justify-center items-center py-24">
      <span class="loading loading-spinner loading-lg text-primary"></span>
    </div>

    <div v-else-if="!table" class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body text-center py-16">
        <p class="text-base-content/50">餐桌不存在或无权查看</p>
      </div>
    </div>

    <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 左侧：餐桌信息 -->
      <div class="lg:col-span-2 space-y-6">
        <!-- 基础信息卡片 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <h3 class="card-title">基础信息</h3>
              <span :class="['badge badge-sm gap-1', statusBadgeClass]">
                <span class="w-1.5 h-1.5 rounded-full" :class="statusDotClass"></span>
                {{ statusLabel }}
              </span>
            </div>
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-4 mt-3">
              <div>
                <div class="text-xs text-base-content/50">餐桌号</div>
                <div class="font-semibold">{{ table.tableNumber }}</div>
              </div>
              <div>
                <div class="text-xs text-base-content/50">区域</div>
                <div class="font-semibold">{{ table.area || '未设置' }}</div>
              </div>
              <div>
                <div class="text-xs text-base-content/50">座位数</div>
                <div class="font-semibold">{{ table.capacity }} 人</div>
              </div>
              <div>
                <div class="text-xs text-base-content/50">排序权重</div>
                <div class="font-semibold">{{ table.sortOrder ?? 0 }}</div>
              </div>
              <div>
                <div class="text-xs text-base-content/50">创建时间</div>
                <div class="font-semibold text-sm">{{ formatTime(table.createTime) }}</div>
              </div>
              <div v-if="table.remark" class="col-span-2 sm:col-span-3">
                <div class="text-xs text-base-content/50">备注</div>
                <div class="text-sm">{{ table.remark }}</div>
              </div>
            </div>

            <!-- 快捷状态切换 -->
            <div v-if="canManage" class="divider my-2"></div>
            <div v-if="canManage" class="flex flex-wrap gap-2">
              <button class="btn btn-xs" :class="table.status === 0 ? 'btn-success' : 'btn-ghost border border-base-200'" @click="changeStatus(0)" :disabled="acting">空闲</button>
              <button class="btn btn-xs" :class="table.status === 3 ? 'btn-info' : 'btn-ghost border border-base-200'" @click="changeStatus(3)" :disabled="acting">清理中</button>
              <button class="btn btn-xs" :class="table.status === 4 ? 'btn-ghost bg-base-200' : 'btn-ghost border border-base-200'" @click="changeStatus(4)" :disabled="acting">{{ table.status === 4 ? '已停用' : '停用' }}</button>
              <router-link :to="{ name: 'tables' }" class="btn btn-xs btn-ghost border border-base-200 ml-auto">返回列表</router-link>
            </div>
            <div v-else class="flex justify-end mt-2">
              <router-link :to="{ name: 'tables' }" class="btn btn-xs btn-ghost border border-base-200">返回列表</router-link>
            </div>
          </div>
        </div>

        <!-- 当前订单卡片：实现订单与餐桌的关联展示 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <h3 class="card-title">当前订单</h3>
              <span v-if="table.currentOrderId" class="badge badge-warning badge-sm">用餐中</span>
              <span v-else class="badge badge-success badge-sm">无占用</span>
            </div>

            <div v-if="orderLoading" class="flex justify-center py-8">
              <span class="loading loading-spinner text-primary"></span>
            </div>

            <div v-else-if="!table.currentOrderId" class="text-center py-8 text-base-content/40">
              该餐桌当前没有进行中的订单
            </div>

            <div v-else-if="currentOrder" class="mt-2 space-y-2">
              <div class="flex items-center justify-between flex-wrap gap-2">
                <div class="flex items-center gap-2 flex-wrap">
                  <span class="font-mono text-sm text-base-content/70">{{ currentOrder.orderNo }}</span>
                  <span :class="['badge badge-sm', orderStatusBadge(currentOrder.status)]">{{ orderStatusText(currentOrder.status) }}</span>
                </div>
                <span class="text-xs text-base-content/50">{{ formatTime(currentOrder.createTime) }}</span>
              </div>
              <div class="text-sm text-base-content/60">下单人：{{ currentOrder.userNickname || ('用户#' + currentOrder.userId) }}</div>

              <div class="overflow-x-auto rounded-box border border-base-200 mt-2">
                <table class="table table-sm">
                  <thead>
                    <tr><th>菜品</th><th>规格</th><th>数量</th><th class="text-right">小计</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in (currentOrder.items || [])" :key="item.id">
                      <td class="text-sm">{{ item.dishName }}</td>
                      <td class="text-sm text-base-content/60">{{ specNamesText(item.specNames) || '标准' }}</td>
                      <td class="text-sm">×{{ item.quantity }}</td>
                      <td class="text-right text-sm">¥{{ item.totalPrice }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="flex items-center justify-between pt-2">
                <span v-if="currentOrder.remark" class="text-xs text-base-content/50 truncate">备注：{{ currentOrder.remark }}</span>
                <span class="font-bold text-primary ml-auto">应付 ¥{{ currentOrder.payAmount }}</span>
              </div>

              <div class="flex gap-2 pt-2">
                <router-link :to="{ name: 'orders' }" class="btn btn-xs btn-ghost border border-base-200">去订单管理</router-link>
              </div>
            </div>

            <div v-else class="text-center py-8 text-base-content/40">
              订单数据加载失败
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：桌牌二维码 -->
      <div class="space-y-6">
        <div class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body items-center text-center">
            <h3 class="card-title self-start">点餐二维码</h3>
            <p class="text-xs text-base-content/50 self-start -mt-2 mb-1">顾客扫码后自动定位到本桌下单</p>

            <!-- 二维码 -->
            <div class="bg-white p-3 rounded-xl border border-base-200 mt-2">
              <img v-if="qrDataUrl" :src="qrDataUrl" alt="点餐二维码" class="w-56 h-56 object-contain" />
              <div v-else class="w-56 h-56 flex items-center justify-center">
                <span class="loading loading-spinner text-primary"></span>
              </div>
            </div>

            <!-- 桌牌预览文字 -->
            <div class="mt-3">
              <div class="font-bold text-lg">{{ table.tableNumber }}号桌</div>
              <div class="text-sm text-base-content/60">{{ storeName }} · {{ table.area || '' }}</div>
            </div>

            <!-- 扫码链接（可复制） -->
            <div class="w-full mt-3">
              <div class="text-xs text-base-content/50 mb-1">扫码点餐链接</div>
              <div class="flex gap-2">
                <input :value="scanUrl" readonly class="input input-bordered input-xs w-full font-mono text-xs" @focus="$event.target.select()" />
                <button class="btn btn-xs btn-ghost border border-base-200 shrink-0" @click="copyScanUrl">复制</button>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="flex gap-2 w-full mt-4">
              <button class="btn btn-primary btn-sm flex-1" :disabled="!qrDataUrl" @click="downloadQr">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
                下载
              </button>
              <button class="btn btn-ghost btn-sm flex-1 border border-base-200" :disabled="!qrDataUrl" @click="printQr">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z" />
                </svg>
                打印
              </button>
            </div>

            <div v-if="canManage" class="w-full mt-2">
              <button class="btn btn-ghost btn-xs w-full border border-base-200 text-error" :disabled="regenerating" @click="regenerateQrKey">
                <span v-if="regenerating" class="loading loading-spinner loading-xs"></span>
                重置二维码（旧码失效）
              </button>
            </div>
          </div>
        </div>

        <!-- 使用说明 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body p-4">
            <h4 class="font-semibold text-sm mb-2">使用说明</h4>
            <ul class="text-xs text-base-content/60 space-y-1 list-disc pl-4">
              <li>下载二维码图片后打印，贴于桌面桌牌。</li>
              <li>顾客扫码进入门店菜单页，系统自动绑定本桌。</li>
              <li>下单后订单与餐桌自动关联（见左侧「当前订单」）。</li>
              <li>重置二维码后，旧二维码将无法使用，需重新打印。</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { httpGet, httpPost, isSuccess } from '@/utils/fetch'
import { useStoreContext } from '@/stores/storeContext'
import { useAuthStore } from '@/stores/auth'
import toast from '@/utils/toast'
import QRCode from 'qrcode'

const route = useRoute()
const router = useRouter()
const storeContext = useStoreContext()
const authStore = useAuthStore()

const tableId = Number(route.params.id)
const storefrontId = computed(() => storeContext.currentStoreId)

/** 是否有餐桌管理权限（无则只读，隐藏状态切换与重置二维码按钮） */
const canManage = computed(() => authStore.hasAnyPermission(['table:manage']))

const loading = ref(false)
const acting = ref(false)
const table = ref(null)
const storeName = computed(() => storeContext.currentStore?.storeName || '')

// 当前订单
const orderLoading = ref(false)
const currentOrder = ref(null)

// 二维码
const qrDataUrl = ref('')
const regenerating = ref(false)

const STATUS_MAP = {
  0: { label: '空闲', badge: 'badge-success', dot: 'bg-green-400' },
  1: { label: '已占用', badge: 'badge-error', dot: 'bg-red-400' },
  2: { label: '已预订', badge: 'badge-warning', dot: 'bg-amber-400' },
  3: { label: '清理中', badge: 'badge-info', dot: 'bg-blue-400' },
  4: { label: '停用', badge: 'badge-ghost', dot: 'bg-gray-400' }
}
const statusConfig = computed(() => STATUS_MAP[table.value?.status] ?? STATUS_MAP[4])
const statusLabel = computed(() => statusConfig.value.label)
const statusBadgeClass = computed(() => statusConfig.value.badge)
const statusDotClass = computed(() => statusConfig.value.dot)

/** 扫码点餐链接：/store/{门店ID}?table={二维码标识} */
const scanUrl = computed(() => {
  if (!table.value || !table.value.qrCodeKey || !storefrontId.value) return ''
  const base = window.location.origin
  return `${base}/store/${storefrontId.value}?table=${table.value.qrCodeKey}`
})

onMounted(() => {
  if (storefrontId.value) {
    fetchTable()
  }
})

// 门店切换时重新加载
watch(() => storefrontId.value, (val) => {
  if (val) fetchTable()
})

/** 加载餐桌详情 */
const fetchTable = async () => {
  if (!storefrontId.value) return
  loading.value = true
  try {
    const res = await httpGet(`/dining-table/${tableId}?storefrontId=${storefrontId.value}`)
    if (isSuccess(res) && res.data) {
      table.value = res.data
      // 生成二维码
      await generateQrCode()
      // 若有当前订单，加载订单详情
      if (table.value.currentOrderId) {
        fetchCurrentOrder()
      } else {
        currentOrder.value = null
      }
    } else {
      table.value = null
    }
  } catch (e) {
    console.error('加载餐桌失败:', e)
    toast.error('加载餐桌失败')
  } finally {
    loading.value = false
  }
}

/** 加载当前绑定订单详情（订单与餐桌关联） */
const fetchCurrentOrder = async () => {
  if (!table.value?.currentOrderId || !storefrontId.value) return
  orderLoading.value = true
  try {
    const res = await httpGet(`/order/manage/detail/${table.value.currentOrderId}?storeId=${storefrontId.value}`)
    if (isSuccess(res) && res.data) {
      currentOrder.value = res.data
    } else {
      currentOrder.value = null
    }
  } catch (e) {
    console.error('加载当前订单失败:', e)
    currentOrder.value = null
  } finally {
    orderLoading.value = false
  }
}

/** 生成二维码图片（data URL） */
const generateQrCode = async () => {
  const url = scanUrl.value
  if (!url) {
    qrDataUrl.value = ''
    return
  }
  try {
    qrDataUrl.value = await QRCode.toDataURL(url, {
      width: 400,
      margin: 2,
      color: { dark: '#000000', light: '#ffffff' }
    })
  } catch (e) {
    console.error('生成二维码失败:', e)
    toast.error('生成二维码失败')
    qrDataUrl.value = ''
  }
}

// 链接变化时重新生成二维码
watch(scanUrl, () => generateQrCode())

/** 下载二维码图片 */
const downloadQr = () => {
  if (!qrDataUrl.value || !table.value) return
  const a = document.createElement('a')
  a.href = qrDataUrl.value
  a.download = `桌牌_${table.value.tableNumber}_${storeName.value}.png`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

/** 打印桌牌（新窗口，仅含二维码与桌号） */
const printQr = () => {
  if (!qrDataUrl.value || !table.value) return
  const w = window.open('', '_blank', 'width=480,height=640')
  if (!w) {
    toast.warning('请允许弹出窗口以打印桌牌')
    return
  }
  const t = table.value
  w.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>桌牌-${t.tableNumber}</title>
  <style>
    body{margin:0;font-family:-apple-system,"Microsoft YaHei",sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;background:#fff}
    .card{padding:24px 32px;text-align:center}
    img{width:260px;height:260px}
    .num{font-size:42px;font-weight:bold;margin:12px 0 4px}
    .sub{font-size:18px;color:#666;margin-bottom:8px}
    .store{font-size:14px;color:#999}
    .tip{font-size:13px;color:#999;margin-top:16px}
  </style></head><body>
    <div class="card">
      <img src="${qrDataUrl.value}" />
      <div class="num">${t.tableNumber}号桌</div>
      <div class="sub">${t.area || ''} · ${t.capacity}人桌</div>
      <div class="store">${storeName.value}</div>
      <div class="tip">扫码点餐 · 自动绑定本桌</div>
    </div>
  </body></html>`)
  w.document.close()
  w.focus()
  w.onload = () => { w.print() }
}

/** 复制扫码链接 */
const copyScanUrl = async () => {
  if (!scanUrl.value) return
  try {
    await navigator.clipboard.writeText(scanUrl.value)
    toast.success('链接已复制')
  } catch {
    // 降级：选中输入框供用户手动复制
    toast.warning('复制失败，请手动选中复制')
  }
}

/** 切换餐桌状态 */
const changeStatus = async (status) => {
  if (!table.value || table.value.status === status) return
  acting.value = true
  try {
    const res = await httpPost(`/dining-table/status?id=${table.value.id}&storefrontId=${storefrontId.value}&status=${status}`)
    if (isSuccess(res) && res.data) {
      table.value = res.data
      toast.success('状态已更新')
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    toast.error('操作失败')
  } finally {
    acting.value = false
  }
}

/** 重置二维码标识（旧码失效，需重新打印） */
const regenerateQrKey = async () => {
  if (!table.value) return
  if (!confirm('重置后旧二维码将立即失效，顾客无法用旧码点餐，需重新打印桌牌。确定继续吗？')) return
  regenerating.value = true
  try {
    const newKey = generateUuid()
    const res = await httpPost('/dining-table/update', {
      id: table.value.id,
      storefrontId: storefrontId.value,
      qrCodeKey: newKey
    })
    if (isSuccess(res) && res.data) {
      table.value = res.data
      await generateQrCode()
      toast.success('二维码已重置，请重新下载打印')
    } else {
      toast.error(res?.message || '重置失败')
    }
  } catch (e) {
    toast.error('重置失败')
  } finally {
    regenerating.value = false
  }
}

/** 生成简易 UUID（仅用于前端发起重置请求，后端会原样存储） */
function generateUuid() {
  if (crypto && crypto.randomUUID) return crypto.randomUUID().replace(/-/g, '')
  // 降级方案
  return 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/x/g, () => Math.floor(Math.random() * 16).toString(16))
}

const goBack = () => {
  router.push({ name: 'tables' })
}

/** 订单状态展示 */
const orderStatusText = (status) => {
  const map = { 0: '待接单', 1: '已接单', 2: '制作中', 3: '已出餐', 4: '已完成', 5: '已取消' }
  return map[status] ?? '未知'
}
const orderStatusBadge = (status) => {
  const map = { 0: 'badge-warning', 1: 'badge-info', 2: 'badge-accent', 3: 'badge-secondary', 4: 'badge-success', 5: 'badge-error' }
  return map[status] ?? 'badge-ghost'
}

const specNamesText = (specNames) => {
  if (!specNames) return ''
  try {
    const parsed = typeof specNames === 'string' ? JSON.parse(specNames) : specNames
    if (Array.isArray(parsed)) return parsed.join('/')
    if (parsed && typeof parsed === 'object') return Object.values(parsed).join('/')
  } catch { /* 忽略 */ }
  return String(specNames || '')
}

const formatTime = (ts) => {
  if (!ts) return '-'
  const d = new Date(Number(ts))
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>
