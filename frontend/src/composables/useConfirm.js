import { ref } from 'vue'

const visible = ref(false)
const opts = ref({ message: '', type: 'info', confirmText: '确定', cancelText: '取消', inputPlaceholder: '', inputDefault: '' })
let resolvePromise = null

export function useConfirm() {
  function show(options) {
    opts.value = { ...options }
    visible.value = true
    return new Promise((resolve) => { resolvePromise = resolve })
  }

  function confirm(data) {
    visible.value = false
    if (resolvePromise) { resolvePromise(data ?? true); resolvePromise = null }
  }

  function cancel() {
    visible.value = false
    if (resolvePromise) { resolvePromise(false); resolvePromise = null }
  }

  function danger(message, { confirmText = '确定删除', cancelText = '取消' } = {}) {
    return show({ message, type: 'danger', confirmText, cancelText })
  }

  function info(message, { confirmText = '确定', cancelText = '取消' } = {}) {
    return show({ message, type: 'info', confirmText, cancelText })
  }

  function prompt(message, { confirmText = '确定', cancelText = '取消', inputPlaceholder = '', inputDefault = '' } = {}) {
    return show({ message, type: 'prompt', confirmText, cancelText, inputPlaceholder, inputDefault })
  }

  return { visible, opts, confirm, cancel, show, danger, info, prompt }
}
