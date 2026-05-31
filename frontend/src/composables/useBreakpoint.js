import { ref, onMounted, onUnmounted } from 'vue'

const breakpoint = ref('desktop') // 'desktop' | 'tablet' | 'mobile'

export function useBreakpoint() {
  function check() {
    const w = window.innerWidth
    if (w >= 900) breakpoint.value = 'desktop'
    else if (w >= 600) breakpoint.value = 'tablet'
    else breakpoint.value = 'mobile'
  }

  onMounted(() => {
    check()
    window.addEventListener('resize', check)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', check)
  })

  const isDesktop = () => breakpoint.value === 'desktop'
  const isTablet = () => breakpoint.value === 'tablet'
  const isMobile = () => breakpoint.value === 'mobile'

  return { breakpoint, isDesktop, isTablet, isMobile }
}
