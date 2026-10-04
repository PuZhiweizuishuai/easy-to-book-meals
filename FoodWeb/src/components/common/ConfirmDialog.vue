<template>
  <!-- 全局确认弹窗（单例，挂载于 App.vue），替代原生 confirm() -->
  <dialog class="modal" :class="{ 'modal-open': state.open }">
    <div class="modal-box max-w-sm">
      <h3 class="font-bold text-lg flex items-center gap-2">
        <!-- 危险/警示操作前缀图标，强化语义 -->
        <svg v-if="state.tone === 'error'" xmlns="http://www.w3.org/2000/svg"
             class="w-5 h-5 text-error shrink-0" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd"
                d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l6.518 11.61c.75 1.335-.204 3.002-1.743 3.002H3.482c-1.539 0-2.493-1.667-1.743-3.002l6.518-11.61zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-.25-5.25a.75.75 0 00-1.5 0v2.5a.75.75 0 001.5 0v-2.5z"
                clip-rule="evenodd" />
        </svg>
        <svg v-else-if="state.tone === 'warning'" xmlns="http://www.w3.org/2000/svg"
             class="w-5 h-5 text-warning shrink-0" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd"
                d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a.75.75 0 000 1.5h.253a.25.25 0 01.244.304l-.459 2.066A1.75 1.75 0 0010.747 15H11a.75.75 0 000-1.5h-.253a.25.25 0 01-.244-.304l.459-2.066A1.75 1.75 0 009.253 9H9z"
                clip-rule="evenodd" />
        </svg>
        {{ state.title }}
      </h3>
      <p class="py-4 text-sm text-base-content/70 whitespace-pre-line">{{ state.message }}</p>
      <div class="modal-action">
        <button class="btn btn-ghost btn-sm" @click="handleCancel">{{ state.cancelText }}</button>
        <button :class="['btn btn-sm', confirmBtnClass]" @click="handleConfirm">
          {{ state.confirmText }}
        </button>
      </div>
    </div>
    <!-- 点击遮罩视为取消 -->
    <div class="modal-backdrop" @click="handleCancel"></div>
  </dialog>
</template>

<script setup>
import { computed } from 'vue'
import { useConfirm } from '@/composables/useConfirm'

const { confirmState: state, handleConfirm, handleCancel } = useConfirm()

const confirmBtnClass = computed(() => {
  if (state.tone === 'error') return 'btn-error'
  if (state.tone === 'warning') return 'btn-warning'
  return 'btn-primary'
})
</script>
