import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const activeTab = ref('chat') // 'chat' | 'contacts'
  const isChatOpen = ref(false)
  const column2Width = ref(280)

  function readTheme() {
    return localStorage.getItem('xr-theme') || 'dark'
  }
  function applyTheme(val) {
    document.documentElement.setAttribute('data-theme', val)
  }

  const theme = ref(readTheme())
  // 启动时立即应用主题，避免闪烁
  applyTheme(theme.value)

  // 监听其他标签页修改主题
  window.addEventListener('storage', (e) => {
    if (e.key === 'xr-theme' && e.newValue) {
      theme.value = e.newValue
      applyTheme(e.newValue)
    }
  })

  function setActiveTab(tab) { activeTab.value = tab }
  function openChat() { isChatOpen.value = true }
  function closeChat() { isChatOpen.value = false }
  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    localStorage.setItem('xr-theme', theme.value)
    applyTheme(theme.value)
  }
  function setTheme(val) {
    theme.value = val
    localStorage.setItem('xr-theme', val)
    applyTheme(val)
  }
  function resizeColumn2(width) {
    column2Width.value = Math.max(200, Math.min(500, width))
  }

  return { activeTab, isChatOpen, theme, column2Width, setActiveTab, openChat, closeChat, toggleTheme, setTheme, resizeColumn2 }
})
