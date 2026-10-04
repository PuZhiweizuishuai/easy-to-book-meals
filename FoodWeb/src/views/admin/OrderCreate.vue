<template>
  <div>
    <div class="flex items-center gap-4 mb-6">
      <router-link :to="{ name: 'orders' }" class="btn btn-ghost btn-sm btn-square">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </router-link>
      <div>
        <h2 class="text-2xl font-bold text-base-content">协助下单</h2>
        <p class="text-sm text-base-content/60 mt-1">店员代客下单：选择菜品 → 选择餐桌/顾客信息 → 提交</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 左侧：菜品选择 -->
      <div class="lg:col-span-2">
        <div class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body p-4">
            <label class="input input-bordered flex items-center gap-2 w-full mb-3">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" class="w-4 h-4 opacity-70">
                <path fill-rule="evenodd" d="M9.965 11.026a5 5 0 1 1 1.06-1.06l2.755 2.754a.75.75 0 1 1-1.06 1.06l-2.755-2.754ZM10.5 7a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0Z" clip-rule="evenodd" />
              </svg>
              <input v-model="dishKey" type="text" placeholder="搜索菜品名称..." class="grow" @keyup.enter="fetchDishes" />
            </label>

            <div v-if="dishLoading" class="flex justify-center py-16">
              <span class="loading loading-spinner loading-lg text-primary"></span>
            </div>

            <div v-else-if="dishes.length === 0" class="text-center py-16 text-base-content/40">
              <p class="text-lg">暂无菜品</p>
            </div>

            <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-[60vh] overflow-y-auto pr-1">
              <div v-for="dish in dishes" :key="dish.id"
                   class="flex items-center gap-3 bg-base-200 rounded-xl p-3 cursor-pointer hover:bg-base-300 transition-colors"
                   @click="openSkuPicker(dish)">
                <div class="avatar">
                  <div class="w-14 h-14 rounded bg-base-300">
                    <img v-if="dish.topImage" :src="dish.topImage" :alt="dish.name" class="w-full h-full object-cover rounded" />
                    <div v-else class="w-full h-full flex items-center justify-center text-2xl">🍜</div>
                  </div>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="text-sm font-medium truncate">{{ dish.name }}</div>
                  <div class="text-xs text-base-content/50 line-clamp-1">{{ dish.description || '暂无描述' }}</div>
                  <div class="text-sm font-bold text-primary mt-0.5">¥{{ dish.basePrice }}<span class="text-xs text-base-content/40 ml-1">起</span></div>
                </div>
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-primary shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：下单信息 -->
      <div class="space-y-4">
        <!-- 订单配置 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body p-4 space-y-3">
            <h3 class="font-bold text-base">订单信息</h3>

            <!-- 订单类型 -->
            <div class="form-control">
              <label class="label py-1"><span class="label-text text-sm">订单类型</span></label>
              <div class="flex gap-2">
                <button v-for="t in orderTypes" :key="t.value"
                        :class="['btn btn-sm flex-1', orderType === t.value ? 'btn-primary' : 'btn-ghost border border-base-200']"
                        @click="changeOrderType(t.value)">
                  {{ t.label }}
                </button>
              </div>
            </div>

            <!-- 餐桌选择（堂食） -->
            <div v-if="orderType === 0" class="form-control">
              <label class="label py-1"><span class="label-text text-sm">选择餐桌 <span class="text-error">*</span></span></label>
              <select v-model="selectedTableId" class="select select-bordered select-sm w-full focus:select-primary">
                <option :value="null">请选择餐桌</option>
                <option v-for="t in tables" :key="t.id" :value="t.id">
                  {{ t.tableNumber }}号桌 · {{ t.area || '未分区' }} · {{ t.capacity }}人
                  <template v-if="t.status === 1"> · 已占用</template>
                </option>
              </select>
              <p class="text-xs text-base-content/40 mt-1">堂食订单需绑定餐桌，下单后餐桌自动置为占用</p>
            </div>

            <!-- 顾客信息 -->
            <div class="form-control">
              <label class="label py-1"><span class="label-text text-sm">顾客称呼</span></label>
              <input v-model="customerName" type="text" placeholder="如：到店顾客" maxlength="50"
                     class="input input-bordered input-sm w-full" />
            </div>

            <!-- 联系电话：外送必填用于配送；堂食选填，填写后结算可累积会员积分 -->
            <div class="form-control">
              <label class="label py-1">
                <span class="label-text text-sm">联系电话 <span v-if="orderType === 1" class="text-error">*</span></span>
              </label>
              <input v-model.trim="customerPhone" type="tel" inputmode="numeric" maxlength="11"
                     :placeholder="orderType === 1 ? '外送必填' : '选填，填写后结算可累积会员积分'"
                     class="input input-bordered input-sm w-full font-mono" />
              <label v-if="orderType !== 1" class="label py-0">
                <span class="label-text-alt text-base-content/40">选填，仅用于本店会员积分</span>
              </label>
            </div>

            <div v-if="orderType === 1" class="form-control">
              <label class="label py-1"><span class="label-text text-sm">收货地址 <span class="text-error">*</span></span></label>
              <textarea v-model="deliveryAddress" rows="2" placeholder="外送配送地址" maxlength="255"
                        class="textarea textarea-bordered textarea-sm w-full"></textarea>
            </div>

            <div class="form-control">
              <label class="label py-1"><span class="label-text text-sm">备注</span></label>
              <textarea v-model="remark" rows="2" placeholder="如：少辣、不要香菜" maxlength="255"
                        class="textarea textarea-bordered textarea-sm w-full"></textarea>
            </div>
          </div>
        </div>

        <!-- 已选菜品 -->
        <div class="card bg-base-100 shadow-sm border border-base-200 sticky top-4">
          <div class="card-body p-4">
            <div class="flex items-center justify-between">
              <h3 class="font-bold text-base">已选菜品</h3>
              <span class="badge badge-primary badge-sm">{{ totalCount }}件</span>
            </div>
            <div v-if="cartItems.length === 0" class="py-8 text-center text-base-content/40 text-sm">
              尚未选择菜品
            </div>
            <div v-else class="mt-2 space-y-1 max-h-[30vh] overflow-y-auto">
              <div v-for="(it, idx) in cartItems" :key="idx"
                   class="flex items-center justify-between bg-base-200 rounded-lg px-3 py-2 text-sm">
                <div class="min-w-0 mr-2">
                  <div class="truncate font-medium">{{ it.dishName }}</div>
                  <div class="text-xs text-base-content/50">{{ it.specNamesText }} · ¥{{ Number(it.dealPrice).toFixed(2) }}</div>
                </div>
                <div class="flex items-center gap-1 shrink-0">
                  <button class="btn btn-ghost btn-xs btn-circle" @click="changeQty(idx, -1)">−</button>
                  <span class="w-6 text-center">{{ it.quantity }}</span>
                  <button class="btn btn-ghost btn-xs btn-circle" @click="changeQty(idx, 1)">+</button>
                </div>
              </div>
            </div>
            <div v-if="cartItems.length > 0" class="mt-2 flex justify-between items-center border-t border-base-200 pt-2">
              <span class="text-sm text-base-content/60">合计</span>
              <span class="text-lg font-bold text-primary">¥{{ totalPrice.toFixed(2) }}</span>
            </div>

            <button class="btn btn-primary w-full mt-3" :disabled="submitting || cartItems.length === 0" @click="submitOrder">
              <span v-if="submitting" class="loading loading-spinner loading-sm"></span>
              {{ submitting ? '提交中...' : '提交订单' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 规格（SKU）选择弹窗（与订单加菜共用 SkuPickerModal：按规格分组单选 + 选数量） -->
    <SkuPickerModal
      :dish="skuPickerDish"
      confirm-text="加入已选菜品"
      @close="skuPickerDish = null"
      @confirm="handleSkuConfirm"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { httpGet, httpPost, isSuccess, parsePage } from '@/utils/fetch'
import { useStoreContext } from '@/stores/storeContext'
import { useAuthStore } from '@/stores/auth'
import toast from '@/utils/toast'
import SkuPickerModal from '@/components/dish/SkuPickerModal.vue'

const router = useRouter()
const storeContext = useStoreContext()
const authStore = useAuthStore()

const dishLoading = ref(false)
const dishes = ref([])
const dishKey = ref('')

const orderTypes = [
  { value: 0, label: '堂食' },
  { value: 1, label: '外送' },
  { value: 2, label: '线上' },
]
const orderType = ref(0)
const selectedTableId = ref(null)
const tables = ref([])
const customerName = ref('')
const customerPhone = ref('')
const deliveryAddress = ref('')
const remark = ref('')

const cartItems = ref([])
const submitting = ref(false)

/** 规格选择弹窗当前菜品（SKU 加载与规格匹配由 SkuPickerModal 内部完成） */
const skuPickerDish = ref(null)

const totalCount = computed(() =>
  cartItems.value.reduce((s, i) => s + i.quantity, 0)
)
const totalPrice = computed(() =>
  cartItems.value.reduce((s, i) => s + Number(i.dealPrice ?? i.unitPrice ?? 0) * i.quantity, 0)
)

onMounted(() => {
  fetchDishes()
  fetchTables()
})

const fetchDishes = async () => {
  if (!storeContext.currentStoreId) return
  dishLoading.value = true
  try {
    const res = await httpGet(`/dishes/list?storefrontId=${storeContext.currentStoreId}&page=1&size=100&key=${encodeURIComponent(dishKey.value || '')}`)
    if (isSuccess(res) && res.data) {
      dishes.value = parsePage(res.data).content.filter(d => d.status === 1)
    }
  } catch (e) {
    toast.error('加载菜品失败')
  } finally {
    dishLoading.value = false
  }
}

const fetchTables = async () => {
  if (!storeContext.currentStoreId) return
  try {
    const res = await httpGet(`/dining-table/list/all?storefrontId=${storeContext.currentStoreId}`)
    if (isSuccess(res) && res.data) {
      tables.value = res.data
    }
  } catch (e) {
    console.error('加载餐桌失败:', e)
  }
}

const changeOrderType = (t) => {
  orderType.value = t
  // 非堂食清空餐桌选择
  if (t !== 0) {
    selectedTableId.value = null
  }
}

/** 打开规格选择弹窗（SKU 数据由 SkuPickerModal 组件自行加载） */
const openSkuPicker = (dish) => {
  skuPickerDish.value = dish
}

/**
 * 规格选择完成：加入已选菜品并关闭弹窗
 * @param {{sku: object, quantity: number, dealPrice: number, specNamesText: string}} payload
 */
const handleSkuConfirm = ({ sku, quantity = 1, dealPrice, specNamesText }) => {
  if (!skuPickerDish.value) return
  const existing = cartItems.value.find(i => i.skuId === sku.id)
  if (existing) {
    existing.quantity = Math.min(existing.quantity + quantity, 99)
  } else {
    cartItems.value.push({
      skuId: sku.id,
      dishId: skuPickerDish.value.id,
      dishName: skuPickerDish.value.name,
      specNamesText: specNamesText || '标准',
      unitPrice: Number(sku.unitPrice),
      dealPrice: dealPrice ?? Number(sku.unitPrice),
      quantity
    })
  }
  toast.success('已加入')
  skuPickerDish.value = null
}

const changeQty = (idx, delta) => {
  const it = cartItems.value[idx]
  if (!it) return
  it.quantity += delta
  if (it.quantity <= 0) {
    cartItems.value.splice(idx, 1)
  }
}

const submitOrder = async () => {
  if (cartItems.value.length === 0) {
    toast.error('请先选择菜品')
    return
  }
  // 堂食必须选餐桌
  if (orderType.value === 0 && !selectedTableId.value) {
    toast.warning('堂食订单请选择餐桌')
    return
  }
  // 外送必填电话和地址
  if (orderType.value === 1) {
    if (!customerPhone.value) {
      toast.warning('外送订单请填写联系电话')
      return
    }
    if (!deliveryAddress.value) {
      toast.warning('外送订单请填写收货地址')
      return
    }
  }

  submitting.value = true
  try {
    const payload = {
      storeId: storeContext.currentStoreId,
      orderType: orderType.value,
      customerName: customerName.value || null,
      customerPhone: customerPhone.value || null,
      remark: remark.value || null,
      items: cartItems.value.map(i => ({ skuId: i.skuId, quantity: i.quantity }))
    }
    // 堂食绑定餐桌
    if (orderType.value === 0 && selectedTableId.value) {
      payload.tableId = selectedTableId.value
    }
    // 外送附带配送信息；联系电话无论何种订单类型都提交（用于配送 / 关联会员积分）
    if (customerPhone.value) {
      payload.contactPhone = customerPhone.value
    }
    if (orderType.value === 1) {
      payload.deliveryAddress = deliveryAddress.value
    }

    const res = await httpPost('/order/manage/staff-create', payload)
    if (isSuccess(res)) {
      toast.success('下单成功')
      cartItems.value = []
      router.push({ name: 'orders' })
    } else {
      toast.error(res?.message || '下单失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('下单失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
