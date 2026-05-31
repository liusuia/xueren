<template>
  <Teleport to="body">
    <div class="cg-panel" :style="panelStyle" @click.stop>
      <div class="cg-hd"><span>创建群聊</span><button class="cg-close" @click="$emit('close')">&times;</button></div>
      <div class="cg-body">
        <input v-model="groupName" class="cg-inp" placeholder="群聊名称" />
        <div class="cg-label">选择成员 ({{ selected.length }})</div>
        <div class="cg-members">
          <div v-for="f in contactStore.friends" :key="f.userId" class="cg-mem" :class="{ sel: selected.includes(f.userId) }" @click="toggle(f.userId)">
            <Avatar :src="f.avatar" :name="f.remark || f.nickname || f.username" :size="32" />
            <span class="cg-mem-name">{{ f.remark || f.nickname || f.username }}</span>
            <svg v-if="selected.includes(f.userId)" viewBox="0 0 24 24" width="16" height="16" fill="#07C160"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
          </div>
        </div>
        <button class="cg-btn" :disabled="!groupName.trim() || !selected.length" @click="onCreate">创建群聊</button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import Avatar from '../common/Avatar.vue'
import { useContactStore } from '../../stores/contacts'
import { useGroupStore } from '../../stores/groups'
const contactStore = useContactStore()
const groupStore = useGroupStore()
const emit = defineEmits(['close'])
defineProps({ panelStyle: { type: Object, default: () => ({}) } })
const groupName = ref('')
const selected = ref([])
function toggle(id) { const i = selected.value.indexOf(id); if (i >= 0) selected.value.splice(i, 1); else selected.value.push(id) }
async function onCreate() { try { await groupStore.createGroup({ name: groupName.value.trim(), memberIds: selected.value }); emit('close') } catch {} }
</script>

<style scoped>
.cg-panel { position: fixed; width: 360px; max-height: 420px; border-radius: 10px; background: var(--bg-dialog, #252529); box-shadow: 0 8px 32px rgba(0,0,0,0.35); display: flex; flex-direction: column; overflow: hidden; z-index: 200; }
.cg-hd { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; border-bottom: 1px solid var(--border, #3a3c44); }
.cg-hd span { font-size: 14px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.cg-close { background: none; border: none; color: var(--text-muted, #888); font-size: 20px; cursor: pointer; }
.cg-body { padding: 14px 18px; flex: 1; overflow-y: auto; }
.cg-inp { width: 100%; border: 1px solid var(--border, #3a3c44); border-radius: 6px; padding: 8px 12px; font-size: 14px; color: var(--text-primary, #e8e8ea); background: var(--bg-input, #2e3038); outline: none; margin-bottom: 12px; }
.cg-inp:focus { border-color: var(--accent, #f7931e); }
.cg-label { font-size: 12px; color: var(--text-muted, #999); margin-bottom: 6px; }
.cg-members { max-height: 200px; overflow-y: auto; display: flex; flex-direction: column; gap: 2px; }
.cg-mem { display: flex; align-items: center; gap: 8px; padding: 6px; cursor: pointer; border-radius: 4px; font-size: 13px; color: var(--text-primary, #e8e8ea); }
.cg-mem:hover { background: var(--bg-hover, rgba(255,255,255,0.04)); }
.cg-mem.sel { background: rgba(247,147,30,0.1); }
.cg-mem svg { margin-left: auto; }
.cg-mem-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cg-btn { width: 100%; padding: 8px; border: none; border-radius: 6px; background: var(--accent, #f7931e); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; margin-top: 12px; }
.cg-btn:disabled { opacity: 0.4; cursor: default; }
</style>
