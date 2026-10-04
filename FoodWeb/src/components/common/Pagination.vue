<template>
  <!-- 仅一页时不展示分页条 -->
  <div v-if="totalPages > 1" :class="['flex', alignClass, containerClass]">
    <div class="join shadow-sm">
      <button type="button" :class="['join-item btn', sizeClass]"
              :disabled="modelValue <= 1"
              @click="goPage(modelValue - 1)">«</button>

      <!-- 简洁模式：只显示 当前页/总页数 -->
      <button v-if="mode === 'simple'" type="button"
              :class="['join-item btn', sizeClass, 'btn-active no-animation']">
        第 {{ modelValue }} / {{ totalPages }} 页
      </button>

      <!-- 数字模式：页码窗口（总页数多时用 ... 折叠） -->
      <template v-else>
        <button v-for="p in visiblePages" :key="p" type="button"
                :class="['join-item btn', sizeClass,
                        { 'btn-active': p === modelValue, 'btn-disabled no-animation': p === '...' }]"
                @click="p !== '...' && goPage(p)">
          {{ p }}
        </button>
      </template>

      <button type="button" :class="['join-item btn', sizeClass]"
              :disabled="modelValue >= totalPages"
              @click="goPage(modelValue + 1)">»</button>
    </div>

    <div v-if="totalElements !== null" class="text-sm text-base-content/50 mt-2 w-full text-center md:text-right">
      共 {{ totalElements }} 条记录，第 {{ modelValue }} / {{ totalPages || 1 }} 页
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

/**
 * 通用分页组件（DaisyUI join 按钮组风格）
 *
 * 两种模式：
 * - simple：« 第 X / Y 页 »（适合移动端/列表弹窗，订单/会员/门店页在用）
 * - numbered：« 1 … 4 [5] 6 … 10 »（适合桌面端全量浏览，菜品/用户页在用）
 *
 * 页码约定：全部 1-based。父组件通过 v-model 绑定当前页，监听 @change 加载数据。
 */
const props = defineProps({
  /** 当前页（1-based），支持 v-model */
  modelValue: {
    type: Number,
    required: true
  },
  /** 总页数 */
  totalPages: {
    type: Number,
    required: true
  },
  /** 总记录数；传 null 不显示统计文案 */
  totalElements: {
    type: Number,
    default: null
  },
  /** simple=简洁翻页；numbered=数字页码 */
  mode: {
    type: String,
    default: 'simple',
    validator: (v) => ['simple', 'numbered'].includes(v)
  },
  /** 按钮尺寸 */
  size: {
    type: String,
    default: 'sm',
    validator: (v) => ['sm', 'xs'].includes(v)
  },
  /** 对齐方式 */
  align: {
    type: String,
    default: 'center',
    validator: (v) => ['center', 'end'].includes(v)
  },
  /** 追加在容器上的间距类（如 mt-6 / pt-2） */
  containerClass: {
    type: String,
    default: 'mt-4'
  },
  /** 数字模式下最多直接展示的页码数 */
  maxVisible: {
    type: Number,
    default: 7
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const sizeClass = computed(() => `btn-${props.size}`)
const alignClass = computed(() =>
  props.align === 'end' ? 'justify-center md:justify-end flex-wrap' : 'justify-center'
)

/** 数字模式的页码窗口：首尾页常驻，当前页附近各展开 1 页，其余以 ... 折叠 */
const visiblePages = computed(() => {
  const total = props.totalPages
  const current = props.modelValue
  if (total <= props.maxVisible) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }
  const pages = [1]
  if (current > 3) pages.push('...')
  for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) {
    pages.push(i)
  }
  if (current < total - 2) pages.push('...')
  pages.push(total)
  return pages
})

const goPage = (page) => {
  if (page === '...' || page < 1 || page > props.totalPages || page === props.modelValue) return
  emit('update:modelValue', page)
  emit('change', page)
}
</script>
