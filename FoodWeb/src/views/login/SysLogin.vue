<template>
  <div class="min-h-screen flex items-center justify-center bg-base-200 p-4">
    <div class="card bg-base-100 shadow-2xl w-full max-w-md border border-base-200">
      <div class="card-body p-6 sm:p-8">
        <!-- 标题区域 -->
        <div class="text-center mb-6">
          <div class="text-4xl mb-3">🍔</div>
          <h1 class="text-2xl font-bold">超级后台</h1>
          <p class="text-sm text-base-content/60 mt-1">系统管理平台登录</p>
        </div>

        <!-- 登录表单 -->
        <form @submit.prevent="handleLogin" class="flex flex-col gap-4">
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">手机号 <span class="text-error">*</span></span></label>
            <input v-model="formData.username" type="text" placeholder="请输入手机号"
              class="input input-bordered w-full focus:input-primary" required maxlength="11" />
          </div>

          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">密码 <span class="text-error">*</span></span></label>
            <input v-model="formData.password" :type="showPassword ? 'text' : 'password'" placeholder="请输入密码"
              class="input input-bordered w-full focus:input-primary pr-10" required minlength="6" />
            <button type="button" class="absolute right-3 top-[38px] text-base-content/40 hover:text-base-content/70"
              @click="showPassword = !showPassword" tabindex="-1">
              <!-- 眼睛图标 -->
              <svg v-if="!showPassword" xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none"
                viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24"
                stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
              </svg>
            </button>
          </div>

          <!-- 错误提示 -->
          <div v-if="errorMsg" class="alert alert-error alert-sm py-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-4 w-4" fill="none"
              viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span class="text-sm">{{ errorMsg }}</span>
          </div>

          <button type="submit" class="btn btn-primary w-full mt-2" :disabled="loading">
            <span v-if="loading" class="loading loading-spinner loading-sm"></span>
            {{ loading ? '登录中...' : '登 录' }}
          </button>
        </form>

        <!-- 开发提示 -->
        <div class="mt-6 bg-base-200/60 rounded-xl p-4">
          <div class="text-xs text-base-content/50 mb-2 font-medium">开发环境提示</div>
          <div class="text-xs text-base-content/70 space-y-1">
            <p>默认管理员手机号：<code class="bg-base-300 px-1 rounded">13800000000</code></p>
            <p>默认密码：<code class="bg-base-300 px-1 rounded">123456</code></p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { httpPost, isSuccess } from '@/utils/fetch'
import { useAuthStore } from '@/stores/auth'
import toast from '@/utils/toast'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const showPassword = ref(false)
const errorMsg = ref('')
const formData = ref({
  username: '',
  password: ''
})

/**
 * 处理登录
 * 后端接口：POST /auth/login
 */
const handleLogin = async () => {
  errorMsg.value = ''

  if (!formData.value.username || !formData.value.password) {
    errorMsg.value = '请输入手机号和密码'
    return
  }

  loading.value = true
  try {
    const res = await httpPost('/auth/login', {
      username: formData.value.username,
      password: formData.value.password
    })

    if (isSuccess(res) && res.data) {
      // 保存登录信息到 store
      authStore.setLoginInfo(res.data)
      toast.success('登录成功')
      // 跳转到系统管理首页
      router.push('/system/index')
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
