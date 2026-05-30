<template>
  <div class="login-page" :class="theme">
    <div class="theme-toggle" @click="toggleTheme" :title="theme === 'dark' ? '白天模式' : '夜间模式'">
      <svg v-if="theme === 'dark'" viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-3.03 0-5.5-2.47-5.5-5.5 0-1.82.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z"/></svg>
      <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0 .39-.39.39-1.03 0-1.41l-1.06-1.06zm1.06-10.96c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"/></svg>
    </div>

    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <svg viewBox="0 0 100 100" width="44" height="44" fill="#fff">
            <path d="M50 8C35 8 22 18 18 32c-2 7-1 14 2 20l-8 20c-1 2 0 4 2 5l14 6c3 1 6 0 8-2l2-3c5 3 10 4 16 4 5 0 10-1 15-4l2 3c2 2 5 3 8 2l14-6c2-1 3-3 2-5l-8-20c3-6 4-13 2-20C78 18 65 8 50 8z"/>
            <path d="M50 20c-8 0-14 6-14 14 0 3 1 5 2 8l-6 15c-1 2 0 3 1 4l8 3c2 1 4 0 5-1l1-2c3 2 7 3 11 3s7-1 10-3l1 2c1 1 3 2 5 1l8-3c1-1 2-2 1-4l-6-15c1-3 2-5 2-8 0-8-6-14-14-14z" fill="#f7931e" opacity="0.85"/>
            <circle cx="50" cy="34" r="8" fill="#fff" opacity="0.9"/>
            <path d="M50 30c-2 0-4 2-4 4s2 4 4 4 4-2 4-4-2-4-4-4z" fill="#f7931e"/>
            <path d="M46 42c0 0 2 2 4 2s4-2 4-2" stroke="#fff" stroke-width="1.5" fill="none" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="login-title">NARUTO</div>
        <div class="login-subtitle">木ノ葉隠れの里</div>
      </div>

      <div class="login-form-area">
        <div class="tab-switch">
          <span class="tab-btn" :class="{ active: tab === 'login' }" @click="tab = 'login'">登录</span>
          <span class="tab-divider">|</span>
          <span class="tab-btn" :class="{ active: tab === 'register' }" @click="tab = 'register'">注册</span>
        </div>

        <form v-show="tab === 'login'" @submit.prevent="onLogin">
          <div class="input-group">
            <div class="input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            </div>
            <input v-model="loginForm.username" placeholder="请输入用户名" class="login-input" />
          </div>
          <div class="input-group">
            <div class="input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            </div>
            <input v-model="loginForm.password" type="password" placeholder="请输入密码" class="login-input" />
          </div>
          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="loading" class="btn-loading"></span>
            <span v-else>登 录</span>
          </button>
        </form>

        <form v-show="tab === 'register'" @submit.prevent="onRegister">
          <div class="input-group">
            <div class="input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            </div>
            <input v-model="registerForm.username" placeholder="请输入用户名" class="login-input" />
          </div>
          <div class="input-group">
            <div class="input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            </div>
            <input v-model="registerForm.nickname" placeholder="请输入昵称" class="login-input" />
          </div>
          <div class="input-group">
            <div class="input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            </div>
            <input v-model="registerForm.password" type="password" placeholder="请输入密码" class="login-input" />
          </div>
          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="loading" class="btn-loading"></span>
            <span v-else>注 册</span>
          </button>
        </form>
      </div>

      <div class="login-footer">
        <span>NARUTO &mdash; 火之意志 · 永不熄灭</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElNotification } from 'element-plus'

function showSuccess(msg) {
  ElNotification({ title: '', message: msg, type: 'success', duration: 2000, offset: 60, customClass: 'xr-notify' })
}
function showError(msg) {
  ElNotification({ title: '', message: msg, type: 'error', duration: 3000, offset: 60, customClass: 'xr-notify' })
}
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const tab = ref('login')
const loading = ref(false)

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', nickname: '' })

const theme = ref(localStorage.getItem('xr-theme') || 'dark')
function applyTheme(val) {
  document.documentElement.setAttribute('data-theme', val)
}
function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('xr-theme', theme.value)
  applyTheme(theme.value)
}
onMounted(() => { applyTheme(theme.value) })

async function onLogin() {
  loading.value = true
  try {
    await auth.login(loginForm.value)
    showSuccess('登录成功')
    router.push('/')
  } catch (e) {
    showError(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  loading.value = true
  try {
    await auth.register(registerForm.value)
    showSuccess('注册成功')
    router.push('/')
  } catch (e) {
    showError(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%; display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
  padding: 20px;
  transition: background 0.4s;
}
.login-page.dark {
  background: linear-gradient(160deg, #0f0f13 0%, #1a1a1e 40%, #0d0d10 100%);
}
.login-page.light {
  background: linear-gradient(160deg, #e8ecf0 0%, #f0f2f5 40%, #dce0e4 100%);
}

.theme-toggle {
  position: absolute; top: 20px; right: 24px;
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; z-index: 10;
  transition: background 0.2s, color 0.2s;
  color: var(--text-muted, #999);
}
.theme-toggle:hover {
  background: var(--bg-hover, rgba(255,255,255,0.08));
  color: var(--text-primary, #e8e8ea);
}

.login-card {
  width: 100%; max-width: 400px;
  border-radius: 16px; overflow: hidden;
  background: var(--bg-dialog, #252529);
  box-shadow: 0 12px 48px rgba(0,0,0,0.25);
  transition: background 0.3s, box-shadow 0.3s;
  position: relative; z-index: 1;
}
.light .login-card {
  box-shadow: 0 8px 32px rgba(0,0,0,0.08);
}

.login-header {
  padding: 40px 20px 12px; text-align: center;
}

.login-logo {
  width: 68px; height: 68px; border-radius: 50%;
  background: linear-gradient(135deg, #f7931e 0%, #e67e22 100%);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 14px;
  box-shadow: 0 6px 20px rgba(247,147,30,0.35);
  transition: transform 0.3s;
}
.login-logo:hover { transform: scale(1.05); }

.login-title {
  font-size: 26px; font-weight: 400; letter-spacing: 4px;
  color: var(--text-primary, #e8e8ea);
  font-family: 'Russo One', sans-serif;
  transition: color 0.3s;
}
.login-subtitle {
  font-size: 12px; color: var(--text-muted, #777); margin-top: 2px;
  letter-spacing: 3px; text-transform: uppercase;
}

.login-form-area {
  padding: 16px 28px 0;
}

.tab-switch {
  text-align: center; margin-bottom: 28px; font-size: 15px; user-select: none;
}
.tab-btn {
  color: var(--text-muted, #777); cursor: pointer; transition: color 0.2s; padding: 0 6px;
  font-weight: 500;
}
.tab-btn.active { color: var(--accent, #f7931e); font-weight: 700; }
.tab-btn:hover { color: var(--text-secondary, #999); }
.tab-divider { color: var(--border, #333); margin: 0 12px; opacity: 0.5; }

.input-group {
  display: flex; align-items: center;
  border: 1px solid var(--border-input, #3d3d42); border-radius: 10px;
  padding: 0 14px; margin-bottom: 16px;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
  background: var(--bg-input, #2e2e32);
}
.input-group:focus-within {
  border-color: var(--accent, #f7931e);
  box-shadow: 0 0 0 3px rgba(247,147,30,0.08);
}

.input-icon {
  display: flex; align-items: center; flex-shrink: 0; margin-right: 10px;
  color: var(--text-muted, #777);
}
.input-group:focus-within .input-icon { color: var(--accent, #f7931e); }

.login-input {
  flex: 1; border: none; outline: none; height: 46px;
  font-size: 15px; color: var(--text-primary, #e8e8ea);
  background: transparent;
}
.login-input::placeholder { color: var(--text-placeholder, #555); }

.login-btn {
  width: 100%; height: 46px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #f7931e 0%, #e67e22 100%);
  color: #fff; font-size: 16px; font-weight: 600;
  cursor: pointer; margin-top: 10px; margin-bottom: 6px;
  box-shadow: 0 6px 16px rgba(247,147,30,0.35);
  transition: opacity 0.2s, transform 0.15s, box-shadow 0.2s;
  letter-spacing: 4px;
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.login-btn::after {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(135deg, transparent 40%, rgba(255,255,255,0.12) 100%);
}
.login-btn:hover { opacity: 0.94; box-shadow: 0 8px 24px rgba(247,147,30,0.45); }
.login-btn:active { transform: scale(0.97); }
.login-btn:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.btn-loading {
  display: inline-block; width: 20px; height: 20px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff; border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.login-footer {
  padding: 22px 24px 28px; text-align: center;
  font-size: 11px; color: var(--text-muted, #777);
  letter-spacing: 1px;
}
</style>
