<template>
  <!--
    菜品规格（SKU）选择弹窗
    协助下单、订单加菜等场景共用：按规格分组单选 → 匹配 SKU → 选数量 → 确认
    组件只负责「选什么、选多少」，具体加到哪里由父组件在 confirm 事件中决定
  -->
  <dialog class="modal" :class="{ 'modal-open': dish }">
    <div class="modal-box" :class="boxClass">
      <template v-if="dish">
        <!-- 头部扩展区：顾客端可插入菜品图册 -->
        <slot name="header" />
        <h3 class="font-bold text-lg">{{ dish.name }}</h3>
        <p v-if="dish.description" class="text-sm text-base-content/60 mt-1 line-clamp-2">{{ dish.description }}</p>

        <div class="divider my-3 text-sm">选择规格</div>

        <div v-if="loading" class="flex justify-center py-6">
          <span class="loading loading-spinner text-primary"></span>
        </div>

        <div v-else-if="specGroups.length === 0 && skus.length === 0" class="text-center py-6 text-base-content/40">
          该菜品暂无可选规格
        </div>

        <div v-else class="space-y-4">
          <!-- 分组单选规格 -->
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

          <!-- 无规格菜品：直接展示价格 -->
          <div v-if="specGroups.length === 0 && matchedSku"
               class="bg-base-200 rounded-xl px-4 py-3 flex items-center justify-between">
            <div class="text-sm text-base-content/60">
              <span v-if="matchedSku.stock === -1">库存充足</span>
              <span v-else>剩余 {{ matchedSku.stock }}</span>
            </div>
            <div class="text-lg font-bold text-primary">¥{{ currentPrice != null ? currentPrice.toFixed(2) : '—' }}</div>
          </div>
        </div>

        <!-- 底部：价格 + 数量 + 确认 -->
        <div v-if="!loading && (specGroups.length > 0 || skus.length > 0)" class="mt-4">
          <div v-if="matchedSku" class="flex items-center justify-between bg-base-200 rounded-xl px-4 py-3 mb-3">
            <div class="min-w-0">
              <div class="text-sm text-base-content/60">
                <span v-if="matchedSku.stock === -1">库存充足</span>
                <span v-else-if="matchedSku.stock <= 0" class="text-error">已售罄</span>
                <span v-else>剩余 {{ matchedSku.stock }}</span>
              </div>
              <div class="flex items-baseline gap-2">
                <span class="text-2xl font-bold text-primary">
                  ¥{{ currentPrice != null ? currentPrice.toFixed(2) : '—' }}
                </span>
                <span v-if="hasDiscount(matchedSku)" class="text-xs text-base-content/40 line-through">
                  ¥{{ Number(matchedSku.unitPrice).toFixed(2) }}
                </span>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <button class="btn btn-sm btn-circle" :disabled="quantity <= 1" @click="quantity--">−</button>
              <span class="w-6 text-center font-medium">{{ quantity }}</span>
              <button class="btn btn-sm btn-circle" :disabled="quantity >= 99" @click="quantity++">+</button>
            </div>
          </div>
          <div v-else-if="specGroups.length > 0" class="text-center text-sm text-base-content/40 py-3 mb-3">
            请选择完整规格组合
          </div>

          <div class="modal-action">
            <button class="btn btn-ghost" @click="emit('close')">关闭</button>
            <button
              v-if="matchedSku"
              class="btn btn-primary"
              :disabled="matchedSku.stock !== -1 && matchedSku.stock <= 0"
              @click="handleConfirm"
            >{{ confirmText }}</button>
          </div>
        </div>
      </template>
    </div>
    <div class="modal-backdrop" @click="emit('close')"></div>
  </dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { httpGet, isSuccess } from '@/utils/fetch'
import toast from '@/utils/toast'
import { formatSpecNames } from '@/utils/format'

const props = defineProps({
  /** 当前选择的菜品（null 表示弹窗关闭），需包含 id / name / description */
  dish: { type: Object, default: null },
  /** 确认按钮文案（协助下单为「加入已选菜品」，加菜为「加入加菜清单」，顾客端为「加入购物车」） */
  confirmText: { type: String, default: '加入' },
  /** 弹窗宽度类（顾客端菜品详情更宽，可放图册） */
  boxClass: { type: String, default: 'max-w-md' }
})

const emit = defineEmits(['close', 'confirm', 'loaded'])

const loading = ref(false)
const skus = ref([])
const specGroups = ref([])
/** { groupId: specValueId } */
const selectedSpec = ref({})
const quantity = ref(1)

/**
 * 监听「菜品ID」变化：打开时加载规格与 SKU，并重置选择
 * <p>
 * 注意：必须监听 id 而不是 dish 对象本身。
 * 顾客端会在 loaded 回调里把完整菜品数据合并回 dish（产生新的对象引用），
 * 若监听整个对象会形成「加载 → loaded → 替换引用 → 再加载」的死循环，
 * 表现为页面疯狂请求 /dishes/{id} 但规格始终不显示。
 */
watch(() => props.dish?.id, async (dishId) => {
  if (dishId == null) {
    return
  }
  skus.value = []
  specGroups.value = []
  selectedSpec.value = {}
  quantity.value = 1
  loading.value = true
  try {
    const res = await httpGet(`/dishes/${dishId}`)
    if (isSuccess(res)) {
      skus.value = (res.data?.skuList || []).filter(s => s.status === 1)
      specGroups.value = res.data?.specGroups || []
      // 单规格组自动选中第一项，方便直接加购
      if (specGroups.value.length === 1) {
        const firstValues = specGroups.value[0].values || []
        if (firstValues.length > 0) {
          selectedSpec.value[specGroups.value[0].id] = firstValues[0].id
        }
      }
      // 对外抛出原始响应，供调用方做额外处理（如顾客端取完整菜品图册）
      // 放在最后抛出：调用方此时已拿到完整 skus/specGroups，避免状态被中途重置
      emit('loaded', res.data)
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('加载规格失败')
  } finally {
    loading.value = false
  }
})

/** 当前选中的规格组合匹配到的 SKU（无规格菜品取唯一 SKU） */
const matchedSku = computed(() => {
  if (specGroups.value.length === 0) {
    return skus.value[0] || null
  }
  for (const g of specGroups.value) {
    if (selectedSpec.value[g.id] == null) return null
  }
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

/** 成交价（含 SKU 折扣） */
const currentPrice = computed(() => {
  const sku = matchedSku.value
  return sku ? dealPriceOf(sku) : null
})

const selectSpec = (groupId, valueId) => {
  selectedSpec.value = { ...selectedSpec.value, [groupId]: valueId }
}

const hasDiscount = (sku) => {
  const d = Number(sku?.discount)
  return d > 0 && d < 1
}

const dealPriceOf = (sku) => {
  return hasDiscount(sku) ? Number(sku.unitPrice) * Number(sku.discount) : Number(sku.unitPrice)
}

const handleConfirm = () => {
  const sku = matchedSku.value
  if (!sku) return
  emit('confirm', {
    sku,
    quantity: quantity.value,
    dealPrice: dealPriceOf(sku),
    specNamesText: formatSpecNames(sku.specNames) || '标准'
  })
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
