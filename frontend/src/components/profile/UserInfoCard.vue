<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="show" class="uic-overlay" @click.self="show = false; $emit('close')">
        <div class="uic-card">
          <div class="uic-top">
            <Avatar :src="user.avatar" :name="user.nickname || user.username" :size="72" />
            <div class="uic-name">{{ user.nickname || user.username }}</div>
            <div class="uic-uid">@{{ user.username }}</div>
            <OnlineDot :active="!!(user.isOnline ?? user.online)" :s="8" />
            <span class="uic-status">{{ (user.isOnline ?? user.online) ? '在线' : '离线' }}</span>
          </div>
          <button class="uic-btn primary" @click="onChat">发消息</button>
          <button class="uic-btn add" @click="onAddFriend" :disabled="added">添加好友</button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Avatar from '../common/Avatar.vue'
import OnlineDot from '../common/OnlineDot.vue'
import { userApi, friendApi } from '../../api/endpoints'
import { useContactStore } from '../../stores/contacts'
import { useNotification } from '../../composables/useNotification'

const props = defineProps({ userId: { type: Number, required: true } })
const emit = defineEmits(['close', 'chat'])
const user = ref({})
const show = ref(true)
const added = ref(false)
const contactStore = useContactStore()
const { success } = useNotification()

onMounted(async () => {
  try { user.value = await userApi.getUser(props.userId) } catch { user.value = {} }
  added.value = contactStore.friends.some(f => f.userId === props.userId)
})

function onChat() {
  show.value = false
  emit('chat', { targetType: 1, targetId: user.value.id, targetName: user.value.nickname || user.value.username, targetAvatar: user.value.avatar || '', unreadCount: 0 })
}

async function onAddFriend() {
  try { await friendApi.sendRequest(props.userId); added.value = true; success('好友请求已发送') } catch {}
}
</script>

<style scoped>
.uic-overlay { position: fixed; inset: 0; z-index: 200; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; }
.uic-card { width: 300px; border-radius: 14px; background: var(--bg-dialog, #252529); box-shadow: 0 16px 48px rgba(0,0,0,0.4); padding: 32px 24px 24px; text-align: center; }
.uic-top { display: flex; flex-direction: column; align-items: center; gap: 6px; margin-bottom: 24px; }
.uic-name { font-size: 18px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.uic-uid { font-size: 13px; color: var(--text-muted, #999); }
.uic-status { font-size: 12px; color: var(--text-muted, #999); }
.uic-btn { width: 100%; padding: 8px; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; margin-bottom: 6px; }
.uic-btn.primary { background: var(--accent, #f7931e); color: #fff; font-weight: 600; }
.uic-btn.add { background: var(--bg-input, #2e3038); color: var(--text-secondary, #bbb); }
.uic-btn:hover { opacity: 0.9; }
.uic-btn:disabled { opacity: 0.4; cursor: default; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
