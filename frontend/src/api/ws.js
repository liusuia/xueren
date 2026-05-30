let socket = null
let listeners = []
let manualClose = false
let currentToken = null
let pingTimer = null
let reconnectTimer = null
let onStateChange = null

function getWsUrl() {
  const hostname = window.location.hostname
  const isLan = /^\d+\.\d+\.\d+\.\d+$/.test(hostname)
  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    return 'ws://localhost:8081/ws'
  }
  if (isLan) {
    return `ws://${hostname}:8081/ws`
  }
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${hostname}/ws`
}

export function setOnStateChange(fn) {
  onStateChange = fn
}

function notifyState() {
  if (onStateChange) onStateChange(socket && socket.readyState === WebSocket.OPEN)
}

export function connectWs(token) {
  if (!token) return
  currentToken = token
  doConnect()
}

function doConnect() {
  if (!currentToken) return
  cleanup()

  const url = `${getWsUrl()}?token=${encodeURIComponent(currentToken)}`
  console.log('[WS] 连接:', url)
  manualClose = false

  try {
    socket = new WebSocket(url)
  } catch (e) {
    console.error('[WS] 创建失败:', e.message)
    scheduleReconnect()
    return
  }

  socket.onopen = () => {
    console.log('[WS] 已连接')
    notifyState()
    startPing()
  }

  socket.onmessage = (event) => {
    try {
      const packet = JSON.parse(event.data)
      if (packet.type === 'PONG') return
      listeners.forEach((fn) => fn(packet))
    } catch (e) {}
  }

  socket.onerror = () => {
    console.error('[WS] 连接错误')
  }

  socket.onclose = (event) => {
    console.warn('[WS] 断开:', event.code)
    notifyState()
    stopPing()
    if (!manualClose) scheduleReconnect()
  }
}

function scheduleReconnect() {
  if (reconnectTimer) return
  console.log('[WS] 3秒后重连...')
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    if (!manualClose && currentToken) doConnect()
  }, 3000)
}

function cleanup() {
  stopPing()
  if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
  if (socket) { try { socket.close() } catch (e) {}; socket = null }
}

function startPing() {
  stopPing()
  pingTimer = setInterval(() => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'PING' }))
    }
  }, 30000)
}

function stopPing() {
  if (pingTimer) { clearInterval(pingTimer); pingTimer = null }
}

export function disconnectWs(clearListeners = true) {
  manualClose = true
  if (clearListeners) listeners = []
  cleanup()
}

export function sendChat(data) {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    throw new Error('WebSocket 未连接')
  }
  socket.send(JSON.stringify({ type: 'CHAT', data }))
}

export function addWsListener(fn) {
  listeners.push(fn)
  return () => { listeners = listeners.filter((item) => item !== fn) }
}

export function getSocket() {
  return socket
}
