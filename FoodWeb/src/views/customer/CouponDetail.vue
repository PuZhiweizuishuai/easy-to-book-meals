<template>
  <div class="min-h-screen bg-base-200 flex items-center justify-center p-4">
    <div class="card bg-base-100 shadow-md w-full max-w-md">
      <div class="card-body items-center text-center">
        <div v-if="loading" class="py-12">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>

        <template v-else-if="coupon">
          <span class="text-4xl">🎟️</span>
          <h2 class="text-xl font-bold mt-2">{{ coupon.name }}</h2>
          <span :class="['badge badge-sm', couponTypeBadge(coupon.type)]">{{ couponTypeText(coupon.type) }}</span>

          <div class="py-4">
            <span class="text-4xl font-bold text-error">{{ couponRuleText(coupon) }}</span>
          </div>
          <p v-if="coupon.type !== 3" class="text-sm text-base-content/60">
            满 ¥{{ Number(coupon.thresholdAmount).toFixed(2) }} 可用
          </p>
          <p class="text-xs text-base-content/40 mt-1">有效期：{{ periodText(coupon) }}</p>
          <p v-if="coupon.description" class="text-xs text-base-content/50 mt-2">{{ coupon.description }}</p>

          <!-- 券码二维码：未登录也可出示给店员扫码核销 -->
          <div class="mt-4">
            <QrCode :value="userCoupon?.code || ''" :size="180" show-text />
            <p class="text-xs text-base-content/40 mt-2">出示二维码或券码给店员即可核销</p>
          </div>

          <div class="mt-3">
            <span :class="['badge', statusBadge(userCoupon?.status)]">{{ statusText(userCoupon?.status) }}</span>
            <span v-if="userCoupon?.userId" class="badge badge-outline badge-sm ml-1">已关联账户</span>
          </div>

          <div class="card-actions justify-center mt-4 w-full">
            <router-link to="/" class="btn btn-ghost btn-sm">去点餐</router-link>
            <button
              v-if="canClaim"
              class="btn btn-primary btn-sm"
              :disabled="claiming"
              @click="handleClaim"
            >
              <span v-if="claiming" class="loading loading-spinner loading-xs"></span>
              领取到我的账户
            </button>
            <router-link v-else-if="!isLogin" to="/login" class="btn btn-outline btn-sm">
              登录后领取
            </router-link>
          </div>
          <p v-if="!canClaim && isLogin && userCoupon?.status === 0" class="text-xs text-base-content/40">
            该券已被领取或不支持重复领取
          </p>
        </template>

        <div v-else class="py-8">
          <span class="text-4xl">😕</span>
          <p class="text-base-content/50 mt-3">{{ errorMessage || '优惠券不存在' }}</p>
          <router-link to="/" class="btn btn-primary btn-sm mt-4">返回首页</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { httpGet, httpPost, isSuccess } from '@/utils/fetch'
import { useAuthStore } from '@/stores/auth'
import toast from '@/utils/toast'
import QrCode from '@/components/common/QrCode.vue'

const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const claiming = ref(false)
const coupon = ref(null)
const userCoupon = ref(null)
const errorMessage = ref('')

const isLogin = computed(() => authStore.isLoggedIn)

/** 未使用、已登录且尚未关联到本人时可领取 */
const canClaim = computed(() => {
  if (!isLogin.value) return false
  if (!userCoupon.value) return false
  if (userCoupon.value.status !== 0) return false
  if (userCoupon.value.userId == null) return true
  return String(userCoupon.value.userId) === String(authStore.userInfo?.id)
})

onMounted(loadCoupon)

async function loadCoupon() {
  loading.value = true
  try {
    const res = await httpGet(`/coupon/detail/${encodeURIComponent(route.params.code)}`)
    if (isSuccess(res) && res.data) {
      // 券详情接口公开，未登录也可展示券面信息与二维码
      coupon.value = res.data.coupon
      userCoupon.value = res.data.userCoupon
    } else {
      errorMessage.value = res?.message || '优惠券不存在'
    }
  } catch (e) {
    errorMessage.value = '加载优惠券失败'
  } finally {
    loading.value = false
  }
}

async function handleClaim() {
  claiming.value = true
  try {
    const res = await httpPost(`/coupon/claim/${encodeURIComponent(userCoupon.value.code)}`)
    if (isSuccess(res)) {
      toast.success('领取成功，结算时可直接使用')
      userCoupon.value = res.data
    } else {
      toast.error(res?.message || '领取失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('领取失败')
  } finally {
    claiming.value = false
  }
}

const couponRuleText = (c) => {
  if (c.type === 2) {
    const text = `${(Number(c.discountRate ?? 1) * 10).toFixed(1)} 折`
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

const statusText = (status) => {
  const map = { 0: '未使用', 1: '已使用', 2: '已作废' }
  return map[status] ?? '未使用'
}

const statusBadge = (status) => {
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
