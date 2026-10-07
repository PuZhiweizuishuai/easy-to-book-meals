<template>
  <div class="min-h-screen bg-base-200">
    <!-- 顶部导航（通用组件） -->
    <AppHeader title="我的订单" show-back>
      <template #actions>
        <router-link to="/" class="btn btn-ghost btn-sm">继续点餐</router-link>
      </template>
    </AppHeader>

    <main class="container mx-auto px-4 py-5 max-w-3xl">
      <!-- 状态页签 -->
      <div class="flex gap-2 overflow-x-auto pb-4">
        <button
          v-for="tab in statusTabs"
          :key="String(tab.value)"
          :class="['btn btn-sm shrink-0', activeStatus === tab.value ? 'btn-primary' : 'btn-ghost bg-base-100 border border-base-200']"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="loading" class="flex justify-center py-24">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>

      <div v-else-if="orders.length === 0" class="card bg-base-100 shadow-sm">
        <div class="card-body items-center py-20 text-center">
          <span class="text-5xl mb-3">📭</span>
          <p class="text-base-content/50">暂无订单</p>
          <router-link to="/" class="btn btn-primary btn-sm mt-3">去点餐</router-link>
        </div>
      </div>

      <div v-else class="space-y-4">
        <div v-for="order in orders" :key="order.id" class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body p-4 sm:p-5">
            <!-- 订单头 -->
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-mono text-sm text-base-content/70">{{ order.orderNo }}</span>
                <span :class="['badge badge-sm', orderTypeBadge(order.orderType)]">{{ orderTypeText(order.orderType) }}</span>
                <span v-if="order.tableNumber" class="badge badge-ghost badge-sm">{{ order.tableNumber }}号桌</span>
                <span v-if="order.contactPhone" class="badge badge-outline badge-sm font-mono">{{ order.contactPhone }}</span>
              </div>
              <span :class="['badge badge-sm', statusBadge(order.status)]">{{ statusText(order.status) }}</span>
            </div>

            <!-- 菜品摘要 -->
            <div class="space-y-1 py-2">
              <div v-for="item in (order.items || [])" :key="item.id" class="flex items-center gap-3" :class="{ 'opacity-50': item.status === 1 }">
                <div class="avatar">
                  <div class="w-10 h-10 rounded-lg bg-base-200">
                    <img v-if="item.topImage" :src="item.topImage" :alt="item.dishName" class="w-full h-full object-cover rounded-lg" />
                  </div>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="text-sm font-medium truncate flex items-center gap-1">
                    <span class="truncate">{{ item.dishName }}</span>
                    <span v-if="item.addedLater === 1" class="badge badge-warning badge-xs">加菜</span>
                    <span v-if="item.status === 1" class="badge badge-error badge-xs">已退菜</span>
                  </div>
                  <div class="text-xs text-base-content/50">{{ specNamesText(item.specNames) || '标准' }} · ¥{{ item.dealPrice }}</div>
                </div>
                <!-- <span v-if="item.status !== 1"
                      :class="['badge badge-xs sm:badge-sm shrink-0', item.served === 1 ? 'badge-success' : 'badge-ghost']">
                  {{ item.served === 1 ? '已出餐' : '未出餐' }}
                </span> -->
                <div class="text-sm text-base-content/60">×{{ item.quantity }}</div>
              </div>
            </div>

            <!-- 订单尾 -->
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pt-2 border-t border-base-200">
              <div class="text-xs text-base-content/50">{{ formatTime(order.createTime) }}</div>
              <div class="flex items-center gap-3">
                <div class="font-bold">合计：<span class="text-primary text-lg">¥{{ order.payAmount }}</span></div>
                <div class="flex gap-2">
                  <button
                    v-if="order.status < 4"
                    class="btn btn-info btn-sm"
                    :disabled="addItems.submitting"
                    @click="addItems.open(order)"
                  >加菜</button>
                  <button
                    v-if="order.status === 0"
                    class="btn btn-ghost btn-sm"
                    :disabled="acting"
                    @click="handleCancel(order)"
                  >取消订单</button>
                  <button class="btn btn-ghost btn-sm" @click="toggleDetail(order)">
                    {{ expandedId === order.id ? '收起' : '详情' }}
                  </button>
                  <button
                    v-if="order.status === 4 && !reviewedMap[order.id]"
                    class="btn btn-primary btn-sm"
                    @click="openReview(order)"
                  >评价</button>
                  <span v-if="order.status === 4 && reviewedMap[order.id]" class="text-xs text-base-content/40 self-center">已评价</span>
                </div>
              </div>
            </div>

            <!-- 展开详情 -->
            <div v-if="expandedId === order.id" class="bg-base-200 rounded-xl p-4 text-sm space-y-1">
              <p><span class="text-base-content/50">原价合计：</span>¥{{ order.totalAmount }}</p>
              <p v-if="Number(order.discountAmount) > 0"><span class="text-base-content/50">优惠：</span><span class="text-success">-¥{{ order.discountAmount }}</span></p>
              <p v-if="order.remark"><span class="text-base-content/50">备注：</span>{{ order.remark }}</p>
              <p v-if="order.cancelReason"><span class="text-base-content/50">取消原因：</span>{{ order.cancelReason }}</p>
              <p><span class="text-base-content/50">支付状态：</span>{{ order.payStatus === 1 ? '已支付' : '到店支付' }}</p>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <Pagination v-model="page" :total-pages="totalPages" container-class="pt-2" @change="changePage" />
      </div>
    </main>

    <!-- 评价弹窗 -->
    <dialog class="modal" :class="{ 'modal-open': reviewOrder }">
      <div class="modal-box max-w-md">
        <h3 class="font-bold text-lg mb-1">评价订单</h3>
        <p class="text-sm text-base-content/50 mb-4">{{ reviewOrder?.orderNo }}</p>

        <div class="form-control mb-3">
          <label class="label"><span class="label-text">评分</span></label>
          <div class="rating rating-lg">
            <input v-for="i in 5" :key="i" type="radio" name="review-rating" class="mask mask-star-2 bg-warning" :checked="reviewForm.rating === i" @change="reviewForm.rating = i" />
          </div>
        </div>

        <div class="form-control">
          <label class="label"><span class="label-text">评价内容</span></label>
          <textarea v-model="reviewForm.content" class="textarea textarea-bordered w-full" rows="4" maxlength="500" placeholder="菜品口味、服务体验如何？"></textarea>
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost" @click="closeReview" :disabled="reviewSubmitting">取消</button>
          <button class="btn btn-primary" @click="submitReview" :disabled="reviewSubmitting || !reviewForm.content.trim()">
            <span v-if="reviewSubmitting" class="loading loading-spinner loading-sm"></span>
            {{ reviewSubmitting ? '提交中...' : '提交评价' }}
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="closeReview"></div>
    </dialog>

    <!-- 加菜弹窗（菜品选择 + 规格选择，逻辑见 useAddItems） -->
    <AddItemsDialog :api="addItems" :show-order-no="false" empty-text="暂无菜品" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { httpGet, httpPost, isSuccess, parsePage } from '@/utils/fetch'
import toast from '@/utils/toast'
import AppHeader from '@/components/common/AppHeader.vue'
import Pagination from '@/components/common/Pagination.vue'
import AddItemsDialog from '@/components/order/AddItemsDialog.vue'
import { useAddItems } from '@/composables/useAddItems'
import { useConfirm } from '@/composables/useConfirm'
import { formatSpecNames as specNamesText } from '@/utils/format'
import {
  orderStatusText as statusText,
  orderStatusBadge as statusBadge,
  orderTypeText,
  orderTypeBadge
} from '@/utils/order'

const { confirm } = useConfirm()

const loading = ref(false)
const acting = ref(false)
const orders = ref([])
const page = ref(1)
const size = 10
const totalPages = ref(1)
const activeStatus = ref(null)
const expandedId = ref(null)
const reviewedMap = ref({})

const reviewOrder = ref(null)
const reviewForm = ref({ rating: 5, content: '' })
const reviewSubmitting = ref(false)

/** 加菜功能（顾客端：公开菜单 + 自助加菜接口） */
const addItems = useAddItems({
  title: '订单加菜',
  loadDishes: async (order) => {
    const res = await httpGet(`/store/${order.storeId}/dishes`)
    return isSuccess(res) ? (res.data || []) : []
  },
  submitItems: (order, items) => httpPost(`/order/add-items/${order.id}`, { items }),
  onSuccess: () => fetchOrders()
})

const statusTabs = [
  { value: null, label: '全部' },
  { value: 0, label: '待接单' },
  { value: 1, label: '已接单' },
  { value: 2, label: '制作中' },
  { value: 3, label: '已出餐' },
  { value: 4, label: '已完成' },
  { value: 5, label: '已取消' },
]

onMounted(() => {
  fetchOrders()
})

/** 加载我的订单 */
const fetchOrders = async () => {
  loading.value = true
  try {
    let url = `/order/my?page=${page.value}&size=${size}`
    if (activeStatus.value !== null) {
      url += `&status=${activeStatus.value}`
    }
    const res = await httpGet(url)
    if (isSuccess(res) && res.data) {
      const p = parsePage(res.data)
      orders.value = p.content
      totalPages.value = p.totalPages
      // 已完成订单检查是否已评价
      checkReviewed()
    }
  } catch (e) {
    console.error('加载订单失败:', e)
    toast.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

/** 批量检查已完成订单的评价状态 */
const checkReviewed = async () => {
  const completed = orders.value.filter(o => o.status === 4)
  for (const order of completed) {
    try {
      const res = await httpGet(`/review/order/${order.id}`)
      if (isSuccess(res) && res.data) {
        reviewedMap.value[order.id] = !!res.data.reviewed
      }
    } catch { /* 忽略 */ }
  }
}

const switchTab = (status) => {
  activeStatus.value = status
  page.value = 1
  fetchOrders()
}

const changePage = (p) => {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  fetchOrders()
}

/** 展开/收起详情 */
const toggleDetail = (order) => {
  expandedId.value = expandedId.value === order.id ? null : order.id
}

/** 取消订单 */
const handleCancel = async (order) => {
  const ok = await confirm({
    title: '取消订单',
    message: '确定取消该订单吗？',
    confirmText: '确认取消',
    tone: 'warning'
  })
  if (!ok) return
  acting.value = true
  try {
    const res = await httpPost(`/order/cancel/${order.id}`)
    if (isSuccess(res)) {
      toast.success('订单已取消')
      fetchOrders()
    } else {
      toast.error(res?.message || '取消失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('取消失败')
  } finally {
    acting.value = false
  }
}

/** 打开评价弹窗 */
const openReview = (order) => {
  reviewOrder.value = order
  reviewForm.value = { rating: 5, content: '' }
}

const closeReview = () => {
  reviewOrder.value = null
}

/** 提交评价 */
const submitReview = async () => {
  if (!reviewForm.value.content.trim()) {
    toast.error('请填写评价内容')
    return
  }
  reviewSubmitting.value = true
  try {
    const res = await httpPost('/review/add', {
      orderId: reviewOrder.value.id,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content.trim()
    })
    if (isSuccess(res)) {
      toast.success('感谢您的评价')
      reviewedMap.value[reviewOrder.value.id] = true
      closeReview()
    } else {
      toast.error(res?.message || '评价失败')
    }
  } catch (e) {
    toast.error('评价失败，请稍后重试')
  } finally {
    reviewSubmitting.value = false
  }
}

const formatTime = (ts) => {
  if (!ts) return '-'
  const d = new Date(Number(ts))
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>
