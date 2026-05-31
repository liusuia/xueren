import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body.data
  },
  (err) => {
    if (err.response) {
      const status = err.response.status
      // 401/403 自动跳登录
      if (status === 401 || status === 403) {
        const auth = useAuthStore()
        if (auth.token && window.location.pathname !== '/login') {
          auth.logout()
        }
      }
      const body = err.response.data
      const msg = body?.message || err.message || '请求失败'
      return Promise.reject(new Error(msg))
    }
    return Promise.reject(new Error('网络异常，请检查连接'))
  }
)

export default http
