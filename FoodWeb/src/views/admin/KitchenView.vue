<template>
  <div>
    <!-- 页头 -->
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-base-content">后厨出餐</h2>
        <p class="text-sm text-base-content/60 mt-1">点击「开始制作」认领菜品，认领后其他人不可见</p>
      </div>
      <div class="flex items-center gap-2">
        <label class="label cursor-pointer gap-2">
          <span class="label-text text-sm">自动刷新</span>
          <input v-model="autoRefresh" type="checkbox" class="checkbox checkbox-primary checkbox-sm" @change="toggleAutoRefresh" />
        </label>
        <button class="btn btn-ghost btn-sm bg-base-100 border border-base-200" @click="fetchQueue" :disabled="loading">
          <span v-if="refreshing" class="loading loading-spinner loading-xs"></span>
          刷新
        </button>
      </div>
    </div>

    <!-- 概览 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div class="stat bg-base-100 shadow-sm rounded-lg border border-base-200">
        <div class="stat-title text-xs">待制作</div>
        <div class="stat-value text-warning text-2xl">{{ pendingCount }}</div>
        <div class="stat-desc">无人认领</div>
      </div>
      <div class="stat bg-base-100 shadow-sm rounded-lg border border-base-200">
        <div class="stat-title text-xs">我的制作中</div>
        <div class="stat-value text-primary text-2xl">{{ myPreparingCount }}</div>
        <div class="stat-desc">已认领待出餐</div>
      </div>
      <div class="stat bg-base-100 shadow-sm rounded-lg border border-base-200">
        <div class="stat-title text-xs">最长等待</div>
        <div class="stat-value text-2xl" :class="waitTone(maxWaitedMinutes)">
          {{ maxWaitedMinutes ?? 0 }} 分钟
        </div>
        <div class="stat-desc">{{ waitLabel(maxWaitedMinutes) }}</div>
      </div>
      <div class="stat bg-base-100 shadow-sm rounded-lg border border-base-200">
        <div class="stat-title text-xs">超时菜品</div>
        <div class="stat-value text-error text-2xl">{{ overdueCount }}</div>
        <div class="stat-desc">等待超过 {{ OVERDUE_MINUTES }} 分钟</div>
      </div>
    </div>

    <!-- 加载 / 空态 -->
    <div v-if="loading" class="flex justify-center py-24">
      <span class="loading loading-spinner loading-lg text-primary"></span>
    </div>

    <div v-else-if="items.length === 0" class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body items-center py-20 text-center">
        <span class="text-5xl mb-3">🍳</span>
        <p class="text-base-content/50">当前没有待制作菜品</p>
        <p class="text-base-content/40 text-sm mt-1">顾客下单后需先在前台接单，接单后会出现在这里</p>
      </div>
    </div>

    <!-- 菜品队列 -->
    <div v-else class="space-y-2">
      <!-- 我的制作中 -->
      <template v-if="myItems.length > 0">
        <div class="text-sm font-semibold text-primary mb-1 flex items-center gap-1">
          <span class="inline-block w-2 h-2 rounded-full bg-primary"></span>
          我的制作中（{{ myItems.length }}）
        </div>
        <div v-for="item in myItems" :key="item.itemId"
             class="card bg-base-100 shadow-sm border border-primary/30">
          <div class="card-body p-3 flex-row items-center gap-3 flex-wrap">
            <!-- 等待时长指示条 -->
            <div class="flex flex-col items-center justify-center min-w-[64px] px-2"
                 :class="waitTone(item.waitedMinutes)">
              <span class="text-2xl font-bold tabular-nums">{{ item.waitedMinutes ?? 0 }}</span>
              <span class="text-[10px] opacity-70">分钟</span>
            </div>

            <div class="divider divider-horizontal mx-0 self-stretch"></div>

            <!-- 菜品信息 -->
            <div class="flex-1 min-w-[180px]">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-semibold text-base">{{ item.dishName }}</span>
                <span class="font-bold text-primary text-lg">×{{ qty(item.quantity) }}</span>
                <span class="badge badge-primary badge-sm">制作中</span>
                <span v-if="item.addedLater === 1" class="badge badge-warning badge-sm">加菜</span>
                <span v-if="isOverdue(item)" class="badge badge-error badge-sm">超时</span>
              </div>
              <div class="text-xs text-base-content/60 mt-0.5">
                {{ specNamesText(item.specNames) || '标准' }}
              </div>
              <div v-if="item.remark" class="text-xs text-warning mt-1">
                备注：{{ item.remark }}
              </div>
            </div>

            <!-- 订单信息 -->
            <div class="flex flex-col gap-1 text-xs text-base-content/70 min-w-[160px]">
              <div class="flex items-center gap-1 flex-wrap">
                <span v-if="item.tableNumber" class="badge badge-primary badge-sm">{{ item.tableNumber }}号桌</span>
                <span :class="['badge badge-sm', orderTypeBadge(item.orderType)]">{{ orderTypeText(item.orderType) }}</span>
              </div>
              <div class="font-mono text-[11px] text-base-content/50 truncate">{{ item.orderNo }}</div>
              <div v-if="item.orderType !== 0 && item.contactPhone" class="text-[11px] text-base-content/60">
                <span class="opacity-60">电话：</span>
                <span class="font-mono">{{ item.contactPhone }}</span>
              </div>
            </div>

            <!-- 操作 -->
            <div class="flex flex-col gap-1 ml-auto">
              <button class="btn btn-success btn-sm" :disabled="servingId === item.itemId" @click="serveItem(item)">
                <span v-if="servingId === item.itemId" class="loading loading-spinner loading-xs"></span>
                出餐
              </button>
              <button class="btn btn-ghost btn-xs text-base-content/50"
                      :disabled="servingId === `release-${item.itemId}`"
                      @click="releaseItem(item)">
                <span v-if="servingId === `release-${item.itemId}`" class="loading loading-spinner loading-xs"></span>
                释放
              </button>
            </div>
          </div>
        </div>
      </template>

      <!-- 待制作（无人认领） -->
      <template v-if="pendingItems.length > 0">
        <div class="text-sm font-semibold text-base-content/70 mb-1 mt-4 flex items-center gap-1">
          <span class="inline-block w-2 h-2 rounded-full bg-warning"></span>
          待制作（{{ pendingItems.length }}）
        </div>
        <div v-for="item in pendingItems" :key="item.itemId"
             class="card bg-base-100 shadow-sm border"
             :class="isOverdue(item) ? 'border-error/50' : 'border-base-200'">
          <div class="card-body p-3 flex-row items-center gap-3 flex-wrap">
            <!-- 等待时长指示条 -->
            <div class="flex flex-col items-center justify-center min-w-[64px] px-2"
                 :class="waitTone(item.waitedMinutes)">
              <span class="text-2xl font-bold tabular-nums">{{ item.waitedMinutes ?? 0 }}</span>
              <span class="text-[10px] opacity-70">分钟</span>
            </div>

            <div class="divider divider-horizontal mx-0 self-stretch"></div>

            <!-- 菜品信息 -->
            <div class="flex-1 min-w-[180px]">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-semibold text-base">{{ item.dishName }}</span>
                <span class="font-bold text-primary text-lg">×{{ qty(item.quantity) }}</span>
                <span v-if="item.addedLater === 1" class="badge badge-warning badge-sm">加菜</span>
                <span v-if="isOverdue(item)" class="badge badge-error badge-sm">超时</span>
              </div>
              <div class="text-xs text-base-content/60 mt-0.5">
                {{ specNamesText(item.specNames) || '标准' }}
              </div>
              <div v-if="item.remark" class="text-xs text-warning mt-1">
                备注：{{ item.remark }}
              </div>
            </div>

            <!-- 订单信息 -->
            <div class="flex flex-col gap-1 text-xs text-base-content/70 min-w-[160px]">
              <div class="flex items-center gap-1 flex-wrap">
                <span v-if="item.tableNumber" class="badge badge-primary badge-sm">{{ item.tableNumber }}号桌</span>
                <span :class="['badge badge-sm', orderTypeBadge(item.orderType)]">{{ orderTypeText(item.orderType) }}</span>
                <span v-if="pendingOfSameOrder(item) > 1"
                      class="badge badge-ghost badge-sm">
                  同单 {{ pendingOfSameOrder(item) }} 道
                </span>
              </div>
              <div class="font-mono text-[11px] text-base-content/50 truncate">{{ item.orderNo }}</div>
              <div v-if="item.orderType !== 0 && item.contactPhone" class="text-[11px] text-base-content/60">
                <span class="opacity-60">电话：</span>
                <span class="font-mono">{{ item.contactPhone }}</span>
              </div>
            </div>

            <!-- 操作 -->
            <div class="flex flex-col gap-1 ml-auto">
              <button class="btn btn-primary btn-sm" :disabled="servingId === `prep-${item.itemId}`" @click="startPreparing(item)">
                <span v-if="servingId === `prep-${item.itemId}`" class="loading loading-spinner loading-xs"></span>
                开始制作
              </button>
              <button class="btn btn-success btn-xs" :disabled="servingId === item.itemId" @click="serveItem(item)">
                <span v-if="servingId === item.itemId" class="loading loading-spinner loading-xs"></span>
                直接出餐
              </button>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { httpGet, httpPost, isSuccess } from '@/utils/fetch'
import { useStoreContext } from '@/stores/storeContext'
import { useAuthStore } from '@/stores/auth'
import toast from '@/utils/toast'
import { formatSpecNames as specNamesText } from '@/utils/format'
import { orderTypeText, orderTypeBadge } from '@/utils/order'

const storeContext = useStoreContext()
const authStore = useAuthStore()
const STORE_ID = () => storeContext.currentStoreId

/** 超时阈值（分钟）：超过该时长在界面上标红预警 */
const OVERDUE_MINUTES = 15
/** 自动刷新间隔（毫秒） */
const REFRESH_INTERVAL = 30000

const loading = ref(false)
const refreshing = ref(false)
const items = ref([])
const maxWaitedMinutes = ref(0)
const itemCount = ref(0)
const servingId = ref(null)
const autoRefresh = ref(true)
let timer = null

/** 是否有后厨出餐权限（否则只读） */
const canServe = computed(() =>
  authStore.hasAnyPermission(['kitchen:manage', 'order:manage'])
)

/** 我的制作中菜品 */
const myItems = computed(() => items.value.filter(i => i.isMine))

/** 待制作菜品（无人认领） */
const pendingItems = computed(() => items.value.filter(i => !i.isMine))

/** 待制作数量 */
const pendingCount = computed(() => pendingItems.value.length)

/** 我的制作中数量 */
const myPreparingCount = computed(() => myItems.value.length)

/** 超时菜品数 */
const overdueCount = computed(() =>
  items.value.filter(o => Number(o.waitedMinutes) >= OVERDUE_MINUTES).length
)

const isOverdue = (item) => Number(item.waitedMinutes) >= OVERDUE_MINUTES

const waitTone = (minutes) => {
  const m = Number(minutes || 0)
  if (m >= OVERDUE_MINUTES * 2) return 'text-error'
  if (m >= OVERDUE_MINUTES) return 'text-warning'
  return 'text-success'
}

const waitLabel = (minutes) => {
  const m = Number(minutes || 0)
  if (m >= OVERDUE_MINUTES * 2) return '严重超时，请优先处理'
  if (m >= OVERDUE_MINUTES) return '已超过出餐时长'
  return '时效正常'
}

/**
 * 该菜品所属订单还有几道在当前列表中（用于「同单 N 道」提示）
 */
const pendingOfSameOrder = (item) => {
  if (!item) return 0
  return items.value.filter(i => i.orderId === item.orderId).length
}

const toggleAutoRefresh = () => {
  if (autoRefresh.value) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

const startAutoRefresh = () => {
  stopAutoRefresh()
  timer = setInterval(() => {
    if (!document.hidden) fetchQueue(true)
  }, REFRESH_INTERVAL)
}

const stopAutoRefresh = () => {
  if (timer) clearInterval(timer)
  timer = null
}

onMounted(() => {
  if (STORE_ID()) {
    fetchQueue()
    startAutoRefresh()
  }
})

watch(() => storeContext.currentStoreId, () => {
  if (STORE_ID()) fetchQueue()
})

onBeforeUnmount(stopAutoRefresh)

/** 加载后厨队列（silent=true 时静默刷新，不显示 loading 遮罩） */
const fetchQueue = async (silent = false) => {
  if (!STORE_ID()) return
  if (silent) {
    refreshing.value = true
  } else {
    loading.value = true
  }
  try {
    const res = await httpGet(`/order/manage/kitchen/queue?storeId=${STORE_ID()}`)
    if (isSuccess(res) && res.data) {
      items.value = res.data.items || []
      itemCount.value = res.data.itemCount ?? 0
      maxWaitedMinutes.value = res.data.maxWaitedMinutes ?? 0
    } else {
      toast.error(res?.message || '获取后厨队列失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('获取后厨队列失败')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

/** 开始制作（认领菜品） */
const startPreparing = async (item) => {
  if (!canServe.value) {
    toast.error('您没有后厨操作权限')
    return
  }
  servingId.value = `prep-${item.itemId}`
  try {
    const res = await httpPost(`/order/manage/start-preparing/${item.itemId}?storeId=${STORE_ID()}`)
    if (isSuccess(res)) {
      toast.success(`已开始制作「${item.dishName}」`)
      await fetchQueue(true)
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('操作失败')
  } finally {
    servingId.value = null
  }
}

/** 释放制作（取消认领） */
const releaseItem = async (item) => {
  servingId.value = `release-${item.itemId}`
  try {
    const res = await httpPost(`/order/manage/release-preparing/${item.itemId}?storeId=${STORE_ID()}`)
    if (isSuccess(res)) {
      toast.success(`已释放「${item.dishName}」`)
      await fetchQueue(true)
    } else {
      toast.error(res?.message || '释放失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('释放失败')
  } finally {
    servingId.value = null
  }
}

/** 单个菜品出餐 */
const serveItem = async (item) => {
  if (!canServe.value) {
    toast.error('您没有出餐权限')
    return
  }
  servingId.value = item.itemId
  try {
    const res = await httpPost(`/order/manage/serve-item/${item.itemId}?storeId=${STORE_ID()}`)
    if (isSuccess(res)) {
      toast.success(`「${item.dishName}」已出餐`)
      await fetchQueue(true)
    } else {
      toast.error(res?.message || '出餐失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('出餐失败')
  } finally {
    servingId.value = null
  }
}

const qty = (v) => {
  const n = Number(v)
  return Number.isFinite(n) ? String(n) : '0'
}
</script>