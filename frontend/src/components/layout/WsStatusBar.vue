<template>
  <Transition name="ws-fade">
    <div v-if="!connected" class="ws-bar">
      <span class="ws-dot"></span>
      <span>{{ reconnecting ? '连接断开，正在重连...' : '连接已断开' }}</span>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { setOnStateChange } from '../../api/ws'

const connected = ref(true)
const reconnecting = ref(false)
let unsub = null
let timer = null

onMounted(() => {
  unsub = setOnStateChange((isOpen) => {
    connected.value = isOpen
    if (isOpen) {
      reconnecting.value = false
      clearTimeout(timer)
    } else {
      timer = setTimeout(() => { reconnecting.value = true }, 2000)
    }
  })
})

onUnmounted(() => {
  unsub?.()
  clearTimeout(timer)
})
</script>

<style scoped>
.ws-bar {
  position: fixed; top: 0; left: 0; right: 0; z-index: 9999;
  height: 28px; display: flex; align-items: center; justify-content: center; gap: 8px;
  background: #e74c3c; color: #fff; font-size: 12px;
}
.ws-dot {
  width: 8px; height: 8px; border-radius: 50%; background: #fff;
  animation: ws-blink 1s infinite;
}
@keyframes ws-blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
.ws-fade-enter-active, .ws-fade-leave-active { transition: all 0.3s; }
.ws-fade-enter-from, .ws-fade-leave-to { transform: translateY(-100%); opacity: 0; }
</style>
