<template>
  <div>
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
      <div>
        <h2 class="text-2xl font-bold text-base-content">店铺管理</h2>
        <p class="text-sm text-base-content/60 mt-1">审核新店入驻申请，管理平台内所有门店</p>
      </div>
      <button class="btn btn-ghost btn-sm bg-base-100 border border-base-200" @click="fetchStores" :disabled="loading">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        刷新
      </button>
    </div>

    <div class="card bg-base-100 shadow-sm border border-base-200">
      <div class="card-body p-4 sm:p-6">
        <!-- 状态筛选 + 搜索 -->
        <div class="flex flex-col lg:flex-row gap-3 mb-4">
          <div class="flex flex-wrap gap-2 flex-1">
            <button
              v-for="tab in statusTabs"
              :key="String(tab.value)"
              :class="['btn btn-sm', activeStatus === tab.value ? 'btn-primary' : 'btn-ghost bg-base-200 border border-base-200']"
              @click="switchTab(tab.value)"
            >
              {{ tab.label }}
              <span v-if="tab.value === -1 && pendingCount > 0" class="badge badge-xs badge-error text-error-content">{{ pendingCount }}</span>
            </button>
          </div>
          <div class="flex gap-2">
            <label class="input input-bordered input-sm flex items-center gap-2 w-full sm:w-56">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" class="w-4 h-4 opacity-70">
                <path fill-rule="evenodd" d="M9.965 11.026a5 5 0 1 1 1.06-1.06l2.755 2.754a.75.75 0 1 1-1.06 1.06l-2.755-2.754ZM10.5 7a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0Z" clip-rule="evenodd" />
              </svg>
              <input v-model="searchKey" type="text" placeholder="搜索店名..." class="grow" @keyup.enter="handleSearch" />
            </label>
            <button class="btn btn-primary btn-sm" @click="handleSearch">搜索</button>
          </div>
        </div>

        <div v-if="loading" class="flex justify-center items-center py-20">
          <span class="loading loading-spinner loading-lg text-primary"></span>
        </div>

        <div v-else-if="stores.length === 0" class="text-center py-16">
          <p class="text-base-content/40 text-lg">{{ activeStatus === -1 ? '没有待审核的门店' : '暂无门店数据' }}</p>
        </div>

        <div v-else class="overflow-x-auto rounded-box border border-base-200">
          <table class="table table-zebra">
            <thead class="bg-base-200 text-base-content">
              <tr>
                <th>门店信息</th>
                <th>联系电话</th>
                <th>标签</th>
                <th>状态</th>
                <th>创建时间</th>
                <th class="text-right pr-4">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="store in stores" :key="store.id" class="hover">
                <td>
                  <div class="flex items-center gap-3">
                    <div class="avatar">
                      <div class="w-12 h-12 rounded-xl bg-base-300">
                        <img v-if="store.logoUrl" :src="store.logoUrl" :alt="store.storeName" class="w-full h-full object-cover rounded-xl" />
                      </div>
                    </div>
                    <div class="min-w-0">
                      <div class="font-bold">{{ store.storeName }}</div>
                      <div class="text-xs text-base-content/50 truncate max-w-52">{{ store.address }}</div>
                    </div>
                  </div>
                </td>
                <td class="text-sm">{{ store.phone }}</td>
                <td>
                  <div class="flex flex-wrap gap-1" v-if="store.tags && store.tags.length">
                    <span v-for="tag in store.tags.slice(0, 3)" :key="tag.id" class="badge badge-ghost badge-xs">{{ tag.tagName }}</span>
                  </div>
                  <span v-else class="text-base-content/30 text-xs">无</span>
                </td>
                <td><span :class="['badge badge-sm', statusBadge(store.status)]">{{ statusText(store.status) }}</span></td>
                <td class="text-sm">{{ formatTime(store.createTime) }}</td>
                <td class="text-right pr-4">
                  <div class="join">
                    <!-- 待审核：通过 / 拒绝 -->
                    <template v-if="store.status === -1">
                      <button class="btn btn-success btn-xs join-item" :disabled="acting" @click="audit(store, 0, '通过审核')">通过</button>
                      <button class="btn btn-error btn-xs join-item" :disabled="acting" @click="audit(store, 2, '拒绝入驻')">拒绝</button>
                    </template>
                    <!-- 营业中/休息中：停业 -->
                    <button v-if="store.status === 0 || store.status === 1" class="btn btn-warning btn-xs join-item" :disabled="acting" @click="audit(store, 2, '停业该门店')">停业</button>
                    <!-- 已停业：恢复营业 -->
                    <button v-if="store.status === 2" class="btn btn-success btn-xs join-item" :disabled="acting" @click="audit(store, 0, '恢复营业')">恢复营业</button>
                    <button class="btn btn-ghost btn-xs join-item text-error" :disabled="acting" @click="handleDelete(store)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <Pagination v-model="page" :total-pages="totalPages" @change="changePage" />
      </div>
    </div>

    <!-- 操作确认弹窗 -->
    <dialog class="modal" :class="{ 'modal-open': confirmAction }">
      <div class="modal-box max-w-sm">
        <h3 class="font-bold text-lg">{{ confirmAction?.title }}</h3>
        <p class="py-4 text-base-content/70">
          确定要对「{{ confirmAction?.store?.storeName }}」执行此操作吗？
          <span v-if="confirmAction?.hint" class="block text-xs text-base-content/50 mt-1">{{ confirmAction.hint }}</span>
        </p>
        <div class="modal-action">
          <button class="btn btn-ghost" @click="confirmAction = null" :disabled="acting">取消</button>
          <button :class="['btn', confirmAction?.btnClass]" @click="executeAudit" :disabled="acting">
            <span v-if="acting" class="loading loading-spinner loading-xs"></span>
            {{ acting ? '处理中...' : '确认' }}
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="!acting && (confirmAction = null)"></div>
    </dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { httpGet, httpPost, isSuccess, parsePage } from '@/utils/fetch'
import toast from '@/utils/toast'
import Pagination from '@/components/common/Pagination.vue'
import { useConfirm } from '@/composables/useConfirm'

const { confirm } = useConfirm()

const loading = ref(false)
const acting = ref(false)
const stores = ref([])
const page = ref(1)
const size = 10
const totalPages = ref(1)
const activeStatus = ref(null)
const searchKey = ref('')
/** 待审核数量角标（从全部列表页获取一次） */
const pendingCount = ref(0)
const confirmAction = ref(null)

/** 状态筛选页签（与后端 StoreStatus 对应） */
const statusTabs = [
  { value: null, label: '全部' },
  { value: -1, label: '待审核' },
  { value: 0, label: '营业中' },
  { value: 1, label: '休息中' },
  { value: 2, label: '已停业' },
]

onMounted(() => {
  fetchStores()
  fetchPendingCount()
})

/**
 * 加载门店列表（超管端，含待审核，按状态筛选由后端完成）
 */
const fetchStores = async () => {
  loading.value = true
  try {
    let url = `/sys/store/list?page=${page.value}&size=${size}`
    if (searchKey.value) {
      url += `&key=${encodeURIComponent(searchKey.value)}`
    }
    if (activeStatus.value !== null) {
      url += `&status=${activeStatus.value}`
    }
    const res = await httpGet(url)
    if (isSuccess(res) && res.data) {
      const p = parsePage(res.data)
      stores.value = p.content
      totalPages.value = p.totalPages
    } else {
      toast.error(res?.message || '获取门店列表失败')
    }
  } catch (e) {
    console.error('获取门店列表失败:', e)
    toast.error('获取门店列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 获取待审核数量（角标提示，利用后端状态过滤的 totalElements）
 */
const fetchPendingCount = async () => {
  try {
    const res = await httpGet('/sys/store/list?page=1&size=1&status=-1')
    if (isSuccess(res) && res.data) {
      pendingCount.value = parsePage(res.data).totalElements
    }
  } catch { /* 忽略 */ }
}

const switchTab = (status) => {
  activeStatus.value = status
  page.value = 1
  fetchStores()
}

const handleSearch = () => {
  page.value = 1
  fetchStores()
}

const changePage = (p) => {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  fetchStores()
}

/**
 * 发起审核/状态变更确认
 */
const audit = (store, status, title) => {
  const btnClass = status === 0 ? 'btn-success' : (status === 2 ? 'btn-error' : 'btn-primary')
  const hint = status === 0 ? '门店将对外展示，可正常营业接单' : (status === 2 ? '门店将被下架，顾客不可见，无法下单' : '')
  confirmAction.value = { store, status, title, btnClass, hint }
}

/**
 * 执行审核（POST /sys/store/audit）
 */
const executeAudit = async () => {
  if (!confirmAction.value) return
  const { store, status } = confirmAction.value
  acting.value = true
  try {
    const res = await httpPost(`/sys/store/audit?id=${store.id}&status=${status}`)
    if (isSuccess(res)) {
      toast.success('操作成功')
      confirmAction.value = null
      fetchStores()
      fetchPendingCount()
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    toast.error('操作失败，请稍后重试')
  } finally {
    acting.value = false
  }
}

/**
 * 删除门店（逻辑删除）
 */
const handleDelete = async (store) => {
  const ok = await confirm({
    title: '删除门店',
    message: `确定删除门店「${store.storeName}」吗？删除后顾客不可见，为逻辑删除。`,
    confirmText: '确认删除',
    tone: 'error'
  })
  if (!ok) return
  acting.value = true
  try {
    const res = await httpPost(`/sys/store/delete/${store.id}`)
    if (isSuccess(res)) {
      toast.success('门店已删除')
      fetchStores()
      fetchPendingCount()
    } else {
      toast.error(res?.message || '删除失败')
    }
  } catch (e) {
    if (e?.type !== 'UNAUTHORIZED') toast.error('删除失败')
  } finally {
    acting.value = false
  }
}

/** 状态展示 */
const statusText = (status) => {
  const map = { '-1': '待审核', 0: '营业中', 1: '休息中', 2: '已停业' }
  return map[status] ?? '未知'
}

const statusBadge = (status) => {
  const map = { '-1': 'badge-warning', 0: 'badge-success', 1: 'badge-info', 2: 'badge-error' }
  return map[status] ?? 'badge-ghost'
}

const formatTime = (ts) => {
  if (!ts) return '-'
  const d = new Date(Number(ts))
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
</script>
