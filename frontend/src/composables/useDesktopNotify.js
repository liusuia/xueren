import { ref } from 'vue'

const enabled = ref(localStorage.getItem('xr-desk-notify') !== '0')
const permission = ref(Notification.permission)

export function useDesktopNotify() {
  async function request() {
    if (!('Notification' in window)) return false
    const result = await Notification.requestPermission()
    permission.value = result
    return result === 'granted'
  }

  function notify(title, body, icon) {
    if (!enabled.value || permission.value !== 'granted') return
    if (document.visibilityState === 'visible') return // 窗口可见时不弹
    const n = new Notification(title, { body, icon, tag: 'qingyu-msg', requireInteraction: false })
    n.onclick = () => { window.focus(); n.close() }
    setTimeout(() => n.close(), 5000)
  }

  function toggle() {
    enabled.value = !enabled.value
    localStorage.setItem('xr-desk-notify', enabled.value ? '1' : '0')
    if (enabled.value && permission.value !== 'granted') {
      request()
    }
  }

  return { enabled, permission, request, notify, toggle }
}
