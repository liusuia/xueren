<template>
  <div class="login-page" :class="theme">
    <div class="theme-toggle" @click="toggleTheme" :title="theme === 'dark' ? '切换白天模式' : '切换夜间模式'">
      <!-- 夜间 → 月亮+星星（点击切白天） -->
      <svg v-if="theme === 'dark'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        <circle cx="17" cy="5" r="1.2" fill="currentColor" stroke="none"/>
        <circle cx="7" cy="17" r="0.8" fill="currentColor" stroke="none"/>
      </svg>
      <!-- 白天 → 太阳+光芒 -->
      <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="5"/>
        <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
      </svg>
    </div>

    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <img src="/logo.png" alt="logo" class="login-logo-img" />
        </div>
        <div class="login-title">雪人 Xueren</div>
        <div class="login-subtitle">即时通讯</div>
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
            <input v-model="loginForm.username" placeholder="用户名" class="login-input" autocomplete="username" />
          </div>
          <div class="input-group">
            <div class="input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            </div>
            <input v-model="loginForm.password" type="password" placeholder="密码" class="login-input" autocomplete="current-password" />
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
            <input v-model="registerForm.username" placeholder="用户名" class="login-input" autocomplete="username" />
          </div>
          <div class="input-group">
            <div class="input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            </div>
            <input v-model="registerForm.nickname" placeholder="昵称" class="login-input" />
          </div>
          <div class="input-group">
            <div class="input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
            </div>
            <input v-model="registerForm.password" type="password" placeholder="密码（至少6位）" class="login-input" autocomplete="new-password" />
          </div>
          <button type="submit" class="login-btn" :disabled="loading">
            <span v-if="loading" class="btn-loading"></span>
            <span v-else>注 册</span>
          </button>
        </form>
      </div>

      <div class="login-footer">
        <span>雪人 Xueren &mdash; 即时通讯</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useNotification } from '../composables/useNotification'

const router = useRouter()
const auth = useAuthStore()
const { success, error: showError } = useNotification()
const tab = ref('login')
const loading = ref(false)

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', nickname: '' })

const theme = ref(localStorage.getItem('xr-theme') || 'dark')
function applyTheme(val) { document.documentElement.setAttribute('data-theme', val) }
function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('xr-theme', theme.value)
  applyTheme(theme.value)
}
onMounted(() => { applyTheme(theme.value) })

async function onLogin() {
  if (!loginForm.value.username.trim() || !loginForm.value.password) {
    showError('请输入用户名和密码'); return
  }
  loading.value = true
  try {
    await auth.login(loginForm.value)
    success('登录成功')
    router.push('/')
  } catch (e) {
    showError(e.message || '登录失败')
  } finally { loading.value = false }
}

async function onRegister() {
  if (!registerForm.value.username.trim() || !registerForm.value.nickname.trim() || !registerForm.value.password) {
    showError('请填写所有字段'); return
  }
  if (registerForm.value.password.length < 6) {
    showError('密码至少6位'); return
  }
  loading.value = true
  try {
    await auth.register(registerForm.value)
    success('注册成功')
    router.push('/')
  } catch (e) {
    showError(e.message || '注册失败')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  height: 100%; display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden; padding: 20px; transition: background 0.4s;
}
.login-page.dark { background: linear-gradient(160deg, #0f0f13 0%, #1a1a1e 40%, #0d0d10 100%); }
.login-page.light { background: linear-gradient(160deg, #e8ecf0 0%, #f0f2f5 40%, #dce0e4 100%); }
.theme-toggle {
  position: absolute; top: 20px; right: 24px;
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; z-index: 10; transition: background 0.2s, color 0.2s;
  color: var(--text-muted, #999);
}
.theme-toggle:hover { background: var(--bg-hover, rgba(255,255,255,0.08)); color: var(--text-primary, #e8e8ea); }
.login-card {
  width: 100%; max-width: 400px; border-radius: 16px; overflow: hidden;
  background: var(--bg-dialog, #252529); box-shadow: 0 12px 48px rgba(0,0,0,0.25);
  transition: background 0.3s, box-shadow 0.3s; position: relative; z-index: 1;
}
.light .login-card { box-shadow: 0 8px 32px rgba(0,0,0,0.08); }
.login-header { padding: 40px 20px 12px; text-align: center; }
.login-logo {
  width: 68px; height: 68px; border-radius: 50%;
  background: linear-gradient(135deg, #f7931e 0%, #e67e22 100%);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 14px; box-shadow: 0 6px 20px rgba(247,147,30,0.35);
  transition: transform 0.3s;
}
.login-logo:hover { transform: scale(1.05); }
.login-title { font-size: 26px; font-weight: 400; letter-spacing: 4px; color: var(--text-primary, #e8e8ea); transition: color 0.3s; }
.login-subtitle { font-size: 12px; color: var(--text-muted, #777); margin-top: 2px; letter-spacing: 3px; }
.login-form-area { padding: 16px 28px 0; }
.tab-switch { text-align: center; margin-bottom: 28px; font-size: 15px; user-select: none; }
.tab-btn { color: var(--text-muted, #777); cursor: pointer; transition: color 0.2s; padding: 0 6px; font-weight: 500; }
.tab-btn.active { color: var(--accent, #f7931e); font-weight: 700; }
.tab-divider { color: var(--border, #333); margin: 0 12px; opacity: 0.5; }
.input-group {
  display: flex; align-items: center; border: 1px solid var(--border-input, #3d3d42);
  border-radius: 10px; padding: 0 14px; margin-bottom: 16px;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
  background: var(--bg-input, #2e2e32);
}
.input-group:focus-within { border-color: var(--accent, #f7931e); box-shadow: 0 0 0 3px rgba(247,147,30,0.08); }
.input-icon { display: flex; align-items: center; flex-shrink: 0; margin-right: 10px; color: var(--text-muted, #777); }
.input-group:focus-within .input-icon { color: var(--accent, #f7931e); }
.login-input {
  flex: 1; border: none; outline: none; height: 46px; font-size: 15px;
  color: var(--text-primary, #e8e8ea); background: transparent;
}
.login-input::placeholder { color: var(--text-placeholder, #555); }
.login-btn {
  width: 100%; height: 46px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #f7931e 0%, #e67e22 100%);
  color: #fff; font-size: 16px; font-weight: 600; cursor: pointer;
  margin-top: 10px; margin-bottom: 6px;
  box-shadow: 0 6px 16px rgba(247,147,30,0.35);
  transition: opacity 0.2s, transform 0.15s, box-shadow 0.2s;
  letter-spacing: 4px; display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.login-btn::after { content: ''; position: absolute; inset: 0; background: linear-gradient(135deg, transparent 40%, rgba(255,255,255,0.12) 100%); }
.login-btn:hover { opacity: 0.94; box-shadow: 0 8px 24px rgba(247,147,30,0.45); }
.login-btn:active { transform: scale(0.97); }
.login-btn:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }
.btn-loading {
  display: inline-block; width: 20px; height: 20px;
  border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.login-footer { padding: 22px 24px 28px; text-align: center; font-size: 11px; color: var(--text-muted, #777); letter-spacing: 1px; }
.login-logo { overflow: hidden; }
.login-logo-img { width: 100%; height: 100%; object-fit: cover; }
</style>
