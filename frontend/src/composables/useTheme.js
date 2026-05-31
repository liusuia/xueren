import { ref, watch } from 'vue'

const theme = ref(localStorage.getItem('xr-theme') || 'dark')

function applyTheme(val) {
  document.documentElement.setAttribute('data-theme', val)
}

applyTheme(theme.value)

watch(theme, (val) => {
  localStorage.setItem('xr-theme', val)
  applyTheme(val)
})

export function useTheme() {
  function toggle() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  return { theme, toggle }
}
