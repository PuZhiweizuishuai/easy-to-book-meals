<template>
  <div class="min-h-screen bg-base-200">
    <!-- 顶部导航 -->
    <header class="navbar bg-base-100 shadow-sm sticky top-0 z-30 px-4">
      <div class="flex-1">
        <router-link to="/" class="btn btn-ghost btn-sm btn-square">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </router-link>
        <span class="font-bold text-lg ml-1">消息通知</span>
      </div>
      <div class="flex-none">
        <button class="btn btn-ghost btn-sm" :disabled="!hasUnread" @click="handleReadAll">全部已读</button>
      </div>
    </header>

    <main class="container mx-auto px-4 py-5 max-w-3xl">
      <!-- 待处理邀请置顶提示 -->
      <div v-if="pendingInvites.length > 0" class="alert alert-info mb-4 shadow-sm">
        <span>你有 {{ pendingInvites.length }} 条门店邀请待处理，同意后即可获得对应门店的管理权限。</span>
      </div>

      <div v-if="loading" class="flex justify-center py-24">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>

      <div v-else-if="notifications.length === 0" class="card bg-base-100 shadow-sm">
        <div class="card-body items-center py-20 text-center">
          <span class="text-5xl mb-3">🔔</span>
          <p class="text-base-content/50">暂无消息通知</p>
        </div>
      </div>

      <div v-else class="space-y-3">
        <div v-for="n in notifications" :key="n.id"
             class="card bg-base-100 shadow-sm border"
             :class="cardTone(n)">
          <div class="card-body p-4">
            <div class="flex items-start justify-between gap-3">
              <div class="flex items-center gap-2 min-w-0">
                <span class="text-xl shrink-0">{{ iconOf(n) }}</span>
                <span class="font-semibold truncate">{{ n.title }}</span>
                <span v-if="n.status === 0" class="badge badge-error badge-sm shrink-0">待处理</span>
                <span v-else-if="n.type === 'STAFF_INVITE' && n.status === 1" class="badge badge-success badge-sm shrink-0">已同意</span>
                <span v-else-if="n.type === 'STAFF_INVITE' && n.status === 2" class="badge badge-ghost badge-sm shrink-0">已拒绝</span>
                <span v-else class="badge badge-ghost badge-sm shrink-0">已读</span>
              </div>
              <span class="text-xs text-base-content/40 whitespace-nowrap shrink-0">{{ timeText(n.createTime) }}</span>
            </div>

            <p class="text-sm text-base-content/70 mt-1">{{ n.content }}</p>

            <div v-if="n.inviterName" class="text-xs text-base-content/50 mt-1">
              发起人：{{ n.inviterName }}
            </div>

            <!-- 员工邀请操作 -->
            <div v-if="n.type === 'STAFF_INVITE' && n.status === 0" class="card-actions justify-end mt-3">
              <button class="btn btn-ghost btn-sm" :disabled="acting === n.id" @click="handleReject(n)">拒绝</button>
              <button class="btn btn-primary btn-sm" :disabled="acting === n.id" @click="handleAccept(n)">
                <span v-if="acting === n.id" class="loading loading-spinner loading-xs"></span>
                同意并加入
              </button>
            </div>

            <!-- 已处理：可删除 -->
            <div v-else class="card-actions justify-end mt-2">
              <button class="btn btn-ghost btn-xs text-base-content/40" @click="handleDelete(n)">删除</button>
            </div>
          </div>
        </div>

        <div v-if="hasMore" class="flex justify-center pt-2">
          <button class="btn btn-ghost btn-sm" :disabled="loadingMore" @click="loadMore">
            <span v-if="loadingMore" class="loading loading-spinner loading-xs"></span>
            加载更多
          </button>
        </div>
      </div>
    </main>

    <!-- 接受邀请成功后的引导 -->
    <dialog class="modal" :class="{ 'modal-open': showJoined }">
      <div class="modal-box max-w-sm text-center">
        <span class="text-5xl">🎉</span>
        <h3 class="font-bold text-lg mt-3">已加入门店</h3>
        <p class="text-sm text-base-content/60 mt-2">权限已生效，现在可以进入门店管理处理订单与出餐了。</p>
        <div class="modal-action justify-center">
          <button class="btn btn-ghost" @click="showJoined = false">知道了</button>
          <router-link to="/admin" class="btn btn-primary">进入门店管理</router-link>
        </div>
      </div>
      <div class="modal-backdrop" @click="showJoined = false"></div>
    </dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { httpGet, httpPost, isSuccess, parsePage } from '@/utils/fetch'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { useConfirm } from '@/composables/useConfirm'
import toast from '@/utils/toast'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const { confirm } = useConfirm()

const PAGE_SIZE = 20

const loading = ref(false)
const loadingMore = ref(false)
const acting = ref(null)
const notifications = ref([])
const page = ref(1)
const totalPages = ref(1)
const showJoined = ref(false)

const hasMore = computed(() => page.value < totalPages.value)
const hasUnread = computed(() => notifications.value.some(n => n.status === 0))
const pendingInvites = computed(() =>
  notifications.value.filter(n => n.type === 'STAFF_INVITE' && n.status === 0)
)

onMounted(() => fetchList())

const cardTone = (n) => {
  if (n.type === 'STAFF_INVITE' && n.status === 0) return 'border-primary/40'
  return 'border-base-200'
}

const iconOf = (n) => (n.type === 'STAFF_INVITE' ? '👥' : '🔔')

const timeText = (ts) => {
  if (!ts) return ''
  const d = new Date(Number(ts))
  const pad = (v) => String(v).padStart(2, '0')
  return `${d.getMonth() + 1}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 拉取通知列表（reset=true 时回到第一页） */
const fetchList = async (reset = false) => {
  if (reset) {
    page.value = 1
    loading.value = true
  } else {
    loading.value = true
  }
  try {
    const res = await httpGet(`/notification/list?page=${page.value}&size=${PAGE_SIZE}`)
    if (isSuccess(res)) {
      const parsed = parsePage(res.data)
      notifications.value = parsed.content
      totalPages.value = parsed.totalPages
    } else {
      toast.error(res?.message || '获取通知失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('获取通知失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = async () => {
  if (!hasMore.value) return
  loadingMore.value = true
  page.value += 1
  try {
    const res = await httpGet(`/notification/list?page=${page.value}&size=${PAGE_SIZE}`)
    if (isSuccess(res)) {
      const parsed = parsePage(res.data)
      notifications.value = [...notifications.value, ...parsed.content]
      totalPages.value = parsed.totalPages
    }
  } catch (e) {
    if (e?.type !== 'UNAUTHORIZED') toast.error('加载失败')
    page.value -= 1
  } finally {
    loadingMore.value = false
  }
}

/**
 * 同意邀请
 *
 * 同意后服务端才写入角色绑定，因此必须刷新本地用户信息，
 * 否则路由守卫仍按旧角色判断，无法进入 /admin。
 */
const handleAccept = async (n) => {
  acting.value = n.id
  try {
    const res = await httpPost(`/notification/staff-invite/accept/${n.id}`)
    if (isSuccess(res)) {
      await authStore.refreshUserInfo()
      notificationStore.fetchUnreadCount()
      await fetchList(true)
      showJoined.value = true
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('操作失败')
  } finally {
    acting.value = null
  }
}

const handleReject = async (n) => {
  const ok = await confirm({
    title: '拒绝邀请',
    message: '拒绝后该门店需要重新邀请你，确定拒绝吗？',
    confirmText: '确认拒绝',
    tone: 'error'
  })
  if (!ok) return
  acting.value = n.id
  try {
    const res = await httpPost(`/notification/staff-invite/reject/${n.id}`)
    if (isSuccess(res)) {
      toast.success('已拒绝邀请')
      notificationStore.fetchUnreadCount()
      await fetchList(true)
    } else {
      toast.error(res?.message || '操作失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('操作失败')
  } finally {
    acting.value = null
  }
}

const handleReadAll = async () => {
  try {
    const res = await httpPost('/notification/read-all')
    if (isSuccess(res)) {
      toast.success('已全部标记为已读')
      notificationStore.fetchUnreadCount()
      await fetchList(true)
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('操作失败')
  }
}

const handleDelete = async (n) => {
  try {
    const res = await httpPost(`/notification/delete/${n.id}`)
    if (isSuccess(res)) {
      notifications.value = notifications.value.filter(i => i.id !== n.id)
      notificationStore.fetchUnreadCount()
    } else {
      toast.error(res?.message || '删除失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('删除失败')
  }
}
</script>
