import { onMounted, onUnmounted } from 'vue'

export function useKeyboard(shortcuts) {
  // shortcuts = [{ key: 'k', ctrl: true, handler: () => {} }, ...]

  function onKeyDown(e) {
    for (const s of shortcuts) {
      const keyMatch = e.key.toLowerCase() === s.key.toLowerCase()
      const ctrlMatch = s.ctrl ? (e.ctrlKey || e.metaKey) : true
      const shiftMatch = s.shift ? e.shiftKey : (!s.shift || e.shiftKey)
      if (keyMatch && ctrlMatch && shiftMatch) {
        // 如果焦点在输入框内，只处理特定快捷键
        const tag = e.target.tagName
        const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || e.target.isContentEditable
        if (isInput && !s.allowInInput) continue
        e.preventDefault()
        s.handler(e)
        return
      }
    }
  }

  onMounted(() => document.addEventListener('keydown', onKeyDown))
  onUnmounted(() => document.removeEventListener('keydown', onKeyDown))
}
