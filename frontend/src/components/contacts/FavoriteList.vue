<template>
  <Teleport to="body">
    <div class="fav-overlay" @click.self="$emit('close')">
      <div class="fav-panel">
        <div class="fav-hd">
          <span>收藏的消息</span>
          <button class="fav-close" @click="$emit('close')">&times;</button>
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
            <div class="fav-content">{{ m.content || '[图片/文件]' }}</div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { favoriteApi } from '../../api/endpoints'
import { useChatStore } from '../../stores/chat'
import { useUiStore } from '../../stores/ui'
import LoadingSpinner from '../common/LoadingSpinner.vue'
import { formatFullTime } from '../../utils/format'

defineEmits(['close'])
const chat = useChatStore()
const ui = useUiStore()
const items = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try { items.value = await favoriteApi.list() || [] } catch {}
  finally { loading.value = false }
})

function jumpTo(m) {
  chat.openChat({ targetType: m.chatType, targetId: m.chatType === 1 ? m.fromUserId : m.groupId, targetName: '', targetAvatar: '', draft: '' })
  chat.fetchMessages(50)
  ui.openChat()
  emit('close')
}

async function onDelete(id) {
  try { await favoriteApi.remove(id); items.value = items.value.filter(i => i.id !== id) } catch {}
}
</script>

<style scoped>
.fav-overlay { position: fixed; inset: 0; z-index: 250; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; }
.fav-panel { width: 420px; max-height: 500px; border-radius: 12px; background: var(--bg-dialog, #252529); box-shadow: 0 12px 40px rgba(0,0,0,0.3); overflow: hidden; display: flex; flex-direction: column; }
.fav-hd { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; border-bottom: 1px solid var(--border, #3a3c44); }
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
