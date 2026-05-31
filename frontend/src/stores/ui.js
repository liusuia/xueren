import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUiStore = defineStore('ui', () => {
  const activeTab = ref('chat') // 'chat' | 'contacts'
  const isChatOpen = ref(false)
  const column2Width = ref(280)

  // 主题使用 localStorage 直接读取，保持与 useTheme 一致
  const theme = ref(localStorage.getItem('xr-theme') || 'dark')

  function setActiveTab(tab) { activeTab.value = tab }
  function openChat() { isChatOpen.value = true }
  function closeChat() { isChatOpen.value = false }
  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    localStorage.setItem('xr-theme', theme.value)
    document.documentElement.setAttribute('data-theme', theme.value)
  }
  function setTheme(val) {
    theme.value = val
    localStorage.setItem('xr-theme', val)
    document.documentElement.setAttribute('data-theme', val)
  }
  function resizeColumn2(width) {
    column2Width.value = Math.max(200, Math.min(500, width))
  }

  return { activeTab, isChatOpen, theme, column2Width, setActiveTab, openChat, closeChat, toggleTheme, setTheme, resizeColumn2 }
})
