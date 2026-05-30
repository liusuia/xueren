<template>
  <div class="m-login-page" :class="theme">
    <div class="m-login-bg"></div>

    <div class="m-login-body">
      <div class="m-login-header">
        <div class="m-login-logo">
          <svg viewBox="0 0 100 100" width="48" height="48" fill="#fff">
            <path d="M50 8C35 8 22 18 18 32c-2 7-1 14 2 20l-8 20c-1 2 0 4 2 5l14 6c3 1 6 0 8-2l2-3c5 3 10 4 16 4 5 0 10-1 15-4l2 3c2 2 5 3 8 2l14-6c2-1 3-3 2-5l-8-20c3-6 4-13 2-20C78 18 65 8 50 8z"/>
            <path d="M50 20c-8 0-14 6-14 14 0 3 1 5 2 8l-6 15c-1 2 0 3 1 4l8 3c2 1 4 0 5-1l1-2c3 2 7 3 11 3s7-1 10-3l1 2c1 1 3 2 5 1l8-3c1-1 2-2 1-4l-6-15c1-3 2-5 2-8 0-8-6-14-14-14z" fill="#f7931e" opacity="0.85"/>
            <circle cx="50" cy="34" r="8" fill="#fff" opacity="0.9"/>
            <path d="M50 30c-2 0-4 2-4 4s2 4 4 4 4-2 4-4-2-4-4-4z" fill="#f7931e"/>
            <path d="M46 42c0 0 2 2 4 2s4-2 4-2" stroke="#fff" stroke-width="1.5" fill="none" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="m-login-title">NARUTO</div>
        <div class="m-login-subtitle">木ノ葉隠れの里</div>
      </div>

      <div class="m-login-card">
        <div class="m-tab-switch">
          <span class="m-tab-btn" :class="{ active: tab === 'login' }" @click="tab = 'login'">登 录</span>
          <span class="m-tab-btn" :class="{ active: tab === 'register' }" @click="tab = 'register'">注 册</span>
        </div>

        <form v-show="tab === 'login'" @submit.prevent="onLogin" class="m-form">
          <div class="m-input-wrap">
            <svg class="m-input-icon" viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            <input v-model="loginForm.username" placeholder="用户名" class="m-input" autocomplete="username" />
          </div>
          <div class="m-input-wrap">
            <svg class="m-input-icon" viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            <input v-model="loginForm.password" type="password" placeholder="密码" class="m-input" autocomplete="current-password" />
          </div>
          <button type="submit" class="m-btn" :disabled="loading">
            <span v-if="loading" class="m-btn-spinner"></span>
            <span v-else>登 录</span>
          </button>
        </form>

        <form v-show="tab === 'register'" @submit.prevent="onRegister" class="m-form">
          <div class="m-input-wrap">
            <svg class="m-input-icon" viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            <input v-model="registerForm.username" placeholder="用户名" class="m-input" autocomplete="username" />
          </div>
          <div class="m-input-wrap">
            <svg class="m-input-icon" viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            <input v-model="registerForm.nickname" placeholder="昵称" class="m-input" />
          </div>
          <div class="m-input-wrap">
            <svg class="m-input-icon" viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            <input v-model="registerForm.password" type="password" placeholder="密码" class="m-input" autocomplete="new-password" />
          </div>
          <button type="submit" class="m-btn" :disabled="loading">
            <span v-if="loading" class="m-btn-spinner"></span>
            <span v-else>注 册</span>
          </button>
        </form>
      </div>

      <div class="m-login-footer">火之意志 · 永不熄灭</div>
    </div>

    <button class="m-theme-toggle" @click="toggleTheme">
      <svg v-if="theme === 'dark'" viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0 .39-.39.39-1.03 0-1.41l-1.06-1.06zm1.06-10.96c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"/></svg>
      <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-3.03 0-5.5-2.47-5.5-5.5 0-1.82.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z"/></svg>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElNotification } from 'element-plus'
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

function notify(msg, type) {
  ElNotification({ title: '', message: msg, type, duration: type === 'success' ? 2000 : 3000, offset: 60, customClass: 'xr-notify' })
}

async function onLogin() {
  if (!loginForm.value.username.trim() || !loginForm.value.password) {
    notify('请输入用户名和密码', 'error'); return
  }
  loading.value = true
  try {
    await auth.login(loginForm.value)
    notify('登录成功', 'success')
    router.push('/')
  } catch (e) {
    notify(e.message || '登录失败', 'error')
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  if (!registerForm.value.username.trim() || !registerForm.value.nickname.trim() || !registerForm.value.password) {
    notify('请填写所有字段', 'error'); return
  }
  if (registerForm.value.password.length < 6) {
    notify('密码至少6位', 'error'); return
  }
  loading.value = true
  try {
    await auth.register(registerForm.value)
    notify('注册成功', 'success')
    router.push('/')
  } catch (e) {
    notify(e.message || '注册失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.m-login-page {
  height: var(--app-height, 100vh); height: 100dvh;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
  padding: 24px 24px env(safe-area-inset-bottom, 24px);
  transition: background 0.4s;
}
.m-login-page.dark { background: linear-gradient(170deg, #0a0a0f 0%, #14141a 50%, #0a0a0f 100%); }
.m-login-page.light { background: linear-gradient(170deg, #e4e8ec 0%, #f0f2f5 50%, #dce0e4 100%); }

.m-login-bg {
  position: absolute; inset: 0; opacity: 0.06; pointer-events: none;
  background: radial-gradient(ellipse at 20% 50%, #f7931e 0%, transparent 70%),
              radial-gradient(ellipse at 80% 20%, #e67e22 0%, transparent 60%);
}

.m-login-body {
  width: 100%; max-width: 360px; position: relative; z-index: 1;
  display: flex; flex-direction: column; align-items: center;
}

.m-login-header { text-align: center; margin-bottom: 36px; }
.m-login-logo {
  width: 80px; height: 80px; border-radius: 50%;
  background: linear-gradient(135deg, #f7931e 0%, #e67e22 100%);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 8px 30px rgba(247,147,30,0.4);
}
.m-login-title {
  font-size: 32px; font-weight: 400; letter-spacing: 6px;
  font-family: 'Russo One', sans-serif;
  color: var(--text-primary, #e8e8ea); transition: color 0.3s;
}
.m-login-subtitle {
  font-size: 13px; color: var(--text-muted, #777); margin-top: 4px;
  letter-spacing: 4px; text-transform: uppercase;
}

.m-login-card {
  width: 100%; background: var(--bg-dialog, #1a1d23);
  border-radius: 20px; padding: 28px 24px 20px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.3);
  transition: background 0.3s;
}

.m-tab-switch {
  display: flex; margin-bottom: 28px;
  border-radius: 10px; background: var(--bg-input, #2a2d35); padding: 4px;
}
.m-tab-btn {
  flex: 1; text-align: center; padding: 10px 0;
  font-size: 15px; font-weight: 500; color: var(--text-muted, #777);
  border-radius: 8px; cursor: pointer; transition: all 0.25s;
}
.m-tab-btn.active {
  background: var(--accent, #f7931e); color: #fff; font-weight: 600;
  box-shadow: 0 2px 8px rgba(247,147,30,0.3);
}

.m-form { display: flex; flex-direction: column; gap: 0; }

.m-input-wrap {
  display: flex; align-items: center; gap: 12px;
  background: var(--bg-input, #2a2d35);
  border: 1.5px solid var(--border-input, #3a3c44);
  border-radius: 12px; padding: 0 16px;
  margin-bottom: 14px; height: 50px;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.m-input-wrap:focus-within {
  border-color: var(--accent, #f7931e);
  box-shadow: 0 0 0 3px rgba(247,147,30,0.1);
}
.m-input-icon { flex-shrink: 0; color: var(--text-muted, #777); }
.m-input-wrap:focus-within .m-input-icon { color: var(--accent, #f7931e); }

.m-input {
  flex: 1; border: none; outline: none; height: 100%;
  font-size: 16px; color: var(--text-primary, #e8e8ea);
  background: transparent;
}
.m-input::placeholder { color: var(--text-placeholder, #555); font-size: 15px; }

.m-btn {
  width: 100%; height: 50px; border: none; border-radius: 12px;
  background: linear-gradient(135deg, #f7931e 0%, #e67e22 100%);
  color: #fff; font-size: 17px; font-weight: 600;
  cursor: pointer; margin-top: 10px;
  box-shadow: 0 6px 20px rgba(247,147,30,0.4);
  transition: opacity 0.2s, transform 0.15s;
  display: flex; align-items: center; justify-content: center;
  letter-spacing: 6px;
  -webkit-tap-highlight-color: transparent;
}
.m-btn:active { transform: scale(0.97); }
.m-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

.m-btn-spinner {
  width: 22px; height: 22px; border: 2.5px solid rgba(255,255,255,0.3);
  border-top-color: #fff; border-radius: 50%;
  animation: m-spin 0.6s linear infinite;
}
@keyframes m-spin { to { transform: rotate(360deg); } }

.m-login-footer {
  margin-top: 24px; font-size: 12px; color: var(--text-muted, #777);
  letter-spacing: 2px;
}

.m-theme-toggle {
  position: absolute; top: max(16px, env(safe-area-inset-top, 16px)); right: 20px;
  width: 40px; height: 40px; border-radius: 50%; border: none;
  background: var(--bg-dialog, rgba(255,255,255,0.05));
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; z-index: 10; color: var(--text-muted, #999);
  transition: background 0.2s, color 0.2s;
  -webkit-tap-highlight-color: transparent;
}
.m-theme-toggle:active { background: var(--bg-hover, rgba(255,255,255,0.1)); }
</style>
