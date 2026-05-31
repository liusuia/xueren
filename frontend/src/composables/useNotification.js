import { ElNotification } from 'element-plus'

export function useNotification() {
  function success(msg) {
    ElNotification({ title: '', message: msg, type: 'success', duration: 2000, offset: 60, customClass: 'xr-notify' })
  }
  function error(msg) {
    ElNotification({ title: '', message: msg, type: 'error', duration: 3000, offset: 60, customClass: 'xr-notify' })
  }
  function info(msg) {
    ElNotification({ title: '', message: msg, type: 'info', duration: 2500, offset: 60, customClass: 'xr-notify' })
  }
  function warning(msg) {
    ElNotification({ title: '', message: msg, type: 'warning', duration: 2500, offset: 60, customClass: 'xr-notify' })
  }
  return { success, error, info, warning }
}
