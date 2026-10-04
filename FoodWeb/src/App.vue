<script setup>
import { RouterView } from 'vue-router'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
</script>

<template>
  <RouterView />
  <!-- 全局确认弹窗（替代原生 confirm），单例 -->
  <ConfirmDialog />
</template>
