<template>
  <Teleport to="body">
    <div class="adf-panel" :style="panelStyle" @click.stop>
      <div class="adf-hd"><span>添加好友</span><button class="adf-close" @click="$emit('close')">&times;</button></div>
      <div class="adf-search">
        <input v-model="keyword" class="adf-inp" placeholder="输入用户名或昵称搜索" @input="onInput" autofocus />
      </div>
      <div class="adf-list" v-if="results.length">
        <div v-for="u in results" :key="u.id" class="adf-item">
          <Avatar :src="u.avatar" :name="u.nickname || u.username" :size="36" />
          <div class="adf-info"><div class="adf-name">{{ u.nickname || u.username }}</div><div class="adf-sub">@{{ u.username }}</div></div>
          <button class="adf-add" @click="onAdd(u.id)" :disabled="u._added">{{ u._added ? '已添加' : '添加' }}</button>
        </div>
      </div>
      <div v-if="searched && !results.length" class="adf-empty">未找到用户</div>
      <div v-if="!keyword" class="adf-empty">输入用户名或昵称搜索</div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import Avatar from '../common/Avatar.vue'
import { useContactStore } from '../../stores/contacts'
import { useAuthStore } from '../../stores/auth'
const contactStore = useContactStore()
const auth = useAuthStore()
defineEmits(['close'])
defineProps({ panelStyle: { type: Object, default: () => ({}) } })
const keyword = ref('')
const results = ref([])
const searched = ref(false)
let t = null
function onInput() { clearTimeout(t); t = setTimeout(() => onSearch(), 300) }
async function onSearch() {
  const q = keyword.value.trim(); if (!q) { results.value = []; searched.value = false; return }
  searched.value = true
  try { results.value = (await contactStore.searchUsers(q)).filter(u => u.id !== auth.user?.id) } catch { results.value = [] }
}
async function onAdd(uid) { try { await contactStore.sendFriendRequest(uid); const u = results.value.find(x => x.id === uid); if (u) u._added = true } catch {} }
</script>

<style scoped>
.adf-panel { position: fixed; width: 360px; max-height: 400px; border-radius: 10px; background: var(--bg-dialog, #252529); box-shadow: 0 8px 32px rgba(0,0,0,0.35); display: flex; flex-direction: column; overflow: hidden; z-index: 200; }
.adf-hd { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; border-bottom: 1px solid var(--border, #3a3c44); }
.adf-hd span { font-size: 14px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.adf-close { background: none; border: none; color: var(--text-muted, #888); font-size: 20px; cursor: pointer; }
.adf-search { padding: 10px 14px; }
.adf-inp { width: 100%; border: 1px solid var(--border, #3a3c44); border-radius: 6px; padding: 8px 12px; font-size: 13px; color: var(--text-primary, #e8e8ea); background: var(--bg-input, #2e3038); outline: none; }
.adf-inp:focus { border-color: var(--accent, #f7931e); }
.adf-list { flex: 1; overflow-y: auto; padding: 0 14px; }
.adf-item { display: flex; align-items: center; gap: 8px; padding: 8px 0; }
.adf-info { flex: 1; min-width: 0; }
.adf-name { font-size: 13px; color: var(--text-primary, #e8e8ea); }
.adf-sub { font-size: 11px; color: var(--text-muted, #999); }
.adf-add { padding: 4px 10px; border-radius: 4px; font-size: 11px; background: var(--accent, #f7931e); color: #fff; border: none; cursor: pointer; white-space: nowrap; }
.adf-add:disabled { opacity: 0.4; cursor: default; }
.adf-empty { text-align: center; padding: 24px; color: var(--text-muted, #999); font-size: 13px; }
</style>
