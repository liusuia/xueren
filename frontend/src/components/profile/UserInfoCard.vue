<template>
  <Teleport to="body">
    <div class="uic-overlay" @click.self="$emit('close')">
      <!-- 选择会话弹窗 -->
      <div v-if="showPicker" class="cd-dialog" @click.stop>
        <div class="cd-hd">选择发送给</div>
        <div class="cd-list">
          <div v-for="c in convStore.list" :key="c.id" class="cd-item" @click="doSend(c)">
            <Avatar :src="c.targetAvatar" :name="c.targetName" :size="36" />
            <span>{{ c.targetName }}</span>
          </div>
          <div v-if="!convStore.list.length" class="cd-empty">暂无会话</div>
        </div>
        <button class="cd-cancel" @click="showPicker = false">取消</button>
      </div>

      <!-- 用户资料卡 -->
      <div v-else class="uic-card" @click.stop>
        <div class="uic-top">
          <Avatar :src="user.avatar" :name="user.nickname || user.username" :size="72" />
          <div class="uic-name">{{ user.nickname || user.username }}</div>
          <div class="uic-uid">@{{ user.username }}</div>
          <OnlineDot :active="!!(user.isOnline ?? user.online)" :s="8" />
          <span class="uic-status">{{ (user.isOnline ?? user.online) ? '在线' : '离线' }}</span>
          <div class="uic-info" v-if="user.region || friendCreatedAt">
            <div class="uic-info-row" v-if="user.region"><span class="uic-info-label">地区</span><span class="uic-info-val">{{ user.region }}</span></div>
            <div class="uic-info-row" v-if="friendCreatedAt"><span class="uic-info-label">添加时间</span><span class="uic-info-val">{{ friendCreatedAt }}</span></div>
          </div>
        </div>
        <button class="uic-btn primary" @click="onChat">发消息</button>
        <button class="uic-btn add" @click="showPicker = true">发送名片</button>
        <button class="uic-btn add" @click="onAddFriend" :disabled="added">添加好友</button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import Avatar from '../common/Avatar.vue'
import OnlineDot from '../common/OnlineDot.vue'
import { userApi, friendApi } from '../../api/endpoints'
import { useContactStore } from '../../stores/contacts'
import { useConversationStore } from '../../stores/conversations'
import { useChatStore } from '../../stores/chat'
import { useNotification } from '../../composables/useNotification'
import { formatFullTime } from '../../utils/format'

const props = defineProps({ userId: { type: Number, required: true } })
const emit = defineEmits(['close', 'chat'])
const user = ref({})
const added = ref(false)
const showPicker = ref(false)
const contactStore = useContactStore()
const convStore = useConversationStore()
const chatStore = useChatStore()
const { success } = useNotification()

const friendCreatedAt = computed(() => {
  const f = contactStore.friends.find(x => x.userId === props.userId)
  return f?.createdAt ? formatFullTime(f.createdAt) : ''
})

onMounted(async () => {
  try { user.value = await userApi.getUser(props.userId) } catch { user.value = {} }
  added.value = contactStore.friends.some(f => f.userId === props.userId)
})

function onChat() {
  emit('close')
  emit('chat', { targetType: 1, targetId: user.value.id, targetName: user.value.nickname || user.value.username, targetAvatar: user.value.avatar || '', unreadCount: 0 })
}

async function onAddFriend() {
  try { await friendApi.sendRequest(props.userId); added.value = true; success('好友请求已发送') } catch {}
}

async function doSend(conv) {
  showPicker.value = false
  emit('close')
  const c = { targetType: conv.targetType, targetId: conv.targetId, targetName: conv.targetName, targetAvatar: conv.targetAvatar, unreadCount: 0 }
  chatStore.openChat(c)
  await chatStore.fetchMessages(50)
  try { await chatStore.sendContactCard(user.value); success('名片已发送') } catch {}
}
</script>

<style scoped>
.uic-overlay { position: fixed; inset: 0; z-index: 200; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; }
.uic-card { width: 300px; border-radius: 14px; background: var(--bg-dialog, #252529); box-shadow: 0 16px 48px rgba(0,0,0,0.4); padding: 32px 24px 24px; text-align: center; }
.uic-top { display: flex; flex-direction: column; align-items: center; gap: 6px; margin-bottom: 24px; }
.uic-name { font-size: 18px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.uic-uid { font-size: 13px; color: var(--text-muted, #999); }
.uic-status { font-size: 12px; color: var(--text-muted, #999); }
.uic-info { width: 100%; margin-top: 4px; padding: 8px 0; border-top: 1px solid var(--border, #3a3c44); }
.uic-info-row { display: flex; justify-content: space-between; padding: 3px 0; }
.uic-info-label { font-size: 11px; color: var(--text-muted, #999); }
.uic-info-val { font-size: 11px; color: var(--text-secondary, #bbb); }
.uic-btn { width: 100%; padding: 8px; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; margin-bottom: 6px; }
.uic-btn.primary { background: var(--accent, #f7931e); color: #fff; font-weight: 600; }
.uic-btn.add { background: var(--bg-input, #2e3038); color: var(--text-secondary, #bbb); }
.uic-btn:hover { opacity: 0.9; }
.uic-btn:disabled { opacity: 0.4; cursor: default; }
.cd-dialog { width: 320px; max-height: 400px; border-radius: 12px; background: var(--bg-dialog, #252529); overflow: hidden; display: flex; flex-direction: column; }
.cd-hd { padding: 14px 18px; font-size: 14px; font-weight: 600; color: var(--text-primary, #e8e8ea); border-bottom: 1px solid var(--border, #3a3c44); }
.cd-list { flex: 1; overflow-y: auto; padding: 4px 0; min-height: 100px; }
.cd-item { display: flex; align-items: center; gap: 10px; padding: 10px 18px; cursor: pointer; color: var(--text-primary, #e8e8ea); font-size: 13px; }
.cd-item:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); }
.cd-empty { text-align: center; padding: 24px; color: var(--text-muted, #999); font-size: 13px; }
.cd-cancel { width: 100%; padding: 10px; border: none; border-top: 1px solid var(--border, #3a3c44); background: transparent; color: var(--text-muted, #888); font-size: 13px; cursor: pointer; }
</style>
