<template>
  <div class="card bg-base-100 shadow-sm border border-base-200">
    <div class="card-body p-4 sm:p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-bold text-base flex items-center gap-2">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 text-warning" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          预定时间线
        </h3>
      </div>

      <!-- 日期筛选 -->
      <div class="flex flex-wrap items-center gap-2 mb-4">
        <button
          v-for="shortcut in dateShortcuts"
          :key="shortcut.label"
          :class="['btn btn-xs', selectedDate === shortcut.value ? 'btn-primary' : 'btn-ghost bg-base-200']"
          @click="selectDate(shortcut.value)"
        >{{ shortcut.label }}</button>
        <input
          type="date"
          class="input input-xs input-bordered w-auto"
          :value="selectedDate"
          @change="onDateChange($event.target.value)"
        />
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
                  <span class="font-mono">{{ formatDateTime(reservation.reservedStartTime) }} - {{ formatDateTime(reservation.reservedEndTime) }}</span>
                </div>
              </div>

              <div v-if="reservation.contactPhone" class="text-xs text-base-content/40 mt-1">
                {{ reservation.contactPhone }}
              </div>

              <!-- 展开详情 -->
              <div v-if="expandedId === reservation.id" class="mt-2 pt-2 border-t border-base-200 text-xs text-base-content/60 space-y-1">
                <div class="flex justify-between">
                  <span>预定编号</span>
                  <span class="font-mono">{{ reservation.id }}</span>
                </div>
                <div class="flex justify-between">
                  <span>餐桌ID</span>
                  <span class="font-mono">{{ reservation.tableId }}</span>
                </div>
                <div class="flex justify-between">
                  <span>开始时间</span>
                  <span class="font-mono">{{ formatFullDateTime(reservation.reservedStartTime) }}</span>
                </div>
                <div class="flex justify-between">
                  <span>结束时间</span>
                  <span class="font-mono">{{ formatFullDateTime(reservation.reservedEndTime) }}</span>
                </div>
                <div v-if="reservation.remark" class="flex justify-between">
                  <span>备注</span>
                  <span>{{ reservation.remark }}</span>
                </div>
                <div v-if="reservation.createTime" class="flex justify-between">
                  <span>创建时间</span>
                  <span class="font-mono">{{ formatFullDateTime(reservation.createTime) }}</span>
                </div>
              </div>

              <div class="flex gap-1 mt-2">
                <button
                  class="btn btn-xs btn-ghost"
                  @click="toggleDetail(reservation.id)"
                >{{ expandedId === reservation.id ? '收起' : '详情' }}</button>
                <template v-if="!isInactive(reservation.status)">
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
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="flex items-center justify-between mt-4 pt-3 border-t border-base-200">
        <span class="text-xs text-base-content/50">共 {{ totalElements }} 条</span>
        <div class="join">
          <button
            class="join-item btn btn-xs"
            :disabled="currentPage <= 1"
            @click="changePage(currentPage - 1)"
          >&laquo;</button>
          <button
            v-for="p in visiblePages"
            :key="p"
            :class="['join-item btn btn-xs', p === currentPage ? 'btn-primary' : '']"
            @click="changePage(p)"
          >{{ p }}</button>
          <button
            class="join-item btn btn-xs"
            :disabled="currentPage >= totalPages"
            @click="changePage(currentPage + 1)"
          >&raquo;</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  reservations: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  currentPage: { type: Number, default: 1 },
  totalPages: { type: Number, default: 1 },
  totalElements: { type: Number, default: 0 }
})

const emit = defineEmits(['confirm', 'seat', 'complete', 'cancel', 'dateChange', 'pageChange'])

const expandedId = ref(null)
const selectedDate = ref(getTodayStr())

function getTodayStr() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

function getYesterdayStr() {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function getTomorrowStr() {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const dateShortcuts = computed(() => [
  { label: '昨天', value: getYesterdayStr() },
  { label: '今天', value: getTodayStr() },
  { label: '明天', value: getTomorrowStr() }
])

const selectDate = (date) => {
  selectedDate.value = date
  emit('dateChange', date)
}

const onDateChange = (value) => {
  if (value) {
    selectedDate.value = value
    emit('dateChange', value)
  }
}

const toggleDetail = (id) => {
  expandedId.value = expandedId.value === id ? null : id
}

const changePage = (page) => {
  emit('pageChange', page)
}

const visiblePages = computed(() => {
  const pages = []
  const total = props.totalPages
  const current = props.currentPage
  let start = Math.max(1, current - 2)
  let end = Math.min(total, start + 4)
  if (end - start < 4) start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
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

const formatDateTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}/${m}/${d} ${h}:${min}`
}

const formatFullDateTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${y}/${m}/${d} ${h}:${min}:${s}`
}
</script>
