import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '../api/http'
import { connectWs, disconnectWs } from '../api/ws'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  function saveAuth(data) {
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    user.value = data.user
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('user', JSON.stringify(data.user))
    connectWs(data.accessToken)
  }

  async function register(form) {
    const data = await http.post('/auth/register', form)
    saveAuth(data)
  }

  async function login(form) {
    const data = await http.post('/auth/login', form)
    saveAuth(data)
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    disconnectWs()
  }

  function restoreWs() {
    if (token.value) {
      connectWs(token.value)
    }
  }

  return { token, refreshToken, user, register, login, logout, restoreWs }
})
