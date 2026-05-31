<template>
  <div class="ci-root" :class="{ active }">
    <div class="ci-avatar">
      <div v-if="isFileHelper" class="ci-fh-avatar">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="#f7931e"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11z"/></svg>
      </div>
      <Avatar v-else
        :src="conv.targetAvatar"
        :name="displayName"
        :size="44"
        :online="conv.targetType === 1 && conv.targetIsOnline"
      />
    </div>
    <div class="ci-body">
      <div class="ci-top">
        <span class="ci-name">{{ displayName }}</span>
        <span class="ci-icons">
          <svg v-if="muted" class="ci-icon-mute" viewBox="0 0 24 24" width="12" height="12" fill="currentColor"><path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/></svg>
          <svg v-if="pinned" class="ci-icon-pin" viewBox="0 0 24 24" width="12" height="12" fill="currentColor"><path d="M16 12V4h1V2H7v2h1v8l-2 2v2h5.2v6h1.6v-6H18v-2z"/></svg>
        </span>
        <span class="ci-time">{{ formatConversationTime(conv.lastMessageAt) }}</span>
      </div>
      <div class="ci-bottom">
        <span class="ci-preview">{{ conv.lastMessagePreview || '' }}</span>
        <Badge v-if="conv.unreadCount > 0" :count="conv.unreadCount" :max="99" :size="18" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Avatar from '../common/Avatar.vue'
import Badge from '../common/Badge.vue'
import { useContactStore } from '../../stores/contacts'
import { useGroupStore } from '../../stores/groups'
import { useAuthStore } from '../../stores/auth'
import { useConversationStore } from '../../stores/conversations'
import { formatConversationTime } from '../../utils/format'

const props = defineProps({
  conv: { type: Object, required: true },
  active: { type: Boolean, default: false }
})

const contactStore = useContactStore()
const groupStore = useGroupStore()
const auth = useAuthStore()
const convStore = useConversationStore()

const muted = computed(() => convStore.isMuted(props.conv.targetType, props.conv.targetId))
const pinned = computed(() => convStore.isPinned(props.conv.targetType, props.conv.targetId))

const isFileHelper = computed(() => props.conv.targetType === 1 && props.conv.targetId === 1)
const displayName = computed(() => {
  if (isFileHelper.value) return '文件助手'
  if (props.conv.targetType === 1) {
    const f = contactStore.friends.find(x => x.userId === props.conv.targetId)
    if (f) return f.remark || f.nickname || props.conv.targetName
  } else {
    const gm = groupStore.list.find(g => g.id === props.conv.targetId)
    let r = gm?.remark; if (r) { try { const p = JSON.parse(r); r = p.remark || r } catch {} }
    if (r) return r
  }
  return props.conv.targetName
})
</script>

<style scoped>
.ci-root {
  display: flex; align-items: center; gap: 12px; padding: 12px 16px;
  cursor: pointer; transition: background 0.12s;
  border-left: 3px solid transparent;
}
.ci-root:hover { background: var(--bg-hover, rgba(255,255,255,0.04)); }
.ci-root.active {
  background: var(--bg-active, rgba(247,147,30,0.10));
  border-left-color: var(--accent, #f7931e);
}
.ci-avatar { flex-shrink: 0; }
.ci-fh-avatar { width: 44px; height: 44px; border-radius: 10px; background: rgba(247,147,30,0.15); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ci-body { flex: 1; min-width: 0; }
.ci-top { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 4px; }
.ci-name {
  font-size: 14px; font-weight: 500; color: var(--text-primary, #e8e8ea);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; min-width: 0;
}
.ci-icons { display: flex; gap: 2px; align-items: center; flex-shrink: 0; margin-right: 4px; }
.ci-icon-mute { color: var(--text-muted, #888); opacity: 0.5; }
.ci-icon-pin { color: var(--accent, #f7931e); }
.ci-time { font-size: 11px; color: var(--text-muted, #888); flex-shrink: 0; margin-left: 4px; }
.ci-bottom { display: flex; justify-content: space-between; align-items: center; }
.ci-preview {
  font-size: 12px; color: var(--text-muted, #999);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1;
}
</style>
