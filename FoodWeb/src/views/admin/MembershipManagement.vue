<template>
  <div>
    <!-- 页头 -->
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-base-content">会员管理</h2>
        <p class="text-sm text-base-content/60 mt-1" v-if="storeContext.currentStore">
          {{ storeContext.currentStore.storeName }} · 会员积分与消费记录
        </p>
      </div>
      <button class="btn btn-ghost btn-sm bg-base-100 border border-base-200" @click="fetchMembers(page)" :disabled="loading">
        刷新
      </button>
    </div>

    <!-- 概览统计卡片 -->
    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
      <div class="stat bg-base-100 shadow-sm rounded-lg border border-base-200">
        <div class="stat-figure text-primary">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
        </div>
        <div class="stat-title">会员总数</div>
        <div class="stat-value text-primary">{{ totalElements }}</div>
        <div class="stat-desc">累计注册会员</div>
      </div>
      <div class="stat bg-base-100 shadow-sm rounded-lg border border-base-200">
        <div class="stat-figure text-secondary">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <div class="stat-title">积分发放总量</div>
        <div class="stat-value text-secondary">{{ totalPointsIssued }}</div>
        <div class="stat-desc">当前会员积分合计</div>
      </div>
      <div class="stat bg-base-100 shadow-sm rounded-lg border border-base-200">
        <div class="stat-figure text-accent">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
        </div>
        <div class="stat-title">活跃会员</div>
        <div class="stat-value text-accent">{{ activeCount }}</div>
        <div class="stat-desc">有消费记录的会员</div>
      </div>
    </div>

    <!-- 会员列表 -->
    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-4 sm:p-6">
        <!-- 搜索栏 -->
        <div class="flex flex-col sm:flex-row justify-between gap-4 mb-4">
          <label class="input input-bordered flex items-center gap-2 w-full sm:w-72">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" class="w-5 h-4 opacity-70">
              <path fill-rule="evenodd" d="M9.965 11.026a5 5 0 1 1 1.06-1.06l2.755 2.754a.75.75 0 1 1-1.06 1.06l-2.755-2.754ZM10.5 7a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0Z" clip-rule="evenodd" />
            </svg>
            <input type="text" v-model.trim="searchKey" placeholder="按手机号搜索会员..." class="grow" @keyup.enter="fetchMembers(1)" />
          </label>
          <button v-if="searchKey" class="btn btn-ghost btn-sm" @click="searchKey = ''; fetchMembers(1)">清除筛选</button>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="flex justify-center items-center py-20">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="members.length === 0" class="text-center py-16">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 mx-auto mb-4 text-base-content/20" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
          </svg>
          <p class="text-base-content/50 text-lg">暂无会员数据</p>
          <p class="text-base-content/30 text-sm mt-1">顾客完成订单并填写手机号后将自动成为会员</p>
        </div>

        <!-- 移动端卡片布局 -->
        <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-3 md:hidden">
          <div v-for="m in members" :key="m.id"
            class="bg-base-200/50 border border-base-200 rounded-2xl p-4 cursor-pointer transition-all hover:shadow-md"
            @click="openDetail(m)">
            <div class="flex items-center justify-between mb-2">
              <span class="font-mono text-sm font-medium">{{ m.phone }}</span>
              <span class="badge badge-primary badge-sm">{{ m.num ?? 0 }} 积分</span>
            </div>
            <div class="text-xs text-base-content/60 flex items-center gap-3">
              <span>{{ m.nickname || '未设置昵称' }}</span>
              <span>·</span>
              <span>消费 {{ m.count ?? 0 }} 次</span>
            </div>
          </div>
        </div>

        <!-- 桌面端表格 -->
        <div v-if="!loading && members.length > 0" class="hidden md:block overflow-x-auto rounded-box border border-base-200">
          <table class="table table-zebra w-full">
            <thead class="bg-base-200 text-base-content">
              <tr>
                <th>手机号</th>
                <th>昵称</th>
                <th>积分</th>
                <th>用餐次数</th>
                <th>备注</th>
                <th>更新时间</th>
                <th class="text-right pr-6">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="m in members" :key="m.id" class="hover">
                <td class="font-mono">{{ m.phone }}</td>
                <td>{{ m.nickname || '—' }}</td>
                <td>
                  <span class="badge badge-primary badge-sm font-mono">{{ m.num ?? 0 }}</span>
                </td>
                <td>{{ m.count ?? 0 }}</td>
                <td class="max-w-32 truncate text-sm text-base-content/60">{{ m.remarks || '—' }}</td>
                <td class="text-sm text-base-content/60">{{ formatDate(m.updateTime) }}</td>
                <td class="text-right pr-6">
                  <button class="btn btn-ghost btn-xs" @click="openDetail(m)">详情</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <Pagination v-if="!loading" v-model="page" :total-pages="totalPages" @change="fetchMembers" />
      </div>
    </div>

    <!-- 会员详情弹窗 -->
    <dialog class="modal" :class="{ 'modal-open': detailMember }">
      <div class="modal-box max-w-3xl max-h-[90vh] overflow-y-auto">
        <template v-if="detailMember">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-bold text-lg">会员详情</h3>
            <button class="btn btn-ghost btn-xs btn-circle" @click="detailMember = null">✕</button>
          </div>

          <!-- 会员概览 -->
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
            <div class="stat bg-base-200/70 rounded-lg">
              <div class="stat-figure text-primary">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div class="stat-title text-xs">积分余额</div>
              <div class="stat-value text-primary text-xl">{{ detailMember.num ?? 0 }}</div>
            </div>
            <div class="stat bg-base-200/70 rounded-lg">
              <div class="stat-figure text-secondary">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3v18h18M9 17V9m4 8V5m4 12v-8" />
                </svg>
              </div>
              <div class="stat-title text-xs">用餐次数</div>
              <div class="stat-value text-secondary text-xl">{{ detailMember.count ?? 0 }}</div>
            </div>
            <div class="stat bg-base-200/70 rounded-lg col-span-2">
              <div class="stat-figure text-base-content/40">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.289a12.115 12.115 0 005.516 5.516l1.289-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                </svg>
              </div>
              <div class="stat-title text-xs">手机号</div>
              <div class="stat-value text-base-content text-lg font-mono">{{ detailMember.phone }}</div>
              <div class="stat-desc text-xs">{{ detailMember.nickname || '未设置昵称' }}</div>
            </div>
          </div>

          <!-- Tab 切换 -->
          <div role="tablist" class="tabs tabs-boxed mb-4">
            <a role="tab" class="tab" :class="{ 'tab-active': detailTab === 'transactions' }" @click="switchTab('transactions')">
              积分流水
              <span v-if="!txLoading" class="badge badge-xs badge-ghost ml-1">{{ txTotalElements }}</span>
            </a>
            <a role="tab" class="tab" :class="{ 'tab-active': detailTab === 'orders' }" @click="switchTab('orders')">
              消费记录
              <span v-if="!orderLoading" class="badge badge-xs badge-ghost ml-1">{{ orderTotalElements }}</span>
            </a>
          </div>

          <!-- 积分流水 -->
          <div v-show="detailTab === 'transactions'">
            <div v-if="txLoading" class="flex justify-center py-10">
              <span class="loading loading-spinner text-primary"></span>
            </div>
            <div v-else-if="transactions.length === 0" class="text-center py-10 text-base-content/40">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto mb-2 text-base-content/20" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <p class="text-sm">暂无积分流水记录</p>
            </div>
            <div v-else class="overflow-x-auto rounded-box border border-base-200">
              <table class="table table-sm table-zebra">
                <thead class="bg-base-200 text-base-content">
                  <tr>
                    <th>时间</th>
                    <th>类型</th>
                    <th>积分变动</th>
                    <th>订单号</th>
                    <th>备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="tx in transactions" :key="tx.id" class="hover">
                    <td class="text-sm text-base-content/60 whitespace-nowrap">{{ formatDate(tx.createTime) }}</td>
                    <td>
                      <span :class="['badge badge-sm', txTypeBadge(tx.type)]">{{ txTypeText(tx.type) }}</span>
                    </td>
                    <td>
                      <span :class="tx.points >= 0 ? 'text-success font-bold' : 'text-error font-bold'">
                        {{ tx.points >= 0 ? '+' : '' }}{{ tx.points }}
                      </span>
                    </td>
                    <td class="font-mono text-xs">{{ tx.orderNo || '—' }}</td>
                    <td class="text-sm text-base-content/60 max-w-32 truncate">{{ tx.remark || '—' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <Pagination v-model="txPage" :total-pages="txTotalPages" size="xs"
                        container-class="mt-3" @change="fetchTransactions" />
          </div>

          <!-- 消费记录 -->
          <div v-show="detailTab === 'orders'">
            <div v-if="orderLoading" class="flex justify-center py-10">
              <span class="loading loading-spinner text-primary"></span>
            </div>
            <div v-else-if="orders.length === 0" class="text-center py-10 text-base-content/40">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto mb-2 text-base-content/20" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
              <p class="text-sm">暂无消费记录</p>
            </div>
            <div v-else class="overflow-x-auto rounded-box border border-base-200">
              <table class="table table-sm table-zebra">
                <thead class="bg-base-200 text-base-content">
                  <tr>
                    <th>订单号</th>
                    <th>金额</th>
                    <th>状态</th>
                    <th>时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="o in orders" :key="o.id" class="hover">
                    <td class="font-mono text-xs">{{ o.orderNo }}</td>
                    <td class="font-semibold text-primary">¥{{ o.payAmount }}</td>
                    <td><span :class="['badge badge-sm', orderStatusBadge(o.status)]">{{ orderStatusText(o.status) }}</span></td>
                    <td class="text-sm text-base-content/60 whitespace-nowrap">{{ formatDate(o.createTime) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <Pagination v-model="orderPage" :total-pages="orderTotalPages" size="xs"
                        container-class="mt-3" @change="fetchOrders" />
          </div>

          <!-- 操作区 -->
          <div class="modal-action mt-6">
            <button class="btn btn-outline btn-sm" @click="openAdjustDialog">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              调整积分
            </button>
            
          </div>
        </template>
      </div>
      <div class="modal-backdrop" @click="detailMember = null"></div>
    </dialog>

    <!-- 手动调整积分弹窗 -->
    <dialog class="modal" :class="{ 'modal-open': showAdjustDialog }">
      <div class="modal-box max-w-md">
        <h3 class="font-bold text-lg mb-4">手动调整积分</h3>
        <div class="space-y-4">
          <div class="bg-base-200 rounded-xl p-4 space-y-1">
            <div class="flex justify-between items-center text-sm">
              <span class="text-base-content/60">会员手机号</span>
              <span class="font-mono font-medium">{{ detailMember?.phone }}</span>
            </div>
            <div class="flex justify-between items-center text-sm">
              <span class="text-base-content/60">当前积分</span>
              <span class="font-bold text-primary text-base">{{ detailMember?.num ?? 0 }}</span>
            </div>
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">积分变动值</span></label>
            <div class="join">
              <button type="button" class="join-item btn btn-outline" @click="adjustForm.points = -Math.abs(adjustForm.points || 0)">−</button>
              <input v-model.number="adjustForm.points" type="number" class="input input-bordered join-item flex-1" placeholder="正数增加，负数扣减" />
              <button type="button" class="join-item btn btn-outline" @click="adjustForm.points = Math.abs(adjustForm.points || 0)">+</button>
            </div>
            <label class="label">
              <span class="label-text-alt text-base-content/50">调整后积分为：{{ predictedPoints }}</span>
            </label>
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">备注（可选）</span></label>
            <input v-model.trim="adjustForm.remark" type="text" class="input input-bordered w-full" placeholder="如：活动赠送、积分纠正" maxlength="100" />
          </div>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="showAdjustDialog = false">取消</button>
          <button class="btn btn-primary" :disabled="adjusting || !adjustForm.points || adjustForm.points === 0" @click="handleAdjustPoints">
            <span v-if="adjusting" class="loading loading-spinner loading-sm"></span>
            确认调整
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="showAdjustDialog = false"></div>
    </dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { httpGet, httpPost, isSuccess, parsePage } from '@/utils/fetch'
import { useStoreContext } from '@/stores/storeContext'
import { formatDate } from '@/utils/format'
import toast from '@/utils/toast'
import Pagination from '@/components/common/Pagination.vue'

const storeContext = useStoreContext()
const STORE_ID = () => storeContext.currentStoreId

const loading = ref(false)
const members = ref([])
const searchKey = ref('')
const page = ref(1)
const totalPages = ref(1)
const totalElements = ref(0)

// 详情
const detailMember = ref(null)
const detailTab = ref('transactions')

// 积分流水
const txLoading = ref(false)
const transactions = ref([])
const txPage = ref(1)
const txTotalPages = ref(1)
const txTotalElements = ref(0)

// 消费记录
const orderLoading = ref(false)
const orders = ref([])
const orderPage = ref(1)
const orderTotalPages = ref(1)
const orderTotalElements = ref(0)

// 调整积分
const showAdjustDialog = ref(false)
const adjusting = ref(false)
const adjustForm = ref({ points: null, remark: '' })

// 统计卡片计算属性
const totalPointsIssued = computed(() => {
  return members.value.reduce((sum, m) => sum + (m.num ?? 0), 0)
})

const activeCount = computed(() => {
  return members.value.filter(m => (m.count ?? 0) > 0).length
})

const predictedPoints = computed(() => {
  const current = detailMember.value?.num ?? 0
  const delta = Number(adjustForm.value.points) || 0
  return current + delta
})

onMounted(() => {
  if (STORE_ID()) {
    fetchMembers(1)
  }
})

watch(() => storeContext.currentStoreId, () => {
  if (storeContext.currentStoreId) {
    fetchMembers(1)
  }
})

const fetchMembers = async (targetPage) => {
  if (!STORE_ID()) return
  loading.value = true
  page.value = targetPage
  try {
    const keyParam = searchKey.value ? `&key=${encodeURIComponent(searchKey.value)}` : ''
    const res = await httpGet(`/manage/membership/list?storeId=${STORE_ID()}&page=${targetPage}&size=10${keyParam}`)
    if (isSuccess(res)) {
      const p = parsePage(res.data)
      members.value = p.content
      totalPages.value = p.totalPages
      totalElements.value = p.totalElements
    }
  } catch (e) {
    console.error('获取会员列表失败:', e)
    toast.error('获取会员列表失败')
  } finally {
    loading.value = false
  }
}

const switchTab = (tab) => {
  detailTab.value = tab
}

const openDetail = (member) => {
  detailMember.value = member
  detailTab.value = 'transactions'
  txPage.value = 1
  orderPage.value = 1
  transactions.value = []
  orders.value = []
  txTotalElements.value = 0
  orderTotalElements.value = 0
  fetchTransactions(1)
  fetchOrders(1)
}

const fetchTransactions = async (targetPage) => {
  if (!detailMember.value || !STORE_ID()) return
  txLoading.value = true
  txPage.value = targetPage
  try {
    const res = await httpGet(`/manage/membership/${detailMember.value.id}/transactions?storeId=${STORE_ID()}&page=${targetPage}&size=10`)
    if (isSuccess(res)) {
      const p = parsePage(res.data)
      transactions.value = p.content
      txTotalPages.value = p.totalPages
      txTotalElements.value = p.totalElements
    }
  } catch (e) {
    console.error('获取积分流水失败:', e)
  } finally {
    txLoading.value = false
  }
}

const fetchOrders = async (targetPage) => {
  if (!detailMember.value || !STORE_ID()) return
  orderLoading.value = true
  orderPage.value = targetPage
  try {
    const res = await httpGet(`/manage/membership/${detailMember.value.id}/orders?storeId=${STORE_ID()}&page=${targetPage}&size=10`)
    if (isSuccess(res)) {
      const p = parsePage(res.data)
      orders.value = p.content
      orderTotalPages.value = p.totalPages
      orderTotalElements.value = p.totalElements
    }
  } catch (e) {
    console.error('获取消费记录失败:', e)
  } finally {
    orderLoading.value = false
  }
}

const openAdjustDialog = () => {
  adjustForm.value = { points: null, remark: '' }
  showAdjustDialog.value = true
}

const handleAdjustPoints = async () => {
  if (!detailMember.value || !STORE_ID()) return
  if (adjustForm.value.points == null || adjustForm.value.points === 0) {
    toast.warning('请输入非零积分值')
    return
  }
  adjusting.value = true
  try {
    const params = new URLSearchParams({
      storeId: STORE_ID(),
      membershipId: detailMember.value.id,
      points: adjustForm.value.points
    })
    if (adjustForm.value.remark) params.append('remark', adjustForm.value.remark)
    const res = await httpPost(`/manage/membership/adjust?${params.toString()}`)
    if (isSuccess(res)) {
      toast.success('积分调整成功')
      detailMember.value = res.data?.membership || detailMember.value
      showAdjustDialog.value = false
      fetchTransactions(1)
      fetchMembers(page.value)
    } else {
      toast.error(res?.message || '调整失败')
    }
  } catch (e) {
    toast.error('调整失败')
  } finally {
    adjusting.value = false
  }
}

const txTypeText = (type) => {
  const map = { EARN: '消费获取', REDEEM: '积分消费', ADJUST: '手动调整' }
  return map[type] || type
}

const txTypeBadge = (type) => {
  const map = { EARN: 'badge-success', REDEEM: 'badge-warning', ADJUST: 'badge-info' }
  return map[type] || 'badge-ghost'
}

const orderStatusText = (status) => {
  const map = { 0: '待接单', 1: '已接单', 2: '制作中', 3: '已出餐', 4: '已完成', 5: '已取消' }
  return map[status] ?? status
}

const orderStatusBadge = (status) => {
  const map = { 0: 'badge-warning', 1: 'badge-info', 2: 'badge-info', 3: 'badge-primary', 4: 'badge-success', 5: 'badge-ghost' }
  return map[status] ?? 'badge-ghost'
}
</script>
