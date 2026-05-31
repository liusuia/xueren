import { ref } from 'vue'

const visible = ref(false)
const position = ref({ x: 0, y: 0 })
const items = ref([])

let closeHandler = null

function adjustPosition(x, y, menuW = 160, menuH = 200) {
  const vw = window.innerWidth
  const vh = window.innerHeight
  if (x + menuW > vw) x = vw - menuW - 8
  if (y + menuH > vh) y = vh - menuH - 8
  if (x < 4) x = 4
  if (y < 4) y = 4
  return { x, y }
}

export function useContextMenu() {
  function open(e, menuItems) {
    e.preventDefault()
    e.stopPropagation()
    visible.value = false
    items.value = menuItems.filter(m => !m.hidden)
    const { x, y } = adjustPosition(e.clientX, e.clientY)
    position.value = { x, y }
    // 等一帧确保 visible=false → true 触发 transition
    requestAnimationFrame(() => { visible.value = true })

    // 绑定全局关闭
    if (closeHandler) document.removeEventListener('click', closeHandler)
    closeHandler = () => { visible.value = false }
    setTimeout(() => document.addEventListener('click', closeHandler), 0)
  }

  function close() {
    visible.value = false
  }

  return { visible, position, items, open, close }
}
