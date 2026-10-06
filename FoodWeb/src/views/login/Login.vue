<template>
  <div class="min-h-screen flex bg-base-100">
    <!-- 返回首页（移动端落白底用深色，桌面端落彩色区用白色） -->
    <button
      class="btn btn-ghost btn-circle fixed top-4 left-4 z-50 bg-base-100/70 text-base-content border border-base-300 backdrop-blur hover:bg-base-200 lg:bg-base-100/20 lg:text-white lg:border-white/30 lg:hover:bg-white/30"
      title="返回首页"
      aria-label="返回首页"
      @click="goHome"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
      </svg>
    </button>

    <!-- 左侧装饰区域（桌面端显示） -->
    <div class="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-primary via-primary/90 to-secondary relative overflow-hidden">
      <!-- 装饰性背景图案 -->
      <div class="absolute inset-0 opacity-10">
        <div class="absolute top-20 left-10 w-64 h-64 rounded-full bg-white/20 blur-3xl"></div>
        <div class="absolute bottom-20 right-10 w-96 h-96 rounded-full bg-secondary/30 blur-3xl"></div>
        <div class="absolute top-1/2 left-1/3 w-48 h-48 rounded-full bg-accent/20 blur-2xl"></div>
      </div>

      <!-- 装饰内容 -->
      <div class="relative z-10 flex flex-col justify-center items-center w-full px-16 text-primary-content">
        <div class="text-8xl mb-8 drop-shadow-lg">🍔</div>
        <h1 class="text-4xl font-bold mb-4 tracking-wide">餐易订</h1>
        <p class="text-lg opacity-80 text-center leading-relaxed max-w-md">
          一站式餐饮管理平台<br />
          轻松管理门店、菜品与订单
        </p>

        <!-- 特性展示 -->
        <div class="mt-12 space-y-4 w-full max-w-sm">
          <div class="flex items-center gap-3 bg-white/10 rounded-xl px-4 py-3 backdrop-blur-sm">
            <span class="text-2xl">🏪</span>
            <div>
              <div class="font-semibold text-sm">多门店管理</div>
              <div class="text-xs opacity-70">统一管理所有分店数据</div>
            </div>
          </div>
          <div class="flex items-center gap-3 bg-white/10 rounded-xl px-4 py-3 backdrop-blur-sm">
            <span class="text-2xl">📋</span>
            <div>
              <div class="font-semibold text-sm">智能点餐</div>
              <div class="text-xs opacity-70">扫码点餐，高效便捷</div>
            </div>
          </div>
          <div class="flex items-center gap-3 bg-white/10 rounded-xl px-4 py-3 backdrop-blur-sm">
            <span class="text-2xl">📊</span>
            <div>
              <div class="font-semibold text-sm">数据分析</div>
              <div class="text-xs opacity-70">实时掌握经营状况</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单区域 -->
    <div class="flex-1 flex items-center justify-center p-4 sm:p-8">
      <div class="w-full max-w-md">
        <!-- 移动端 Logo -->
        <div class="lg:hidden text-center mb-8">
          <div class="text-5xl mb-2">🍔</div>
          <h1 class="text-2xl font-bold text-primary">餐易订</h1>
        </div>

        <!-- 欢迎语 -->
        <div class="mb-8">
          <h2 class="text-2xl sm:text-3xl font-bold">欢迎回来</h2>
          <p class="text-base-content/50 mt-2">登录您的账号以继续</p>
        </div>

        <!-- 登录表单 -->
        <form @submit.prevent="handleLogin" class="space-y-5">
          <!-- 用户名/手机号/邮箱 -->
          <div class="form-control w-full">
            <label class="label">
              <span class="label-text font-medium">手机号 / 邮箱</span>
            </label>
            <input
              v-model="formData.username"
              type="text"
              placeholder="请输入手机号或邮箱"
              class="input input-bordered w-full focus:input-primary"
              required
              autocomplete="username"
            />
          </div>

          <!-- 密码 -->
          <div class="form-control w-full">
            <label class="label">
              <span class="label-text font-medium">密码</span>
            </label>
            <div class="relative">
              <input
                v-model="formData.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                class="input input-bordered w-full focus:input-primary pr-10"
                required
                minlength="6"
                autocomplete="current-password"
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-base-content/40 hover:text-base-content/70"
                @click="showPassword = !showPassword"
                tabindex="-1"
                aria-label="切换密码可见性"
              >
                <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                </svg>
              </button>
            </div>
          </div>

          <!-- 错误提示 -->
          <div v-if="errorMsg" class="alert alert-error alert-sm py-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span class="text-sm">{{ errorMsg }}</span>
          </div>

          <!-- 登录按钮 -->
          <button type="submit" class="btn btn-primary w-full" :disabled="loading">
            <span v-if="loading" class="loading loading-spinner loading-sm"></span>
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>

        <!-- 注册入口 -->
        <div class="mt-6 text-center text-sm text-base-content/60">
          还没有账号？
          <a href="javascript:void(0)" class="link link-primary link-hover font-medium" @click="goRegister">
            立即注册
          </a>
        </div>

        <!-- 开发环境提示 -->
        <!-- <div class="mt-8 bg-base-200/60 rounded-xl p-4">
          <div class="text-xs text-base-content/50 mb-2 font-medium">开发环境提示</div>
          <div class="text-xs text-base-content/70 space-y-1">
            <p>管理员手机号：<code class="bg-base-300 px-1.5 py-0.5 rounded text-[11px]">13800000000</code></p>
            <p>默认密码：<code class="bg-base-300 px-1.5 py-0.5 rounded text-[11px]">123456</code></p>
          </div>
        </div> -->
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { httpPost, isSuccess } from '@/utils/fetch'
import { useAuthStore } from '@/stores/auth'
import toast from '@/utils/toast'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const showPassword = ref(false)
const errorMsg = ref('')
const formData = ref({
  username: '',
  password: ''
})

const goRegister = () => {
  router.push({ name: 'register' })
}

const goHome = () => {
  router.push('/')
}

/**
 * 处理登录
 * 1. 调用后端登录接口
 * 2. 将 token 和用户信息持久化到 localStorage（通过 authStore）
 * 3. 跳转到登录前的页面，若无则跳转首页
 */
const handleLogin = async () => {
  errorMsg.value = ''

  if (!formData.value.username || !formData.value.password) {
    errorMsg.value = '请输入手机号/邮箱和密码'
    return
  }

  loading.value = true
  try {
    const res = await httpPost('/auth/login', {
      username: formData.value.username,
      password: formData.value.password
    })

    if (isSuccess(res) && res.data) {
      authStore.setLoginInfo(res.data)
      toast.success('登录成功')
      // 跳转到登录前访问的页面，若无则跳转首页
      const redirect = route.query.redirect || '/'
      router.push(redirect)
    } else {
      errorMsg.value = res?.message || '登录失败，请检查账号密码'
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') {
      errorMsg.value = '认证失败，请重新登录'
    } else {
      errorMsg.value = '网络异常，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>
