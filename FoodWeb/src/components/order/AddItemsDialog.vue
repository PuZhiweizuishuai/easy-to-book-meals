<template>
  <!-- 加菜主弹窗：选择菜品 -->
  <dialog class="modal" :class="{ 'modal-open': api.order }">
    <div class="modal-box max-w-2xl">
      <template v-if="api.order">
        <h3 class="font-bold text-lg">{{ api.title }}</h3>
        <p class="text-sm text-base-content/60 mt-1">
          <span v-if="!showOrderNo" class="font-mono">{{ api.order.orderNo }}</span>
          <template v-else>订单号 <span class="font-mono">{{ api.order.orderNo }}</span> ·</template>
          当前 {{ api.itemCount(api.order) }} 件 · ¥{{ api.order.payAmount }}
        </p>
        <div class="divider my-3 text-sm">选择菜品</div>
        <div v-if="api.dishLoading" class="flex justify-center py-8">
          <span class="loading loading-spinner text-primary"></span>
        </div>
        <div v-else-if="api.dishes.length === 0" class="text-center py-8 text-base-content/40">
          {{ emptyText }}
        </div>
        <div v-else class="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-[40vh] overflow-y-auto">
          <div v-for="dish in api.dishes" :key="dish.id"
               class="flex items-center justify-between bg-base-200 rounded-xl px-3 py-2 cursor-pointer hover:bg-base-300"
               @click="api.openSkuPicker(dish)">
            <div class="min-w-0">
              <div class="text-sm font-medium truncate">{{ dish.name }}</div>
              <div class="text-xs text-base-content/50">¥{{ dish.basePrice }}起</div>
            </div>
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-primary shrink-0" fill="none"
                 viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
          </div>
        </div>

        <!-- 已选加菜清单 -->
        <div v-if="api.pendingItems.length > 0" class="mt-3 space-y-1">
          <div class="text-sm font-medium">待加菜品：</div>
          <div v-for="(it, idx) in api.pendingItems" :key="idx"
               class="flex items-center justify-between bg-base-100 border border-base-200 rounded px-3 py-1.5 text-sm">
            <span class="truncate mr-2">{{ it.dishName }}（{{ it.specNamesText || '标准' }}）</span>
            <div class="flex items-center gap-1">
              <button class="btn btn-ghost btn-xs btn-circle" @click="api.changePendingQty(idx, -1)">−</button>
              <span class="w-6 text-center">{{ it.quantity }}</span>
              <button class="btn btn-ghost btn-xs btn-circle" @click="api.changePendingQty(idx, 1)">+</button>
              <button class="btn btn-ghost btn-xs text-error" @click="api.removePending(idx)">✕</button>
            </div>
          </div>
        </div>

        <div class="modal-action">
          <button class="btn btn-ghost" @click="api.close()">取消</button>
          <button class="btn btn-primary"
                  :disabled="api.submitting || api.pendingItems.length === 0"
                  @click="api.submit()">
            <span v-if="api.submitting" class="loading loading-spinner loading-xs"></span>
            确认加菜
          </button>
        </div>
      </template>
    </div>
    <div class="modal-backdrop" @click="api.close()"></div>
  </dialog>

  <!-- 规格选择弹窗（与协助下单共用的 SkuPickerModal） -->
  <SkuPickerModal
    :dish="api.skuPickerDish"
    confirm-text="加入加菜清单"
    @close="api.closeSkuPicker()"
    @confirm="api.addPendingFromPicker"
  />
</template>

<script setup>
import SkuPickerModal from '@/components/dish/SkuPickerModal.vue'

/**
 * 加菜弹窗（菜品选择 + 规格选择）
 * 纯展示组件，所有状态与行为由 useAddItems composable 提供；
 * 规格选择复用通用组件 SkuPickerModal，避免与协助下单重复实现。
 */
defineProps({
  /** useAddItems() 返回的 reactive API 对象 */
  api: {
    type: Object,
    required: true
  },
  /** 无菜品时的空态文案（顾客端公开菜单为空时显示「暂无菜品」） */
  emptyText: {
    type: String,
    default: '暂无可选菜品'
  },
  /** 头部是否展示「订单号」前缀（管理端展示，顾客端历史样式不展示） */
  showOrderNo: {
    type: Boolean,
    default: true
  }
})
</script>
