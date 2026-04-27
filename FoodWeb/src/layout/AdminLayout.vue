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
    icon: `<svg t="1777278118446" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="11105"><path d="M979.2 158.848a32 32 0 0 0-38.4-23.936l-166.848 38.656a32 32 0 0 0-15.808 53.408L794.56 264.64l-194.56 173.568-101.024-95.68a32 32 0 0 0-45.152 1.152l-216.736 227.264A64 64 0 1 0 288 633.6c0-6.944-1.376-13.472-3.424-19.712l193.536-202.944 99.232 93.952a32 32 0 0 0 43.296 0.64L839.04 310.72l41.504 42.976a32 32 0 0 0 53.92-13.92l44.448-165.408a31.84 31.84 0 0 0 0.288-15.52z" p-id="11106"></path><path d="M928 450.464a32 32 0 0 0-32 32V736a32 32 0 0 1-32 32H160a32 32 0 0 1-32-32V160a32 32 0 0 1 32-32h530.656a32 32 0 0 0 0-64H160a96 96 0 0 0-96 96v576a96 96 0 0 0 96 96h704a96 96 0 0 0 96-96v-253.536a32 32 0 0 0-32-32zM912 896h-800a32 32 0 0 0 0 64h800a32 32 0 0 0 0-64z" p-id="11107"></path></svg>`
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
  },
  {
    path: '/admin/tables',
    label: '餐桌管理',
    icon: `<svg t="1777277174765" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="1305"><path d="M886.848 416a64 64 0 0 1 64 64v64a64 64 0 0 1-64 64H137.152a64 64 0 0 1-64-64v-64a64 64 0 0 1 64-64h749.696z m0 64H137.152v64h749.696v-64z" fill="#666666" p-id="1306"></path><path d="M761.856 128a64 64 0 0 1 59.104 39.424L950.88 480H73.088l129.92-312.576A64 64 0 0 1 262.144 128h499.712zM224 544a32 32 0 0 1 32 32v288a32 32 0 0 1-64 0v-288a32 32 0 0 1 32-32zM800 544a32 32 0 0 1 32 32v288a32 32 0 0 1-64 0v-288a32 32 0 0 1 32-32z" fill="#666666" p-id="1307"></path></svg>` 
  },
  {
    path: '/admin/categories',
    label: '菜品分类',
    icon: `<svg t="1777278012393" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="6148"><path d="M278.15 192.43h129.47a80 80 0 0 1 80 80v209.88H278.15a80 80 0 0 1-80-80V272.43a80 80 0 0 1 80-80zM752.31 482.31H542.84V272.43a80 80 0 0 1 80-80h129.47a80 80 0 0 1 80 80v129.88a80 80 0 0 1-80 80zM752.31 828.53H622.84a80 80 0 0 1-80-80V538.65h209.47a80 80 0 0 1 80 80v129.88a80 80 0 0 1-80 80zM278.15 538.64h209.47v209.88a80 80 0 0 1-80 80H278.15a80 80 0 0 1-80-80V618.64a80 80 0 0 1 80-80z" fill="#999999" p-id="6149"></path></svg>`
  },
  {
    path: '/admin/memberships',
    label: '会员管理',
    icon: `<svg t="1777278354441" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="15655"><path d="M148.992 344.107l54.87 466.56h616.277l54.869-466.56-171.093 114.048L512 189.483 320.085 458.155 148.992 344.107z m-29.483-122.24l179.158 119.466L477.269 91.307a42.667 42.667 0 0 1 69.462 0l178.602 250.026 179.2-119.466A42.667 42.667 0 0 1 970.54 262.4l-70.102 595.925A42.667 42.667 0 0 1 858.07 896H165.931a42.667 42.667 0 0 1-42.368-37.675L53.46 262.357a42.667 42.667 0 0 1 66.006-40.49zM512 640a85.333 85.333 0 1 1 0-170.667A85.333 85.333 0 0 1 512 640z" fill="#CDCDCD" p-id="15656"></path></svg>`
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