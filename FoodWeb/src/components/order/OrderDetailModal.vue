<template>
  <!--
    订单详情弹窗（订单管理、会员消费记录等场景共用）
    组件只负责展示与交互事件派发，具体业务（出餐/退菜/改量）由父组件处理
  -->
  <dialog class="modal" :class="{ 'modal-open': order }">
    <div class="modal-box max-w-2xl">
      <template v-if="order">
        <div class="flex items-start justify-between gap-2">
          <h3 class="font-bold text-lg">订单详情</h3>
          <button class="btn btn-ghost btn-xs btn-circle" @click="emit('close')">✕</button>
        </div>

        <div class="mt-4 space-y-1 text-sm">
          <p><span class="text-base-content/50">订单号：</span><span class="font-mono">{{ order.orderNo }}</span></p>
          <p>
            <span class="text-base-content/50">订单类型：</span>
            <span :class="['badge badge-sm', orderTypeBadge(order.orderType)]">{{ orderTypeText(order.orderType) }}</span>
          </p>
          <p>
            <span class="text-base-content/50">状态：</span>
            <span :class="['badge badge-sm', orderStatusBadge(displayStatus)]">{{ orderStatusText(displayStatus) }}</span>
            <span v-if="displayStatus !== order.status" class="text-xs text-base-content/40 ml-1">
              （按菜品出餐情况计）
            </span>
          </p>
          <p><span class="text-base-content/50">下单人：</span>{{ order.userNickname || ('用户#' + order.userId) }}</p>
          <p v-if="order.contactPhone"><span class="text-base-content/50">联系电话：</span><span class="font-mono">{{ order.contactPhone }}</span></p>
          <p v-if="order.tableNumber"><span class="text-base-content/50">桌号：</span>{{ order.tableNumber }}号桌</p>
          <p><span class="text-base-content/50">下单时间：</span>{{ formatTime(order.createTime) }}</p>
          <p v-if="order.remark"><span class="text-base-content/50">备注：</span>{{ order.remark }}</p>
          <p v-if="order.cancelReason"><span class="text-base-content/50">取消原因：</span>{{ order.cancelReason }}</p>
          <p v-if="order.couponCode">
            <span class="text-base-content/50">优惠券码：</span>
            <span class="font-mono text-xs">{{ order.couponCode }}</span>
          </p>
        </div>

        <div class="divider my-3">菜品明细</div>
        <div class="overflow-x-auto">
          <table class="table table-sm">
            <thead>
              <tr>
                <th>菜品</th>
                <th>规格</th>
                <th>单价</th>
                <th>数量</th>
                <th>小计</th>
                <th>出餐</th>
                <th v-if="editable" class="text-right pr-2">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in (order.items || [])" :key="item.id" :class="{ 'opacity-50': item.status === 1 }">
                <td>
                  <div class="flex items-center gap-2">
                    <div class="avatar">
                      <div class="w-9 h-9 rounded bg-base-200">
                        <img v-if="item.topImage" :src="item.topImage" :alt="item.dishName" class="w-full h-full object-cover rounded" />
                      </div>
                    </div>
                    <div class="flex flex-col">
                      <span class="flex items-center gap-1">
                        {{ item.dishName }}
                        <span v-if="item.addedLater === 1" class="badge badge-warning badge-xs" title="加菜追加">加</span>
                        <span v-if="item.status === 1" class="badge badge-error badge-xs">已退菜</span>
                      </span>
                    </div>
                  </div>
                </td>
                <td class="text-sm text-base-content/60">{{ formatSpecNames(item.specNames) || '标准' }}</td>
                <td>¥{{ item.dealPrice }}</td>
                <td>×{{ formatQty(item.quantity) }}</td>
                <td class="font-medium">¥{{ item.totalPrice }}</td>
                <td>
                  <span v-if="item.status === 1" class="text-base-content/40 text-xs">-</span>
                  <span v-else-if="item.served === 1" class="badge badge-success badge-sm">已出餐</span>
                  <!-- 出餐前置：订单必须先接单（待接单订单不允许出餐） -->
                  <button v-else-if="editable && canServe"
                          class="btn btn-warning btn-xs" :disabled="busy" @click="emit('serve', item)">出餐</button>
                  <span v-else-if="editable && !canServe"
                        class="badge badge-ghost badge-sm" title="订单接单后才能出餐">待接单</span>
                  <span v-else class="badge badge-ghost badge-sm">未出餐</span>
                </td>
                <td v-if="editable" class="text-right pr-2">
                  <div v-if="item.status !== 1" class="join">
                    <button class="btn btn-ghost btn-xs join-item" :disabled="busy" @click="emit('adjust', item)">改量</button>
                    <button class="btn btn-error btn-xs join-item" :disabled="busy" @click="emit('remove', item)">退菜</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mt-3 text-right space-y-1 text-sm">
          <p><span class="text-base-content/50">原价合计：</span>¥{{ order.totalAmount }}</p>
          <p v-if="Number(order.discountAmount) > 0">
            <span class="text-base-content/50">菜品优惠：</span><span class="text-success">-¥{{ order.discountAmount }}</span>
          </p>
          <p v-if="Number(order.couponAmount) > 0">
            <span class="text-base-content/50">优惠券：</span><span class="text-success">-¥{{ order.couponAmount }}</span>
          </p>
          <p v-if="Number(order.manualDiscountAmount) > 0">
            <span class="text-base-content/50">手动减免：</span><span class="text-success">-¥{{ order.manualDiscountAmount }}</span>
          </p>
          <p class="text-lg">
            <span class="text-base-content/50 font-normal">应付：</span>
            <span class="font-bold text-primary">¥{{ order.payAmount }}</span>
          </p>
        </div>

        <div class="modal-action">
          <button v-if="editable" class="btn btn-info btn-sm mr-auto" :disabled="busy" @click="emit('add', order)">
            加菜
          </button>
          <button class="btn btn-ghost" @click="emit('close')">关闭</button>
        </div>
      </template>
    </div>
    <div class="modal-backdrop" @click="emit('close')"></div>
  </dialog>
</template>

<script setup>
import { computed } from 'vue'
import { formatSpecNames } from '@/utils/format'
import {
  orderStatusText,
  orderStatusBadge,
  orderTypeText,
  orderTypeBadge
} from '@/utils/order'

const props = defineProps({
  /** 订单对象（null 表示弹窗关闭） */
  order: { type: Object, default: null },
  /** 是否展示可操作按钮（出餐/改量/退菜/加菜），只读场景传 false */
  editable: { type: Boolean, default: false },
  /** 操作进行中（禁用按钮防重复提交） */
  busy: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'serve', 'remove', 'adjust', 'add'])

/**
 * 有效订单状态：制作中/已出餐阶段以菜品出餐情况为准，
 * 全部正常菜品出餐才显示「已出餐」，否则显示「制作中」（如已出餐后又加菜）
 */
const displayStatus = computed(() => {
  if (!props.order) return 0
  const status = props.order.status
  if (status !== 2 && status !== 3) return status
  const items = props.order.items || []
  const allServed = items.length > 0 && items.every(i => i.status === 1 || i.served === 1)
  return allServed ? 3 : 2
})

/**
 * 是否允许对该订单的菜品出餐
 * <p>
 * 出餐前置条件：订单必须已接单（待接单订单不允许出餐）。
 * 仅已接单(1)/制作中(2)/已出餐(3，加菜后回退场景) 可出餐；
 * 待接单(0)、已完成(4)、已取消(5) 不允许。
 * 与后端 OrderService.serveOrderItem 的 PENDING 校验保持一致。
 */
const canServe = computed(() => {
  if (!props.order) return false
  const status = props.order.status
  return status === 1 || status === 2 || status === 3
})

/** 数量/重量展示格式化：去掉多余的小数尾零（1.600 -> 1.6，2.000 -> 2） */
const formatQty = (q) => {
  const n = Number(q)
  return Number.isFinite(n) ? String(n) : '0'
}

const formatTime = (ts) => {
  if (!ts) return '-'
  const d = new Date(Number(ts))
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>
