export function isMobile() {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
    || (window.innerWidth < 768 && 'ontouchstart' in window)
}

export function setAppHeight() {
  const set = () => {
    document.documentElement.style.setProperty('--app-height', window.innerHeight + 'px')
  }
  set()
  window.addEventListener('resize', set)
  window.addEventListener('orientationchange', () => setTimeout(set, 100))
}
