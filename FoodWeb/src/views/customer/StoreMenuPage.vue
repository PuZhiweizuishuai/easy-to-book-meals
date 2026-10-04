<template>
  <div class="min-h-screen bg-base-200 pb-24">
    <!-- 顶部导航 -->
    <header class="navbar bg-base-100 shadow-sm sticky top-0 z-30 px-4">
      <div class="flex-1">
        <router-link to="/" class="btn btn-ghost btn-sm btn-square">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </router-link>
      </div>
      <div class="flex-none gap-2">
        <router-link v-if="authStore.isLoggedIn" to="/my/orders" class="btn btn-ghost btn-sm">我的订单</router-link>
        <router-link v-else to="/login" class="btn btn-primary btn-sm">登录</router-link>
      </div>
    </header>

    <!-- 门店信息头部 -->
    <div class="bg-base-100 shadow-sm">
      <div class="container mx-auto px-4 py-5 max-w-6xl">
        <div class="flex items-center gap-4">
          <div class="avatar">
            <div class="w-16 h-16 rounded-xl bg-base-200">
              <img v-if="store?.logoUrl" :src="store.logoUrl" :alt="store.storeName" class="w-full h-full object-cover rounded-xl" />
              <div v-else class="w-full h-full flex items-center justify-center text-3xl">🍽️</div>
            </div>
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <h1 class="text-xl font-bold truncate">{{ store?.storeName || '加载中...' }}</h1>
              <span :class="['badge badge-sm', store?.status === 0 ? 'badge-success' : 'badge-info']">
                {{ store?.status === 0 ? '营业中' : '休息中' }}
              </span>
            </div>
            <p class="text-sm text-base-content/60 mt-1 truncate">
              {{ store?.description || '暂无简介' }}
            </p>
            <div class="flex items-center gap-3 text-xs text-base-content/50 mt-1">
              <span v-if="store?.businessHours">营业时间：{{ store.businessHours }}</span>
              <span v-if="store?.address">{{ store.address }}</span>
            </div>
          </div>
        </div>
        <!-- 扫码餐桌提示 -->
        <div v-if="table" class="mt-3 alert alert-info py-2 px-3 text-sm">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3v18h18M7 16m4 0m4 0m-8-4a4 4 0 11-8 0 4 4 0 018 0zm0 0v1a3 3 0 006 0v-1m-6-4a4 4 0 014-4h2a4 4 0 014 4" />
          </svg>
          <span>扫码点餐 · {{ table.tableNumber }}号桌（{{ table.capacity }}人桌），下单后将自动绑定此餐桌</span>
        </div>
      </div>

      <!-- 本桌已下单菜品（扫码刷新后展示，支持继续加菜） -->
      <div v-if="currentTableOrder" class="container mx-auto px-4 max-w-6xl mt-3">
        <div class="card bg-base-100 shadow-sm border border-primary/30">
          <div class="card-body p-4">
            <div class="flex items-center justify-between mb-2">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="badge badge-primary badge-sm">本桌订单</span>
                <span class="font-mono text-xs text-base-content/60">{{ currentTableOrder.orderNo }}</span>
              </div>
              <span class="badge badge-sm badge-ghost">{{ guestStatusText(currentTableOrder.status) }}</span>
            </div>
            <div class="space-y-1.5">
              <div v-for="it in currentTableOrder.items" :key="it.id"
                   class="flex items-center gap-2 text-sm"
                   :class="{ 'opacity-50': it.status === 1 }">
                <span class="min-w-0 flex-1 truncate">{{ it.dishName }}<span class="text-base-content/40" v-if="specNamesText(it.specNames)">（{{ specNamesText(it.specNames) }}）</span></span>

                <!-- 出餐状态：固定宽度，保证多行纵向对齐 -->
                <span v-if="it.status === 1"
                      class="badge badge-error badge-xs sm:badge-sm w-16 shrink-0 gap-1">
                  已退菜
                </span>
                <span v-else
                      :class="['badge badge-xs sm:badge-sm w-16 shrink-0 gap-1', it.served === 1 ? 'badge-success' : 'badge-ghost']">
              
                  {{ it.served === 1 ? '已出餐' : '未出餐' }}
                </span>

                <span class="w-8 shrink-0 text-right text-base-content/60">×{{ it.quantity }}</span>
                <span class="w-16 shrink-0 text-right font-medium text-primary">¥{{ (Number(it.dealPrice) * it.quantity).toFixed(2) }}</span>
              </div>
            </div>
            <div class="flex items-center justify-between mt-3 pt-2 border-t border-base-200">
              <span class="text-sm text-base-content/60">合计</span>
              <span class="font-bold text-primary">¥{{ currentTableOrder.payAmount }}</span>
            </div>
            <p class="text-xs text-base-content/40 mt-1">继续点菜加入购物车，提交将自动追加到本桌订单</p>
          </div>
        </div>
      </div>

      <div class="container mx-auto px-4 py-5 max-w-6xl"><br/></div>

      <!-- 分类页签 -->
      <div class="container mx-auto px-4 max-w-6xl">
        <div class="flex gap-2 overflow-x-auto pb-3 pt-1">
          <button
            v-for="cat in categories"
            :key="cat.id ?? 'all'"
            :class="['btn btn-sm shrink-0', activeCategory === cat.id ? 'btn-primary' : 'btn-ghost bg-base-200']"
            @click="activeCategory = cat.id"
          >
            {{ cat.name }}
          </button>
        </div>
      </div>
    </div>

    <!-- 菜品列表 -->
    <main class="container mx-auto px-4 py-5 max-w-6xl">
      <div v-if="loading" class="flex justify-center py-24">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>

      <div v-else-if="filteredDishes.length === 0" class="card bg-base-100 shadow-sm">
        <div class="card-body items-center py-20 text-center">
          <span class="text-5xl mb-3">📋</span>
          <p class="text-base-content/50">该分类暂无菜品</p>
        </div>
      </div>

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="dish in filteredDishes"
          :key="dish.id"
          class="card bg-base-100 shadow-sm border border-base-200 hover:shadow-md transition-shadow cursor-pointer"
          @click="openDishDetail(dish)"
        >
          <figure class="h-40 bg-base-200 overflow-hidden relative">
            <img v-if="dish.topImage" :src="dish.topImage" :alt="dish.name" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-5xl">🍜</div>
            <span v-if="dish.stock !== -1 && dish.stock <= 0" class="absolute inset-0 bg-base-300/70 flex items-center justify-center text-lg font-bold text-base-content/70">已售罄</span>
          </figure>
          <div class="card-body p-4">
            <h2 class="card-title text-base">{{ dish.name }}</h2>
            <p class="text-sm text-base-content/60 line-clamp-2 min-h-10">{{ dish.description || '暂无描述' }}</p>
            <div class="flex items-center justify-between">
              <div>
                <span class="text-lg font-bold text-primary">¥{{ dish.basePrice }}</span>
                <span class="text-xs text-base-content/40 ml-1">起</span>
                <span v-if="Number(dish.monthlySales) > 0" class="text-xs text-base-content/40 ml-2">月售{{ dish.monthlySales }}</span>
              </div>
              <button class="btn btn-primary btn-sm btn-circle" @click.stop="openDishDetail(dish)">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 门店评价 -->
      <div class="mt-8">
        <h2 class="text-lg font-bold mb-3">门店评价</h2>
        <div v-if="reviews.length === 0" class="card bg-base-100 shadow-sm">
          <div class="card-body py-8 text-center text-base-content/40">暂无评价</div>
        </div>
        <div v-else class="space-y-3">
          <div v-for="review in reviews" :key="review.id" class="card bg-base-100 shadow-sm border border-base-200">
            <div class="card-body p-4">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-2">
                  <div class="avatar placeholder">
                    <div class="bg-neutral text-neutral-content rounded-full w-8">
                      <span class="text-xs">{{ (review.userNickname || '匿')[0] }}</span>
                    </div>
                  </div>
                  <span class="text-sm font-medium">{{ review.userNickname || '匿名用户' }}</span>
                </div>
                <div class="rating rating-sm pointer-events-none">
                  <input v-for="i in 5" :key="i" type="radio" class="mask mask-star-2 bg-warning" :checked="review.rating === i" />
                </div>
              </div>
              <p class="text-sm text-base-content/80">{{ review.content }}</p>
              <div v-if="review.merchantReply" class="bg-base-200 rounded-lg p-3 text-sm mt-1">
                <span class="font-medium text-primary">商家回复：</span>{{ review.merchantReply }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 购物车底部栏 -->
    <div v-if="cart.totalCount > 0" class="fixed bottom-0 inset-x-0 z-40 p-3">
      <div class="container mx-auto max-w-6xl">
        <div class="navbar bg-base-100 rounded-2xl shadow-lg border border-base-200 px-4 cursor-pointer" @click="showCartDrawer = true">
          <div class="flex-1 flex items-center gap-3">
            <div class="indicator">
              <span class="indicator-item badge badge-primary">{{ cart.totalCount }}</span>
              <div class="btn btn-circle btn-primary pointer-events-none">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
              </div>
            </div>
            <div>
              <div class="font-bold text-primary">¥{{ cart.totalPrice.toFixed(2) }}</div>
              <div class="text-xs text-base-content/50">已选 {{ cart.totalCount }} 件 · 点击查看购物车</div>
            </div>
          </div>
          <button class="btn btn-primary" @click.stop="openCheckout">去结算</button>
        </div>
      </div>
    </div>

    <!-- 购物车抽屉 -->
    <dialog class="modal" :class="{ 'modal-open': showCartDrawer }">
      <div class="modal-box max-w-md">
        <div class="flex items-center justify-between mb-3">
          <h3 class="font-bold text-lg">购物车</h3>
          <button v-if="cart.items.length > 0" class="btn btn-ghost btn-xs text-error" @click="cart.clear()">清空</button>
        </div>
        <div v-if="cart.items.length === 0" class="py-12 text-center text-base-content/40">
          <span class="text-5xl block mb-2">🛒</span>
          购物车空空如也
        </div>
        <div v-else class="space-y-2 max-h-[60vh] overflow-y-auto">
          <div v-for="item in cart.items" :key="item.skuId" class="flex items-center gap-3 bg-base-200 rounded-xl p-3">
            <div class="avatar">
              <div class="w-12 h-12 rounded bg-base-300">
                <img v-if="item.topImage" :src="item.topImage" :alt="item.dishName" class="w-full h-full object-cover rounded" />
                <div v-else class="w-full h-full flex items-center justify-center text-2xl">🍜</div>
              </div>
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-sm font-medium truncate">{{ item.dishName }}</div>
              <div class="text-xs text-base-content/50">{{ item.specNamesText || '标准' }}</div>
              <div class="text-sm font-bold text-primary mt-0.5">¥{{ Number(item.dealPrice ?? item.unitPrice).toFixed(2) }}</div>
            </div>
            <div class="flex items-center gap-1">
              <button class="btn btn-ghost btn-xs btn-circle" @click="cart.changeQuantity(item.skuId, -1)">−</button>
              <span class="w-6 text-center text-sm font-medium">{{ item.quantity }}</span>
              <button class="btn btn-ghost btn-xs btn-circle" @click="cart.changeQuantity(item.skuId, 1)">+</button>
            </div>
          </div>
        </div>
        <div v-if="cart.items.length > 0" class="mt-3 flex items-center justify-between">
          <span class="text-sm text-base-content/60">合计 {{ cart.totalCount }} 件</span>
          <span class="text-xl font-bold text-primary">¥{{ cart.totalPrice.toFixed(2) }}</span>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="showCartDrawer = false">继续点餐</button>
          <button v-if="cart.items.length > 0" class="btn btn-primary" @click="showCartDrawer = false; openCheckout()">去结算</button>
        </div>
      </div>
      <div class="modal-backdrop" @click="showCartDrawer = false"></div>
    </dialog>

    <!-- 菜品详情弹窗（SKU 选择） -->
    <dialog class="modal" :class="{ 'modal-open': detailDish }">
      <div class="modal-box max-w-lg">
        <template v-if="detailDish">
          <!-- 主图 + 缩略图画廊 -->
          <figure class="h-44 rounded-xl bg-base-200 overflow-hidden mb-2">
            <img v-if="mainImage" :src="mainImage" :alt="detailDish.name" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-6xl">🍜</div>
          </figure>
          <!-- 详情图片缩略图列表 -->
          <div v-if="dishGallery.length > 1" class="flex gap-2 mb-3 overflow-x-auto pb-1">
            <button
              v-for="(img, idx) in dishGallery"
              :key="idx"
              class="flex-shrink-0 w-16 h-16 rounded-lg overflow-hidden border-2 transition-all"
              :class="mainImage === img ? 'border-primary' : 'border-base-300 hover:border-primary/50'"
              @click="mainImage = img"
            >
              <img :src="img" :alt="`${detailDish.name} ${idx + 1}`" class="w-full h-full object-cover" />
            </button>
          </div>

          <h3 class="font-bold text-xl">{{ detailDish.name }}</h3>
          <p class="text-sm text-base-content/60 mt-1">{{ detailDish.description || '暂无描述' }}</p>

          <div class="divider my-3 text-sm">选择规格</div>
          <div v-if="detailLoading" class="flex justify-center py-6">
            <span class="loading loading-spinner text-primary"></span>
          </div>
          <div v-else-if="specGroups.length === 0 && skus.length === 0" class="text-center py-6 text-base-content/40">
            该菜品暂无可选规格
          </div>
          <div v-else class="space-y-4">
            <!-- 分组单选规格选择 -->
            <div v-for="group in specGroups" :key="group.id" class="space-y-2">
              <div class="text-sm font-medium text-base-content/70">{{ group.name }}</div>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="val in (group.values || [])"
                  :key="val.id"
                  class="badge badge-lg cursor-pointer transition-all"
                  :class="selectedSpec[group.id] === val.id ? 'badge-primary' : 'badge-ghost border border-base-300'"
                  @click="selectSpec(group.id, val.id)"
                >{{ val.name }}</button>
              </div>
            </div>

            <!-- 无规格菜品的价格展示 -->
            <div v-if="specGroups.length === 0 && matchedSku" class="bg-base-200 rounded-xl px-4 py-3 flex items-center justify-between">
              <div class="text-sm text-base-content/60">
                <span v-if="matchedSku.stock === -1">库存充足</span>
                <span v-else>剩余 {{ matchedSku.stock }}</span>
              </div>
              <div class="text-lg font-bold text-primary">¥{{ currentPrice != null ? currentPrice.toFixed(2) : '—' }}</div>
            </div>
          </div>

          <!-- 底部加购栏 -->
          <div v-if="!detailLoading && (specGroups.length > 0 || skus.length > 0)" class="mt-4">
            <div v-if="matchedSku" class="flex items-center justify-between bg-base-200 rounded-xl px-4 py-3 mb-3">
              <div class="min-w-0">
                <div class="text-sm text-base-content/60">
                  <span v-if="matchedSku.stock === -1">库存充足</span>
                  <span v-else-if="matchedSku.stock <= 0" class="text-error">已售罄</span>
                  <span v-else>剩余 {{ matchedSku.stock }}</span>
                </div>
                <div class="flex items-baseline gap-2">
                  <span class="text-2xl font-bold text-primary">¥{{ currentPrice != null ? currentPrice.toFixed(2) : '—' }}</span>
                  <span v-if="Number(matchedSku.discount) > 0 && Number(matchedSku.discount) < 1" class="text-xs text-base-content/40 line-through">¥{{ Number(matchedSku.unitPrice).toFixed(2) }}</span>
                </div>
              </div>
              <!-- 数量选择 -->
              <div class="flex items-center gap-2">
                <button class="btn btn-sm btn-circle" :disabled="selectedQuantity <= 1" @click="selectedQuantity--">−</button>
                <span class="w-6 text-center font-medium">{{ selectedQuantity }}</span>
                <button class="btn btn-sm btn-circle" @click="selectedQuantity++">+</button>
              </div>
            </div>
            <div class="modal-action">
              <button class="btn btn-ghost" @click="closeDishDetail">关闭</button>
              <button
                v-if="matchedSku"
                class="btn btn-primary"
                :disabled="matchedSku.stock !== -1 && matchedSku.stock <= 0"
                @click="addSkuToCart(matchedSku, selectedQuantity)"
              >加入购物车</button>
            </div>
          </div>
        </template>
      </div>
      <div class="modal-backdrop" @click="closeDishDetail"></div>
    </dialog>

    <!-- 结算弹窗 -->
    <dialog class="modal" :class="{ 'modal-open': showCheckout }">
      <div class="modal-box max-w-md">
        <h3 class="font-bold text-lg mb-4">确认订单</h3>
        <div class="space-y-3">
          <div class="bg-base-200 rounded-xl p-4 space-y-1">
            <div v-for="item in cart.items" :key="item.skuId" class="flex justify-between text-sm">
              <span class="truncate mr-2">{{ item.dishName }}（{{ item.specNamesText || '标准' }}）× {{ item.quantity }}</span>
              <span class="shrink-0">¥{{ (Number(item.dealPrice ?? item.unitPrice) * item.quantity).toFixed(2) }}</span>
            </div>
            <div class="divider my-1"></div>
            <div class="flex justify-between font-bold">
              <span>合计</span>
              <span class="text-primary">¥{{ cart.totalPrice.toFixed(2) }}</span>
            </div>
          </div>
          <div v-if="table" class="text-sm text-base-content/60">
            就餐桌：<span class="badge badge-primary badge-sm">{{ table.tableNumber }}号桌</span>
            <span class="ml-1 text-base-content/40">（堂食扫码，免登录下单）</span>
          </div>
          <div v-else class="text-sm text-base-content/60">
            订单类型：<span class="badge badge-info badge-sm">线上订单</span>
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">备注（如：少辣、不要香菜）</span></label>
            <textarea v-model="remark" class="textarea textarea-bordered w-full" rows="2" maxlength="255"></textarea>
          </div>
          <p class="text-xs text-base-content/40">堂食点餐为到店支付，下单后请等待店家接单{{ table ? '，可在本页继续扫码加菜' : '' }}。</p>
        </div>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="showCheckout = false" :disabled="submitting">取消</button>
          <button class="btn btn-primary" @click="submitOrder" :disabled="submitting">
            <span v-if="submitting" class="loading loading-spinner loading-sm"></span>
            {{ submitting ? '提交中...' : '提交订单' }}
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="!submitting && (showCheckout = false)"></div>
    </dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { httpGet, httpPost, isSuccess, parsePage } from '@/utils/fetch'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import toast from '@/utils/toast'
import { orderStatusText as guestStatusText } from '@/utils/order'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const cart = useCartStore()

const storeId = Number(route.params.id)
/** 扫码点餐：?table=二维码标识 */
const tableQrKey = route.query.table || null

const store = ref(null)
const dishes = ref([])
const reviews = ref([])
const table = ref(null)
const loading = ref(true)
const activeCategory = ref(null)

const detailDish = ref(null)
const detailLoading = ref(false)
const skus = ref([])
const specGroups = ref([])
const mainImage = ref('')

const dishGallery = computed(() => {
  const dish = detailDish.value
  if (!dish) return []
  const images = []
  if (dish.topImage) images.push(dish.topImage)
  if (dish.imageList) {
    try {
      const extra = typeof dish.imageList === 'string' ? JSON.parse(dish.imageList) : dish.imageList
      if (Array.isArray(extra)) images.push(...extra.filter(Boolean))
    } catch { /* ignore */ }
  }
  return images
})
const selectedSpec = ref({})  // { groupId: specValueId }
const selectedQuantity = ref(1)
const showCheckout = ref(false)
const showCartDrawer = ref(false)
const remark = ref('')
const submitting = ref(false)
/** 本桌当前未结算订单（扫码刷新后展示已下单菜品） */
const currentTableOrder = ref(null)

/** 分类列表（全部 + 菜品实际出现的分类） */
const categories = computed(() => {
  const map = new Map()
  for (const dish of dishes.value) {
    for (const tag of (dish.categoryIds || [])) {
      if (tag && tag.id && !map.has(tag.id)) {
        map.set(tag.id, tag)
      }
    }
  }
  return [{ id: null, name: '全部' }, ...map.values()]
})

/** 当前分类下的菜品 */
const filteredDishes = computed(() => {
  if (activeCategory.value === null) return dishes.value
  return dishes.value.filter(d =>
    (d.categoryIds || []).some(t => t && t.id === activeCategory.value)
  )
})

onMounted(async () => {
  await Promise.all([fetchStore(), fetchMenu(), fetchReviews()])
  if (tableQrKey) {
    await fetchTable()
    await fetchGuestCurrentOrder()
  }
  // 绑定购物车门店
  if (store.value) {
    cart.bindStore(storeId, store.value.storeName, tableQrKey)
  }
  loading.value = false
})

/** 查询本桌当前未结算订单（扫码刷新后展示已下单菜品） */
const fetchGuestCurrentOrder = async () => {
  if (!tableQrKey) return
  try {
    const res = await httpGet(`/order/guest-current?qrCodeKey=${encodeURIComponent(tableQrKey)}`)
    if (isSuccess(res)) {
      currentTableOrder.value = res.data || null
    } else {
      currentTableOrder.value = null
    }
  } catch (e) {
    currentTableOrder.value = null
  }
}

/** 加载门店信息 */
const fetchStore = async () => {
  try {
    const res = await httpGet(`/store/${storeId}`)
    if (isSuccess(res)) {
      store.value = res.data
    }
  } catch (e) {
    console.error('加载门店失败:', e)
  }
}

/** 加载门店菜单 */
const fetchMenu = async () => {
  try {
    const res = await httpGet(`/store/${storeId}/dishes`)
    if (isSuccess(res)) {
      dishes.value = res.data || []
    }
  } catch (e) {
    console.error('加载菜单失败:', e)
  }
}

/** 加载门店评价 */
const fetchReviews = async () => {
  try {
    const res = await httpGet(`/review/store/${storeId}?page=1&size=5`)
    if (isSuccess(res) && res.data) {
      reviews.value = parsePage(res.data).content
    }
  } catch (e) {
    // 评价加载失败不阻断点餐
    console.error('加载评价失败:', e)
  }
}

/** 扫码餐桌解析 */
const fetchTable = async () => {
  try {
    const res = await httpGet(`/dining-table/qr/${tableQrKey}`)
    if (isSuccess(res)) {
      // 校验餐桌归属当前门店
      if (res.data && Number(res.data.storefrontId) === storeId) {
        table.value = res.data
      }
    }
  } catch (e) {
    console.error('餐桌解析失败:', e)
  }
}

/** 打开菜品详情（加载规格组与 SKU） */
const openDishDetail = async (dish) => {
  detailDish.value = dish
  mainImage.value = dish.topImage || ''
  skus.value = []
  specGroups.value = []
  selectedSpec.value = {}
  selectedQuantity.value = 1
  detailLoading.value = true
  try {
    const res = await httpGet(`/dishes/${dish.id}`)
    if (isSuccess(res)) {
      // 合并完整菜品数据（含 imageList 详情图）
      if (res.data?.food) {
        detailDish.value = { ...dish, ...res.data.food }
        mainImage.value = res.data.food.topImage || ''
      }
      skus.value = (res.data?.skuList || []).filter(s => s.status === 1)
      specGroups.value = res.data?.specGroups || []
      // 单规格菜品自动选中第一项，方便顾客直接加购
      if (specGroups.value.length === 1) {
        const firstValues = specGroups.value[0].values || []
        if (firstValues.length > 0) {
          selectedSpec.value[specGroups.value[0].id] = firstValues[0].id
        }
      }
    }
  } catch (e) {
    console.error('加载菜品详情失败:', e)
  } finally {
    detailLoading.value = false
  }
}

const closeDishDetail = () => {
  detailDish.value = null
  mainImage.value = ''
}

/** 当前选中的规格组合是否完整（每组都选了），匹配到的 SKU */
const matchedSku = computed(() => {
  if (specGroups.value.length === 0) {
    // 无规格菜品：只有一个 SKU 或无 SKU
    return skus.value[0] || null
  }
  // 校验每组都已选
  for (const g of specGroups.value) {
    if (selectedSpec.value[g.id] == null) return null
  }
  // 收集选中的规格值ID（排序后匹配）
  const selectedIds = Object.values(selectedSpec.value).sort((a, b) => a - b)
  return skus.value.find(sku => {
    try {
      const ids = JSON.parse(sku.specs || '[]').sort((a, b) => a - b)
      return ids.length === selectedIds.length && ids.every((v, i) => v === selectedIds[i])
    } catch {
      return false
    }
  }) || null
})

/** 当前展示价格（折后） */
const currentPrice = computed(() => {
  const sku = matchedSku.value
  if (!sku) return null
  return dealPriceOf(sku)
})

/** 选择规格值 */
const selectSpec = (groupId, valueId) => {
  selectedSpec.value = { ...selectedSpec.value, [groupId]: valueId }
}

/** SKU 折后价 */
const dealPriceOf = (sku) => {
  const d = Number(sku.discount)
  if (d > 0 && d < 1) {
    return Number(sku.unitPrice) * d
  }
  return Number(sku.unitPrice)
}

/** 规格文本 */
const specNamesText = (specNames) => {
  if (!specNames) return ''
  try {
    const parsed = typeof specNames === 'string' ? JSON.parse(specNames) : specNames
    if (Array.isArray(parsed)) return parsed.join('/')
    if (parsed && typeof parsed === 'object') return Object.values(parsed).join('/')
  } catch { /* 忽略 */ }
  return String(specNames || '')
}

/** 加入购物车 */
const addSkuToCart = (sku, qty = 1) => {
  const deal = dealPriceOf(sku)
  cart.addItem({
    skuId: sku.id,
    dishId: detailDish.value.id,
    dishName: detailDish.value.name,
    topImage: detailDish.value.topImage,
    specNamesText: specNamesText(sku.specNames) || '标准',
    unitPrice: Number(sku.unitPrice),
    dealPrice: deal,
    quantity: qty
  })
  toast.success('已加入购物车')
  closeDishDetail()
}

/** 打开结算（扫码堂食免登录，其它场景需登录） */
const openCheckout = () => {
  if (cart.isDineInScan) {
    // 扫码点餐无需登录
    showCheckout.value = true
    return
  }
  if (!authStore.isLoggedIn) {
    toast.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  showCheckout.value = true
}

/** 提交订单（扫码堂食走免登录端点，其它走登录端点） */
const submitOrder = async () => {
  if (cart.items.length === 0) {
    toast.error('购物车为空')
    return
  }
  submitting.value = true
  try {
    const items = cart.toOrderItems()
    const isGuest = cart.isDineInScan

    // 扫码且本桌已有未结算订单 → 自动走加菜
    if (isGuest && currentTableOrder.value) {
      const url = `/order/guest-add-items/${currentTableOrder.value.id}?qrCodeKey=${encodeURIComponent(cart.tableQrCodeKey)}`
      const res = await httpPost(url, { items })
      if (isSuccess(res)) {
        showCheckout.value = false
        cart.clear()
        toast.success('加菜成功，等待店家处理')
        await fetchGuestCurrentOrder()
      } else {
        toast.error(res?.message || '加菜失败')
      }
      return
    }

    const payload = {
      storeId: cart.storeId,
      remark: remark.value || null,
      items
    }
    // 扫码堂食：提交二维码标识，走免登录端点
    if (isGuest) {
      payload.qrCodeKey = cart.tableQrCodeKey
      payload.orderType = 0 // 堂食
    }
    const url = isGuest ? '/order/guest-create' : '/order/create'
    const res = await httpPost(url, payload)
    if (isSuccess(res)) {
      showCheckout.value = false
      cart.clear()
      toast.success('下单成功，等待店家接单')
      // 免登录顾客：加载本桌订单展示，便于继续加菜；登录用户跳订单列表
      if (isGuest) {
        await fetchGuestCurrentOrder()
      } else {
        router.push('/my/orders')
      }
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
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
