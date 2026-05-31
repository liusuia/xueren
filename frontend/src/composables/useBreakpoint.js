import { ref, onMounted, onUnmounted } from 'vue'

const breakpoint = ref('desktop')
let listeners = 0

function check() {
  const w = window.innerWidth
  if (w >= 900) breakpoint.value = 'desktop'
  else if (w >= 600) breakpoint.value = 'tablet'
  else breakpoint.value = 'mobile'
}

export function useBreakpoint() {
  onMounted(() => {
    if (listeners === 0) {
      check()
      window.addEventListener('resize', check)
    }
    listeners++
  })

  onUnmounted(() => {
    listeners--
    if (listeners <= 0) {
      listeners = 0
      window.removeEventListener('resize', check)
    }
  })

  const isDesktop = () => breakpoint.value === 'desktop'
  const isTablet = () => breakpoint.value === 'tablet'
  const isMobile = () => breakpoint.value === 'mobile'

  return { breakpoint, isDesktop, isTablet, isMobile }
}
