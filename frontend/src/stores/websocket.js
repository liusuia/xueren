import { defineStore } from 'pinia'
import { ref } from 'vue'
import { setOnStateChange } from '../api/ws'

export const useWebSocketStore = defineStore('websocket', () => {
  const isConnected = ref(false)

  setOnStateChange((connected) => {
    isConnected.value = connected
  })

  return { isConnected }
})
