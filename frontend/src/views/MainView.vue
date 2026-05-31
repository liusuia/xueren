<template>
  <MainLayout />
</template>

<script setup>
import MainLayout from '../components/layout/MainLayout.vue'
import { onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()

onMounted(() => {
  // 页面刷新时恢复 WebSocket
  if (auth.token) {
    auth.restoreWs()
  }
})
</script>
