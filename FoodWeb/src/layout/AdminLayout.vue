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
          🍔 点餐后台
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
          v-for="item in menuItems"
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
          <span class="text-sm text-base-content/60 font-medium">欢迎回来，管理员</span>
        </div>
        
        <div class="flex-none gap-4">
          <!-- <button class="btn btn-ghost btn-circle">
            <div class="indicator">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" /></svg>
              <span class="badge badge-xs badge-primary indicator-item"></span>
            </div>
          </button> -->
          
          <div class="dropdown dropdown-end">
            <div tabindex="0" role="button" class="btn btn-ghost btn-circle avatar">
              <div class="bg-neutral text-neutral-content w-8 rounded-full">
                <img alt="User Avatar" src="https://api.dicebear.com/7.x/notionists/svg?seed=Admin" />
              </div>
            </div>
            <ul tabindex="0" class="mt-3 z-[1] p-2 shadow-lg menu menu-sm dropdown-content bg-base-100 rounded-box w-52 border border-base-200">
              <li><a>个人中心</a></li>
              <li><a>系统设置</a></li>
              <div class="divider my-0"></div>
              <li><a class="text-error hover:bg-error/10">退出登录</a></li>
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
import { ref } from 'vue'

// 控制左侧抽屉状态：false 为展开，true 为收起
const isCollapsed = ref(false)

const toggleDrawer = () => {
  isCollapsed.value = !isCollapsed.value
}

// 5. 定义路由及对应 SVG 图标 (使用内联 SVG 以保证无需引入额外组件库即可渲染)
const menuItems = [
  {
    path: '/admin/dashboard',
    label: '数据看板',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" /></svg>`
  },
  {
    path: '/admin/orders',
    label: '订单管理',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" /></svg>`
  },
  {
    path: '/admin/dishes',
    label: '菜品管理',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>`
  }
]
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