<template>
  <div>
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-base-content">数据看板</h2>
        <p class="text-sm text-base-content/60 mt-1" v-if="storeContext.currentStore">
          {{ storeContext.currentStore.storeName }} · 今日经营概况
        </p>
      </div>
      <button class="btn btn-ghost btn-sm bg-base-100 border border-base-200" @click="fetchStats" :disabled="loading">
        刷新
      </button>
    </div>

    <div v-if="loading" class="flex justify-center items-center py-24">
      <span class="loading loading-spinner loading-lg text-primary"></span>
    </div>

    <template v-else>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div class="stat bg-base-100 shadow-md rounded-lg">
          <div class="stat-figure text-primary">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div class="stat-title">今日营业额</div>
          <div class="stat-value text-primary">¥{{ stats.todayRevenue ?? '0.00' }}</div>
          <div class="stat-desc">已完成订单收入</div>
        </div>

        <div class="stat bg-base-100 shadow-md rounded-lg">
          <div class="stat-figure text-secondary">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
          </div>
          <div class="stat-title">今日订单</div>
          <div class="stat-value text-secondary">{{ stats.todayOrderCount ?? 0 }}</div>
          <div class="stat-desc">含所有状态订单</div>
        </div>

        <div class="stat bg-base-100 shadow-md rounded-lg">
          <div class="stat-figure text-warning">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div class="stat-title">待处理订单</div>
          <div class="stat-value text-warning">{{ stats.pendingCount ?? 0 }}</div>
          <div class="stat-desc">待接单，请及时处理</div>
        </div>

        <div class="stat bg-base-100 shadow-md rounded-lg">
          <div class="stat-figure text-accent">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
          <div class="stat-title">制作中订单</div>
          <div class="stat-value text-accent">{{ stats.cookingCount ?? 0 }}</div>
          <div class="stat-desc">已接单 + 制作中</div>
        </div>
      </div>

      <div class="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 最近订单 -->
        <div class="card bg-base-100 shadow-md">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <h3 class="card-title">最近订单</h3>
              <router-link to="/admin/orders" class="btn btn-ghost btn-xs">查看全部 →</router-link>
            </div>
            <div v-if="recentOrders.length === 0" class="text-center py-10 text-base-content/40">
              暂无订单
            </div>
            <div v-else class="overflow-x-auto">
              <table class="table table-zebra">
                <thead>
                  <tr>
                    <th>订单号</th>
                    <th>金额</th>
                    <th>状态</th>
                    <th>时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="order in recentOrders" :key="order.id">
                    <td class="font-mono text-sm">{{ order.orderNo }}</td>
                    <td>¥{{ order.payAmount }}</td>
                    <td><span :class="['badge badge-sm', statusBadge(order.status)]">{{ statusText(order.status) }}</span></td>
                    <td class="text-sm">{{ formatTime(order.createTime) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 热销菜品 -->
        <div class="card bg-base-100 shadow-md">
          <div class="card-body">
            <h3 class="card-title">热销菜品 TOP 5</h3>
            <div v-if="topDishes.length === 0" class="text-center py-10 text-base-content/40">
              暂无销量数据
            </div>
            <ul v-else class="menu">
              <li v-for="(dish, index) in topDishes" :key="dish.id">
                <a class="flex items-center justify-between">
                  <span class="flex items-center gap-2">
                    <span :class="['badge mr-2', badgeClass(index)]">{{ index + 1 }}</span>
                    {{ dish.name }}
                  </span>
                  <span class="text-sm text-base-content/50">已售 {{ dish.allSales || 0 }}</span>
                </a>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { httpGet, isSuccess, parsePage } from '@/utils/fetch'
import { useStoreContext } from '@/stores/storeContext'
import { orderStatusText as statusText, orderStatusBadge as statusBadge } from '@/utils/order'

const storeContext = useStoreContext()
const loading = ref(false)
const stats = ref({})
const recentOrders = ref([])
const topDishes = ref([])

onMounted(() => {
  if (storeContext.currentStoreId) {
    fetchStats()
    fetchRecentOrders()
    fetchTopDishes()
  }
})

// 门店切换后重新加载
watch(() => storeContext.currentStoreId, () => {
  if (storeContext.currentStoreId) {
    fetchStats()
    fetchRecentOrders()
    fetchTopDishes()
  }
})

/** 加载经营统计 */
const fetchStats = async () => {
  if (!storeContext.currentStoreId) return
  loading.value = true
  try {
    const res = await httpGet(`/order/manage/stats?storeId=${storeContext.currentStoreId}`)
    if (isSuccess(res)) {
      stats.value = res.data || {}
    }
  } catch (e) {
    console.error('加载统计失败:', e)
  } finally {
    loading.value = false
  }
}

/** 加载最近订单 */
const fetchRecentOrders = async () => {
  try {
    const res = await httpGet(`/order/manage/list?storeId=${storeContext.currentStoreId}&page=1&size=5`)
    if (isSuccess(res) && res.data) {
      recentOrders.value = parsePage(res.data).content
    }
  } catch (e) {
    console.error('加载最近订单失败:', e)
  }
}

/** 加载热销菜品（按总销量取前5） */
const fetchTopDishes = async () => {
  try {
    const res = await httpGet(`/dishes/list?storefrontId=${storeContext.currentStoreId}&page=1&size=5`)
    if (isSuccess(res) && res.data) {
      const list = parsePage(res.data).content
      topDishes.value = [...list].sort((a, b) => (b.allSales || 0) - (a.allSales || 0)).slice(0, 5)
    }
  } catch (e) {
    console.error('加载热销菜品失败:', e)
  }
}

const badgeClass = (index) => {
  return ['badge-primary', 'badge-secondary', 'badge-accent', 'badge-info', 'badge-neutral'][index] || 'badge-ghost'
}

const formatTime = (ts) => {
  if (!ts) return '-'
  const d = new Date(Number(ts))
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>
