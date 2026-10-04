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
        <button class="btn btn-ghost btn-sm bg-base-100 border border-base-200" @click="fetchOrders" :disabled="loading">
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

    <!-- 订单详情弹窗 -->
    <dialog class="modal" :class="{ 'modal-open': detailOrder }">
      <div class="modal-box max-w-2xl">
        <template v-if="detailOrder">
          <h3 class="font-bold text-lg">订单详情</h3>
          <div class="mt-4 space-y-1 text-sm">
            <p><span class="text-base-content/50">订单号：</span><span class="font-mono">{{ detailOrder.orderNo }}</span></p>
            <p><span class="text-base-content/50">订单类型：</span><span :class="['badge badge-sm', orderTypeBadge(detailOrder.orderType)]">{{ orderTypeText(detailOrder.orderType) }}</span></p>
            <p><span class="text-base-content/50">状态：</span><span :class="['badge badge-sm', statusBadge(effectiveStatus(detailOrder))]">{{ statusText(effectiveStatus(detailOrder)) }}</span></p>
            <p><span class="text-base-content/50">下单人：</span>{{ detailOrder.userNickname || ('用户#' + detailOrder.userId) }}</p>
            <p v-if="detailOrder.contactPhone"><span class="text-base-content/50">联系电话：</span><span class="font-mono">{{ detailOrder.contactPhone }}</span></p>
            <p v-if="detailOrder.tableNumber"><span class="text-base-content/50">桌号：</span>{{ detailOrder.tableNumber }}号桌</p>
            <p><span class="text-base-content/50">下单时间：</span>{{ formatTime(detailOrder.createTime) }}</p>
            <p v-if="detailOrder.remark"><span class="text-base-content/50">备注：</span>{{ detailOrder.remark }}</p>
            <p v-if="detailOrder.cancelReason"><span class="text-base-content/50">取消原因：</span>{{ detailOrder.cancelReason }}</p>
          </div>

          <div class="divider my-3">菜品明细</div>
          <div class="overflow-x-auto">
            <table class="table table-sm">
              <thead>
                <tr>
                  <th>菜品</th>
                  <th>规格</th>
                  <th>单价</th>
                  <th>数量</th>
                  <th>小计</th>
                  <th>出餐</th>
                  <th v-if="canManage && detailOrder.status < 4" class="text-right pr-2">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in (detailOrder.items || [])" :key="item.id" :class="{ 'opacity-50': item.status === 1 }">
                  <td>
                    <div class="flex items-center gap-2">
                      <div class="avatar">
                        <div class="w-9 h-9 rounded bg-base-200">
                          <img v-if="item.topImage" :src="item.topImage" :alt="item.dishName" class="w-full h-full object-cover rounded" />
                        </div>
                      </div>
                      <div class="flex flex-col">
                        <span class="flex items-center gap-1">
                          {{ item.dishName }}
                          <span v-if="item.addedLater === 1" class="badge badge-warning badge-xs" title="加菜追加">加</span>
                          <span v-if="item.status === 1" class="badge badge-error badge-xs">已退菜</span>
                        </span>
                      </div>
                    </div>
                  </td>
                  <td class="text-sm text-base-content/60">{{ specNamesText(item.specNames) || '标准' }}</td>
                  <td>¥{{ item.dealPrice }}</td>
                  <td>×{{ formatQty(item.quantity) }}</td>
                  <td class="font-medium">¥{{ item.totalPrice }}</td>
                  <td>
                    <span v-if="item.status === 1" class="text-base-content/40 text-xs">-</span>
                    <span v-else-if="item.served === 1" class="badge badge-success badge-sm">已出餐</span>
                    <button v-else-if="canManage && detailOrder.status < 4"
                            class="btn btn-warning btn-xs" :disabled="acting" @click="serveItem(item)">出餐</button>
                    <span v-else class="badge badge-ghost badge-sm">未出餐</span>
                  </td>
                  <td v-if="canManage && detailOrder.status < 4" class="text-right pr-2">
                    <div v-if="item.status !== 1" class="join">
                      <button class="btn btn-ghost btn-xs join-item" :disabled="acting" @click="adjustItem(item)">改量</button>
                      <button class="btn btn-error btn-xs join-item" :disabled="acting" @click="removeItem(item)">退菜</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="mt-3 text-right space-y-1 text-sm">
            <p><span class="text-base-content/50">原价合计：</span>¥{{ detailOrder.totalAmount }}</p>
            <p v-if="Number(detailOrder.discountAmount) > 0">
              <span class="text-base-content/50">优惠：</span><span class="text-success">-¥{{ detailOrder.discountAmount }}</span>
            </p>
            <p class="text-lg"><span class="text-base-content/50 font-normal">应付：</span><span class="font-bold text-primary">¥{{ detailOrder.payAmount }}</span></p>
          </div>

          <div class="modal-action">
            <button class="btn btn-ghost" @click="detailOrder = null">关闭</button>
          </div>
        </template>
      </div>
      <div class="modal-backdrop" @click="detailOrder = null"></div>
    </dialog>

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

    <!-- 结算弹窗（完成订单 + 可录入手机号关联会员积分） -->
    <dialog class="modal" :class="{ 'modal-open': completeOrder }">
      <div class="modal-box max-w-md">
        <template v-if="completeOrder">
          <h3 class="font-bold text-lg mb-3">结算确认</h3>
          <div class="space-y-2 text-sm bg-base-200 rounded-lg p-3">
            <p><span class="text-base-content/50">订单号：</span><span class="font-mono">{{ completeOrder.orderNo }}</span></p>
            <p><span class="text-base-content/50">下单人：</span>{{ completeOrder.userNickname || ('用户#' + completeOrder.userId) }}</p>
            <p><span class="text-base-content/50">应付金额：</span><span class="font-bold text-primary text-lg">¥{{ completeOrder.payAmount }}</span></p>
          </div>
          <div class="form-control mt-4">
            <label class="label"><span class="label-text">顾客手机号（可选）</span></label>
            <input v-model="completePhone" type="tel" maxlength="20" class="input input-bordered w-full" placeholder="输入手机号可累积会员积分">
            <label class="label">
              <span class="label-text-alt text-base-content/40">录入后将关联会员并按 1元=1积分 累积</span>
              <span v-if="completePhone && completePhone.trim()" class="label-text-alt text-success">本次约积 {{ completePoints }} 分</span>
            </label>
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
import { useAddItems } from '@/composables/useAddItems'
import { useConfirm } from '@/composables/useConfirm'
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

/** 结算相关状态（完成订单 + 可录入手机号关联会员积分） */
const completeOrder = ref(null)
const completePhone = ref('')
const completePoints = computed(() => {
  if (!completeOrder.value) return 0
  const amt = Number(completeOrder.value.payAmount)
  return isNaN(amt) ? 0 : Math.floor(amt)
})

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
 * 加载门店订单列表
 */
const fetchOrders = async () => {
  if (!storeContext.currentStoreId) return
  loading.value = true
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
    } else {
      toast.error(res?.message || '获取订单失败')
    }
  } catch (e) {
    console.error('获取订单失败:', e)
    toast.error('获取订单失败')
  } finally {
    loading.value = false
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

/** 数量/重量展示格式化：去掉多余的小数尾零（1.600 -> 1.6，2.000 -> 2） */
const formatQty = (q) => {
  const n = Number(q)
  return Number.isFinite(n) ? String(n) : '0'
}

/** 调整菜品数量/重量（称重菜按实际重量重新计价） */
const adjustItem = async (item) => {
  if (!detailOrder.value) return
  const current = Number(item.quantity)
  const input = prompt(
    `调整「${item.dishName}」的数量/重量（单价 ¥${item.dealPrice}，支持小数，如按斤称重填 1.6）`,
    Number.isFinite(current) ? current : ''
  )
  if (input === null) return
  const qty = Number(String(input).trim())
  if (!Number.isFinite(qty) || qty <= 0) {
    toast.warning('请输入大于 0 的数量')
    return
  }
  if (qty === current) return
  acting.value = true
  try {
    const res = await httpPost(
      `/order/manage/adjust-item/${item.id}?storeId=${detailOrder.value.storeId}&quantity=${qty}`
    )
    if (isSuccess(res)) {
      toast.success('数量已调整')
      detailOrder.value = res.data
      fetchOrders()
    } else {
      toast.error(res?.message || '调整失败')
    }
  } catch (e) {
    toast.error('调整失败')
  } finally {
    acting.value = false
  }
}

/** 订单是否包含加菜菜品 */
const hasAddedItems = (order) => {
  return (order.items || []).some(i => i.addedLater === 1 && i.status === 0)
}

/** 结算弹窗：完成订单，可录入顾客手机号关联会员积分（需所有菜品已出餐） */
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
}

const cancelComplete = () => {
  completeOrder.value = null
  completePhone.value = ''
}

const confirmComplete = async () => {
  if (!completeOrder.value) return
  acting.value = true
  try {
    const phone = completePhone.value.trim()
    const pts = (phone && completePoints.value > 0) ? completePoints.value : 0
    let url = `/order/manage/complete/${completeOrder.value.id}?storeId=${completeOrder.value.storeId}`
    if (phone) {
      url += `&phone=${encodeURIComponent(phone)}`
    }
    const res = await httpPost(url)
    if (isSuccess(res)) {
      const tip = phone && pts > 0 ? `订单已结算完成，已累积 ${pts} 会员积分` : '订单已结算完成'
      toast.success(tip)
      cancelComplete()
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
