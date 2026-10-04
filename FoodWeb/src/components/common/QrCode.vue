<template>
  <!-- 通用二维码组件：把文本内容渲染为二维码图片（用于优惠券码、餐桌二维码等） -->
  <div class="flex flex-col items-center gap-2">
    <div class="bg-white rounded-lg p-2 border border-base-200 inline-flex">
      <img v-if="dataUrl" :src="dataUrl" :alt="value" :style="{ width: size + 'px', height: size + 'px' }" />
      <div v-else class="bg-base-200 animate-pulse rounded"
           :style="{ width: size + 'px', height: size + 'px' }"></div>
    </div>
    <span v-if="showText" class="font-mono text-xs text-base-content/50 break-all text-center max-w-full">{{ value }}</span>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import QRCode from 'qrcode'

const props = defineProps({
  /** 二维码内容 */
  value: { type: String, default: '' },
  /** 边长（像素） */
  size: { type: Number, default: 160 },
  /** 是否在二维码下方展示原始文本 */
  showText: { type: Boolean, default: false }
})

const dataUrl = ref('')

const generate = async () => {
  if (!props.value) {
    dataUrl.value = ''
    return
  }
  try {
    dataUrl.value = await QRCode.toDataURL(props.value, {
      width: props.size * 2,
      margin: 1,
      color: { dark: '#000000', light: '#FFFFFF' }
    })
  } catch (e) {
    console.error('二维码生成失败:', e)
    dataUrl.value = ''
  }
}

onMounted(generate)
watch(() => [props.value, props.size], generate)
</script>
