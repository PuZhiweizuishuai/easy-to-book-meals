<template>
  <div>
    <div class="mb-6">
      <h2 class="text-2xl font-bold text-base-content">系统设置</h2>
      <p class="text-sm text-base-content/60 mt-1">配置注册开关与登录安全策略，保存后实时生效</p>
    </div>

    <div class="grid grid-cols-1 gap-6 max-w-3xl">
      <!-- 注册与登录安全 -->
      <div class="card bg-base-100 shadow-sm border border-base-200">
        <div class="card-body">
          <h3 class="font-bold text-lg">注册与登录安全</h3>

          <!-- 是否开放注册 -->
          <div class="flex items-center justify-between py-2">
            <div>
              <div class="font-medium">开放用户注册</div>
              <p class="text-xs text-base-content/50 mt-0.5">
                关闭后，任何用户都无法注册新账号，已注册用户登录不受影响
              </p>
            </div>
            <input v-model="form.registrationEnabled" type="checkbox" class="toggle toggle-primary" />
          </div>

          <div class="divider my-1"></div>

          <!-- 登录错误次数 -->
          <div class="flex items-center justify-between py-2">
            <div>
              <div class="font-medium">登录失败次数上限</div>
              <p class="text-xs text-base-content/50 mt-0.5">
                同一账号或同一 IP 在限制时间内连续密码错误达到该次数后将被锁定（1-100）
              </p>
            </div>
            <input v-model.number="form.maxLoginFailCount" type="number" min="1" max="100"
                   class="input input-bordered w-28" />
          </div>

          <div class="divider my-1"></div>

          <!-- 锁定 / 限制时间 -->
          <div class="flex items-center justify-between py-2">
            <div>
              <div class="font-medium">失败限制时间（分钟）</div>
              <p class="text-xs text-base-content/50 mt-0.5">
                失败计数的时间窗口；触发上限后账号 / IP 锁定此时长，到期自动解锁（1-1440）
              </p>
            </div>
            <input v-model.number="form.loginLockMinutes" type="number" min="1" max="1440"
                   class="input input-bordered w-28" />
          </div>

          <div v-if="errorMsg" class="alert alert-error alert-sm py-2 mt-2">
            <span class="text-sm">{{ errorMsg }}</span>
          </div>

          <div class="card-actions justify-end mt-3">
            <button class="btn btn-primary" :disabled="saving" @click="handleSave">
              <span v-if="saving" class="loading loading-spinner loading-sm"></span>
              {{ saving ? '保存中...' : '保存设置' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { httpGet, httpPost, isSuccess } from '@/utils/fetch'
import toast from '@/utils/toast'

const loading = ref(false)
const saving = ref(false)
const errorMsg = ref('')

const form = ref({
  registrationEnabled: true,
  maxLoginFailCount: 3,
  loginLockMinutes: 10
})

onMounted(() => {
  loadSettings()
})

/** 加载当前系统设置 */
const loadSettings = async () => {
  loading.value = true
  try {
    const res = await httpGet('/sys/settings')
    if (isSuccess(res) && res.data) {
      form.value = {
        registrationEnabled: res.data.registrationEnabled !== false,
        maxLoginFailCount: res.data.maxLoginFailCount ?? 3,
        loginLockMinutes: res.data.loginLockMinutes ?? 10
      }
    } else {
      toast.error(res?.message || '加载系统设置失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('加载系统设置失败')
  } finally {
    loading.value = false
  }
}

/** 校验并保存 */
const handleSave = async () => {
  errorMsg.value = ''
  const { maxLoginFailCount, loginLockMinutes } = form.value

  if (!Number.isInteger(maxLoginFailCount) || maxLoginFailCount < 1 || maxLoginFailCount > 100) {
    errorMsg.value = '登录失败次数需为 1-100 之间的整数'
    return
  }
  if (!Number.isInteger(loginLockMinutes) || loginLockMinutes < 1 || loginLockMinutes > 1440) {
    errorMsg.value = '失败限制时间需为 1-1440 分钟之间的整数'
    return
  }

  saving.value = true
  try {
    const res = await httpPost('/sys/settings', {
      registrationEnabled: form.value.registrationEnabled,
      maxLoginFailCount,
      loginLockMinutes
    })
    if (isSuccess(res)) {
      toast.success('系统设置已保存')
      if (res.data) {
        form.value = {
          registrationEnabled: res.data.registrationEnabled !== false,
          maxLoginFailCount: res.data.maxLoginFailCount,
          loginLockMinutes: res.data.loginLockMinutes
        }
      }
    } else {
      errorMsg.value = res?.message || '保存失败'
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    errorMsg.value = '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}
</script>
