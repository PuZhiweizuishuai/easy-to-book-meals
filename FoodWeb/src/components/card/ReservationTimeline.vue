<template>
  <div class="card bg-base-100 shadow-sm border border-base-200">
    <div class="card-body p-4 sm:p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-bold text-base flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-warning" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          今日预定时间线
        </h3>
        <span class="text-xs text-base-content/50">{{ todayLabel }}</span>
      </div>

      <div v-if="loading" class="flex justify-center py-8">
        <span class="loading loading-spinner loading-md text-primary"></span>
      </div>

      <div v-else-if="reservations.length === 0" class="text-center py-8 text-base-content/40">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mx-auto mb-2 opacity-30" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <p class="text-sm">暂无预定记录</p>
      </div>

      <div v-else class="space-y-0">
        <div class="relative">
          <div class="absolute left-[18px] top-0 bottom-0 w-0.5 bg-base-200"></div>

          <div
            v-for="(reservation, index) in reservations"
            :key="reservation.id"
            class="relative pl-10 pb-4 last:pb-0"
          >
            <div
              class="absolute left-[11px] top-1 w-4 h-4 rounded-full border-2 bg-base-100"
              :class="[getStatusDotClass(reservation.status)]"
            ></div>

            <div
              class="bg-base-200/50 border border-base-200 rounded-xl p-3 transition-all hover:shadow-sm"
              :class="{ 'opacity-60': isInactive(reservation.status) }"
            >
              <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div class="flex items-center gap-2 min-w-0">
                  <span class="font-bold text-sm">{{ reservation.contactName }}</span>
                  <span class="text-xs text-base-content/50">{{ reservation.guestCount }}人</span>
                  <div :class="['badge badge-xs', getStatusBadgeClass(reservation.status)]">
                    {{ getStatusLabel(reservation.status) }}
                  </div>
                </div>
                <div class="flex items-center gap-2 text-xs text-base-content/60">
                  <span class="font-mono">{{ formatTime(reservation.reservedStartTime) }} - {{ formatTime(reservation.reservedEndTime) }}</span>
                </div>
              </div>

              <div v-if="reservation.contactPhone" class="text-xs text-base-content/40 mt-1">
                {{ reservation.contactPhone }}
              </div>

              <div v-if="!isInactive(reservation.status)" class="flex gap-1 mt-2">
                <button
                  v-if="reservation.status === 0"
                  class="btn btn-xs btn-outline btn-success"
                  @click="$emit('confirm', reservation)"
                >确认</button>
                <button
                  v-if="reservation.status === 0 || reservation.status === 1"
                  class="btn btn-xs btn-outline btn-info"
                  @click="$emit('seat', reservation)"
                >到店</button>
                <button
                  v-if="reservation.status === 2"
                  class="btn btn-xs btn-outline btn-primary"
                  @click="$emit('complete', reservation)"
                >完成</button>
                <button
                  v-if="reservation.status === 0 || reservation.status === 1"
                  class="btn btn-xs btn-outline btn-error"
                  @click="$emit('cancel', reservation)"
                >取消</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatDate } from '@/utils/format.js'

const props = defineProps({
  reservations: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['confirm', 'seat', 'complete', 'cancel'])

const todayLabel = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}/${String(now.getMonth() + 1).padStart(2, '0')}/${String(now.getDate()).padStart(2, '0')}`
})

const STATUS_CONFIG = {
  0: { label: '待确认', badge: 'badge-warning', dot: 'border-warning bg-warning/30' },
  1: { label: '已确认', badge: 'badge-success', dot: 'border-success bg-success/30' },
  2: { label: '已到店', badge: 'badge-info', dot: 'border-info bg-info/30' },
  3: { label: '已取消', badge: 'badge-ghost', dot: 'border-base-300 bg-base-200' },
  4: { label: '未到店', badge: 'badge-error', dot: 'border-error bg-error/30' },
  5: { label: '已完成', badge: 'badge-primary', dot: 'border-primary bg-primary/30' }
}

const getStatusLabel = (status) => STATUS_CONFIG[status]?.label || '未知'
const getStatusBadgeClass = (status) => STATUS_CONFIG[status]?.badge || 'badge-ghost'
const getStatusDotClass = (status) => STATUS_CONFIG[status]?.dot || 'border-base-300 bg-base-200'

const isInactive = (status) => status === 3 || status === 4 || status === 5

const formatTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>
