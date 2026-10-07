<template>
  <header class="navbar bg-base-100 shadow-sm sticky top-0 z-30 px-4">
    <!-- 左侧：返回上一页（可选）+ 餐易订Logo（点击回首页）+ 页面标题 -->
    <div class="flex-1 flex items-center gap-1 min-w-0">
      <button
        v-if="showBack"
        class="btn btn-ghost btn-sm btn-square shrink-0"
        title="返回上一页"
        @click="goBack"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <router-link to="/" class="flex items-center gap-2 shrink-0">
        <span class="text-2xl">🍔</span>
        <span class="font-bold text-xl text-primary">餐易订</span>
      </router-link>
      <span v-if="title" class="font-bold text-lg ml-2 truncate">{{ title }}</span>
    </div>

    <!-- 中部插槽：首页门店搜索框等 -->
    <slot name="middle" />

    <!-- 右侧：页面自定义操作 + 统一用户区 -->
    <div class="flex-none flex items-center gap-2">
      <slot name="actions" />
      <template v-if="authStore.isLoggedIn">
        <NotificationBell />
        <template v-if="showQuickLinks">
          <router-link to="/my/orders" class="btn btn-ghost btn-sm">我的订单</router-link>
          <!-- 所有登录用户均可开店/管理自己创建的门店 -->
          <router-link to="/my/stores" class="btn btn-ghost btn-sm">我的门店</router-link>
          <!-- 有门店角色的用户显示管理入口 -->
          <router-link
            v-if="canManageStore"
            to="/admin"
            class="btn btn-primary btn-sm"
          >门店管理</router-link>
        </template>
        <div class="dropdown dropdown-end">
          <div tabindex="0" role="button" class="btn btn-ghost btn-sm avatar">
            <div class="w-7 rounded-full bg-neutral text-neutral-content overflow-hidden">
              <!-- 用户头像：有 avatarUrl 显示真实头像，否则显示昵称首字 -->
              <img v-if="authStore.userInfo?.avatarUrl"
                   :src="authStore.userInfo.avatarUrl" alt="用户头像"
                   class="w-full h-full object-cover" />
              <span v-else class="text-xs flex items-center justify-center h-full">{{ (authStore.nickname || '客')[0] }}</span>
            </div>
          </div>
          <ul tabindex="0" class="mt-2 z-[1] p-2 shadow-lg menu menu-sm dropdown-content bg-base-100 rounded-box w-44 border border-base-200">
            <li class="px-3 py-1.5 text-xs text-base-content/50">{{ authStore.nickname || '未设置昵称' }}</li>
            <li><router-link to="/my/orders">我的订单</router-link></li>
            <li><router-link to="/my/stores">我的门店</router-link></li>
            <li><router-link to="/my/notifications">消息通知</router-link></li>
            <li><router-link to="/my/account">账号设置</router-link></li>
            <li v-if="canManageStore"><router-link to="/admin">门店管理</router-link></li>
            <li><a class="text-error" @click.prevent="handleLogout">退出登录</a></li>
          </ul>
        </div>
      </template>
      <router-link v-else to="/login" class="btn btn-primary btn-sm">登录 / 注册</router-link>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useStoreContext } from '@/stores/storeContext'
import NotificationBell from '@/components/common/NotificationBell.vue'
import toast from '@/utils/toast'

defineProps({
  /** 页面标题，展示在 Logo 右侧 */
  title: {
    type: String,
    default: ''
  },
  /** 是否展示返回上一页按钮 */
  showBack: {
    type: Boolean,
    default: false
  },
  /** 是否展示「我的订单 / 我的门店 / 门店管理」快捷入口 */
  showQuickLinks: {
    type: Boolean,
    default: true
  }
})

const router = useRouter()
const authStore = useAuthStore()
const storeContext = useStoreContext()

/** 是否显示门店管理入口（拥有任意门店角色） */
const canManageStore = computed(() =>
  authStore.hasAnyRole(['STORE_OWNER', 'STORE_CASHIER', 'STORE_KITCHEN', 'STORE_WAITER', 'STORE_INVENTORY'])
)

/** 返回上一页；若没有可回退的历史记录（如直接打开链接）则回首页 */
const goBack = () => {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push('/')
  }
}

const handleLogout = async () => {
  await authStore.logout()
  storeContext.clear()
  toast.success('已退出登录')
  router.push('/')
}
</script>
