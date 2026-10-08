<template>
  <div class="min-h-screen flex bg-base-100">
    <!-- 返回首页 -->
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
      <div class="absolute inset-0 opacity-10">
        <div class="absolute top-20 left-10 w-64 h-64 rounded-full bg-white/20 blur-3xl"></div>
        <div class="absolute bottom-20 right-10 w-96 h-96 rounded-full bg-secondary/30 blur-3xl"></div>
      </div>

      <div class="relative z-10 flex flex-col justify-center items-center w-full px-16 text-primary-content">
        <div class="text-8xl mb-8 drop-shadow-lg">🔑</div>
        <h1 class="text-4xl font-bold mb-4 tracking-wide">找回账号</h1>
        <p class="text-lg opacity-80 text-center leading-relaxed max-w-md">
          账号绑定的手机号或邮箱收到验证码<br />
          校验通过后即可设置新密码
        </p>
      </div>
    </div>

    <!-- 右侧重置表单区域 -->
    <div class="flex-1 flex items-center justify-center p-4 sm:p-8">
      <div class="w-full max-w-md">
        <div class="lg:hidden text-center mb-8">
          <div class="text-5xl mb-2">🍔</div>
          <h1 class="text-2xl font-bold text-primary">餐易订</h1>
        </div>

        <div class="mb-8">
          <h2 class="text-2xl sm:text-3xl font-bold">重置密码</h2>
          <p class="text-base-content/50 mt-2">输入注册时使用的手机号或邮箱</p>
        </div>

        <form @submit.prevent="handleReset" class="space-y-5">
          <!-- 手机号/邮箱 -->
          <div class="form-control w-full">
            <label class="label">
              <span class="label-text font-medium">手机号 / 邮箱</span>
            </label>
            <input
              v-model="formData.target"
              type="text"
              placeholder="请输入已注册的手机号或邮箱"
              class="input input-bordered w-full focus:input-primary"
              required
              autocomplete="username"
              @blur="validateTarget"
            />
            <label v-if="targetError" class="label">
              <span class="label-text-alt text-error">{{ targetError }}</span>
            </label>
          </div>

          <!-- 验证码 -->
          <div class="form-control w-full">
            <label class="label">
              <span class="label-text font-medium">验证码</span>
            </label>
            <div class="flex gap-3">
              <input
                v-model="formData.code"
                type="text"
                inputmode="numeric"
                placeholder="请输入验证码"
                class="input input-bordered w-full focus:input-primary"
                required
                maxlength="6"
                autocomplete="one-time-code"
              />
              <VerifyCodeButton
                scene="reset_password"
                :target="formData.target"
                :disabled="!isTargetValid"
              />
            </div>
          </div>

          <!-- 新密码 -->
          <div class="form-control w-full">
            <label class="label">
              <span class="label-text font-medium">新密码</span>
            </label>
            <input
              v-model="formData.password"
              type="password"
              placeholder="6-32 位密码"
              class="input input-bordered w-full focus:input-primary"
              required
              minlength="6"
              maxlength="32"
              autocomplete="new-password"
            />
          </div>

          <!-- 确认新密码 -->
          <div class="form-control w-full">
            <label class="label">
              <span class="label-text font-medium">确认新密码</span>
            </label>
            <input
              v-model="formData.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              class="input input-bordered w-full focus:input-primary"
              required
              minlength="6"
              maxlength="32"
              autocomplete="new-password"
            />
          </div>

          <!-- 错误提示 -->
          <div v-if="errorMsg" class="alert alert-error alert-sm py-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-4 w-4" fill="none" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span class="text-sm">{{ errorMsg }}</span>
          </div>

          <button type="submit" class="btn btn-primary w-full" :disabled="loading">
            <span v-if="loading" class="loading loading-spinner loading-sm"></span>
            {{ loading ? '提交中...' : '重置密码' }}
          </button>
        </form>

        <div class="mt-6 text-center text-sm text-base-content/60">
          想起密码了？
          <a href="javascript:void(0)" class="link link-primary link-hover font-medium" @click="goLogin">
            返回登录
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { httpPost, isSuccess } from '@/utils/fetch'
import toast from '@/utils/toast'
import VerifyCodeButton from '@/components/common/VerifyCodeButton.vue'

const router = useRouter()

const loading = ref(false)
const errorMsg = ref('')
const targetError = ref('')

const formData = reactive({
  target: '',
  code: '',
  password: '',
  confirmPassword: ''
})

const PHONE_PATTERN = /^1[3-9]\d{9}$/
const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

/** 目标是否为格式合法的手机号或邮箱 */
const isTargetValid = computed(() => {
  const t = formData.target.trim()
  return PHONE_PATTERN.test(t) || EMAIL_PATTERN.test(t)
})

const validateTarget = () => {
  const t = formData.target.trim()
  if (!t) {
    targetError.value = ''
    return
  }
  targetError.value = isTargetValid.value ? '' : '请输入正确的手机号或邮箱'
}

const goLogin = () => {
  router.push({ name: 'login' })
}

const goHome = () => {
  router.push('/')
}

/**
 * 提交重置密码
 * 1. 前端校验目标格式、验证码非空、两次新密码一致
 * 2. 调用公开重置接口（后端校验验证码正确性与账号存在性）
 * 3. 成功后跳转登录页
 */
const handleReset = async () => {
  errorMsg.value = ''

  if (!isTargetValid.value) {
    errorMsg.value = '请输入正确的手机号或邮箱'
    return
  }
  if (!formData.code.trim()) {
    errorMsg.value = '请输入验证码'
    return
  }
  if (formData.password.length < 6 || formData.password.length > 32) {
    errorMsg.value = '密码长度需在 6-32 位之间'
    return
  }
  if (formData.password !== formData.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  try {
    const res = await httpPost('/user/reset/password', {
      target: formData.target.trim(),
      code: formData.code.trim(),
      newPassword: formData.password
    })
    if (isSuccess(res)) {
      toast.success(res.message || '密码重置成功，请登录')
      router.push({ name: 'login' })
    } else {
      errorMsg.value = res?.message || '重置失败，请稍后重试'
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    errorMsg.value = e?.message || '重置失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>
