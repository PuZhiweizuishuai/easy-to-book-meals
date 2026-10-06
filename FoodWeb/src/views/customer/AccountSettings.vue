<template>
  <div class="min-h-screen bg-base-200">
    <!-- 顶部导航：返回上一页 + Logo + 页面名 -->
    <header class="navbar bg-base-100 shadow-sm sticky top-0 z-30 px-4">
      <div class="flex-1 flex items-center gap-2">
        <button class="btn btn-ghost btn-circle btn-sm" title="返回上一页" @click="goBack">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <router-link to="/" class="flex items-center gap-2">
          <span class="text-2xl">🍔</span>
          <span class="font-bold text-xl text-primary hidden sm:inline">餐易订</span>
        </router-link>
      </div>
      <div class="flex-none">
        <span class="text-sm font-medium text-base-content/70">账号设置</span>
      </div>
    </header>

    <div class="container mx-auto px-4 py-6 max-w-5xl">
      <div class="mb-6">
        <h2 class="text-2xl font-bold text-base-content">账号设置</h2>
        <p class="text-sm text-base-content/60 mt-1">管理头像昵称、登录密码与绑定手机号</p>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 个人资料：头像 + 昵称 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body">
            <h3 class="font-bold text-lg">个人资料</h3>
            <form @submit.prevent="handleUpdateProfile" class="space-y-4">
              <!-- 头像 -->
              <div class="form-control">
                <label class="label"><span class="label-text">头像</span></label>
                <div class="flex items-center gap-4">
                  <figure class="w-20 h-20 rounded-full bg-base-200 overflow-hidden flex-shrink-0 border border-base-300">
                    <img v-if="profileForm.avatarUrl" :src="profileForm.avatarUrl" alt="用户头像"
                         class="w-full h-full object-cover" />
                    <div v-else class="w-full h-full flex items-center justify-center text-2xl text-base-content/40">
                      {{ (profileForm.nickname || '用户')[0] }}
                    </div>
                  </figure>
                  <div class="flex flex-col gap-2">
                    <button type="button" class="btn btn-outline btn-sm" @click="showAvatarModal = true">
                      {{ profileForm.avatarUrl ? '更换头像' : '上传头像' }}
                    </button>
                    <button v-if="profileForm.avatarUrl" type="button"
                            class="btn btn-ghost btn-xs text-error" @click="profileForm.avatarUrl = ''">移除</button>
                  </div>
                </div>
              </div>

              <!-- 昵称 -->
              <div class="form-control">
                <label class="label"><span class="label-text">昵称 *</span></label>
                <input v-model.trim="profileForm.nickname" type="text" maxlength="50"
                       class="input input-bordered w-full" placeholder="给自己起个昵称" required />
              </div>

              <div class="card-actions justify-end pt-1">
                <button type="submit" class="btn btn-primary" :disabled="savingProfile">
                  <span v-if="savingProfile" class="loading loading-spinner loading-sm"></span>
                  {{ savingProfile ? '保存中...' : '保存修改' }}
                </button>
              </div>
            </form>
          </div>
        </div>

        <!-- 修改密码 -->
        <div class="card bg-base-100 shadow-sm border border-base-200">
          <div class="card-body">
            <h3 class="font-bold text-lg">修改密码</h3>
            <form @submit.prevent="handleChangePassword" class="space-y-4">
              <div class="form-control">
                <label class="label"><span class="label-text">原密码 *</span></label>
                <input v-model="passwordForm.oldPassword" type="password" autocomplete="current-password"
                       class="input input-bordered w-full" placeholder="请输入当前密码" required />
              </div>
              <div class="form-control">
                <label class="label"><span class="label-text">新密码 *</span></label>
                <input v-model="passwordForm.newPassword" type="password" autocomplete="new-password"
                       class="input input-bordered w-full" placeholder="6-32 位新密码" required minlength="6" maxlength="32" />
              </div>
              <div class="form-control">
                <label class="label"><span class="label-text">确认新密码 *</span></label>
                <input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password"
                       class="input input-bordered w-full" placeholder="请再次输入新密码" required minlength="6" maxlength="32" />
              </div>

              <div v-if="passwordError" class="text-error text-sm">{{ passwordError }}</div>

              <div class="card-actions justify-end pt-1">
                <button type="submit" class="btn btn-primary" :disabled="savingPassword">
                  <span v-if="savingPassword" class="loading loading-spinner loading-sm"></span>
                  {{ savingPassword ? '提交中...' : '确认修改' }}
                </button>
              </div>
            </form>
          </div>
        </div>

        <!-- 手机号（更换 / 首次绑定） -->
        <div class="card bg-base-100 shadow-sm border border-base-200 lg:col-span-2">
          <div class="card-body">
            <div class="flex items-center justify-between">
              <h3 class="font-bold text-lg">手机号</h3>
              <span v-if="hasPhone" class="text-sm text-base-content/50">
                当前：<span class="font-medium text-base-content/70">{{ maskedPhone }}</span>
              </span>
              <span v-else class="badge badge-ghost badge-sm">未绑定</span>
            </div>

            <form @submit.prevent="handleChangePhone" class="space-y-4 mt-2">
              <!-- 原手机号验证码（已绑定手机号时才需要） -->
              <div v-if="hasPhone" class="form-control">
                <label class="label"><span class="label-text">原手机号验证码 *</span></label>
                <div class="flex gap-2">
                  <input v-model.trim="phoneForm.oldCode" type="text" inputmode="numeric" maxlength="6"
                         class="input input-bordered flex-1" placeholder="发送到原手机号 {{ maskedPhone }} 的验证码" required />
                  <VerifyCodeButton :phone="currentPhone" scene="change_phone_old" />
                </div>
              </div>

              <!-- 新手机号 -->
              <div class="form-control">
                <label class="label">
                  <span class="label-text">{{ hasPhone ? '新手机号 *' : '绑定手机号 *' }}</span>
                  <span v-if="phoneForm.newPhone && !isNewPhoneValid" class="label-text-alt text-error">手机号格式不正确</span>
                </label>
                <input v-model.trim="phoneForm.newPhone" type="tel" maxlength="11"
                       class="input input-bordered w-full" placeholder="请输入 11 位手机号" required />
              </div>

              <!-- 新手机号验证码 -->
              <div class="form-control">
                <label class="label"><span class="label-text">新手机号验证码 *</span></label>
                <div class="flex gap-2">
                  <input v-model.trim="phoneForm.newCode" type="text" inputmode="numeric" maxlength="6"
                         class="input input-bordered flex-1" placeholder="发送到新手机号的验证码" required />
                  <VerifyCodeButton :phone="phoneForm.newPhone" scene="change_phone_new"
                                    :disabled="!isNewPhoneValid" />
                </div>
              </div>

              <p class="text-xs text-base-content/40">
                暂无短信网关，验证码将打印在后端日志中；请在日志里查看后填入。
              </p>

              <div v-if="phoneError" class="text-error text-sm">{{ phoneError }}</div>

              <div class="card-actions justify-end pt-1">
                <button type="submit" class="btn btn-primary" :disabled="savingPhone">
                  <span v-if="savingPhone" class="loading loading-spinner loading-sm"></span>
                  {{ savingPhone ? '提交中...' : (hasPhone ? '确认更换' : '确认绑定') }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>

    <!-- 头像上传弹窗 -->
    <Teleport to="body">
      <dialog :class="['modal modal-middle', { 'modal-open': showAvatarModal }]">
        <div class="modal-box w-11/12 max-w-2xl">
          <h3 class="font-bold text-lg mb-4">上传头像</h3>
          <ImageUpload :allow-multiple="false" aspect-ratio="1:1" @upload-success="handleAvatarUploadSuccess" />
          <div class="modal-action">
            <button class="btn btn-ghost" @click="showAvatarModal = false">完成</button>
          </div>
        </div>
        <div class="modal-backdrop" @click="showAvatarModal = false"></div>
      </dialog>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { httpPost, isSuccess } from '@/utils/fetch'
import { useAuthStore } from '@/stores/auth'
import toast from '@/utils/toast'
import ImageUpload from '@/components/upload/ImageUpload.vue'
import VerifyCodeButton from '@/components/common/VerifyCodeButton.vue'

const router = useRouter()
const authStore = useAuthStore()

/** 返回上一页；若没有可回退的历史记录（如直接打开链接）则回首页 */
const goBack = () => {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.push('/')
  }
}

const showAvatarModal = ref(false)
const savingProfile = ref(false)
const savingPassword = ref(false)
const savingPhone = ref(false)
const passwordError = ref('')
const phoneError = ref('')

const profileForm = ref({
  nickname: authStore.userInfo?.nickname || '',
  avatarUrl: authStore.userInfo?.avatarUrl || ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const phoneForm = ref({
  oldCode: '',
  newPhone: '',
  newCode: ''
})

/** 当前账号已绑定的手机号（真实号码） */
const currentPhone = computed(() => authStore.userInfo?.phone || '')
const hasPhone = computed(() => !!currentPhone.value)

const maskedPhone = computed(() => {
  const p = currentPhone.value
  return p.length === 11 ? p.substring(0, 3) + '****' + p.substring(7) : p
})

const isNewPhoneValid = computed(() => /^1[3-9]\d{9}$/.test(phoneForm.value.newPhone))

/** 头像上传成功回调 */
const handleAvatarUploadSuccess = (response) => {
  if (!response?.data?.[0]?.fileUrl) return
  profileForm.value.avatarUrl = response.data[0].fileUrl
  showAvatarModal.value = false
}

/** 更新头像昵称，成功后刷新本地用户信息 */
const handleUpdateProfile = async () => {
  if (!profileForm.value.nickname) {
    toast.error('昵称不能为空')
    return
  }
  savingProfile.value = true
  try {
    const res = await httpPost('/user/account/profile', {
      nickname: profileForm.value.nickname,
      avatarUrl: profileForm.value.avatarUrl
    })
    if (isSuccess(res)) {
      toast.success('个人资料已更新')
      await authStore.refreshUserInfo()
    } else {
      toast.error(res?.message || '保存失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('保存失败，请稍后重试')
  } finally {
    savingProfile.value = false
  }
}

/** 修改密码（前端基础校验 + 后端校验原密码） */
const handleChangePassword = async () => {
  passwordError.value = ''
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value

  if (!oldPassword) {
    passwordError.value = '请输入原密码'
    return
  }
  if (newPassword.length < 6 || newPassword.length > 32) {
    passwordError.value = '新密码长度需在 6-32 位之间'
    return
  }
  if (newPassword !== confirmPassword) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }
  if (newPassword === oldPassword) {
    passwordError.value = '新密码不能与原密码相同'
    return
  }

  savingPassword.value = true
  try {
    const res = await httpPost('/user/account/password', { oldPassword, newPassword })
    if (isSuccess(res)) {
      toast.success('密码修改成功')
      passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    } else {
      passwordError.value = res?.message || '密码修改失败'
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    passwordError.value = '密码修改失败，请稍后重试'
  } finally {
    savingPassword.value = false
  }
}

/** 更换 / 首次绑定手机号（后端对新旧号码验证码做校验） */
const handleChangePhone = async () => {
  phoneError.value = ''
  const { oldCode, newPhone, newCode } = phoneForm.value

  if (!isNewPhoneValid.value) {
    phoneError.value = '请输入正确的 11 位新手机号'
    return
  }
  if (hasPhone.value && !oldCode) {
    phoneError.value = '请输入原手机号验证码'
    return
  }
  if (!newCode) {
    phoneError.value = '请输入新手机号验证码'
    return
  }

  savingPhone.value = true
  try {
    const res = await httpPost('/user/account/phone', { oldCode, newPhone, newCode })
    if (isSuccess(res)) {
      toast.success(res.message || (hasPhone.value ? '手机号修改成功' : '手机号绑定成功'))
      phoneForm.value = { oldCode: '', newPhone: '', newCode: '' }
      // 刷新本地用户信息，当前手机号变为新号码
      await authStore.refreshUserInfo()
    } else {
      phoneError.value = res?.message || '操作失败'
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    phoneError.value = '操作失败，请稍后重试'
  } finally {
    savingPhone.value = false
  }
}
</script>
