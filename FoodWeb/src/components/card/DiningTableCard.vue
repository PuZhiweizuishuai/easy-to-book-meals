<template>
  <div
    class="card bg-base-100 border-2 transition-all duration-300 cursor-pointer hover:shadow-lg hover:-translate-y-0.5 active:scale-[0.98] rounded-2xl overflow-hidden"
    :class="[borderColorClass]"
    @click="$emit('click', table)"
  >
    <div class="card-body p-4 gap-2">
      <div class="flex justify-between items-start">
        <div class="flex items-center gap-2">
          <div class="w-10 h-10 rounded-xl flex items-center justify-center text-lg font-bold"
            :class="[bgColorClass, textColorClass]">
            {{ table.tableNumber?.charAt(0) || '#' }}
          </div>
          <div>
            <h3 class="font-bold text-base leading-tight">{{ table.tableNumber }}</h3>
            <p class="text-xs text-base-content/50">{{ table.area || '未分区' }}</p>
          </div>
        </div>
        <div :class="['badge badge-sm gap-1 font-medium', statusBadgeClass]">
          <span class="w-1.5 h-1.5 rounded-full" :class="[statusDotClass]"></span>
          {{ statusLabel }}
        </div>
      </div>

      <div class="flex items-center gap-3 mt-1 text-sm text-base-content/70">
        <span class="flex items-center gap-1">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          {{ table.capacity }}人
        </span>
        <span v-if="table.currentOrderId" class="flex items-center gap-1 text-warning">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          用餐中
        </span>
      </div>

      <div v-if="table.remark" class="text-xs text-base-content/40 truncate mt-1">
        {{ table.remark }}
      </div>

      <div class="flex gap-1 mt-2 pt-2 border-t border-base-200/50">
        <button class="btn btn-ghost btn-xs flex-1 text-info" @click.stop="$emit('edit', table)">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
          </svg>
          编辑
        </button>
        <button class="btn btn-ghost btn-xs flex-1 text-warning" @click.stop="$emit('reserve', table)">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          预定
        </button>
        <button class="btn btn-ghost btn-xs flex-1 text-error" @click.stop="$emit('delete', table)">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          删除
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  table: {
    type: Object,
    required: true
  }
})

defineEmits(['click', 'edit', 'reserve', 'delete'])

const STATUS_MAP = {
  0: { label: '空闲', badge: 'badge-success', dot: 'bg-green-400', border: 'border-success/30', bg: 'bg-success/10', text: 'text-success' },
  1: { label: '已占用', badge: 'badge-error', dot: 'bg-red-400', border: 'border-error/30', bg: 'bg-error/10', text: 'text-error' },
  2: { label: '已预订', badge: 'badge-warning', dot: 'bg-amber-400', border: 'border-warning/30', bg: 'bg-warning/10', text: 'text-warning' },
  3: { label: '清理中', badge: 'badge-info', dot: 'bg-blue-400', border: 'border-info/30', bg: 'bg-info/10', text: 'text-info' },
  4: { label: '停用', badge: 'badge-ghost', dot: 'bg-gray-400', border: 'border-base-300', bg: 'bg-base-200', text: 'text-base-content/40' }
}

const statusConfig = computed(() => {
  return STATUS_MAP[props.table.status] || STATUS_MAP[4]
})

const statusLabel = computed(() => statusConfig.value.label)
const statusBadgeClass = computed(() => statusConfig.value.badge)
const statusDotClass = computed(() => statusConfig.value.dot)
const borderColorClass = computed(() => statusConfig.value.border)
const bgColorClass = computed(() => statusConfig.value.bg)
const textColorClass = computed(() => statusConfig.value.text)
</script>
