<template>
  <div>
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-base-content">订单管理</h2>
        <p class="text-sm text-base-content/60 mt-1">处理堂食扫码订单：接单 → 制作 → 出餐 → 完成</p>
      </div>
      <div class="flex gap-2">
        <router-link v-if="canManage" :to="{ name: 'orderCreate' }" class="btn btn-primary btn-sm">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          协助下单
        </router-link>
        <button class="btn btn-ghost btn-sm bg-base-100 border border-base-200" @click="fetchOrders()" :disabled="loading">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          刷新
        </button>
      </div>
    </div>

    <!-- 状态过滤页签 -->
    <div class="flex flex-wrap gap-2 mb-4">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        :class="['btn btn-sm', activeStatus === tab.value ? 'btn-primary' : 'btn-ghost bg-base-100 border border-base-200']"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.count" class="badge badge-xs" :class="activeStatus === tab.value ? '' : 'badge-primary'">{{ tab.count }}</span>
      </button>
    </div>

    <!-- 订单类型过滤 -->
    <div class="flex flex-wrap gap-2 mb-4 items-center">
      <span class="text-xs text-base-content/50 mr-1">订单类型：</span>
      <button
        v-for="t in orderTypeTabs"
        :key="t.value"
        :class="['btn btn-xs', activeOrderType === t.value ? 'btn-secondary' : 'btn-ghost bg-base-100 border border-base-200']"
        @click="switchOrderType(t.value)"
      >
        {{ t.label }}
      </button>
    </div>

    <!-- 搜索 -->
    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-4">
        <div class="flex flex-col sm:flex-row gap-3 mb-4">
          <label class="input input-bordered flex items-center gap-2 w-full sm:w-72">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" class="w-4 h-4 opacity-70">
              <path fill-rule="evenodd" d="M9.965 11.026a5 5 0 1 1 1.06-1.06l2.755 2.754a.75.75 0 1 1-1.06 1.06l-2.755-2.754ZM10.5 7a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0Z" clip-rule="evenodd" />
            </svg>
            <input v-model="searchKey" type="text" placeholder="搜索订单号..." class="grow" @keyup.enter="handleSearch" />
          </label>
          <button class="btn btn-primary sm:w-24" @click="handleSearch">搜索</button>
        </div>

        <div v-if="loading" class="flex justify-center items-center py-16">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>

        <div v-else-if="orders.length === 0" class="text-center py-16">
          <p class="text-base-content/40 text-lg">暂无订单</p>
        </div>

        <div v-else class="overflow-x-auto rounded-box border border-base-200">
          <table class="table table-zebra">
            <thead class="bg-base-200 text-base-content">
              <tr>
                <th>订单号</th>
                <th>下单人</th>
                <th>类型</th>
                <th>桌号</th>
                <th>菜品</th>
                <th>金额</th>
                <th>状态</th>
                <th>下单时间</th>
                <th class="text-right pr-4">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in orders" :key="order.id" class="hover">
                <td class="font-mono text-sm">{{ order.orderNo }}</td>
                <td>{{ order.userNickname || ('用户#' + order.userId) }}</td>
                <td><span :class="['badge badge-sm', orderTypeBadge(order.orderType)]">{{ orderTypeText(order.orderType) }}</span></td>
                <td>
                  <span v-if="order.tableNumber" class="badge badge-ghost">{{ order.tableNumber }}号桌</span>
                  <span v-else class="text-base-content/40">-</span>
                </td>
                <td class="max-w-56">
                  <div class="text-sm truncate flex items-center gap-1">
                    <span class="truncate">{{ itemsSummary(order) }}</span>
                    <span v-if="hasAddedItems(order)" class="badge badge-warning badge-xs shrink-0" title="包含加菜菜品">加菜</span>
                  </div>
                  <div class="text-xs text-base-content/50">共 {{ itemCount(order) }} 件</div>
                </td>
                <td class="font-semibold text-primary">¥{{ order.payAmount }}</td>
                <td><span :class="['badge badge-sm', statusBadge(effectiveStatus(order))]">{{ statusText(effectiveStatus(order)) }}</span></td>
                <td class="text-sm">{{ formatTime(order.createTime) }}</td>
                <td class="text-right pr-4">
                  <div class="join">
                    <button class="btn btn-ghost btn-xs join-item" @click="showDetail(order)">详情</button>
                    <button v-if="canManage && order.status < 4" class="btn btn-info btn-xs join-item" :disabled="addItems.submitting" @click="addItems.open(order)">加菜</button>
                    <template v-if="order.status === 0">
                      <button class="btn btn-success btn-xs join-item" :disabled="acting" @click="act(order, 'confirm')">接单</button>
                      <button class="btn btn-error btn-xs join-item" :disabled="acting" @click="act(order, 'reject')">拒单</button>
                    </template>
                    <button v-if="order.status === 1" class="btn btn-primary btn-xs join-item" :disabled="acting" @click="act(order, 'cooking')">开始制作</button>
                    <button v-if="order.status === 3 || order.status === 2" class="btn btn-success btn-xs join-item" :disabled="acting" @click="openCompleteDialog(order)">结算完成</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <Pagination v-model="page" :total-pages="totalPages" @change="changePage" />
      </div>
    </div>

    <!-- 订单详情弹窗（与会员消费记录详情共用 OrderDetailModal 组件） -->
    <OrderDetailModal
      :order="detailOrder"
      :editable="canManage && detailOrder !== null && detailOrder.status < 4"
      :busy="acting"
      @close="detailOrder = null"
      @serve="serveItem"
      @remove="removeItem"
      @adjust="openAdjustDialog"
      @add="addItems.open"
    />

    <!-- 拒单原因弹窗 -->
    <dialog class="modal" :class="{ 'modal-open': rejectOrder }">
      <div class="modal-box max-w-md">
        <h3 class="font-bold text-lg mb-4">拒单原因</h3>
        <textarea v-model="rejectReason" class="textarea textarea-bordered w-full" rows="3" placeholder="请输入拒单原因（如：食材不足）"></textarea>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="cancelReject">取消</button>
          <button class="btn btn-error" :disabled="acting" @click="confirmReject">确认拒单</button>
        </div>
      </div>
      <div class="modal-backdrop" @click="cancelReject"></div>
    </dialog>

    <!-- 加菜弹窗（菜品选择 + 规格选择，逻辑见 useAddItems） -->
    <AddItemsDialog :api="addItems" />

    <!-- 结算弹窗：会员查询 + 优惠券 + 手动减免 -->
    <dialog class="modal" :class="{ 'modal-open': completeOrder }">
      <div class="modal-box max-w-lg max-h-[90vh] overflow-y-auto">
        <template v-if="completeOrder">
          <h3 class="font-bold text-lg mb-3">结算确认</h3>

          <!-- 订单摘要 -->
          <div class="space-y-1 text-sm bg-base-200 rounded-lg p-3">
            <p><span class="text-base-content/50">订单号：</span><span class="font-mono">{{ completeOrder.orderNo }}</span></p>
            <p><span class="text-base-content/50">下单人：</span>{{ completeOrder.userNickname || ('用户#' + completeOrder.userId) }}</p>
            <p><span class="text-base-content/50">菜品合计：</span>¥{{ baseAmount.toFixed(2) }}</p>
          </div>

          <!-- 会员查询 -->
          <div class="form-control mt-4">
            <label class="label"><span class="label-text">顾客手机号（可选）</span></label>
            <div class="join w-full">
              <input v-model="completePhone" type="tel" maxlength="20" class="input input-bordered join-item flex-1"
                     placeholder="输入手机号查询会员并累积积分" @keyup.enter="queryMember" />
              <button class="btn btn-primary join-item" :disabled="memberLoading || !completePhone.trim()" @click="queryMember">
                <span v-if="memberLoading" class="loading loading-spinner loading-xs"></span>
                查询
              </button>
            </div>
            <label class="label">
              <span class="label-text-alt text-base-content/40">录入后将关联会员并按 1元=1积分 累积</span>
              <span v-if="completePoints > 0" class="label-text-alt text-success">本次约积 {{ completePoints }} 分</span>
            </label>
          </div>

          <!-- 会员信息 -->
          <div v-if="memberSearched" class="mt-1 rounded-lg border border-base-200 p-3 text-sm">
            <template v-if="memberInfo">
              <div class="flex items-center justify-between">
                <span class="font-medium">{{ memberInfo.nickname || '未设置昵称' }}</span>
                <span class="badge badge-primary badge-sm">{{ memberInfo.num ?? 0 }} 积分</span>
              </div>
              <div class="text-xs text-base-content/50 mt-1">
                手机号 {{ memberInfo.phone }} · 本店消费 {{ memberInfo.count ?? 0 }} 次
              </div>
            </template>
            <div v-else class="text-base-content/50">该手机号暂无会员记录，结算后将自动建档</div>
          </div>

          <!-- 优惠券 -->
          <div class="form-control mt-4">
            <label class="label">
              <span class="label-text">优惠券</span>
              <span v-if="memberCoupons.length > 0" class="label-text-alt text-base-content/40">
                会员名下 {{ memberCoupons.length }} 张可用
              </span>
            </label>

            <!-- 会员名下可用券 -->
            <div v-if="memberCoupons.length > 0" class="space-y-1 max-h-32 overflow-y-auto">
              <label v-for="uc in memberCoupons" :key="uc.id"
                     class="flex items-center justify-between bg-base-200 rounded-lg px-3 py-2 cursor-pointer hover:bg-base-300">
                <div class="flex items-center gap-2 min-w-0">
                  <input type="radio" name="settle-coupon" class="radio radio-primary radio-sm"
                         :checked="selectedCoupon && selectedCoupon.code === uc.code"
                         @change="selectMemberCoupon(uc)" />
                  <div class="min-w-0">
                    <div class="text-sm truncate">{{ uc.coupon?.name || '优惠券' }}</div>
                    <div class="text-xs text-base-content/50 font-mono truncate">{{ uc.code }}</div>
                  </div>
                </div>
                <span class="text-sm font-semibold text-success shrink-0">-¥{{ Number(uc.discountAmount || 0).toFixed(2) }}</span>
              </label>
            </div>

            <!-- 手动输入/扫码输入券码 -->
            <div class="join w-full mt-2">
              <input v-model="couponInput" type="text" class="input input-bordered input-sm join-item flex-1 font-mono text-xs"
                     placeholder="未关联的券可输入/扫码券码核销" @keyup.enter="checkCouponCode" />
              <button class="btn btn-sm btn-outline join-item" :disabled="couponChecking || !couponInput.trim()" @click="checkCouponCode">
                <span v-if="couponChecking" class="loading loading-spinner loading-xs"></span>
                校验
              </button>
              <button v-if="selectedCoupon" class="btn btn-sm btn-ghost join-item text-error" @click="clearCoupon">清除</button>
            </div>
            <label v-if="selectedCoupon" class="label">
              <span class="label-text-alt text-success">
                已选「{{ selectedCoupon.name }}」优惠 ¥{{ Number(selectedCoupon.discountAmount).toFixed(2) }}
              </span>
            </label>
          </div>

          <!-- 手动减免 -->
          <div class="form-control mt-3">
            <label class="label"><span class="label-text">手动减免金额（抹零/议价）</span></label>
            <input v-model.number="manualDiscount" type="number" min="0" step="0.01"
                   class="input input-bordered input-sm w-full" placeholder="0.00" />
          </div>

          <!-- 金额汇总 -->
          <div class="mt-4 space-y-1 text-sm bg-base-200 rounded-lg p-3">
            <div class="flex justify-between"><span class="text-base-content/50">菜品合计</span><span>¥{{ baseAmount.toFixed(2) }}</span></div>
            <div v-if="couponAmount > 0" class="flex justify-between text-success">
              <span>优惠券</span><span>-¥{{ couponAmount.toFixed(2) }}</span>
            </div>
            <div v-if="manualAmount > 0" class="flex justify-between text-success">
              <span>手动减免</span><span>-¥{{ manualAmount.toFixed(2) }}</span>
            </div>
            <div class="flex justify-between items-baseline border-t border-base-300 pt-1">
              <span class="text-base-content/50">应付</span>
              <span class="font-bold text-primary text-xl">¥{{ payableAmount.toFixed(2) }}</span>
            </div>
          </div>
        </template>

        <div class="modal-action">
          <button class="btn btn-ghost" @click="cancelComplete">取消</button>
          <button class="btn btn-success" :disabled="acting" @click="confirmComplete">
            <span v-if="acting" class="loading loading-spinner loading-xs"></span>
            确认结算完成
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="cancelComplete"></div>
    </dialog>

    <!-- 改量弹窗：调整菜品数量/重量（称重菜按实际重量重新计价） -->
    <dialog class="modal" :class="{ 'modal-open': adjustItem_ }">
      <div class="modal-box max-w-sm">
        <template v-if="adjustItem_">
          <h3 class="font-bold text-lg">调整数量</h3>
          <p class="text-sm text-base-content/60 mt-1">
            {{ adjustItem_.dishName }}
            <span v-if="adjustItem_.specNames">（{{ specNamesText(adjustItem_.specNames) }}）</span>
          </p>

          <div class="mt-4 space-y-3">
            <div class="flex items-center justify-between bg-base-200 rounded-xl px-4 py-3">
              <span class="text-sm text-base-content/60">单价</span>
              <span class="font-semibold text-primary">¥{{ adjustItem_.dealPrice }}</span>
            </div>
            <div class="flex items-center justify-between bg-base-200 rounded-xl px-4 py-3">
              <span class="text-sm text-base-content/60">数量 / 重量</span>
              <div class="flex items-center gap-2">
                <button class="btn btn-sm btn-circle" @click="stepAdjustQty(-1)">−</button>
                <input v-model="adjustQty" type="number" min="0.001" step="0.1"
                       class="input input-bordered input-sm w-24 text-center" />
                <button class="btn btn-sm btn-circle" @click="stepAdjustQty(1)">+</button>
              </div>
            </div>
            <div class="flex items-center justify-between px-1">
              <span class="text-sm text-base-content/60">调整后小计</span>
              <span class="font-bold text-primary text-lg">¥{{ adjustSubtotal.toFixed(2) }}</span>
            </div>
            <p class="text-xs text-base-content/40">支持小数（最多 3 位），如按斤称重可填 1.6</p>
          </div>
        </template>

        <div class="modal-action">
          <button class="btn btn-ghost" @click="cancelAdjust">取消</button>
          <button class="btn btn-primary" :disabled="acting || !adjustQty || Number(adjustQty) <= 0" @click="confirmAdjust">
            <span v-if="acting" class="loading loading-spinner loading-xs"></span>
            确认调整
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="cancelAdjust"></div>
    </dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { httpGet, httpPost, isSuccess, parsePage } from '@/utils/fetch'
import toast from '@/utils/toast'
import { useStoreContext } from '@/stores/storeContext'
import { useAuthStore } from '@/stores/auth'
import Pagination from '@/components/common/Pagination.vue'
import AddItemsDialog from '@/components/order/AddItemsDialog.vue'
import OrderDetailModal from '@/components/order/OrderDetailModal.vue'
import { useAddItems } from '@/composables/useAddItems'
import { useConfirm } from '@/composables/useConfirm'
import { useNewOrderAlert } from '@/composables/useNewOrderAlert'
import { formatSpecNames as specNamesText } from '@/utils/format'
import {
  orderStatusText as statusText,
  orderStatusBadge as statusBadge,
  orderTypeText,
  orderTypeBadge,
  orderItemCount as itemCount
} from '@/utils/order'

const storeContext = useStoreContext()
const authStore = useAuthStore()
const { confirm } = useConfirm()

const loading = ref(false)
const acting = ref(false)
const orders = ref([])
const page = ref(1)
const size = 10
const totalPages = ref(1)
const totalElements = ref(0)
const activeStatus = ref(null)
const activeOrderType = ref(null)
const searchKey = ref('')
const detailOrder = ref(null)
const rejectOrder = ref(null)
const rejectReason = ref('')

/** 加菜功能（菜品/规格加载、待加清单、提交均在 composable 内） */
const addItems = useAddItems({
  title: '为订单加菜',
  // 管理端：加载门店启用中的菜品
  loadDishes: async (order) => {
    const res = await httpGet(`/dishes/list?storefrontId=${order.storeId}&page=1&size=100`)
    return isSuccess(res) && res.data ? parsePage(res.data).content.filter(d => d.status === 1) : []
  },
  // 管理端：店员代客加菜
  submitItems: (order, items) =>
    httpPost(`/order/manage/add-items/${order.id}?storeId=${order.storeId}`, { items }),
  onSuccess: () => fetchOrders()
})

/** ============ 结算：会员 + 优惠券 + 手动减免 ============ */
const completeOrder = ref(null)
const completePhone = ref('')
/** 会员查询结果（null=查无会员；undefined=未查询） */
const memberInfo = ref(undefined)
const memberSearched = ref(false)
const memberLoading = ref(false)
/** 会员名下可用优惠券 */
const memberCoupons = ref([])
/** 已选中的券：{ code, name, discountAmount } */
const selectedCoupon = ref(null)
const couponInput = ref('')
const couponChecking = ref(false)
const manualDiscount = ref(0)

/** 结算基准金额：菜品折后合计（与后端一致，不含优惠券与手动减免） */
const baseAmount = computed(() => {
  const order = completeOrder.value
  if (!order) return 0
  return (order.items || [])
    .filter(i => i.status !== 1)
    .reduce((sum, i) => sum + Number(i.totalPrice || 0), 0)
})
const couponAmount = computed(() => Number(selectedCoupon.value?.discountAmount || 0))
const manualAmount = computed(() => {
  const v = Number(manualDiscount.value)
  return Number.isFinite(v) && v > 0 ? v : 0
})
const payableAmount = computed(() => Math.max(0, baseAmount.value - couponAmount.value - manualAmount.value))
/** 积分按实际应付金额累积：1 元 = 1 积分 */
const completePoints = computed(() => Math.floor(payableAmount.value))

/** 是否有订单管理权限（无则只读） */
const canManage = computed(() =>
  authStore.hasAnyPermission(['order:manage'])
)

/** 状态页签定义（与后端 OrderStatus 对应） */
const statusTabs = [
  { value: null, label: '全部' },
  { value: 0, label: '待接单' },
  { value: 1, label: '已接单' },
  { value: 2, label: '制作中' },
  { value: 3, label: '已出餐' },
  { value: 4, label: '已完成' },
  { value: 5, label: '已取消' },
]

/** 订单类型页签（与后端 OrderType 对应） */
const orderTypeTabs = [
  { value: null, label: '全部' },
  { value: 0, label: '堂食' },
  { value: 1, label: '外送' },
  { value: 2, label: '线上' },
]

onMounted(() => {
  fetchOrders()
})

/**
 * 新订单轮询提醒（开关与频率见「店铺设置 - 新订单提醒」）
 * 检测用固定的「最新 20 笔」查询，不受当前页签/搜索条件影响；
 * 发现新单后静默刷新当前列表。
 */
useNewOrderAlert({
  kind: 'order',
  title: '新订单提醒',
  fetcher: async () => {
    const res = await httpGet(
      `/order/manage/list?storeId=${storeContext.currentStoreId}&page=1&size=20`
    )
    if (!isSuccess(res) || !res.data) return null
    return parsePage(res.data).content.map(o => ({
      key: o.id,
      text: `${o.orderNo} ¥${o.payAmount} · ${orderTypeText(o.orderType)}${o.tableNumber ? ` · ${o.tableNumber}号桌` : ''}`,
    }))
  },
  onFresh: () => fetchOrders(true),
})

/**
 * 加载门店订单列表（silent=true 时不显示 loading 遮罩，供后台刷新使用）
 */
const fetchOrders = async (silent = false) => {
  if (!storeContext.currentStoreId) return
  if (!silent) loading.value = true
  try {
    let url = `/order/manage/list?storeId=${storeContext.currentStoreId}&page=${page.value}&size=${size}`
    if (activeStatus.value !== null) {
      url += `&status=${activeStatus.value}`
    }
    if (activeOrderType.value !== null) {
      url += `&orderType=${activeOrderType.value}`
    }
    if (searchKey.value) {
      url += `&key=${encodeURIComponent(searchKey.value)}`
    }
    const res = await httpGet(url)
    if (isSuccess(res) && res.data) {
      const p = parsePage(res.data)
      orders.value = p.content
      totalPages.value = p.totalPages
      totalElements.value = p.totalElements
    } else if (!silent) {
      toast.error(res?.message || '获取订单失败')
    }
  } catch (e) {
    console.error('获取订单失败:', e)
    if (!silent) toast.error('获取订单失败')
  } finally {
    if (!silent) loading.value = false
  }
}

/** 切换状态页签 */
const switchTab = (status) => {
  activeStatus.value = status
  page.value = 1
  fetchOrders()
}

/** 切换订单类型 */
const switchOrderType = (type) => {
  activeOrderType.value = type
  page.value = 1
  fetchOrders()
}

/** 搜索 */
const handleSearch = () => {
  page.value = 1
  fetchOrders()
}

/** 翻页 */
const changePage = (p) => {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  fetchOrders()
}

/** 查看详情 */
const showDetail = (order) => {
  detailOrder.value = order
}

/**
 * 订单操作：接单/制作/出餐/完成
 */
const act = async (order, action) => {
  if (!canManage.value) {
    toast.error('您没有订单操作权限')
    return
  }
  if (action === 'reject') {
    rejectOrder.value = order
    rejectReason.value = ''
    return
  }
  acting.value = true
  try {
    const res = await httpPost(`/order/manage/${action}/${order.id}?storeId=${order.storeId}`)
    if (isSuccess(res)) {
      const tips = { confirm: '已接单', cooking: '开始制作', complete: '订单已完成' }
      toast.success(tips[action] || '操作成功')
      fetchOrders()
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    toast.error('操作失败')
  } finally {
    acting.value = false
  }
}

/** 拒单 */
const cancelReject = () => {
  rejectOrder.value = null
  rejectReason.value = ''
}

const confirmReject = async () => {
  if (!rejectOrder.value) return
  acting.value = true
  try {
    const res = await httpPost(`/order/manage/reject/${rejectOrder.value.id}?storeId=${rejectOrder.value.storeId}&reason=${encodeURIComponent(rejectReason.value || '店家拒单')}`)
    if (isSuccess(res)) {
      toast.success('已拒单')
      cancelReject()
      fetchOrders()
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    toast.error('操作失败')
  } finally {
    acting.value = false
  }
}

/** 菜品级出餐：逐个菜品标记已出餐，全部出餐后后端自动推进订单到「已出餐」 */
const serveItem = async (item) => {
  if (!detailOrder.value) return
  if (!canManage.value) {
    toast.error('您没有订单操作权限')
    return
  }
  acting.value = true
  try {
    const res = await httpPost(`/order/manage/serve-item/${item.id}?storeId=${detailOrder.value.storeId}`)
    if (isSuccess(res)) {
      toast.success(`「${item.dishName}」已出餐`)
      detailOrder.value = res.data
      fetchOrders()
    } else {
      toast.error(res?.message || '出餐失败')
    }
  } catch (e) {
    toast.error('出餐失败')
  } finally {
    acting.value = false
  }
}

/** 退菜（移除未出餐菜品） */
const removeItem = async (item) => {
  if (!detailOrder.value) return
  const ok = await confirm({
    title: '确认退菜',
    message: `确认退菜「${item.dishName}×${item.quantity}」？退菜后恢复库存并重算金额。`,
    confirmText: '确认退菜',
    tone: 'error'
  })
  if (!ok) return
  acting.value = true
  try {
    const res = await httpPost(`/order/manage/remove-item/${item.id}?storeId=${detailOrder.value.storeId}`)
    if (isSuccess(res)) {
      toast.success('已退菜')
      detailOrder.value = res.data
      fetchOrders()
    } else {
      toast.error(res?.message || '退菜失败')
    }
  } catch (e) {
    toast.error('退菜失败')
  } finally {
    acting.value = false
  }
}

/** 订单是否包含加菜菜品 */
const hasAddedItems = (order) => {
  return (order.items || []).some(i => i.addedLater === 1 && i.status === 0)
}

/** 结算弹窗：完成订单（需所有菜品已出餐） */
const openCompleteDialog = (order) => {
  if (!canManage.value) {
    toast.error('您没有订单操作权限')
    return
  }
  const unserved = (order.items || []).filter(i => i.status !== 1 && i.served !== 1)
  if (unserved.length > 0) {
    toast.warning(`「${unserved.map(i => i.dishName).join('、')}」尚未出餐，全部出餐后才能结算`)
    return
  }
  completeOrder.value = order
  completePhone.value = order.contactPhone || ''
  memberInfo.value = undefined
  memberSearched.value = false
  memberCoupons.value = []
  selectedCoupon.value = null
  couponInput.value = ''
  manualDiscount.value = 0
}

const cancelComplete = () => {
  completeOrder.value = null
  completePhone.value = ''
  memberInfo.value = undefined
  memberSearched.value = false
  memberCoupons.value = []
  selectedCoupon.value = null
  couponInput.value = ''
  manualDiscount.value = 0
}

/**
 * 查询手机号对应的会员信息（积分、消费次数）与其名下可用优惠券
 */
const queryMember = async () => {
  const phone = completePhone.value.trim()
  if (!phone || !completeOrder.value) return
  memberLoading.value = true
  memberSearched.value = true
  try {
    const [memberRes, couponRes] = await Promise.all([
      httpGet(`/manage/membership/by-phone?storeId=${completeOrder.value.storeId}&phone=${encodeURIComponent(phone)}`),
      httpGet(`/manage/coupon/member-coupons?storeId=${completeOrder.value.storeId}&phone=${encodeURIComponent(phone)}&amount=${baseAmount.value}`)
    ])
    memberInfo.value = isSuccess(memberRes) ? (memberRes.data || null) : null
    memberCoupons.value = (isSuccess(couponRes) && couponRes.data?.coupons) || []
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('查询会员信息失败')
  } finally {
    memberLoading.value = false
  }
}

/** 选择会员名下的优惠券 */
const selectMemberCoupon = (uc) => {
  selectedCoupon.value = {
    code: uc.code,
    name: uc.coupon?.name || '优惠券',
    discountAmount: Number(uc.discountAmount || 0)
  }
  couponInput.value = uc.code
}

/**
 * 校验手动输入的券码（未关联到用户账户的券也可核销）
 */
const checkCouponCode = async () => {
  const code = couponInput.value.trim()
  if (!code || !completeOrder.value) return
  couponChecking.value = true
  try {
    const res = await httpGet(
      `/coupon/check?storeId=${completeOrder.value.storeId}` +
      `&code=${encodeURIComponent(code)}&amount=${baseAmount.value}`
    )
    if (isSuccess(res) && res.data) {
      selectedCoupon.value = {
        code: res.data.code,
        name: res.data.name,
        discountAmount: Number(res.data.discountAmount || 0)
      }
      toast.success(`优惠券可用，已优惠 ¥${Number(res.data.discountAmount).toFixed(2)}`)
    } else {
      toast.error(res?.message || '优惠券不可用')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('校验优惠券失败')
  } finally {
    couponChecking.value = false
  }
}

const clearCoupon = () => {
  selectedCoupon.value = null
  couponInput.value = ''
}

const confirmComplete = async () => {
  if (!completeOrder.value) return
  if (manualAmount.value > baseAmount.value - couponAmount.value) {
    toast.warning('手动减免金额不能超过订单应付金额')
    return
  }
  acting.value = true
  try {
    const phone = completePhone.value.trim()
    const pts = (phone && completePoints.value > 0) ? completePoints.value : 0
    const params = new URLSearchParams({ storeId: String(completeOrder.value.storeId) })
    if (phone) params.append('phone', phone)
    if (selectedCoupon.value) params.append('couponCode', selectedCoupon.value.code)
    if (manualAmount.value > 0) params.append('manualDiscount', manualAmount.value.toFixed(2))
    const res = await httpPost(`/order/manage/complete/${completeOrder.value.id}?${params.toString()}`)
    if (isSuccess(res)) {
      const tip = phone && pts > 0 ? `订单已结算完成，已累积 ${pts} 会员积分` : '订单已结算完成'
      toast.success(tip)
      cancelComplete()
      fetchOrders()
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('操作失败')
  } finally {
    acting.value = false
  }
}

/** ============ 改量：由原生 prompt 改为 DaisyUI modal ============ */
const adjustItem_ = ref(null)
const adjustQty = ref(1)

const adjustSubtotal = computed(() => {
  if (!adjustItem_.value) return 0
  const qty = Number(adjustQty.value)
  if (!Number.isFinite(qty) || qty <= 0) return 0
  return qty * Number(adjustItem_.value.dealPrice || 0)
})

/** 打开改量弹窗 */
const openAdjustDialog = (item) => {
  if (!canManage.value) {
    toast.error('您没有订单操作权限')
    return
  }
  adjustItem_.value = item
  adjustQty.value = Number(item.quantity)
}

const cancelAdjust = () => {
  adjustItem_.value = null
  adjustQty.value = 1
}

/** 快捷步进（称重菜按 0.1 步进，计件菜按 1 步进） */
const stepAdjustQty = (delta) => {
  const current = Number(adjustQty.value)
  if (!Number.isFinite(current)) {
    adjustQty.value = 1
    return
  }
  const step = Number.isInteger(current) ? 1 : 0.1
  const next = Math.max(0.001, Number((current + delta * step).toFixed(3)))
  adjustQty.value = next
}

/** 提交改量：按成交单价 × 新数量重算小计（后端联动重算订单总额与库存） */
const confirmAdjust = async () => {
  if (!adjustItem_.value || !detailOrder.value) return
  const qty = Number(adjustQty.value)
  if (!Number.isFinite(qty) || qty <= 0) {
    toast.warning('请输入大于 0 的数量')
    return
  }
  if (qty === Number(adjustItem_.value.quantity)) {
    cancelAdjust()
    return
  }
  acting.value = true
  try {
    const res = await httpPost(
      `/order/manage/adjust-item/${adjustItem_.value.id}?storeId=${detailOrder.value.storeId}&quantity=${qty}`
    )
    if (isSuccess(res)) {
      toast.success('数量已调整')
      detailOrder.value = res.data
      cancelAdjust()
      fetchOrders()
    } else {
      toast.error(res?.message || '调整失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('调整失败')
  } finally {
    acting.value = false
  }
}

/** 菜品摘要（列表展示） */
const itemsSummary = (order) => {
  const items = order.items || []
  if (items.length === 0) return '-'
  const names = items.map(i => i.dishName).join('、')
  return names.length > 30 ? names.slice(0, 30) + '...' : names
}

/** 订单内所有正常菜品是否均已出餐（退菜明细视为已处理） */
const allItemsServed = (order) => {
  const items = order.items || []
  return items.length > 0 && items.every(i => i.status === 1 || i.served === 1)
}

/**
 * 有效订单状态：制作中/已出餐阶段以菜品出餐情况为准，
 * 全部出餐才显示「已出餐」，否则显示「制作中」（如已出餐后又加菜）
 */
const effectiveStatus = (order) => {
  if (order.status !== 2 && order.status !== 3) return order.status
  return allItemsServed(order) ? 3 : 2
}

/** 时间格式化 */
const formatTime = (ts) => {
  if (!ts) return '-'
  const d = new Date(Number(ts))
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>
