<template>
  <div class="fav-panel">
    <div class="fav-hd">
      <span>收藏 ({{ items.length }})</span>
    </div>
    <div class="fav-list">
      <LoadingSpinner :visible="loading" />
      <div v-if="!loading && !items.length" class="fav-empty">暂无收藏</div>
      <div v-for="m in items" :key="m.id" class="fav-item" @click="jumpTo(m)">
        <div class="fav-item-hd">
          <span class="fav-from">{{ m.fromNickname || m.fromUserName || '未知' }}</span>
          <span class="fav-time">{{ formatFullTime(m.createdAt) }}</span>
          <button class="fav-del" @click.stop="onDelete(m.id)">×</button>
        </div>
        <div class="fav-content">{{ previewText(m) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { favoriteApi } from '../../api/endpoints'
import { useChatStore } from '../../stores/chat'
import { useUiStore } from '../../stores/ui'
import { useAuthStore } from '../../stores/auth'
import LoadingSpinner from '../common/LoadingSpinner.vue'
import { formatFullTime } from '../../utils/format'

const chat = useChatStore()
const ui = useUiStore()
const auth = useAuthStore()
const items = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try { items.value = await favoriteApi.list() || [] } catch {}
  finally { loading.value = false }
})

function previewText(m) {
  if (m.msgType === 2) return '[图片]'
  if (m.msgType === 7) return '[表情]'
  if (m.msgType === 8) return '[语音]'
  if (m.msgType === 3) return '[文件]'
  if (!m.content) return '[消息]'
  return m.content.length > 50 ? m.content.slice(0, 50) + '...' : m.content
}

function jumpTo(m) {
  const myId = auth.user?.id
  let targetId
  if (m.chatType === 1) {
    targetId = String(m.fromUserId) === String(myId) ? m.toUserId : m.fromUserId
  } else {
    targetId = m.groupId
  }
  ui.setActiveTab('chat')
  chat.openChat({ targetType: m.chatType, targetId: targetId, targetName: '', targetAvatar: '', draft: '' })
  chat.fetchMessages(50)
  ui.openChat()
}

async function onDelete(id) {
  try {
    await favoriteApi.remove(id)
    items.value = items.value.filter(i => i.id !== id)
  } catch (e) { console.error('删除收藏失败', e) }
}
</script>

<style scoped>
.fav-panel { height: 100%; display: flex; flex-direction: column; background: var(--list-bg, #22252d); overflow: hidden; }
.fav-hd { display: flex; align-items: center; padding: 14px 18px; flex-shrink: 0; }
.fav-hd span { font-size: 14px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.fav-close { background: none; border: none; color: var(--text-muted, #888); font-size: 20px; cursor: pointer; }
.fav-list { flex: 1; overflow-y: auto; padding: 4px 0; }
.fav-empty { text-align: center; padding: 32px; color: var(--text-muted, #999); font-size: 13px; }
.fav-item { padding: 10px 18px; border-bottom: 1px solid rgba(255,255,255,0.04); cursor: pointer; }
.fav-item:hover { background: var(--bg-hover, rgba(255,255,255,0.04)); }
.fav-item-hd { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.fav-from { font-size: 12px; color: var(--accent, #f7931e); font-weight: 500; }
.fav-time { font-size: 10px; color: var(--text-muted, #777); margin-left: auto; }
.fav-del { background: none; border: none; color: #e74c3c; font-size: 14px; cursor: pointer; padding: 0 4px; }
.fav-content { font-size: 12px; color: var(--text-secondary, #bbb); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; }
</style>
