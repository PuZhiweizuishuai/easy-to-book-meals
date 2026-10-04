<template>
  <div>
    <!-- 页头 -->
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-base-content">优惠券管理</h2>
        <p class="text-sm text-base-content/60 mt-1">创建满减券 / 折扣券 / 无门槛券，生成券码二维码供顾客核销</p>
      </div>
      <div class="flex gap-2">
        <button v-if="canManage" class="btn btn-primary btn-sm" @click="openCreateDialog">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          新建优惠券
        </button>
        <button class="btn btn-ghost btn-sm bg-base-100 border border-base-200" @click="fetchCoupons(1)" :disabled="loading">
          刷新
        </button>
      </div>
    </div>

    <!-- 加载 / 空态 -->
    <div v-if="loading" class="flex justify-center py-20">
      <span class="loading loading-spinner loading-lg text-primary"></span>
    </div>

    <div v-else-if="coupons.length === 0" class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body items-center py-20 text-center">
        <span class="text-5xl mb-3">🎟️</span>
        <p class="text-base-content/50">暂无优惠券</p>
        <p class="text-base-content/40 text-sm mt-1">创建优惠券后可按发行量生成券码与二维码</p>
      </div>
    </div>

    <!-- 券模板列表 -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div v-for="c in coupons" :key="c.id"
           class="card bg-base-100 shadow-sm border border-base-200 hover:shadow-md transition-shadow">
        <div class="card-body p-4">
          <div class="flex items-start justify-between gap-2">
            <div class="min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-bold text-base truncate">{{ c.name }}</span>
                <span :class="['badge badge-sm', couponTypeBadge(c.type)]">{{ couponTypeText(c.type) }}</span>
                <span :class="['badge badge-sm', c.status === 0 ? 'badge-success' : 'badge-ghost']">
                  {{ c.status === 0 ? '启用中' : '已停用' }}
                </span>
              </div>
              <div class="mt-2 flex items-end gap-2">
                <span class="text-2xl font-bold text-error">{{ couponRuleText(c) }}</span>
                <span v-if="c.type !== 3" class="text-xs text-base-content/50 mb-1">
                  满 ¥{{ c.thresholdAmount }} 可用
                </span>
              </div>
            </div>
          </div>

          <div class="mt-3 grid grid-cols-3 gap-2 text-center">
            <div class="bg-base-200 rounded-lg py-2">
              <div class="text-sm font-bold">{{ c.totalCount }}</div>
              <div class="text-xs text-base-content/50">发行量</div>
            </div>
            <div class="bg-base-200 rounded-lg py-2">
              <div class="text-sm font-bold">{{ c.claimCount ?? 0 }}</div>
              <div class="text-xs text-base-content/50">已领取</div>
            </div>
            <div class="bg-base-200 rounded-lg py-2">
              <div class="text-sm font-bold">{{ c.usedCount ?? 0 }}</div>
              <div class="text-xs text-base-content/50">已核销</div>
            </div>
          </div>

          <div class="mt-3 text-xs text-base-content/50 space-y-0.5">
            <p>有效期：{{ periodText(c) }}</p>
            <p v-if="c.perLimit && c.perLimit > 0">每人限领 {{ c.perLimit }} 张</p>
            <p v-if="c.description" class="truncate">{{ c.description }}</p>
          </div>

          <div class="card-actions justify-end mt-3">
            <button class="btn btn-ghost btn-xs" @click="openCodesDialog(c)">券码 / 二维码</button>
            <button v-if="canManage" class="btn btn-outline btn-xs"
                    :class="c.status === 0 ? 'btn-warning' : 'btn-success'"
                    :disabled="togglingId === c.id" @click="toggleStatus(c)">
              {{ c.status === 0 ? '停用' : '启用' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <Pagination v-if="!loading" v-model="page" :total-pages="totalPages" @change="fetchCoupons" />

    <!-- 创建优惠券弹窗 -->
    <dialog class="modal" :class="{ 'modal-open': showCreateDialog }">
      <div class="modal-box max-w-lg max-h-[90vh] overflow-y-auto">
        <h3 class="font-bold text-lg mb-4">新建优惠券</h3>

        <div class="space-y-3">
          <!-- 券名称 -->
          <div class="form-control">
            <label class="label py-1"><span class="label-text text-sm">券名称 <span class="text-error">*</span></span></label>
            <input v-model.trim="form.name" type="text" maxlength="100" class="input input-bordered input-sm w-full"
                   placeholder="如：满100减20" />
          </div>

          <!-- 券类型 -->
          <div class="form-control">
            <label class="label py-1"><span class="label-text text-sm">券类型 <span class="text-error">*</span></span></label>
            <div class="flex gap-2">
              <button v-for="t in couponTypes" :key="t.value"
                      :class="['btn btn-sm flex-1', form.type === t.value ? 'btn-primary' : 'btn-ghost border border-base-200']"
                      @click="form.type = t.value">
                {{ t.label }}
              </button>
            </div>
          </div>

          <!-- 满减券 / 折扣券：门槛 -->
          <div v-if="form.type !== 3" class="form-control">
            <label class="label py-1"><span class="label-text text-sm">使用门槛（元）<span class="text-error">*</span></span></label>
            <input v-model.number="form.thresholdAmount" type="number" min="0.01" step="0.01"
                   class="input input-bordered input-sm w-full" placeholder="如：100" />
          </div>

          <!-- 满减券 / 无门槛券：减免金额 -->
          <div v-if="form.type === 1 || form.type === 3" class="form-control">
            <label class="label py-1"><span class="label-text text-sm">减免金额（元）<span class="text-error">*</span></span></label>
            <input v-model.number="form.reduceAmount" type="number" min="0.01" step="0.01"
                   class="input input-bordered input-sm w-full" placeholder="如：20" />
          </div>

          <!-- 折扣券：折扣率 + 最高优惠 -->
          <template v-if="form.type === 2">
            <div class="form-control">
              <label class="label py-1">
                <span class="label-text text-sm">折扣率 <span class="text-error">*</span></span>
                <span class="label-text-alt text-base-content/50">
                  当前 {{ ((form.discountRate ?? 1) * 10).toFixed(1) }} 折
                </span>
              </label>
              <input v-model.number="form.discountRate" type="number" min="0.01" max="0.99" step="0.01"
                     class="input input-bordered input-sm w-full" placeholder="如：0.85 表示 85 折" />
            </div>
            <div class="form-control">
              <label class="label py-1">
                <span class="label-text text-sm">最高优惠（元）</span>
                <span class="label-text-alt text-base-content/50">留空表示不限制</span>
              </label>
              <input v-model.number="form.maxDiscountAmount" type="number" min="0.01" step="0.01"
                     class="input input-bordered input-sm w-full" placeholder="如：50" />
            </div>
          </template>

          <!-- 发行量 / 限领 -->
          <div class="grid grid-cols-2 gap-3">
            <div class="form-control">
              <label class="label py-1"><span class="label-text text-sm">发行数量 <span class="text-error">*</span></span></label>
              <input v-model.number="form.totalCount" type="number" min="1" max="10000" step="1"
                     class="input input-bordered input-sm w-full" placeholder="如：100" />
            </div>
            <div class="form-control">
              <label class="label py-1"><span class="label-text text-sm">每人限领</span></label>
              <input v-model.number="form.perLimit" type="number" min="0" step="1"
                     class="input input-bordered input-sm w-full" placeholder="0 表示不限" />
            </div>
          </div>

          <!-- 有效期 -->
          <div class="grid grid-cols-2 gap-3">
            <div class="form-control">
              <label class="label py-1"><span class="label-text text-sm">生效时间</span></label>
              <input v-model="form.startDate" type="date" class="input input-bordered input-sm w-full" />
            </div>
            <div class="form-control">
              <label class="label py-1"><span class="label-text text-sm">失效时间</span></label>
              <input v-model="form.endDate" type="date" class="input input-bordered input-sm w-full" />
            </div>
          </div>
          <p class="text-xs text-base-content/40">不填有效期表示创建后立即生效、长期有效</p>

          <!-- 说明 -->
          <div class="form-control">
            <label class="label py-1"><span class="label-text text-sm">使用说明</span></label>
            <textarea v-model.trim="form.description" rows="2" maxlength="255"
                      class="textarea textarea-bordered textarea-sm w-full" placeholder="如：仅限堂食使用，不与其他优惠同享"></textarea>
          </div>
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost" @click="showCreateDialog = false">取消</button>
          <button class="btn btn-primary" :disabled="creating" @click="handleCreate">
            <span v-if="creating" class="loading loading-spinner loading-sm"></span>
            {{ creating ? '创建中...' : '创建并生成券码' }}
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="showCreateDialog = false"></div>
    </dialog>

    <!-- 券码列表弹窗（含二维码） -->
    <dialog class="modal" :class="{ 'modal-open': codesCoupon }">
      <div class="modal-box max-w-4xl max-h-[90vh] overflow-y-auto">
        <template v-if="codesCoupon">
          <div class="flex items-start justify-between gap-2">
            <div>
              <h3 class="font-bold text-lg">券码列表</h3>
              <p class="text-sm text-base-content/60 mt-1">
                {{ codesCoupon.name }} · {{ couponRuleText(codesCoupon) }} · 共 {{ codesTotalElements }} 张
              </p>
            </div>
            <button class="btn btn-ghost btn-xs btn-circle" @click="codesCoupon = null">✕</button>
          </div>

          <!-- 状态过滤 -->
          <div class="flex flex-wrap gap-2 my-3">
            <button v-for="t in codeStatusTabs" :key="String(t.value)"
                    :class="['btn btn-xs', codeStatus === t.value ? 'btn-primary' : 'btn-ghost bg-base-100 border border-base-200']"
                    @click="switchCodeStatus(t.value)">
              {{ t.label }}
            </button>
          </div>

          <div v-if="codesLoading" class="flex justify-center py-12">
            <span class="loading loading-spinner text-primary"></span>
          </div>

          <div v-else-if="codes.length === 0" class="text-center py-12 text-base-content/40">
            <p>暂无券码</p>
          </div>

          <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            <div v-for="uc in codes" :key="uc.id"
                 class="flex items-center gap-3 border border-base-200 rounded-xl p-3 cursor-pointer hover:bg-base-200 transition-colors"
                 @click="openQrDialog(uc)">
              <QrCode :value="uc.code" :size="72" />
              <div class="min-w-0 flex-1">
                <div class="flex items-center gap-1">
                  <span class="font-mono text-xs truncate">{{ uc.code }}</span>
                </div>
                <div class="mt-1">
                  <span :class="['badge badge-xs', codeStatusBadge(uc.status)]">{{ codeStatusText(uc.status) }}</span>
                </div>
                <div class="text-xs text-base-content/40 mt-1">
                  {{ uc.userId ? ('用户#' + uc.userId) : '未领取' }}
                  <span v-if="uc.orderNo"> · {{ uc.orderNo }}</span>
                </div>
              </div>
            </div>
          </div>

          <Pagination v-model="codesPage" :total-pages="codesTotalPages" size="xs"
                      container-class="mt-3" @change="fetchCodes" />
        </template>
      </div>
      <div class="modal-backdrop" @click="codesCoupon = null"></div>
    </dialog>

    <!-- 券码二维码大图弹窗 -->
    <dialog class="modal" :class="{ 'modal-open': qrCoupon }">
      <div class="modal-box max-w-xs text-center">
        <template v-if="qrCoupon">
          <h3 class="font-bold text-lg mb-1">{{ qrCoupon.coupon?.name || '优惠券' }}</h3>
          <p class="text-sm text-base-content/60 mb-4">{{ couponRuleText(qrCoupon.coupon) }}</p>
          <div class="flex justify-center">
            <QrCode :value="qrCoupon.code" :size="220" show-text />
          </div>
          <div class="mt-4 flex gap-2 justify-center">
            <button class="btn btn-outline btn-sm" @click="copyCode(qrCoupon.code)">复制券码</button>
            <button class="btn btn-ghost btn-sm" @click="qrCoupon = null">关闭</button>
          </div>
        </template>
      </div>
      <div class="modal-backdrop" @click="qrCoupon = null"></div>
    </dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { httpGet, httpPost, isSuccess, parsePage } from '@/utils/fetch'
import { useStoreContext } from '@/stores/storeContext'
import { useAuthStore } from '@/stores/auth'
import toast from '@/utils/toast'
import Pagination from '@/components/common/Pagination.vue'
import QrCode from '@/components/common/QrCode.vue'

const storeContext = useStoreContext()
const authStore = useAuthStore()
const STORE_ID = () => storeContext.currentStoreId

/** 是否有优惠券管理权限（无则只读） */
const canManage = computed(() => authStore.hasAnyPermission(['coupon:manage']))

/** 券类型：与后端 CouponType 一致 */
const couponTypes = [
  { value: 1, label: '满减券' },
  { value: 2, label: '折扣券' },
  { value: 3, label: '无门槛券' }
]

const loading = ref(false)
const coupons = ref([])
const page = ref(1)
const totalPages = ref(1)
const togglingId = ref(null)

/** 创建券弹窗 */
const showCreateDialog = ref(false)
const creating = ref(false)
const createForm = () => ({
  name: '',
  type: 1,
  thresholdAmount: null,
  reduceAmount: null,
  discountRate: null,
  maxDiscountAmount: null,
  totalCount: 100,
  perLimit: 1,
  startDate: '',
  endDate: '',
  description: ''
})
const form = ref(createForm())

/** 券码弹窗 */
const codesCoupon = ref(null)
const codes = ref([])
const codesLoading = ref(false)
const codesPage = ref(1)
const codesTotalPages = ref(1)
const codesTotalElements = ref(0)
const codeStatus = ref(null)
const codeStatusTabs = [
  { value: null, label: '全部' },
  { value: 0, label: '未使用' },
  { value: 1, label: '已使用' },
  { value: 2, label: '已作废' }
]

/** 二维码大图 */
const qrCoupon = ref(null)

onMounted(() => {
  if (STORE_ID()) fetchCoupons(1)
})

watch(() => storeContext.currentStoreId, () => {
  if (STORE_ID()) fetchCoupons(1)
})

/** 加载券模板列表 */
const fetchCoupons = async (targetPage) => {
  if (!STORE_ID()) return
  loading.value = true
  page.value = targetPage
  try {
    const res = await httpGet(`/manage/coupon/list?storeId=${STORE_ID()}&page=${targetPage}&size=10`)
    if (isSuccess(res) && res.data) {
      const p = parsePage(res.data)
      coupons.value = p.content
      totalPages.value = p.totalPages
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('获取优惠券列表失败')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  form.value = createForm()
  showCreateDialog.value = true
}

/** 日期字符串 → 毫秒时间戳（空返回 null） */
const toTimestamp = (dateStr, endOfDay = false) => {
  if (!dateStr) return null
  const t = new Date(`${dateStr}T${endOfDay ? '23:59:59' : '00:00:00'}`).getTime()
  return Number.isFinite(t) ? t : null
}

const handleCreate = async () => {
  const f = form.value
  if (!f.name) {
    toast.warning('请填写券名称')
    return
  }
  if (!f.totalCount || f.totalCount < 1) {
    toast.warning('请填写正确的发行数量')
    return
  }
  if (f.type !== 3 && (!f.thresholdAmount || f.thresholdAmount <= 0)) {
    toast.warning('请填写大于 0 的使用门槛')
    return
  }
  if ((f.type === 1 || f.type === 3) && (!f.reduceAmount || f.reduceAmount <= 0)) {
    toast.warning('请填写大于 0 的减免金额')
    return
  }
  if (f.type === 1 && f.reduceAmount > f.thresholdAmount) {
    toast.warning('减免金额不能大于使用门槛')
    return
  }
  if (f.type === 2 && (!f.discountRate || f.discountRate <= 0 || f.discountRate >= 1)) {
    toast.warning('折扣率需介于 0 与 1 之间（如 0.85 表示 85 折）')
    return
  }

  creating.value = true
  try {
    const payload = {
      storeId: STORE_ID(),
      name: f.name,
      type: f.type,
      totalCount: f.totalCount,
      perLimit: f.perLimit ?? 0,
      startTime: toTimestamp(f.startDate),
      endTime: toTimestamp(f.endDate, true),
      description: f.description || null
    }
    if (f.type === 1) {
      payload.thresholdAmount = f.thresholdAmount
      payload.reduceAmount = f.reduceAmount
    } else if (f.type === 2) {
      payload.thresholdAmount = f.thresholdAmount
      payload.discountRate = f.discountRate
      payload.maxDiscountAmount = f.maxDiscountAmount || null
    } else {
      payload.reduceAmount = f.reduceAmount
    }
    const res = await httpPost('/manage/coupon/add', payload)
    if (isSuccess(res)) {
      toast.success(`优惠券创建成功，已生成 ${f.totalCount} 张券码`)
      showCreateDialog.value = false
      fetchCoupons(1)
    } else {
      toast.error(res?.message || '创建失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error(e?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

/** 启停券模板 */
const toggleStatus = async (c) => {
  if (!canManage.value) {
    toast.error('您没有优惠券管理权限')
    return
  }
  togglingId.value = c.id
  try {
    const target = c.status === 0 ? 1 : 0
    const res = await httpPost(
      `/manage/coupon/status?storeId=${STORE_ID()}&couponId=${c.id}&status=${target}`
    )
    if (isSuccess(res)) {
      toast.success(target === 0 ? '优惠券已启用' : '优惠券已停用')
      fetchCoupons(page.value)
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('操作失败')
  } finally {
    togglingId.value = null
  }
}

/** 打开券码弹窗 */
const openCodesDialog = (c) => {
  codesCoupon.value = c
  codesPage.value = 1
  codes.value = []
  codeStatus.value = null
  fetchCodes(1)
}

const switchCodeStatus = (status) => {
  codeStatus.value = status
  codesPage.value = 1
  fetchCodes(1)
}

const fetchCodes = async (targetPage) => {
  if (!codesCoupon.value || !STORE_ID()) return
  codesLoading.value = true
  codesPage.value = targetPage
  try {
    let url = `/manage/coupon/${codesCoupon.value.id}/codes?storeId=${STORE_ID()}&page=${targetPage}&size=12`
    if (codeStatus.value !== null) url += `&status=${codeStatus.value}`
    const res = await httpGet(url)
    if (isSuccess(res) && res.data) {
      const p = parsePage(res.data)
      codes.value = p.content.map(u => ({ ...u, coupon: codesCoupon.value }))
      codesTotalPages.value = p.totalPages
      codesTotalElements.value = p.totalElements
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('获取券码失败')
  } finally {
    codesLoading.value = false
  }
}

const openQrDialog = (uc) => {
  qrCoupon.value = uc
}

const copyCode = async (code) => {
  try {
    await navigator.clipboard.writeText(code)
    toast.success('券码已复制')
  } catch {
    toast.error('复制失败，请手动选择券码')
  }
}

/** 券规则文案 */
const couponRuleText = (c) => {
  if (!c) return ''
  if (c.type === 1) return `减 ¥${Number(c.reduceAmount ?? 0).toFixed(2)}`
  if (c.type === 2) {
    const rate = Number(c.discountRate ?? 1)
    const text = `${(rate * 10).toFixed(1)} 折`
    return c.maxDiscountAmount ? `${text}（最高减 ¥${Number(c.maxDiscountAmount).toFixed(2)}）` : text
  }
  return `减 ¥${Number(c.reduceAmount ?? 0).toFixed(2)}`
}

const couponTypeText = (type) => {
  const map = { 1: '满减券', 2: '折扣券', 3: '无门槛券' }
  return map[type] || '优惠券'
}

const couponTypeBadge = (type) => {
  const map = { 1: 'badge-warning', 2: 'badge-info', 3: 'badge-success' }
  return map[type] || 'badge-ghost'
}

const codeStatusText = (status) => {
  const map = { 0: '未使用', 1: '已使用', 2: '已作废' }
  return map[status] ?? status
}

const codeStatusBadge = (status) => {
  const map = { 0: 'badge-success', 1: 'badge-ghost', 2: 'badge-error' }
  return map[status] ?? 'badge-ghost'
}

const periodText = (c) => {
  if (!c.startTime && !c.endTime) return '长期有效'
  const fmt = (ts) => {
    if (!ts) return ''
    const d = new Date(Number(ts))
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }
  return `${fmt(c.startTime) || '即时'} ~ ${c.endTime ? fmt(c.endTime) : '长期'}`
}
</script>
