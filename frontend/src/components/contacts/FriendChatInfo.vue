<template>
  <Teleport to="body">
    <Transition name="slide">
      <div class="fci-root">
        <div class="fci-backdrop" @click="$emit('close')"></div>
        <div class="fci-panel">
          <div class="fci-hd">
            <button class="fci-back-btn" @click="$emit('close')">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>
            </button>
            <span class="fci-hd-title">聊天信息</span>
          </div>
          <div class="fci-body" v-if="friendInfo">
            <div class="fci-top">
              <Avatar :src="friendInfo.avatar" :name="friendInfo.nickname || friendInfo.username" :size="56" />
              <div class="fci-name">{{ friendInfo.nickname || friendInfo.username }}</div>
              <div class="fci-uid">@{{ friendInfo.username }}</div>
              <OnlineDot :active="friendInfo.isOnline" :s="8" />
              <span class="fci-status">{{ friendInfo.isOnline ? '在线' : (friendInfo.lastOnlineAt ? '最后在线 ' + formatFullTime(friendInfo.lastOnlineAt) : '离线') }}</span>
            </div>

            <div class="fci-card">
              <div class="fci-card-hd"><span>备注</span></div>
              <input class="fci-inp" :value="friendRemark" placeholder="设置备注" @blur="onSaveRemark" @keydown.enter="$event.target.blur()" />
            </div>

            <div class="fci-card">
              <div class="fci-ops">
                <button class="fci-op" @click="onClearHistory"><span>清空聊天记录</span><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" opacity="0.3"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6z"/></svg></button>
                <button class="fci-op" @click="onSearchChat"><span>查找聊天记录</span><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" opacity="0.3"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6z"/></svg></button>
                <button class="fci-op" @click="onTopConv"><span>置顶聊天</span><span class="fci-sw" :class="{ on: isTopped }">{{ isTopped ? '是' : '否' }}</span></button>
                <button class="fci-op" @click="onBlock"><span>拉黑好友</span><span class="fci-sw" :class="{ on: blocked }">{{ blocked ? '已拉黑' : '否' }}</span></button>
              </div>
            </div>

            <div class="fci-footer">
              <button class="fci-btn danger" @click="onDelete">删除好友</button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import Avatar from '../common/Avatar.vue'
import OnlineDot from '../common/OnlineDot.vue'
import { userApi } from '../../api/endpoints'
import { useContactStore } from '../../stores/contacts'
import { useConversationStore } from '../../stores/conversations'
import { useConfirm } from '../../composables/useConfirm'
import { formatFullTime } from '../../utils/format'

const cfm = useConfirm()

const props = defineProps({ userId: { type: Number, required: true } })
const emit = defineEmits(['close', 'searchChat', 'clearChat'])
const contactStore = useContactStore()
const convStore = useConversationStore()
const friendInfo = ref(null)
const isTopped = computed(() => convStore.isPinned(1, props.userId))

onMounted(async () => { try { friendInfo.value = await userApi.getUser(props.userId) } catch { friendInfo.value = {} } })
const blocked = computed(() => contactStore.isBlocked(props.userId))
const friendRemark = computed(() => {
  const f = contactStore.friends.find(x => x.userId === props.userId)
  return f?.remark || ''
})

async function onSaveRemark(e) {
  const val = e.target.value.trim()
  try { await contactStore.updateRemark(props.userId, val || '') } catch {}
}
async function onBlock() {
  try {
    if (blocked.value) await contactStore.unblockFriend(props.userId)
    else await contactStore.blockFriend(props.userId)
  } catch {}
}
async function onDelete() {
  if (!await cfm.danger('确定删除好友？聊天记录将清空。', { confirmText: '删除' })) return
  try { await contactStore.deleteFriend(props.userId); emit('close') } catch {}
}
async function onClearHistory() {
  if (!await cfm.info('确定清空聊天记录？清空后将无法恢复。')) return
  const conv = convStore.list.find(c => c.targetType === 1 && c.targetId === props.userId)
  if (conv) { conv.lastMessagePreview = ''; conv.unreadCount = 0; conv.lastMessageAt = null }
  emit('clearChat', { targetType: 1, targetId: props.userId })
}
function onSearchChat() {
  emit('close')
  emit('searchChat')
}
function onTopConv() {
  convStore.togglePinned(1, props.userId)
  convStore.fetchConversations()
}
</script>

<style scoped>
.fci-root { position: fixed; inset: 0; z-index: 150; display: flex; }
.fci-backdrop { position: absolute; inset: 0; background: rgba(0,0,0,0.4); }
.fci-panel {
  position: relative; z-index: 1; margin-left: auto;
  width: 340px; height: 100%; background: var(--bg-dialog, #1e2028);
  box-shadow: -4px 0 24px rgba(0,0,0,0.3); display: flex; flex-direction: column;
}
.fci-hd { display: flex; align-items: center; gap: 8px; padding: 14px 16px; border-bottom: 1px solid var(--border, #2e3038); flex-shrink: 0; }
.fci-back-btn { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; border: none; background: transparent; color: var(--text-secondary, #bbb); cursor: pointer; border-radius: 4px; }
.fci-back-btn:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); }
.fci-hd-title { font-size: 15px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.fci-body { flex: 1; overflow-y: auto; padding: 16px; }

.fci-top { display: flex; flex-direction: column; align-items: center; gap: 4px; margin-bottom: 16px; }
.fci-name { font-size: 17px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.fci-uid { font-size: 12px; color: var(--text-muted, #999); }
.fci-status { font-size: 11px; color: var(--text-muted, #999); }

.fci-card { background: var(--bg-input, #22252d); border-radius: 8px; padding: 12px 14px; margin-bottom: 12px; }
.fci-card-hd { font-size: 12px; font-weight: 600; color: var(--text-muted, #999); margin-bottom: 6px; }
.fci-inp { width: 100%; border: none; border-radius: 4px; padding: 6px 10px; font-size: 13px; color: var(--text-primary, #e8e8ea); background: transparent; outline: none; }

.fci-ops { display: flex; flex-direction: column; }
.fci-op {
  display: flex; justify-content: space-between; align-items: center;
  width: 100%; padding: 10px 0; border: none; background: transparent;
  color: var(--text-primary, #e8e8ea); font-size: 13px; cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.04); text-align: left;
}
.fci-op:last-child { border-bottom: none; }
.fci-op:hover { opacity: 0.8; }
.fci-sw { font-size: 11px; color: var(--text-muted, #999); }
.fci-sw.on { color: var(--accent, #f7931e); }

.fci-footer { margin-top: 12px; padding-bottom: 20px; }
.fci-btn { width: 100%; padding: 10px; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; font-weight: 500; }
.fci-btn.danger { background: transparent; color: #e74c3c; }
.fci-btn.danger:hover { background: rgba(231,76,60,0.08); }

.slide-enter-active, .slide-leave-active { transition: all 0.25s ease; }
.slide-enter-from .fci-panel { transform: translateX(100%); }
.slide-enter-to .fci-panel { transform: translateX(0); }
.slide-leave-to .fci-panel { transform: translateX(100%); }
.slide-enter-from .fci-backdrop, .slide-leave-to .fci-backdrop { opacity: 0; }
</style>
