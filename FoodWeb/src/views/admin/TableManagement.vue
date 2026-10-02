<template>
  <div>
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-base-content">餐桌管理</h2>
        <p class="text-sm text-base-content/60 mt-1">管理门店餐桌与预定信息</p>
      </div>
      <div class="flex gap-2 w-full sm:w-auto">
        <button class="btn btn-primary flex-1 sm:flex-none" @click="openAddModal">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          添加餐桌
        </button>
      </div>
    </div>

    <div class="flex flex-wrap gap-2 mb-6">
      <button
        v-for="filter in statusFilters"
        :key="filter.value"
        :class="['btn btn-sm', activeFilter === filter.value ? 'btn-primary' : 'btn-ghost bg-base-100 border border-base-200']"
        @click="setFilter(filter.value)"
      >
        <span v-if="filter.value !== null" class="w-2 h-2 rounded-full" :class="filter.dotClass"></span>
        {{ filter.label }}
        <span v-if="filter.count !== undefined" class="badge badge-xs" :class="activeFilter === filter.value ? 'badge-primary' : ''">
          {{ filter.count }}
        </span>
      </button>
    </div>

    <div v-if="loading" class="flex justify-center items-center py-20">
      <span class="loading loading-spinner loading-lg text-primary"></span>
    </div>

    <template v-else>
      <div v-if="filteredTables.length === 0" class="card bg-base-100 shadow-sm border border-base-200">
        <div class="card-body text-center py-16">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-16 w-16 mx-auto mb-4 text-base-content/20" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
          <p class="text-base-content/50 text-lg">暂无餐桌数据</p>
          <p class="text-base-content/30 text-sm mt-1">点击上方按钮添加第一个餐桌</p>
        </div>
      </div>

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 mb-8">
        <DiningTableCard
          v-for="table in filteredTables"
          :key="table.id"
          :table="table"
          @click="goToTableInfo"
          @edit="openEditModal"
          @reserve="openReserveModal"
          @delete="openDeleteConfirm"
          @clean="handleCleanComplete"
        />
      </div>
    </template>

    <ReservationTimeline
      :reservations="reservations"
      :loading="reservationLoading"
      :current-page="reservationPage"
      :total-pages="reservationTotalPages"
      :total-elements="reservationTotalElements"
      @confirm="handleConfirmReservation"
      @seat="handleSeatReservation"
      @complete="handleCompleteReservation"
      @cancel="handleCancelReservation"
      @dateChange="handleReservationDateChange"
      @pageChange="handleReservationPageChange"
    />

    <TableFormModal
      :show="showFormModal"
      :modal-type="formModalType"
      :table="currentTable"
      :areas="areaOptions"
      @close="closeFormModal"
      @submit="handleFormSubmit"
    />

    <ReservationModal
      :show="showReservationModal"
      :table="currentTable"
      @close="closeReservationModal"
      @submit="handleReservationSubmit"
    />

    <dialog class="modal" :class="{ 'modal-open': showDeleteModal }">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">确认删除</h3>
        <p class="text-base-content/70">确定要删除餐桌「{{ currentTable?.tableNumber }}」吗？此操作为逻辑删除，可恢复。</p>
        <div class="modal-action mt-6">
          <button type="button" class="btn btn-ghost" @click="showDeleteModal = false" :disabled="deleteSubmitting">取消</button>
          <button type="button" class="btn btn-error" @click="handleDelete" :disabled="deleteSubmitting">
            <span v-if="deleteSubmitting" class="loading loading-spinner loading-xs"></span>
            删除
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="!deleteSubmitting && (showDeleteModal = false)"></div>
    </dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { httpGet, httpPost, parsePage } from '@/utils/fetch.js'
import toast from '@/utils/toast.js'
import { useStoreContext } from '@/stores/storeContext.js'
import DiningTableCard from '@/components/card/DiningTableCard.vue'
import TableFormModal from '@/components/card/TableFormModal.vue'
import ReservationModal from '@/components/card/ReservationModal.vue'
import ReservationTimeline from '@/components/card/ReservationTimeline.vue'

const storeContext = useStoreContext()
/** 当前管理门店 ID（由 AdminLayout 门店切换器提供） */
const STORE_ID = computed(() => storeContext.currentStoreId)

function getTodayStr() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

function dateToDayRange(dateStr) {
  const parts = dateStr.split('-')
  const y = parseInt(parts[0])
  const m = parseInt(parts[1]) - 1
  const d = parseInt(parts[2])
  const start = new Date(y, m, d, 0, 0, 0, 0).getTime()
  const end = new Date(y, m, d + 1, 0, 0, 0, 0).getTime()
  return { start, end }
}

const router = useRouter()

const loading = ref(false)
const tables = ref([])
const reservations = ref([])
const reservationLoading = ref(false)
const reservationPage = ref(1)
const reservationTotalPages = ref(1)
const reservationTotalElements = ref(0)
const reservationDate = ref(getTodayStr())
const RESERVATION_PAGE_SIZE = 10

const activeFilter = ref(null)

const showFormModal = ref(false)
const formModalType = ref('add')
const currentTable = ref(null)

const showReservationModal = ref(false)
const showDeleteModal = ref(false)
const deleteSubmitting = ref(false)

const statusFilters = computed(() => {
  const counts = { 0: 0, 1: 0, 2: 0, 3: 0, 4: 0 }
  tables.value.forEach(t => {
    if (counts[t.status] !== undefined) counts[t.status]++
  })
  return [
    { value: null, label: '全部', dotClass: '', count: tables.value.length },
    { value: 0, label: '空闲', dotClass: 'bg-green-400', count: counts[0] },
    { value: 1, label: '占用', dotClass: 'bg-red-400', count: counts[1] },
    { value: 2, label: '预订', dotClass: 'bg-amber-400', count: counts[2] },
    { value: 3, label: '清理', dotClass: 'bg-blue-400', count: counts[3] },
    { value: 4, label: '停用', dotClass: 'bg-gray-400', count: counts[4] }
  ]
})

const filteredTables = computed(() => {
  if (activeFilter.value === null) return tables.value
  return tables.value.filter(t => t.status === activeFilter.value)
})

/**
 * 从已有餐桌数据聚合出区域候选列表（去重、去空），用于区域位置输入建议。
 * 区域位置由用户自由输入，不再写死；这里仅提供历史录入过的值作为快捷选择。
 */
const areaOptions = computed(() => {
  const set = new Set()
  tables.value.forEach(t => {
    if (t.area && String(t.area).trim()) {
      set.add(String(t.area).trim())
    }
  })
  return Array.from(set).sort()
})

onMounted(() => {
  if (STORE_ID.value) {
    fetchTables()
    fetchReservations()
  }
})

const fetchTables = async () => {
  if (!STORE_ID.value) return
  loading.value = true
  try {
    const res = await httpGet(`/dining-table/list/all?storefrontId=${STORE_ID.value}`)
    if (res && res.data) {
      tables.value = res.data
    }
  } catch (e) {
    console.error('获取餐桌列表失败:', e)
    toast.error('获取餐桌列表失败')
  } finally {
    loading.value = false
  }
}

const fetchReservations = async () => {
  if (!STORE_ID.value) return
  reservationLoading.value = true
  try {
    const { start, end } = dateToDayRange(reservationDate.value)
    const res = await httpGet(`/table-reservation/list/date?storeId=${STORE_ID.value}&startTime=${start}&endTime=${end}&page=${reservationPage.value}&size=${RESERVATION_PAGE_SIZE}`)
    if (res && res.data) {
      const p = parsePage(res.data)
      reservations.value = p.content
      reservationTotalPages.value = p.totalPages
      reservationTotalElements.value = p.totalElements
    }
  } catch (e) {
    console.error('获取预定列表失败:', e)
  } finally {
    reservationLoading.value = false
  }
}

const handleReservationDateChange = (date) => {
  reservationDate.value = date
  reservationPage.value = 1
  fetchReservations()
}

const handleReservationPageChange = (page) => {
  reservationPage.value = page
  fetchReservations()
}

const setFilter = (value) => {
  activeFilter.value = value
}

const goToTableInfo = (table) => {
  router.push({ name: 'TableInfo', params: { id: table.id } })
}

const openAddModal = () => {
  formModalType.value = 'add'
  currentTable.value = null
  showFormModal.value = true
}

const openEditModal = (table) => {
  formModalType.value = 'edit'
  currentTable.value = { ...table }
  showFormModal.value = true
}

const closeFormModal = () => {
  showFormModal.value = false
  currentTable.value = null
}

const handleFormSubmit = async (formData) => {
  try {
    const payload = { ...formData, storefrontId: STORE_ID.value }
    const url = formData.id ? '/dining-table/update' : '/dining-table/add'
    const res = await httpPost(url, payload)
    if (res && res.status === 200) {
      toast.success(formData.id ? '餐桌更新成功' : '餐桌添加成功')
      closeFormModal()
      fetchTables()
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    console.error('保存餐桌失败:', e)
    toast.error('保存餐桌失败')
  }
}

const openReserveModal = (table) => {
  currentTable.value = { ...table }
  showReservationModal.value = true
}

const closeReservationModal = () => {
  showReservationModal.value = false
  currentTable.value = null
}

const handleReservationSubmit = async (formData) => {
  if (formData._error) {
    toast.warning(formData._error)
    return
  }
  try {
    const res = await httpPost('/table-reservation/add', formData)
    if (res && res.status === 200) {
      toast.success('预定创建成功')
      closeReservationModal()
      fetchTables()
      fetchReservations()
    } else {
      toast.error(res?.message || '预定创建失败')
    }
  } catch (e) {
    console.error('创建预定失败:', e)
    toast.error('创建预定失败')
  }
}

const openDeleteConfirm = (table) => {
  currentTable.value = { ...table }
  showDeleteModal.value = true
}

const handleDelete = async () => {
  if (!currentTable.value) return
  deleteSubmitting.value = true
  try {
    const res = await httpPost(`/dining-table/delete/${currentTable.value.id}?storefrontId=${STORE_ID.value}`)
    if (res && res.status === 200) {
      toast.success('删除成功')
      showDeleteModal.value = false
      fetchTables()
    } else {
      toast.error(res?.message || '删除失败')
    }
  } catch (e) {
    console.error('删除餐桌失败:', e)
    toast.error('删除餐桌失败')
  } finally {
    deleteSubmitting.value = false
  }
}

const handleConfirmReservation = async (reservation) => {
  try {
    const res = await httpPost(`/table-reservation/confirm/${reservation.id}?storeId=${STORE_ID.value}`)
    if (res && res.status === 200) {
      toast.success('预定已确认')
      fetchReservations()
      fetchTables()
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    toast.error('操作失败')
  }
}

const handleSeatReservation = async (reservation) => {
  try {
    const res = await httpPost(`/table-reservation/seat/${reservation.id}?storeId=${STORE_ID.value}`)
    if (res && res.status === 200) {
      toast.success('已标记到店')
      fetchReservations()
      fetchTables()
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    toast.error('操作失败')
  }
}

const handleCompleteReservation = async (reservation) => {
  try {
    const res = await httpPost(`/table-reservation/complete/${reservation.id}?storeId=${STORE_ID.value}`)
    if (res && res.status === 200) {
      toast.success('就餐已完成')
      fetchReservations()
      fetchTables()
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    toast.error('操作失败')
  }
}

const handleCancelReservation = async (reservation) => {
  try {
    const res = await httpPost(`/table-reservation/cancel/${reservation.id}?storeId=${STORE_ID.value}`)
    if (res && res.status === 200) {
      toast.success('预定已取消')
      fetchReservations()
      fetchTables()
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    toast.error('操作失败')
  }
}

const handleCleanComplete = async (table) => {
  try {
    const res = await httpPost(`/dining-table/status?id=${table.id}&storefrontId=${STORE_ID.value}&status=0`)
    if (res && res.status === 200) {
      toast.success(`餐桌 ${table.tableNumber} 已清理完成`)
      fetchTables()
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    toast.error('操作失败')
  }
}
</script>
