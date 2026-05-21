<template>
  <dialog class="modal" :class="{ 'modal-open': show }">
    <div class="modal-box w-11/12 max-w-lg">
      <h3 class="font-bold text-lg mb-4 flex items-center gap-2">
        <span class="w-1.5 h-6 rounded-full bg-info"></span>
        编辑用户信息
      </h3>

      <form @submit.prevent="handleSubmit" class="flex flex-col gap-4">
        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">头像</span></label>
          <ImageUpload :allow-multiple="false" @upload-success="handleAvatarUpload" @remove-file="handleAvatarRemove" />
          <div v-if="formData.avatarUrl" class="mt-2">
            <div class="avatar">
              <div class="w-16 h-16 rounded-full">
                <img :src="formData.avatarUrl" alt="当前头像" />
              </div>
            </div>
          </div>
        </div>

        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">昵称 <span class="text-error">*</span></span></label>
          <input v-model="formData.nickname" type="text" placeholder="请输入昵称"
            class="input input-bordered w-full focus:input-primary" required />
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
            <label class="label"><span class="label-text font-medium">状态</span></label>
            <select v-model="formData.status" class="select select-bordered w-full focus:select-primary">
              <option :value="0">正常</option>
              <option :value="1">禁用</option>
            </select>
          </div>
        </div>

        <div class="form-control w-full">
          <label class="label"><span class="label-text font-medium">收货地址</span></label>
          <textarea v-model="formData.address" class="textarea textarea-bordered h-16 focus:textarea-primary"
            placeholder="请输入收货地址"></textarea>
        </div>

        <div class="modal-action mt-2">
          <button type="button" class="btn btn-ghost" @click="handleClose" :disabled="submitting">取消</button>
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            <span v-if="submitting" class="loading loading-spinner loading-xs"></span>
            保存
          </button>
        </div>
      </form>
    </div>
    <div class="modal-backdrop" @click="!submitting && handleClose()"></div>
  </dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import ImageUpload from '@/components/upload/ImageUpload.vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  user: { type: Object, default: null }
})

const emit = defineEmits(['close', 'submit'])

const submitting = ref(false)
const formData = ref(getDefaultFormData())

function getDefaultFormData() {
  return {
    id: null,
    nickname: '',
    avatarUrl: '',
    phone: '',
    email: '',
    gender: 0,
    status: 0,
    address: ''
  }
}

watch(() => props.show, (newVal) => {
  if (newVal && props.user) {
    formData.value = {
      id: props.user.id,
      nickname: props.user.nickname || '',
      avatarUrl: props.user.avatarUrl || '',
      phone: props.user.phone || '',
      email: props.user.email || '',
      gender: props.user.gender ?? 0,
      status: props.user.status ?? 0,
      address: props.user.address || ''
    }
  }
})

const handleAvatarUpload = (response) => {
  if (response?.data && Array.isArray(response.data) && response.data.length > 0) {
    formData.value.avatarUrl = response.data[0].url || response.data[0]
  }
}

const handleAvatarRemove = () => {
  formData.value.avatarUrl = ''
}

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
