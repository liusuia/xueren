<template>
  <div class="cl-root">
    <LoadingSpinner :visible="convStore.loading" />
    <div v-if="!convStore.loading && convStore.list.length === 0" class="cl-empty">
      <EmptyState
        :icon="emptyIcon"
        title="暂无会话"
        desc="选择一个联系人或群组开始聊天"
      />
    </div>
    <div v-else class="cl-list" ref="listRef">
      <div v-if="totalUnread > 0" class="cl-read-all" @click="markAll">全部已读 ({{ totalUnread }})</div>
      <ConversationItem
        v-for="conv in convStore.list"
        :key="conv.id"
        :conv="conv"
        :active="chatStore.currentConv?.targetId === conv.targetId && chatStore.currentConv?.targetType === conv.targetType"
        @click="$emit('select', conv)"
        @contextmenu.prevent="onContextMenu($event, conv)"
      />
    </div>

    <ContextMenu
      v-if="ctxVisible"
      :visible="ctxVisible"
      :items="ctxItems"
      :position="ctxPos"
      @close="ctxVisible = false"
      @action="onCtxAction"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import ConversationItem from './ConversationItem.vue'
import LoadingSpinner from '../common/LoadingSpinner.vue'
import EmptyState from '../common/EmptyState.vue'
import ContextMenu from '../common/ContextMenu.vue'
import { useConversationStore } from '../../stores/conversations'
import { useChatStore } from '../../stores/chat'
import { useContextMenu } from '../../composables/useContextMenu'
import { useConfirm } from '../../composables/useConfirm'

const convStore = useConversationStore()
const chatStore = useChatStore()
const totalUnread = computed(() => convStore.list.reduce((s, c) => s + (c.unreadCount || 0), 0))
async function markAll() {
  for (const c of convStore.list) {
    if (c.unreadCount > 0) await convStore.markRead(c.targetType, c.targetId, c.lastMessageId)
  }
}
const cfm = useConfirm()
const ctx = useContextMenu()
const ctxVisible = ref(false)
const ctxPos = ref({ x: 0, y: 0 })
const ctxItems = ref([])
let ctxConv = null

defineEmits(['select'])

const emptyIcon = `<svg viewBox="0 0 24 24" width="48" height="48" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/></svg>`

function onContextMenu(e, conv) {
  ctxConv = conv
  const isPinned = convStore.isPinned(conv.targetType, conv.targetId)
  const isMuted = convStore.isMuted(conv.targetType, conv.targetId)
  ctxItems.value = [
    { label: '标记已读', action: 'read' },
    { label: isMuted ? '取消免打扰' : '消息免打扰', action: 'mute' },
    { label: isPinned ? '取消置顶' : '置顶聊天', action: 'pin' },
    { divider: true },
    { label: '删除会话', action: 'delete', danger: true }
  ]
  ctxPos.value = { x: e.clientX, y: e.clientY }
  ctxVisible.value = true
}

async function onCtxAction(item) {
  ctxVisible.value = false
  if (!ctxConv) return
  if (item.action === 'delete') {
    if (await cfm.info('确定删除该会话？')) {
      await convStore.deleteConversation(ctxConv.id)
    }
  } else if (item.action === 'read') {
    await convStore.markRead(ctxConv.targetType, ctxConv.targetId, ctxConv.lastMessageId)
  } else if (item.action === 'pin') {
    convStore.togglePinned(ctxConv.targetType, ctxConv.targetId)
    convStore.fetchConversations()
  } else if (item.action === 'mute') {
    const newMuted = !convStore.isMuted(ctxConv.targetType, ctxConv.targetId)
    convStore.setMuted(ctxConv.targetType, ctxConv.targetId, newMuted)
  }
}
</script>

<style scoped>
.cl-root { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.cl-list { flex: 1; overflow-y: auto; }
.cl-read-all { text-align: center; padding: 8px; font-size: 12px; color: var(--accent, #f7931e); cursor: pointer; border-bottom: 1px solid var(--border, #2e3038); }
.cl-read-all:hover { background: var(--bg-hover, rgba(255,255,255,0.04)); }
.cl-empty { flex: 1; display: flex; align-items: center; justify-content: center; }
</style>
