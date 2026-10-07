<template>
  <div class="flex h-screen w-full bg-base-200 overflow-hidden font-sans">
    
    <aside
      class="bg-base-100 shadow-xl transition-all duration-300 ease-in-out flex flex-col z-20"
      :class="isCollapsed ? 'w-20' : 'w-64'"
    >
      <div class="h-16 flex items-center justify-between px-4 border-b border-base-200">
        <router-link
          v-if="!isCollapsed"
          to="/"
          title="返回前台首页"
          class="font-bold text-xl text-primary whitespace-nowrap overflow-hidden transition-opacity hover:opacity-80"
        >
          🍔 超级后台
        </router-link>
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
        <div class="flex-1">
          <span class="text-sm text-base-content/60 font-medium">欢迎回来，{{ authStore.nickname }}</span>
        </div>
        
        <div class="flex-none gap-4">
          <div class="dropdown dropdown-end">
            <div tabindex="0" role="button" class="btn btn-ghost gap-2 px-2">
              <!-- 用户头像：有 avatarUrl 则显示图片，否则显示昵称首字 -->
              <div class="avatar placeholder">
                <div class="bg-primary text-primary-content w-8 rounded-full">
                  <img v-if="authStore.userInfo?.avatarUrl" :src="authStore.userInfo.avatarUrl" :alt="authStore.nickname" />
                  <span v-else class="text-sm">{{ authStore.nickname?.charAt(0) || 'U' }}</span>
                </div>
              </div>
              <span class="text-sm font-medium hidden sm:inline">{{ authStore.nickname }}</span>
            </div>
            <ul tabindex="0" class="mt-3 z-[1] p-2 shadow-lg menu menu-sm dropdown-content bg-base-100 rounded-box w-52 border border-base-200">
              <li class="menu-title py-1.5">
                <span class="text-xs">{{ authStore.userInfo?.phone || authStore.userInfo?.email || '' }}</span>
              </li>
              <li v-for="role in authStore.roles" :key="role.id">
                <span class="text-xs">
                  <span class="badge badge-xs" :class="role.domain === 'GLOBAL' ? 'badge-primary' : 'badge-secondary'">
                    {{ role.domain === 'GLOBAL' ? '全局' : '门店' }}
                  </span>
                  {{ role.roleName }}
                </span>
              </li>
              <div class="divider my-0"></div>
              <li><router-link to="/my/account">账号设置</router-link></li>
              <li><a class="text-error hover:bg-error/10" @click="handleLogout">退出登录</a></li>
            </ul>
          </div>
        </div>
      </header>

      <main class="flex-1 overflow-auto p-6">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
      
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import toast from '@/utils/toast'

const router = useRouter()
const authStore = useAuthStore()

// 控制左侧抽屉状态：false 为展开，true 为收起
const isCollapsed = ref(false)

const toggleDrawer = () => {
  isCollapsed.value = !isCollapsed.value
}

/** 退出登录 */
const handleLogout = async () => {
  await authStore.logout()
  toast.success('已退出登录')
  router.push('/login')
}

/** 页面加载时初始化用户信息（从 localStorage 读取，无需请求后端） */
onMounted(() => {
  // authStore 在初始化时已从 localStorage 恢复 userInfo
  // 若 localStorage 中无数据（如被手动清除），则跳转登录页
  if (!authStore.isLoggedIn || !authStore.userInfo) {
    router.push('/login')
  }
})

// 5. 定义路由及对应 SVG 图标 (使用内联 SVG 以保证无需引入额外组件库即可渲染)
const menuItems = [
  {
    path: '/system/index',
    label: '系统管理',
    icon: `<svg t="1777278118446" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="11105"><path d="M979.2 158.848a32 32 0 0 0-38.4-23.936l-166.848 38.656a32 32 0 0 0-15.808 53.408L794.56 264.64l-194.56 173.568-101.024-95.68a32 32 0 0 0-45.152 1.152l-216.736 227.264A64 64 0 1 0 288 633.6c0-6.944-1.376-13.472-3.424-19.712l193.536-202.944 99.232 93.952a32 32 0 0 0 43.296 0.64L839.04 310.72l41.504 42.976a32 32 0 0 0 53.92-13.92l44.448-165.408a31.84 31.84 0 0 0 0.288-15.52z" p-id="11106"></path><path d="M928 450.464a32 32 0 0 0-32 32V736a32 32 0 0 1-32 32H160a32 32 0 0 1-32-32V160a32 32 0 0 1 32-32h530.656a32 32 0 0 0 0-64H160a96 96 0 0 0-96 96v576a96 96 0 0 0 96 96h704a96 96 0 0 0 96-96v-253.536a32 32 0 0 0-32-32zM912 896h-800a32 32 0 0 0 0 64h800a32 32 0 0 0 0-64z" p-id="11107"></path></svg>`
  },
  {
    path: '/system/user',
    label: '用户管理',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6">
  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" />
</svg>
`
  },
  {
    path: '/system/store',
    label: '店铺管理',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6">
  <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 21v-7.5a.75.75 0 0 1 .75-.75h3a.75.75 0 0 1 .75.75V21m-4.5 0H2.36m11.14 0H18m0 0h3.64m-1.39 0V9.349M3.75 21V9.349m0 0a3.001 3.001 0 0 0 3.75-.615A2.993 2.993 0 0 0 9.75 9.75c.896 0 1.7-.393 2.25-1.016a2.993 2.993 0 0 0 2.25 1.016c.896 0 1.7-.393 2.25-1.015a3.001 3.001 0 0 0 3.75.614m-16.5 0a3.004 3.004 0 0 1-.621-4.72l1.189-1.19A1.5 1.5 0 0 1 5.378 3h13.243a1.5 1.5 0 0 1 1.06.44l1.19 1.189a3 3 0 0 1-.621 4.72M6.75 18h3.75a.75.75 0 0 0 .75-.75V13.5a.75.75 0 0 0-.75-.75H6.75a.75.75 0 0 0-.75.75v3.75c0 .414.336.75.75.75Z" />
</svg>
`
  },
  {
    path: '/system/settings',
    label: '系统设置',
    permissions: ['system:config'],
    icon: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="size-6">
  <path stroke-linecap="round" stroke-linejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.325.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 0 1 1.37.49l1.296 2.247a1.125 1.125 0 0 1-.26 1.431l-1.003.827c-.293.241-.438.613-.43.992a7.723 7.723 0 0 1 0 .255c-.008.378.137.75.43.991l1.004.827c.424.35.534.955.26 1.43l-1.298 2.247a1.125 1.125 0 0 1-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.47 6.47 0 0 1-.22.128c-.331.183-.581.495-.644.869l-.213 1.281c-.09.543-.56.94-1.11.94h-2.594c-.55 0-1.019-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 0 1-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 0 1-1.369-.49l-1.297-2.247a1.125 1.125 0 0 1 .26-1.431l1.004-.827c.292-.24.437-.613.43-.991a6.932 6.932 0 0 1 0-.255c.007-.38-.138-.751-.43-.992l-1.004-.827a1.125 1.125 0 0 1-.26-1.43l1.297-2.247a1.125 1.125 0 0 1 1.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.086.22-.128.332-.183.582-.495.644-.869l.214-1.28Z" />
  <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
</svg>
`
  }
]

/**
 * 根据当前用户权限过滤可见菜单
 * （超级管理员在 authStore.hasAnyPermission 中自动放行）
 */
const visibleMenuItems = computed(() => {
  return menuItems.filter(item => {
    if (item.permissions && !authStore.hasAnyPermission(item.permissions)) {
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