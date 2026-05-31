import { ref, onUnmounted } from 'vue'
import { addWsListener, setOnStateChange, connectWs, disconnectWs } from '../api/ws'

const isConnected = ref(false)
const unsubscribers = []

// 全局连接状态追踪
setOnStateChange((connected) => {
  isConnected.value = connected
})

export function useWebSocket() {
  const localUnsubs = []

  function subscribe(type, callback) {
    const unsub = addWsListener((packet) => {
      if (packet.type === type) {
        callback(packet.data || packet)
      }
    })
    localUnsubs.push(unsub)
    return unsub
  }

  function onNewMessage(callback) {
    return subscribe('NEW_MESSAGE', callback)
  }

  function onMessageRecalled(callback) {
    return subscribe('MESSAGE_RECALLED', callback)
  }

  function connect(token) {
    connectWs(token)
  }

  function disconnect() {
    disconnectWs(true)
  }

  onUnmounted(() => {
    localUnsubs.forEach(fn => fn())
  })

  return { isConnected, connect, disconnect, onNewMessage, onMessageRecalled }
}
