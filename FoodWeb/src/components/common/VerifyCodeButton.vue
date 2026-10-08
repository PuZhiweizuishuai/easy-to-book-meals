<template>
  <button
    type="button"
    class="btn btn-outline btn-sm whitespace-nowrap"
    :disabled="disabled || sending || countdown > 0"
    @click="handleSend"
  >
    <span v-if="sending" class="loading loading-spinner loading-xs"></span>
    <span v-if="countdown > 0">{{ countdown }}s 后重发</span>
    <span v-else>{{ label }}</span>
  </button>
</template>

<script setup>
import { ref, onBeforeUnmount } from 'vue'
import { httpPost, isSuccess } from '@/utils/fetch'
import toast from '@/utils/toast'

/**
 * 验证码发送按钮（带发送后倒计时）
 * <p>
 * 由 scene 决定场景与接口：
 * <ul>
 *   <li>注册场景（register）：调用 POST /user/register/verify-code/send，使用 target（手机号或邮箱）</li>
 *   <li>忘记密码场景（reset_password）：调用 POST /user/reset/verify-code/send，使用 target，未登录可用</li>
 *   <li>其它场景（换绑手机号等）：调用 POST /user/verify-code/send，使用 phone</li>
 * </ul>
 * 倒计时与后端 resend-interval-seconds 保持一致（默认 60s）。
 */
const props = defineProps({
  /** 目标手机号（换绑场景下部分场景服务端会忽略） */
  phone: { type: String, default: '' },
  /** 目标手机号或邮箱（注册、忘记密码场景使用） */
  target: { type: String, default: '' },
  /** 场景编码（VerificationCodeScene） */
  scene: { type: String, required: true },
  /** 外部禁用条件（如目标尚未通过格式校验） */
  disabled: { type:Boolean, default: false },
  /** 按钮文案 */
  label: { type: String, default: '获取验证码' },
  /** 倒计时秒数 */
  cooldown: { type: Number, default: 60 }
})

const emit = defineEmits(['sent'])

const sending = ref(false)
const countdown = ref(0)
let timer = null

/** 免登录场景：以 target（手机号或邮箱）为发送目标 */
const PUBLIC_SCENES = {
  register: '/user/register/verify-code/send',
  reset_password: '/user/reset/verify-code/send'
}

const handleSend = async () => {
  if (props.disabled || sending.value || countdown.value > 0) return
  sending.value = true
  try {
    const publicUrl = PUBLIC_SCENES[props.scene]
    const body = publicUrl
      ? { target: props.target, scene: props.scene }
      : { phone: props.phone, scene: props.scene }
    const res = await httpPost(publicUrl || '/user/verify-code/send', body)
    if (isSuccess(res)) {
      toast.success(res.message || '验证码已发送')
      emit('sent')
      startCountdown()
    } else {
      toast.error(res?.message || '验证码发送失败')
    }
  } catch (e) {
    if (e?.type === 'UNAUTHORIZED') return
    toast.error(e?.message || '验证码发送失败')
  } finally {
    sending.value = false
  }
}

const startCountdown = () => {
  countdown.value = props.cooldown
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>
