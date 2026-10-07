<template>
  <div class="min-h-screen bg-base-200">
    <!-- 顶部导航（通用组件） -->
    <AppHeader title="我的门店" show-back />

    <main class="container mx-auto px-4 py-6 max-w-6xl">
      <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h1 class="text-2xl font-bold">我的门店</h1>
          <p class="text-sm text-base-content/50 mt-1">管理自己创建的门店，或申请开设新门店</p>
        </div>
        <button
          class="btn btn-primary btn-sm"
          :disabled="hasPendingStore"
          :title="hasPendingStore ? '有待审核的门店，审核通过后才能创建新门店' : ''"
          @click="showCreateModal = true"
        >+ 创建门店</button>
      </div>

      <!-- 待审核提示 -->
      <div v-if="hasPendingStore" role="alert" class="alert alert-warning mb-6 py-3">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M5.07 19H19a2 2 0 001.74-3L13.74 4a2 2 0 00-3.48 0l-7 12A2 2 0 005.07 19z" />
        </svg>
        <span class="text-sm">您有门店正在等待平台审核，审核通过后才能创建新门店，并自动获得店主身份。</span>
      </div>

      <div v-if="storeContext.loading" class="flex justify-center py-24">
        <span class="loading loading-spinner loading-lg text-primary"></span>
      </div>

      <div v-else-if="stores.length === 0" class="card bg-base-100 shadow-sm">
        <div class="card-body items-center py-20 text-center">
          <span class="text-5xl mb-3">🏪</span>
          <h3 class="text-lg font-bold">还没有自己的门店</h3>
          <p class="text-base-content/50 text-sm max-w-md">
            创建门店提交平台审核，审核通过后您将成为店主，可在门店管理端经营自己的店铺。
          </p>
          <div class="card-actions justify-center mt-2">
            <button class="btn btn-primary" @click="showCreateModal = true">创建门店</button>
          </div>
        </div>
      </div>

      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <div v-for="store in stores" :key="store.id" class="card bg-base-100 shadow-sm border border-base-200 flex flex-col">
          <figure class="h-36 bg-base-200 overflow-hidden relative">
            <img v-if="store.logoUrl" :src="store.logoUrl" :alt="store.storeName" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-5xl">🍽️</div>
            <span :class="['badge badge-sm absolute top-2 right-2', statusBadge(store.status)]">
              {{ statusText(store.status) }}
            </span>
          </figure>
          <div class="card-body p-4 flex-1">
            <h2 class="card-title text-base">{{ store.storeName }}</h2>
            <p class="text-sm text-base-content/60 line-clamp-2 min-h-10">{{ store.description || '暂无简介' }}</p>
            <div class="text-xs text-base-content/50 space-y-1">
              <p class="flex items-center gap-1">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                <span class="truncate">{{ store.address }}</span>
              </p>
              <p class="flex items-center justify-between">
                <span>{{ store.phone }}</span>
                <span v-if="store.businessHours">{{ store.businessHours }}</span>
              </p>
              <p>创建于 {{ formatTime(store.createTime) }}</p>
            </div>
            <div v-if="store.tags && store.tags.length" class="flex flex-wrap gap-1">
              <span v-for="tag in store.tags.slice(0, 3)" :key="tag.id" class="badge badge-ghost badge-xs">{{ tag.tagName }}</span>
            </div>
          </div>
          <div class="card-actions p-4 pt-0">
            <!-- 待审核：创建者尚无店主角色，展示审核说明 -->
            <p v-if="store.status === -1" class="text-xs text-warning flex items-center gap-1">
              <span class="loading loading-spinner loading-xs"></span>
              等待平台审核，通过后自动获得店主身份
            </p>
            <div v-else class="flex gap-2 w-full">
              <button
                v-if="store.hasManageRole"
                class="btn btn-primary btn-sm flex-1"
                @click="enterManage(store)"
              >进入管理</button>
              <router-link
                v-if="store.status === 0 || store.status === 1"
                :to="`/store/${store.id}`"
                class="btn btn-ghost btn-sm"
              >门店主页</router-link>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 创建门店弹窗 -->
    <div class="modal" :class="{ 'modal-open': showCreateModal }">
      <div class="modal-box max-w-lg">
        <h3 class="font-bold text-lg mb-4">创建门店</h3>
        <form @submit.prevent="handleCreateStore" class="space-y-3">
          <!-- 店铺 Logo 上传 -->
          <div class="form-control">
            <label class="label"><span class="label-text">店铺 Logo</span></label>
            <div class="flex items-center gap-4">
              <figure class="w-24 h-24 rounded-xl bg-base-200 overflow-hidden flex-shrink-0 border border-base-300">
                <img v-if="storeForm.logoUrl" :src="storeForm.logoUrl" alt="店铺Logo" class="w-full h-full object-cover" />
                <div v-else class="w-full h-full flex items-center justify-center text-4xl text-base-content/30">🏪</div>
              </figure>
              <div class="flex flex-col gap-2">
                <button type="button" class="btn btn-outline btn-sm" @click="showLogoModal = true">
                  {{ storeForm.logoUrl ? '更换图片' : '上传图片' }}
                </button>
                <button v-if="storeForm.logoUrl" type="button" class="btn btn-ghost btn-xs text-error" @click="storeForm.logoUrl = ''">移除</button>
              </div>
            </div>
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">门店名称 *</span></label>
            <input v-model.trim="storeForm.storeName" type="text" class="input input-bordered w-full" required maxlength="50" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">门店地址 *</span></label>
            <input v-model.trim="storeForm.address" type="text" class="input input-bordered w-full" required maxlength="200" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">联系电话 *</span></label>
            <input v-model.trim="storeForm.phone" type="text" class="input input-bordered w-full" required maxlength="20" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">营业时间</span></label>
            <input v-model.trim="storeForm.businessHours" type="text" class="input input-bordered w-full" placeholder="09:00-21:00" maxlength="50" />
          </div>
          <div class="form-control">
            <label class="label"><span class="label-text">门店简介</span></label>
            <textarea v-model.trim="storeForm.description" class="textarea textarea-bordered w-full" rows="3"></textarea>
          </div>
          <p class="text-xs text-base-content/50">提交后需等待平台管理员审核，审核通过后您将成为店主并可正式营业。</p>
          <div class="modal-action">
            <button type="button" class="btn btn-ghost" @click="showCreateModal = false">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="storeCreating">
              <span v-if="storeCreating" class="loading loading-spinner loading-sm"></span>
              {{ storeCreating ? '提交中...' : '提交申请' }}
            </button>
          </div>
        </form>
      </div>
      <div class="modal-backdrop" @click="showCreateModal = false"></div>
    </div>

    <!-- Logo 上传弹窗 -->
    <Teleport to="body">
      <dialog :class="['modal modal-middle', { 'modal-open': showLogoModal }]">
        <div class="modal-box w-11/12 max-w-2xl">
          <h3 class="font-bold text-lg mb-4">上传店铺 Logo</h3>
          <ImageUpload :allow-multiple="false" @upload-success="handleLogoUploadSuccess" />
          <div class="modal-action">
            <button class="btn btn-ghost" @click="showLogoModal = false">完成</button>
          </div>
        </div>
        <div class="modal-backdrop" @click="showLogoModal = false"></div>
      </dialog>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { httpPost, isSuccess } from '@/utils/fetch'
import { useAuthStore } from '@/stores/auth'
import { useStoreContext } from '@/stores/storeContext'
import toast from '@/utils/toast'
import ImageUpload from '@/components/upload/ImageUpload.vue'
import AppHeader from '@/components/common/AppHeader.vue'

const router = useRouter()
const authStore = useAuthStore()
const storeContext = useStoreContext()

const stores = computed(() => storeContext.stores)

/** 是否存在待审核的门店（后端限制：需审核结束后才能再次创建） */
const hasPendingStore = computed(() => stores.value.some(s => s.status === -1))

const showCreateModal = ref(false)
const showLogoModal = ref(false)
const storeCreating = ref(false)
const storeForm = ref({
  storeName: '',
  address: '',
  phone: '',
  logoUrl: '',
  businessHours: '',
  description: ''
})

onMounted(async () => {
  // 门店审核通过后服务端才授予店主角色，先刷新本地缓存的角色信息
  await authStore.refreshUserInfo()
  await storeContext.loadMyStores(true)
})

/**
 * 进入门店管理端
 */
const enterManage = (store) => {
  storeContext.selectStore(store.id)
  router.push('/admin/dashboard')
}

/**
 * 提交创建门店申请
 */
const handleCreateStore = async () => {
  if (!storeForm.value.storeName || !storeForm.value.address || !storeForm.value.phone) {
    toast.error('请填写门店名称、地址和联系电话')
    return
  }
  storeCreating.value = true
  try {
    const res = await httpPost('/store/manage/add', storeForm.value)
    if (isSuccess(res)) {
      toast.success('门店申请已提交，等待平台审核')
      showCreateModal.value = false
      storeForm.value = { storeName: '', address: '', phone: '', logoUrl: '', businessHours: '', description: '' }
      await storeContext.loadMyStores(true)
    } else {
      // 业务错误（如“有待审核的门店”），展示后端信息并同步列表状态
      toast.error(res?.message || '创建失败')
      await storeContext.loadMyStores(true)
    }
  } catch (e) {
    toast.error('创建失败，请稍后重试')
  } finally {
    storeCreating.value = false
  }
}

/** Logo 上传成功回调 */
const handleLogoUploadSuccess = (response) => {
  if (!response?.data?.[0]) return
  storeForm.value.logoUrl = response.data[0].fileUrl
  nextTick(() => {
    showLogoModal.value = false
  })
}

/** 状态展示（与后端 StoreStatus 对应） */
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

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
