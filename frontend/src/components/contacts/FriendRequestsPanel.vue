<template>
  <Teleport to="body">
    <div class="frp-panel" :style="panelStyle" @click.stop>
      <div class="frp-hd">
        <span>新的朋友</span>
        <button class="frp-close" @click="$emit('close')">&times;</button>
      </div>
      <div class="frp-list">
        <div v-if="!contactStore.requests.length" class="frp-empty">暂无好友请求</div>
        <div v-for="req in contactStore.requests" :key="req.userId" class="frp-item">
          <Avatar :src="req.avatar" :name="req.nickname || req.username" :size="44" />
          <div class="frp-info"><div class="frp-name">{{ req.nickname || req.username }}</div><div class="frp-sub">请求添加你为好友</div></div>
          <div class="frp-actions">
            <button class="frp-accept" @click="accept(req.requesterId)">接受</button>
            <button class="frp-reject" @click="reject(req.requesterId)">拒绝</button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import Avatar from '../common/Avatar.vue'
import { useContactStore } from '../../stores/contacts'
const contactStore = useContactStore()
defineEmits(['close'])
defineProps({ panelStyle: { type: Object, default: () => ({}) } })
async function accept(id) { try { await contactStore.acceptRequest(id) } catch {} }
async function reject(id) { try { await contactStore.rejectRequest(id) } catch {} }
</script>

<style scoped>
.frp-panel {
  position: fixed; width: 360px; max-height: 400px; border-radius: 10px;
  background: var(--bg-dialog, #252529); box-shadow: 0 8px 32px rgba(0,0,0,0.35);
  display: flex; flex-direction: column; overflow: hidden; z-index: 200;
}
.frp-hd { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; border-bottom: 1px solid var(--border, #3a3c44); }
.frp-hd span { font-size: 14px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.frp-close { background: none; border: none; color: var(--text-muted, #888); font-size: 20px; cursor: pointer; }
.frp-list { flex: 1; overflow-y: auto; padding: 8px 0; }
.frp-empty { text-align: center; padding: 40px; color: var(--text-muted, #999); font-size: 13px; }
.frp-item { display: flex; align-items: center; gap: 10px; padding: 10px 18px; }
.frp-info { flex: 1; min-width: 0; }
.frp-name { font-size: 14px; color: var(--text-primary, #e8e8ea); font-weight: 500; }
.frp-sub { font-size: 11px; color: var(--text-muted, #999); margin-top: 2px; }
.frp-actions { display: flex; gap: 8px; flex-shrink: 0; }
.frp-accept, .frp-reject { padding: 5px 12px; border-radius: 4px; font-size: 12px; font-weight: 500; cursor: pointer; border: none; }
.frp-accept { background: var(--accent, #f7931e); color: #fff; }
.frp-reject { background: var(--bg-input, #2e3038); color: var(--text-secondary, #bbb); }
</style>
