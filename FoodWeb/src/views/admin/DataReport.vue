<template>
  <div>
    <!-- 页头 -->
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-base-content">数据报表</h2>
        <p class="text-sm text-base-content/60 mt-1">营业额、订单量与畅销菜品分析（仅统计已完成订单）</p>
      </div>
      <button class="btn btn-ghost btn-sm bg-base-100 border border-base-200" @click="reloadAll" :disabled="loading">
        刷新
      </button>
    </div>

    <!-- 日期与快捷区间 -->
    <div class="card bg-base-100 shadow-sm border border-base-200 mb-6">
      <div class="card-body p-4">
        <div class="flex flex-col lg:flex-row lg:items-end gap-3">
          <div class="form-control">
            <label class="label py-1"><span class="label-text text-sm">统计日期</span></label>
            <input v-model="selectedDate" type="date" :max="today" class="input input-bordered input-sm w-44" @change="onDateChange" />
          </div>
          <div class="flex flex-wrap gap-2">
            <button v-for="p in datePresets" :key="p.label"
                    :class="['btn btn-sm', selectedDate === p.date ? 'btn-primary' : 'btn-ghost bg-base-100 border border-base-200']"
                    @click="applyPreset(p.date)">
              {{ p.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 当日概览 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div class="stat bg-base-100 shadow-sm rounded-lg border border-base-200">
        <div class="stat-title text-xs">当日营业额</div>
        <div class="stat-value text-primary text-2xl">¥{{ money(overview.revenue) }}</div>
        <div class="stat-desc">{{ selectedDate }}</div>
      </div>
      <div class="stat bg-base-100 shadow-sm rounded-lg border border-base-200">
        <div class="stat-title text-xs">当日订单数</div>
        <div class="stat-value text-secondary text-2xl">{{ overview.orderCount ?? 0 }}</div>
        <div class="stat-desc">已完成 {{ overview.completedCount ?? 0 }} · 取消 {{ overview.cancelledCount ?? 0 }}</div>
      </div>
      <div class="stat bg-base-100 shadow-sm rounded-lg border border-base-200">
        <div class="stat-title text-xs">客单价</div>
        <div class="stat-value text-accent text-2xl">¥{{ money(overview.avgOrderAmount) }}</div>
        <div class="stat-desc">营业额 / 已完成订单</div>
      </div>
      <div class="stat bg-base-100 shadow-sm rounded-lg border border-base-200">
        <div class="stat-title text-xs">优惠金额</div>
        <div class="stat-value text-error text-2xl">¥{{ money(overview.discountAmount) }}</div>
        <div class="stat-desc">售出 {{ overview.itemCount ?? 0 }} 件 · 会员单 {{ overview.memberOrderCount ?? 0 }}</div>
      </div>
    </div>

    <!-- 历史累计 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
      <div class="card bg-base-100 shadow-sm border border-base-200">
        <div class="card-body p-4 flex items-center justify-between">
          <div>
            <div class="text-xs text-base-content/50">历史累计营业额</div>
            <div class="text-2xl font-bold text-primary mt-1">¥{{ money(overview.totalRevenue) }}</div>
          </div>
          <span class="text-4xl">💰</span>
        </div>
      </div>
      <div class="card bg-base-100 shadow-sm border border-base-200">
        <div class="card-body p-4 flex items-center justify-between">
          <div>
            <div class="text-xs text-base-content/50">历史累计订单</div>
            <div class="text-2xl font-bold text-secondary mt-1">{{ overview.totalOrderCount ?? 0 }} 单</div>
          </div>
          <span class="text-4xl">🧾</span>
        </div>
      </div>
    </div>

    <!-- 走势 -->
    <div class="card bg-base-100 shadow-sm border border-base-200 mb-6">
      <div class="card-body p-4">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3">
          <h3 class="font-bold text-base">营业走势</h3>
          <div class="flex flex-wrap items-end gap-2">
            <div class="form-control">
              <label class="label py-0"><span class="label-text text-xs">开始</span></label>
              <input v-model="trendStart" type="date" :max="trendEnd" class="input input-bordered input-xs w-36" @change="fetchTrend" />
            </div>
            <div class="form-control">
              <label class="label py-0"><span class="label-text text-xs">结束</span></label>
              <input v-model="trendEnd" type="date" :min="trendStart" :max="today" class="input input-bordered input-xs w-36" @change="fetchTrend" />
            </div>
            <div class="join">
              <button :class="['btn btn-xs join-item', granularity === 'day' ? 'btn-primary' : 'btn-ghost border border-base-200']"
                      @click="switchGranularity('day')">按日</button>
              <button :class="['btn btn-xs join-item', granularity === 'month' ? 'btn-primary' : 'btn-ghost border border-base-200']"
                      @click="switchGranularity('month')">按月</button>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-3">
          <div class="bg-base-200 rounded-lg p-3">
            <div class="text-xs text-base-content/50">区间营业额</div>
            <div class="text-lg font-bold text-primary">¥{{ money(trendSummary.revenue) }}</div>
          </div>
          <div class="bg-base-200 rounded-lg p-3">
            <div class="text-xs text-base-content/50">区间订单数</div>
            <div class="text-lg font-bold text-secondary">{{ trendSummary.orderCount ?? 0 }}</div>
          </div>
          <div class="bg-base-200 rounded-lg p-3">
            <div class="text-xs text-base-content/50">已完成</div>
            <div class="text-lg font-bold">{{ trendSummary.completedCount ?? 0 }}</div>
          </div>
          <div class="bg-base-200 rounded-lg p-3">
            <div class="text-xs text-base-content/50">平均客单价</div>
            <div class="text-lg font-bold text-accent">¥{{ money(trendSummary.avgOrderAmount) }}</div>
          </div>
        </div>

        <div v-if="trendLoading" class="flex justify-center py-16">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>
        <EChart v-else :option="trendOption" :height="340" />
      </div>
    </div>

    <!-- 畅销菜品 -->
    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-4">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-3">
          <h3 class="font-bold text-base">畅销菜品</h3>
          <div role="tablist" class="tabs tabs-boxed">
            <a v-for="t in rankTabs" :key="t.value" role="tab" class="tab tab-sm"
               :class="{ 'tab-active': rankRange === t.value }" @click="switchRank(t.value)">
              {{ t.label }}
            </a>
          </div>
        </div>

        <div v-if="rankLoading" class="flex justify-center py-16">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>

        <template v-else-if="rankDishes.length === 0">
          <div class="text-center py-12 text-base-content/40">该时间范围内暂无已完成订单</div>
        </template>

        <template v-else>
          <EChart :option="rankChartOption" :height="Math.max(200, rankDishes.length * 32 + 60)" />
          <div class="divider my-3">明细</div>
          <div class="overflow-x-auto rounded-box border border-base-200">
            <table class="table table-sm table-zebra">
              <thead class="bg-base-200 text-base-content">
                <tr>
                  <th class="w-12">排名</th>
                  <th>菜品</th>
                  <th class="text-right">销量</th>
                  <th class="text-right">销售额</th>
                  <th class="text-right">出现订单</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="d in rankDishes" :key="d.dishId" class="hover">
                  <td>
                    <span :class="['badge badge-sm', d.rank <= 3 ? 'badge-warning' : 'badge-ghost']">{{ d.rank }}</span>
                  </td>
                  <td>
                    <div class="flex items-center gap-2">
                      <div class="avatar">
                        <div class="w-8 h-8 rounded bg-base-200">
                          <img v-if="d.topImage" :src="d.topImage" :alt="d.dishName" class="w-full h-full object-cover rounded" />
                        </div>
                      </div>
                      <span class="truncate">{{ d.dishName }}</span>
                    </div>
                  </td>
                  <td class="text-right font-medium">{{ qty(d.quantity) }}</td>
                  <td class="text-right font-semibold text-primary">¥{{ money(d.revenue) }}</td>
                  <td class="text-right text-base-content/60">{{ d.orderCount }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { httpGet, isSuccess } from '@/utils/fetch'
import { useStoreContext } from '@/stores/storeContext'
import toast from '@/utils/toast'
import EChart from '@/components/chart/EChart.vue'

const storeContext = useStoreContext()
const STORE_ID = () => storeContext.currentStoreId

/** 今天（yyyy-MM-dd），用于日期选择器上限 */
const today = new Date().toLocaleDateString('sv-SE')

const loading = ref(false)
const selectedDate = ref(today)
const overview = ref({})
const trendSummary = ref({})
const trendPoints = ref([])
const trendLoading = ref(false)
const granularity = ref('day')
const rankLoading = ref(false)
const rankRange = ref('day')
const rankDishes = ref([])

const datePresets = computed(() => {
  const d = (offsetDays) => {
    const t = new Date()
    t.setDate(t.getDate() - offsetDays)
    return t.toLocaleDateString('sv-SE')
  }
  return [
    { label: '今天', date: d(0) },
    { label: '昨天', date: d(1) },
    { label: '前天', date: d(2) }
  ]
})

const rankTabs = [
  { value: 'day', label: '日畅销' },
  { value: 'month', label: '月畅销' },
  { value: 'all', label: '历史畅销' }
]

/** 走势默认区间：按日=近30天，按月=近12个月 */
const defaultTrendRange = (g) => {
  const end = new Date()
  const start = new Date()
  if (g === 'month') {
    start.setMonth(start.getMonth() - 11)
    start.setDate(1)
  } else {
    start.setDate(start.getDate() - 29)
  }
  return { start: start.toLocaleDateString('sv-SE'), end: end.toLocaleDateString('sv-SE') }
}

const initialRange = defaultTrendRange('day')
const trendStart = ref(initialRange.start)
const trendEnd = ref(initialRange.end)

onMounted(() => {
  if (STORE_ID()) reloadAll()
})

watch(() => storeContext.currentStoreId, () => {
  if (STORE_ID()) reloadAll()
})

const reloadAll = () => {
  fetchOverview()
  fetchTrend()
  fetchRank()
}

const applyPreset = (date) => {
  selectedDate.value = date
  fetchOverview()
  fetchRank()
}

const onDateChange = () => {
  fetchOverview()
  fetchRank()
}

const switchGranularity = (g) => {
  if (granularity.value === g) return
  granularity.value = g
  const range = defaultTrendRange(g)
  trendStart.value = range.start
  trendEnd.value = range.end
  fetchTrend()
}

const switchRank = (r) => {
  rankRange.value = r
  fetchRank()
}

/** 单日概览 */
const fetchOverview = async () => {
  if (!STORE_ID()) return
  loading.value = true
  try {
    const res = await httpGet(`/manage/report/overview?storeId=${STORE_ID()}&date=${selectedDate.value}`)
    if (isSuccess(res)) {
      overview.value = res.data || {}
    } else {
      toast.error(res?.message || '获取经营概览失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('获取经营概览失败')
  } finally {
    loading.value = false
  }
}

/** 走势数据 */
const fetchTrend = async () => {
  if (!STORE_ID()) return
  if (!trendStart.value || !trendEnd.value) return
  if (trendStart.value > trendEnd.value) {
    toast.warning('开始日期不能晚于结束日期')
    return
  }
  trendLoading.value = true
  try {
    const res = await httpGet(
      `/manage/report/trend?storeId=${STORE_ID()}` +
      `&start=${trendStart.value}&end=${trendEnd.value}&granularity=${granularity.value}`
    )
    if (isSuccess(res) && res.data) {
      trendPoints.value = res.data.points || []
      trendSummary.value = res.data.summary || {}
    } else {
      toast.error(res?.message || '获取走势数据失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('获取走势数据失败')
  } finally {
    trendLoading.value = false
  }
}

/** 畅销榜 */
const fetchRank = async () => {
  if (!STORE_ID()) return
  rankLoading.value = true
  try {
    let url = `/manage/report/top-dishes?storeId=${STORE_ID()}&range=${rankRange.value}&limit=10`
    if (rankRange.value !== 'all') url += `&date=${selectedDate.value}`
    const res = await httpGet(url)
    if (isSuccess(res) && res.data) {
      rankDishes.value = res.data.dishes || []
    } else {
      toast.error(res?.message || '获取畅销榜失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('获取畅销榜失败')
  } finally {
    rankLoading.value = false
  }
}

// ==================== 图表配置 ====================

/** 浅色主题下的通用文字/轴线颜色 */
const AXIS_COLOR = '#94a3b8'
const SPLIT_COLOR = '#e5e7eb'

const trendOption = computed(() => {
  const points = trendPoints.value
  const labels = points.map(p => p.label)
  const revenue = points.map(p => Number(p.revenue || 0))
  const orders = points.map(p => Number(p.orderCount || 0))
  const avg = points.map(p => Number(p.avgOrderAmount || 0))
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['营业额', '订单数', '客单价'], bottom: 0, textStyle: { color: AXIS_COLOR } },
    grid: { left: 8, right: 8, top: 24, bottom: 48, containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: SPLIT_COLOR } },
      axisLabel: { color: AXIS_COLOR, hideOverlap: true }
    },
    yAxis: [
      {
        type: 'value',
        name: '金额(元)',
        nameTextStyle: { color: AXIS_COLOR },
        axisLabel: { color: AXIS_COLOR },
        splitLine: { lineStyle: { color: SPLIT_COLOR, type: 'dashed' } }
      },
      {
        type: 'value',
        name: '单量',
        nameTextStyle: { color: AXIS_COLOR },
        axisLabel: { color: AXIS_COLOR },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '营业额',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: revenue,
        itemStyle: { color: '#f87272' },
        areaStyle: { color: 'rgba(248,114,114,0.15)' }
      },
      {
        name: '订单数',
        type: 'bar',
        yAxisIndex: 1,
        data: orders,
        itemStyle: { color: 'rgba(59,130,246,0.65)', borderRadius: [3, 3, 0, 0] },
        barMaxWidth: 18
      },
      {
        name: '客单价',
        type: 'line',
        yAxisIndex: 0,
        smooth: true,
        showSymbol: false,
        data: avg,
        itemStyle: { color: '#36d399' }
      }
    ]
  }
})

const rankChartOption = computed(() => {
  // 横向条形图：数据倒序，让第一名显示在顶部
  const list = [...rankDishes.value].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 24, top: 12, bottom: 8, containLabel: true },
    xAxis: {
      type: 'value',
      axisLabel: { color: AXIS_COLOR },
      splitLine: { lineStyle: { color: SPLIT_COLOR, type: 'dashed' } }
    },
    yAxis: {
      type: 'category',
      data: list.map(d => d.dishName),
      axisLine: { lineStyle: { color: SPLIT_COLOR } },
      axisLabel: { color: '#334155', width: 90, overflow: 'truncate' }
    },
    series: [
      {
        name: '销量',
        type: 'bar',
        data: list.map(d => Number(d.quantity || 0)),
        itemStyle: { color: '#7c3aed', borderRadius: [0, 3, 3, 0] },
        barMaxWidth: 18
      }
    ]
  }
})

// ==================== 展示格式化 ====================

const money = (v) => {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const qty = (v) => {
  const n = Number(v || 0)
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}
</script>
