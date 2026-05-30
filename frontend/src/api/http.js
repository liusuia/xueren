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
    if (err.response && err.response.data) {
      const body = err.response.data
      const msg = body.message || err.message || '请求失败'
      return Promise.reject(new Error(msg))
    }
    return Promise.reject(new Error(err.message || '网络异常'))
  }
)

export default http
