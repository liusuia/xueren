<template>
  <Teleport to="body">
    <div v-if="show" class="pp-overlay" @click="show = false; $emit('close')">
      <div class="pp-popup" :style="popStyle" @click.stop>
        <div class="pp-top">
          <Avatar :src="auth.user?.avatar" :name="auth.user?.nickname || auth.user?.username" :size="56" />
          <div class="pp-name">{{ auth.user?.nickname || auth.user?.username }}</div>
          <div class="pp-uid">@{{ auth.user?.username }}</div>
        </div>
        <div class="pp-fields">
          <div class="pp-field"><label>昵称</label><input v-model="form.nickname" placeholder="昵称" @blur="autoSave('nickname')" /></div>
          <div class="pp-field"><label>邮箱</label><input v-model="form.email" placeholder="邮箱" /></div>
          <div class="pp-field"><label>手机</label><input v-model="form.phone" placeholder="手机号" /></div>
          <div class="pp-field"><label>生日</label><input v-model="form.birthday" placeholder="YYYY-MM-DD" /></div>
        </div>
        <div class="pp-avatar-row">
          <button class="pp-change-av" @click="triggerAvatar">更换头像</button>
          <input type="file" ref="avInput" accept="image/*" @change="onAvatarChange" style="display:none" />
        </div>
        <button class="pp-save" @click="onSave" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
        <button class="pp-logout" @click="onLogout">退出登录</button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Avatar from '../common/Avatar.vue'
import { useAuthStore } from '../../stores/auth'
import { useConfirm } from '../../composables/useConfirm'

const auth = useAuthStore()
const router = useRouter()
const cfm = useConfirm()
const emit = defineEmits(['close'])
const saving = ref(false)
const show = ref(true)
const avInput = ref(null)

const form = reactive({
  nickname: auth.user?.nickname || '',
  email: auth.user?.email || '',
  phone: auth.user?.phone || '',
  birthday: auth.user?.birthday || ''
})

const popStyle = { position: 'fixed', left: '64px', top: '16px' }

function autoSave(field) { /* 失焦不自动保存，防止频繁请求 */ }
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
  if (await cfm.info('确定退出登录？')) { auth.logout(); router.push('/login') }
}
</script>

<style scoped>
.pp-overlay { position: fixed; inset: 0; z-index: 200; }
.pp-popup {
  width: 280px; border-radius: 10px; background: var(--bg-dialog, #252529);
  box-shadow: 0 8px 32px rgba(0,0,0,0.35); padding: 20px 18px 16px;
}
.pp-top { display: flex; flex-direction: column; align-items: center; gap: 4px; margin-bottom: 16px; }
.pp-name { font-size: 16px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.pp-uid { font-size: 12px; color: var(--text-muted, #999); }
.pp-fields { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.pp-field { display: flex; justify-content: space-between; align-items: center; }
.pp-field label { font-size: 12px; color: var(--text-muted, #999); flex-shrink: 0; width: 40px; }
.pp-field input {
  flex: 1; border: 1px solid var(--border, #3a3c44); border-radius: 4px;
  padding: 5px 8px; font-size: 12px; color: var(--text-primary, #e8e8ea);
  background: var(--bg-input, #2e3038); outline: none; max-width: 170px;
}
.pp-field input:focus { border-color: var(--accent, #f7931e); }
.pp-avatar-row { text-align: center; margin-bottom: 10px; }
.pp-change-av { background: none; border: none; color: var(--accent, #f7931e); font-size: 12px; cursor: pointer; }
.pp-save { width: 100%; padding: 8px; border: none; border-radius: 6px; background: var(--accent, #f7931e); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; }
.pp-save:disabled { opacity: 0.5; }
.pp-logout { width: 100%; padding: 8px; border: none; border-radius: 6px; background: transparent; color: #e74c3c; font-size: 12px; cursor: pointer; margin-top: 4px; }
.pp-logout:hover { background: rgba(231,76,60,0.08); }
</style>
