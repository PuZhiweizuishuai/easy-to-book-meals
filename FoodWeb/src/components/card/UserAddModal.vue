<template>
  <dialog class="modal" :class="{ 'modal-open': show }">
    <div class="modal-box w-11/12 max-w-lg">
      <h3 class="font-bold text-lg mb-4 flex items-center gap-2">
        <span class="w-1.5 h-6 rounded-full bg-primary"></span>
        新增用户
      </h3>

      <form @submit.prevent="handleSubmit" class="flex flex-col gap-4">
        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">昵称 <span class="text-error">*</span></span></label>
          <input v-model="formData.nickname" type="text" placeholder="请输入昵称"
            class="input input-bordered w-full focus:input-primary" required />
        </div>

        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">密码 <span class="text-error">*</span></span></label>
          <input v-model="formData.password" type="password" placeholder="请输入密码"
            class="input input-bordered w-full focus:input-primary" required minlength="6" />
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">手机号</span></label>
            <input v-model="formData.phone" type="text" placeholder="请输入手机号"
              class="input input-bordered w-full focus:input-primary" maxlength="11" />
          </div>
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">邮箱</span></label>
            <input v-model="formData.email" type="email" placeholder="请输入邮箱"
              class="input input-bordered w-full focus:input-primary" />
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">性别</span></label>
            <select v-model="formData.gender" class="select select-bordered w-full focus:select-primary">
              <option :value="0">未知</option>
              <option :value="1">男</option>
              <option :value="2">女</option>
            </select>
          </div>
          <div class="form-control w-full">
            <label class="label"><span class="label-text font-medium">收货地址</span></label>
            <input v-model="formData.address" type="text" placeholder="请输入收货地址"
              class="input input-bordered w-full focus:input-primary" />
          </div>
        </div>

        <div class="modal-action mt-2">
          <button type="button" class="btn btn-ghost" @click="handleClose" :disabled="submitting">取消</button>
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            <span v-if="submitting" class="loading loading-spinner loading-xs"></span>
            创建
          </button>
        </div>
      </form>
    </div>
    <div class="modal-backdrop" @click="!submitting && handleClose()"></div>
  </dialog>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'submit'])

const submitting = ref(false)
const formData = ref(getDefaultFormData())

function getDefaultFormData() {
  return {
    nickname: '',
    password: '',
    phone: '',
    email: '',
    gender: 0,
    address: ''
  }
}

watch(() => props.show, (newVal) => {
  if (newVal) {
    formData.value = getDefaultFormData()
  }
})

const handleClose = () => {
  emit('close')
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    emit('submit', { ...formData.value })
  } finally {
    submitting.value = false
  }
}
</script>
