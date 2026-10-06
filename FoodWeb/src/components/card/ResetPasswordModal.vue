<template>
  <dialog class="modal" :class="{ 'modal-open': show }">
    <div class="modal-box w-11/12 max-w-md">
      <h3 class="font-bold text-lg mb-4 flex items-center gap-2">
        <span class="w-1.5 h-6 rounded-full bg-error"></span>
        重置密码 - {{ user?.nickname || `用户#${user?.id}` }}
      </h3>

      <div class="alert alert-warning mb-4">
        <svg xmlns="http://www.w3.org/2000/svg" class="stroke-current shrink-0 h-5 w-5" fill="none"
          viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        <span class="text-sm">此操作将重置该用户密码，请谨慎操作</span>
      </div>

      <form @submit.prevent="handleSubmit" class="flex flex-col gap-4">
        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">新密码 <span class="text-error">*</span></span></label>
          <input v-model="newPassword" type="password" placeholder="请输入新密码（至少6位）"
            class="input input-bordered w-full focus:input-primary" required minlength="6" />
        </div>

        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">确认密码 <span class="text-error">*</span></span></label>
          <input v-model="confirmPassword" type="password" placeholder="请再次输入新密码"
            class="input input-bordered w-full focus:input-primary" required minlength="6" />
        </div>

        <div v-if="errorMsg" class="text-error text-sm">{{ errorMsg }}</div>

        <div class="modal-action mt-2">
          <button type="button" class="btn btn-ghost" @click="handleClose" :disabled="submitting">取消</button>
          <button type="submit" class="btn btn-error" :disabled="submitting">
            <span v-if="submitting" class="loading loading-spinner loading-xs"></span>
            确认重置
          </button>
        </div>
      </form>
    </div>
    <div class="modal-backdrop" @click="!submitting && handleClose()"></div>
  </dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { httpPost, isSuccess } from '@/utils/fetch'
import toast from '@/utils/toast'

const props = defineProps({
  show: { type: Boolean, default: false },
  user: { type: Object, default: null }
})

const emit = defineEmits(['close', 'submit'])

const submitting = ref(false)
const newPassword = ref('')
const confirmPassword = ref('')
const errorMsg = ref('')

watch(() => props.show, (newVal) => {
  if (newVal) {
    newPassword.value = ''
    confirmPassword.value = ''
    errorMsg.value = ''
  }
})

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  errorMsg.value = ''

  if (newPassword.value.length < 6) {
    errorMsg.value = '密码长度不能少于6位'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  submitting.value = true
  try {
    // 走管理员专用重置接口：仅更新密码，不清空该用户昵称/手机号/邮箱等资料
    const res = await httpPost(
      `/sys/user/password/reset?userId=${props.user.id}&newPassword=${encodeURIComponent(newPassword.value)}`
    )
    if (isSuccess(res)) {
      toast.success('密码重置成功')
      emit('submit')
      handleClose()
    } else {
      toast.error(res?.message || '密码重置失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error('密码重置失败')
  } finally {
    submitting.value = false
  }
}
</script>
