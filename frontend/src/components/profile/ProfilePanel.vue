<template>
  <Teleport to="body">
    <div v-if="show" class="pp-overlay" @click="show = false; $emit('close')">
      <div class="pp-popup" :style="popStyle" @click.stop>
        <!-- 头像区域 -->
        <div class="pp-head" @click="triggerAvatar">
          <Avatar :src="auth.user?.avatar" :name="auth.user?.nickname || auth.user?.username" :size="64" />
          <div class="pp-camera">📷</div>
        </div>
        <input type="file" ref="avInput" accept="image/*" @change="onAvatarChange" style="display:none" />

        <!-- 昵称 -->
        <div class="pp-name-row">
          <input v-model="form.nickname" class="pp-inp-name" placeholder="设置昵称" maxlength="30" />
        </div>

        <!-- 轻语ID -->
        <div class="pp-id-row">
          <span class="pp-id-label">轻语ID</span>
          <span class="pp-id-val">{{ auth.user?.username }}</span>
          <button class="pp-id-edit" @click="editId = !editId">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
          </button>
        </div>
        <div v-if="editId" class="pp-id-edit-row">
          <input v-model="newId" class="pp-inp-id" placeholder="新轻语ID" maxlength="20" />
          <button class="pp-id-save" @click="saveUsername">保存</button>
          <button class="pp-id-cancel" @click="editId = false">取消</button>
        </div>
        <div class="pp-id-note">一年仅可修改一次</div>

        <!-- 信息列表 -->
        <div class="pp-info-list">
          <div class="pp-info-item">
            <span class="pp-info-label">邮箱</span>
            <span v-if="!emailEditing" class="pp-info-val" @click="emailEditing = true">{{ auth.user?.email || '未设置' }}</span>
            <div v-else class="pp-email-edit">
              <input v-model="emailForm.email" class="pp-inp-text" placeholder="新邮箱" />
              <button class="pp-email-send" @click="sendEmailCode" :disabled="emailSending">{{ emailSending ? '发送中' : '获取验证码' }}</button>
              <input v-model="emailForm.code" class="pp-inp-text" placeholder="验证码" maxlength="6" style="width:60px" />
              <button class="pp-email-confirm" @click="confirmEmail" :disabled="emailSending">确认</button>
              <span class="pp-email-cancel" @click="emailEditing = false; emailForm={email:'',code:''}">取消</span>
            </div>
          </div>
          <div class="pp-info-item">
            <span class="pp-info-label">手机</span>
            <input v-model="form.phone" class="pp-inp-text" placeholder="未设置" />
          </div>
          <div class="pp-info-item">
            <span class="pp-info-label">生日</span>
            <input v-model="form.birthday" class="pp-inp-text" placeholder="YYYY-MM-DD" />
          </div>
        </div>

        <!-- 按钮 -->
        <button class="pp-btn-save" @click="onSave" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
        <button class="pp-btn-logout" @click="onLogout">退出登录</button>
        <button class="pp-btn-delete" @click="onDeleteAccount">注销账号</button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import Avatar from '../common/Avatar.vue'
import { useAuthStore } from '../../stores/auth'
import http from '../../api/http'
import { useConfirm } from '../../composables/useConfirm'
import { useNotification } from '../../composables/useNotification'

const auth = useAuthStore()
const router = useRouter()
const cfm = useConfirm()
const { success, error: showError } = useNotification()
const emit = defineEmits(['close'])
const saving = ref(false)
const show = ref(true)
const avInput = ref(null)
const editId = ref(false)
const newId = ref('')
const emailEditing = ref(false)
const emailForm = ref({ email: '', code: '' })
const emailSending = ref(false)

const form = reactive({
  nickname: auth.user?.nickname || '',
  email: auth.user?.email || '',
  phone: auth.user?.phone || '',
  birthday: auth.user?.birthday || ''
})

const popStyle = { position: 'fixed', left: '64px', top: '16px' }

async function saveUsername() {
  const val = newId.value.trim()
  if (!val || val === auth.user?.username) { editId.value = false; return }
  try {
    const updated = await http.put('/users/username', { username: val })
    auth.user = updated
    success('轻语ID已更新')
    editId.value = false
  } catch (e) { showError(e.message || '修改失败') }
}
function triggerAvatar() { avInput.value?.click() }
async function onAvatarChange(e) {
  const file = e.target.files[0]; if (!file) return
  try { await auth.uploadAvatar(file) } catch {}
  e.target.value = ''
}
async function onSave() {
  saving.value = true
  try { await auth.updateProfile({ ...form }); show.value = false; emit('close') } catch {}
  finally { saving.value = false }
}
async function onLogout() {
  if (await cfm.info('确定退出登录？')) { auth.logout() }
}
async function sendEmailCode() {
  const email = emailForm.value.email.trim()
  if (!email) { showError('请输入新邮箱'); return }
  emailSending.value = true
  try { await http.post('/users/email/send-code', { email }); success('验证码已发送') }
  catch (e) { showError(e.message || '发送失败') }
  finally { emailSending.value = false }
}
async function confirmEmail() {
  const { email, code } = emailForm.value
  if (!email.trim() || !code.trim()) { showError('请填写完整'); return }
  emailSending.value = true
  try {
    const user = await http.put('/users/email', { email: email.trim(), code: code.trim() })
    auth.user = user
    success('邮箱已更新')
    emailEditing.value = false
  } catch (e) { showError(e.message || '验证失败') }
  finally { emailSending.value = false }
}

async function onDeleteAccount() {
  if (!await cfm.danger('确定注销账号？此操作不可撤销，所有数据将被永久删除。', { confirmText: '确认注销' })) return
  try {
    await http.delete('/users/me')
    auth.logout()
    success('账号已注销')
  } catch (e) { showError(e.message || '注销失败') }
}
</script>

<style scoped>
.pp-overlay { position: fixed; inset: 0; z-index: 200; }
.pp-popup {
  width: 300px; border-radius: 14px; background: var(--bg-dialog, #1e2028);
  box-shadow: 0 12px 40px rgba(0,0,0,0.4); padding: 28px 24px 20px;
  display: flex; flex-direction: column; align-items: center;
}
.pp-head { position: relative; cursor: pointer; margin-bottom: 12px; }
.pp-head:hover { opacity: 0.85; }
.pp-camera {
  position: absolute; bottom: 0; right: 0;
  width: 24px; height: 24px; border-radius: 50%;
  background: var(--bg-input, #3a3c44); font-size: 12px;
  display: flex; align-items: center; justify-content: center;
}
.pp-name-row { width: 100%; text-align: center; margin-bottom: 12px; }
.pp-inp-name {
  border: none; background: transparent; font-size: 16px; font-weight: 600;
  color: var(--text-primary, #e8e8ea); text-align: center; outline: none;
  width: 180px; padding: 4px; border-bottom: 2px solid transparent;
  transition: border-color 0.2s;
}
.pp-inp-name:focus { border-bottom-color: var(--accent, #f7931e); }
.pp-id-row { display: flex; align-items: center; gap: 6px; margin-bottom: 2px; }
.pp-id-label { font-size: 11px; color: var(--text-muted, #777); }
.pp-id-val { font-size: 13px; color: var(--text-secondary, #bbb); }
.pp-id-edit { background: none; border: none; color: var(--text-muted, #666); cursor: pointer; padding: 2px; }
.pp-id-edit:hover { color: var(--accent, #f7931e); }
.pp-id-edit-row { display: flex; gap: 6px; margin: 6px 0; align-items: center; }
.pp-inp-id { flex: 1; border: 1px solid var(--border, #3a3c44); border-radius: 6px; padding: 4px 8px; font-size: 12px; color: var(--text-primary, #e8e8ea); background: var(--bg-input, #2e3038); outline: none; }
.pp-inp-id:focus { border-color: var(--accent, #f7931e); }
.pp-id-save { background: var(--accent, #f7931e); color: #fff; border: none; border-radius: 4px; padding: 4px 10px; font-size: 11px; cursor: pointer; }
.pp-id-cancel { background: none; border: 1px solid var(--border, #3a3c44); color: var(--text-muted, #888); border-radius: 4px; padding: 4px 8px; font-size: 11px; cursor: pointer; }
.pp-id-note { font-size: 10px; color: var(--text-muted, #555); margin-bottom: 16px; }
.pp-info-list { width: 100%; border-top: 1px solid var(--border, #2e3038); padding-top: 4px; }
.pp-info-item {
  display: flex; align-items: center; padding: 12px 0;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}
.pp-info-label { font-size: 13px; color: var(--text-muted, #999); width: 44px; flex-shrink: 0; }
.pp-inp-text {
  flex: 1; border: none; background: transparent; font-size: 13px;
  color: var(--text-primary, #e8e8ea); outline: none; padding: 2px 0;
  text-align: right;
}
.pp-inp-text:focus { color: var(--accent, #f7931e); }
.pp-btn-save { width: 100%; margin-top: 16px; padding: 10px; border: none; border-radius: 8px; background: var(--accent, #f7931e); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; transition: opacity 0.15s; }
.pp-btn-save:hover { opacity: 0.9; }
.pp-btn-save:disabled { opacity: 0.5; }
.pp-btn-logout { width: 100%; margin-top: 8px; padding: 10px; border: none; border-radius: 8px; background: transparent; color: #e74c3c; font-size: 13px; cursor: pointer; }
.pp-btn-logout:hover { background: rgba(231,76,60,0.06); }
.pp-btn-delete { width: 100%; margin-top: 4px; padding: 10px; border: none; border-radius: 8px; background: transparent; color: var(--text-muted, #555); font-size: 12px; cursor: pointer; }
.pp-btn-delete:hover { color: #e74c3c; }
.pp-info-val { font-size: 13px; color: var(--text-primary, #e8e8ea); flex: 1; text-align: right; cursor: pointer; }
.pp-email-edit { display: flex; gap: 4px; align-items: center; flex: 1; justify-content: flex-end; flex-wrap: wrap; }
.pp-email-send, .pp-email-confirm { font-size: 10px; padding: 2px 6px; border: none; border-radius: 3px; background: var(--accent, #f7931e); color: #fff; cursor: pointer; white-space: nowrap; }
.pp-email-send:disabled, .pp-email-confirm:disabled { opacity: 0.5; }
.pp-email-cancel { font-size: 10px; color: var(--text-muted, #888); cursor: pointer; }
</style>
