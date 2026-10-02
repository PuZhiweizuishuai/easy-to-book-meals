<template>
  <div class="flex h-screen w-full bg-base-200 overflow-hidden font-sans">

    <aside
      class="bg-base-100 shadow-xl transition-all duration-300 ease-in-out flex flex-col z-20"
      :class="isCollapsed ? 'w-20' : 'w-64'"
    >
      <div class="h-16 flex items-center justify-between px-4 border-b border-base-200">
        <div
          v-if="!isCollapsed"
          class="font-bold text-xl text-primary whitespace-nowrap overflow-hidden transition-opacity"
        >
          🍔 餐易订
        </div>
        <button
          @click="toggleDrawer"
          class="btn btn-square btn-ghost btn-sm"
          :class="{ 'mx-auto': isCollapsed }"
          title="切换导航"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
      </div>

      <nav class="flex-1 overflow-y-auto py-4 space-y-2 px-3">
        <router-link
          v-for="item in visibleMenuItems"
          :key="item.path"
          :to="item.path"
          class="flex items-center px-3 py-3 rounded-xl transition-colors hover:bg-base-200 text-base-content/80 hover:text-base-content"
          active-class="bg-primary/10 text-primary font-semibold"
        >
          <span class="flex items-center justify-center" :class="isCollapsed ? 'mx-auto' : 'mr-3'">
            <span v-html="item.icon" class="w-6 h-6 flex items-center justify-center"></span>
          </span>
          <span v-if="!isCollapsed" class="whitespace-nowrap">{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>

    <div class="flex-1 flex flex-col min-w-0">

      <header class="navbar bg-base-100 shadow-sm z-10 px-6">
        <div class="flex-1 flex items-center gap-3">
          <!-- 门店切换器：多门店用户可切换当前管理门店 -->
          <div v-if="storeContext.hasStore" class="flex items-center gap-2">
            <span class="text-sm text-base-content/60 font-medium hidden sm:inline">当前门店</span>
            <select
              class="select select-bordered select-sm"
              :value="storeContext.currentStoreId"
              @change="handleStoreChange"
            >
              <option v-for="s in storeContext.stores" :key="s.id" :value="s.id">
                {{ s.storeName }}{{ s.status === -1 ? '（待审核）' : '' }}
              </option>
            </select>
          </div>
        </div>

        <div class="flex-none gap-4">
          <div class="dropdown dropdown-end">
            <div tabindex="0" role="button" class="btn btn-ghost btn-circle avatar">
              <div class="bg-neutral text-neutral-content w-8 rounded-full">
                <img alt="User Avatar" :src="authStore.userInfo?.avatarUrl || 'https://api.dicebear.com/7.x/notionists/svg?seed=Admin'" />
              </div>
            </div>
            <ul tabindex="0" class="mt-3 z-[1] p-2 shadow-lg menu menu-sm dropdown-content bg-base-100 rounded-box w-52 border border-base-200">
              <li class="px-3 py-2 text-sm text-base-content/60">
                {{ authStore.nickname || '未设置昵称' }}
              </li>
              <div class="divider my-0"></div>
              <li>
                <router-link to="/">前台首页</router-link>
              </li>
              <li>
                <a class="text-error hover:bg-error/10" @click.prevent="handleLogout">退出登录</a>
              </li>
            </ul>
          </div>
        </div>
      </header>

      <!-- 未选择门店时提示 -->
      <div v-if="!storeContext.loading && !storeContext.hasStore" class="flex-1 flex items-center justify-center">
        <div class="card bg-base-100 shadow-md">
          <div class="card-body items-center text-center">
            <h3 class="text-xl font-bold">暂无管理的门店</h3>
            <p class="text-base-content/60">您还没有任何门店的管理权限，请先联系店主分配门店角色，或创建属于自己的门店。</p>
            <div class="card-actions justify-center mt-2">
              <button class="btn btn-primary" @click="showCreateStoreModal = true">创建门店</button>
            </div>
          </div>
        </div>
      </div>

      <main v-else class="flex-1 overflow-auto p-6">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

    </div>

    <!-- 创建门店弹窗 -->
    <div class="modal" :class="{ 'modal-open': showCreateStoreModal }">
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
          <p class="text-xs text-base-content/50">门店创建后需等待平台管理员审核通过后对外展示。</p>
          <div class="modal-action">
            <button type="button" class="btn btn-ghost" @click="showCreateStoreModal = false">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="storeCreating">
              <span v-if="storeCreating" class="loading loading-spinner loading-sm"></span>
              {{ storeCreating ? '创建中...' : '创建门店' }}
            </button>
          </div>
        </form>
      </div>
      <div class="modal-backdrop" @click="showCreateStoreModal = false"></div>
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

const router = useRouter()
const authStore = useAuthStore()
const storeContext = useStoreContext()

// 控制左侧抽屉状态：false 为展开，true 为收起
const isCollapsed = ref(false)
const showCreateStoreModal = ref(false)
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

const toggleDrawer = () => {
  isCollapsed.value = !isCollapsed.value
}

onMounted(() => {
  // 进入管理端布局时加载门店上下文
  storeContext.loadMyStores()
})

/**
 * 切换当前管理门店
 */
const handleStoreChange = (e) => {
  storeContext.selectStore(Number(e.target.value))
  toast.success('已切换门店')
  // 重新加载当前页面数据
  router.go(0)
}

/**
 * 退出登录
 */
const handleLogout = async () => {
  await authStore.logout()
  storeContext.clear()
  toast.success('已退出登录')
  router.push('/login')
}

/** Logo 上传成功回调 */
const handleLogoUploadSuccess = (response) => {
  if (!response?.data?.[0]) return
  storeForm.value.logoUrl = response.data[0].fileUrl
  nextTick(() => {
    showLogoModal.value = false
  })
}

/**
 * 创建门店
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
      toast.success('门店创建成功，等待平台审核')
      showCreateStoreModal.value = false
      storeForm.value = { storeName: '', address: '', phone: '', logoUrl: '', businessHours: '', description: '' }
      await storeContext.loadMyStores()
      if (storeContext.currentStoreId) {
        storeContext.selectStore(storeContext.currentStoreId)
        router.go(0)
      }
    } else {
      toast.error(res?.message || '创建失败')
    }
  } catch (e) {
    toast.error('创建失败，请稍后重试')
  } finally {
    storeCreating.value = false
  }
}

/**
 * 菜单定义：每个菜单项通过 permissions/roles 控制可见性
 * 超级管理员在 authStore.hasAnyPermission/hasAnyRole 中自动放行
 */
const menuItems = [
  {
    path: '/admin/dashboard',
    label: '数据看板',
    icon: `<svg t="1777278118446" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="11105"><path d="M979.2 158.848a32 32 0 0 0-38.4-23.936l-166.848 38.656a32 32 0 0 0-15.808 53.408L794.56 264.64l-194.56 173.568-101.024-95.68a32 32 0 0 0-45.152 1.152l-216.736 227.264A64 64 0 1 0 288 633.6c0-6.944-1.376-13.472-3.424-19.712l193.536-202.944 99.232 93.952a32 32 0 0 0 43.296 0.64L839.04 310.72l41.504 42.976a32 32 0 0 0 53.92-13.92l44.448-165.408a31.84 31.84 0 0 0 0.288-15.52z" p-id="11106"></path><path d="M928 450.464a32 32 0 0 0-32 32V736a32 32 0 0 1-32 32H160a32 32 0 0 1-32-32V160a32 32 0 0 1 32-32h530.656a32 32 0 0 0 0-64H160a96 96 0 0 0-96 96v576a96 96 0 0 0 96 96h704a96 96 0 0 0 96-96v-253.536a32 32 0 0 0-32-32zM912 896h-800a32 32 0 0 0 0 64h800a32 32 0 0 0 0-64z" p-id="11107"></path></svg>`
  },
  {
    path: '/admin/orders',
    label: '订单管理',
    permissions: ['order:view'],
    icon: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" /></svg>`
  },
  {
    path: '/admin/dishes',
    label: '菜品管理',
    permissions: ['dish:view'],
    icon: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>`
  },
  {
    path: '/admin/tables',
    label: '餐桌管理',
    permissions: ['table:view'],
    icon: `<svg t="1777277174765" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="1305"><path d="M886.848 416a64 64 0 0 1 64 64v64a64 64 0 0 1-64 64H137.152a64 64 0 0 1-64-64v-64a64 64 0 0 1 64-64h749.696z m0 64H137.152v64h749.696v-64z" fill="#666666" p-id="1306"></path><path d="M761.856 128a64 64 0 0 1 59.104 39.424L950.88 480H73.088l129.92-312.576A64 64 0 0 1 262.144 128h499.712zM224 544a32 32 0 0 1 32 32v288a32 32 0 0 1-64 0v-288a32 32 0 0 1 32-32zM800 544a32 32 0 0 1 32 32v288a32 32 0 0 1-64 0v-288a32 32 0 0 1 32-32z" fill="#666666" p-id="1307"></path></svg>`
  },
  {
    path: '/admin/categories',
    label: '菜品分类',
    permissions: ['dish:manage'],
    icon: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6">
  <path stroke-linecap="round" stroke-linejoin="round" d="M9.568 3H5.25A2.25 2.25 0 0 0 3 5.25v4.318c0 .597.237 1.17.659 1.591l9.581 9.581c.699.699 1.78.872 2.607.33a18.095 18.095 0 0 0 5.223-5.223c.542-.827.369-1.908-.33-2.607L11.16 3.66A2.25 2.25 0 0 0 9.568 3Z" />
  <path stroke-linecap="round" stroke-linejoin="round" d="M6 6h.008v.008H6V6Z" />
</svg>
`
  },
  {
    path: '/admin/memberships',
    label: '会员管理',
    permissions: ['member:manage'],
    icon: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6">
  <path stroke-linecap="round" stroke-linejoin="round" d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" />
</svg>
`
  },
  {
    path: '/admin/store',
    label: '店铺管理',
    roles: ['STORE_OWNER'],
    icon: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6">
  <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 21v-7.5a.75.75 0 0 1 .75-.75h3a.75.75 0 0 1 .75.75V21m-4.5 0H2.36m11.14 0H18m0 0h3.64m-1.39 0V9.349M3.75 21V9.349m0 0a3.001 3.001 0 0 0 3.75-.615A2.993 2.993 0 0 0 9.75 9.75c.896 0 1.7-.393 2.25-1.016a2.993 2.993 0 0 0 2.25 1.016c.896 0 1.7-.393 2.25-1.015a3.001 3.001 0 0 0 3.75.614m-16.5 0a3.004 3.004 0 0 1-.621-4.72l1.189-1.19A1.5 1.5 0 0 1 5.378 3h13.243a1.5 1.5 0 0 1 1.06.44l1.19 1.189a3 3 0 0 1-.621 4.72M6.75 18h3.75a.75.75 0 0 0 .75-.75V13.5a.75.75 0 0 0-.75-.75H6.75a.75.75 0 0 0-.75.75v3.75c0 .414.336.75.75.75Z" />
</svg>
`
  }
]

/**
 * 根据当前用户角色/权限过滤可见菜单
 */
const visibleMenuItems = computed(() => {
  return menuItems.filter(item => {
    if (item.permissions && !authStore.hasAnyPermission(item.permissions)) {
      return false
    }
    if (item.roles && !authStore.hasAnyRole(item.roles)) {
      return false
    }
    return true
  })
})
</script>

<style scoped>
/* 定义内部主内容区的淡入淡出动画过渡效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(5px);
}
</style>
