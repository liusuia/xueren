<template>
  <div class="ctl-root">
    <div class="ctl-actions">
      <button class="ctl-act-btn" @click="emit('showFavorites')">
        <span class="ctl-act-icon" style="background:#f1c40f"><svg viewBox="0 0 24 24" width="16" height="16" fill="#fff"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg></span>
        <span>收藏</span>
      </button>
      <button class="ctl-act-btn" @click="$emit('friendRequests')">
        <span class="ctl-act-icon" style="background:#07C160">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="#fff"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
        </span>
        <span>新的朋友</span>
        <Badge v-if="contactStore.pendingRequestCount" :count="contactStore.pendingRequestCount" :size="16" style="margin-left:auto" />
      </button>
      <button class="ctl-act-btn" @click="$emit('createGroup')">
        <span class="ctl-act-icon" style="background:#1485EE"><svg viewBox="0 0 24 24" width="16" height="16" fill="#fff"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg></span>
        <span>创建群聊</span>
      </button>
      <button class="ctl-act-btn" @click="$emit('addFriend')">
        <span class="ctl-act-icon" style="background:#f7931e"><svg viewBox="0 0 24 24" width="16" height="16" fill="#fff"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg></span>
        <span>添加好友</span>
      </button>
      <button class="ctl-act-btn" @click="emit('joinGroup')">
        <span class="ctl-act-icon" style="background:#9B59B6"><svg viewBox="0 0 24 24" width="16" height="16" fill="#fff"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg></span>
        <span>加入群聊</span>
      </button>
      <button class="ctl-act-btn" @click="emit('showBlockedList')">
        <span class="ctl-act-icon" style="background:#e74c3c"><svg viewBox="0 0 24 24" width="16" height="16" fill="#fff"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg></span>
        <span>黑名单</span>
      </button>
    </div>

    <!-- 群聊分组 -->
    <div v-if="groupStore.list.length" class="ctl-section">
      <div class="ctl-section-hd">群聊 ({{ groupStore.list.length }})</div>
      <div v-for="g in groupStore.list" :key="g.id" class="cti-root" @click="$emit('select', toGroupConv(g))" @contextmenu.prevent="onGroupCtx($event, g)">
        <Avatar :src="g.avatar" :name="g.name" :size="40" />
        <span class="cti-name">{{ g.name }}</span>
        <span v-if="groupStore.getPendingCount(g.id)" class="cti-req-badge">{{ groupStore.getPendingCount(g.id) }}</span>
      </div>
    </div>

    <!-- 好友分组 -->
    <div class="ctl-section" v-if="contactStore.friendSections.length">
      <div class="ctl-section-hd">好友 ({{ contactStore.friends.length }})</div>
      <div v-for="section in contactStore.friendSections" :key="section.letter">
        <div class="ctl-letter-hd">{{ section.letter }}</div>
        <div v-for="f in section.items" :key="f.userId" class="cti-root" @click="$emit('select', toConv(f))" @contextmenu.prevent="onFriendCtx($event, f)">
          <div v-if="f.userId === 1" class="cti-fh-avatar">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#f7931e"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 15h8v2H8zm0-4h8v2H8z"/></svg>
          </div>
          <Avatar v-else :src="f.avatar" :name="f.remark || f.nickname || f.username" :size="40" />
          <span class="cti-name">{{ f.remark || f.nickname || f.username }}</span>
        </div>
      </div>
    </div>

    <EmptyState v-if="!groupStore.list.length && !contactStore.friendSections.length" title="暂无好友和群聊" desc="去添加好友或创建群聊吧" />

    <!-- 发送名片 - 选择会话 -->
    <Teleport to="body">
      <div v-if="showCardDialog" class="cd-overlay" @click="showCardDialog = false">
        <div class="cd-dialog" @click.stop>
          <div class="cd-hd">选择发送给</div>
          <div class="cd-list">
            <div v-for="c in convStore.list" :key="c.id" class="cd-item" @click="doSendCard(c)">
              <Avatar :src="c.targetAvatar" :name="c.targetName" :size="36" />
              <span>{{ c.targetName }}</span>
            </div>
            <div v-if="!convStore.list.length" class="cd-empty">暂无会话</div>
          </div>
        </div>
      </div>
    </Teleport>

    <ContextMenu :visible="ctxVisible" :items="ctxItems" :position="ctxPos" @close="ctxVisible = false" @action="onCtxAction" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import Avatar from '../common/Avatar.vue'
import Badge from '../common/Badge.vue'
import EmptyState from '../common/EmptyState.vue'
import ContextMenu from '../common/ContextMenu.vue'
import { useContactStore } from '../../stores/contacts'
import { useGroupStore } from '../../stores/groups'
import { useConversationStore } from '../../stores/conversations'
import { useChatStore } from '../../stores/chat'
import { useConfirm } from '../../composables/useConfirm'

const contactStore = useContactStore()
const groupStore = useGroupStore()
const convStore = useConversationStore()
const chatStore = useChatStore()
const cfm = useConfirm()
const emit = defineEmits(['select', 'addFriend', 'friendRequests', 'createGroup', 'showFriendInfo', 'showGroupInfo', 'showBlockedList', 'joinGroup'])

const ctxVisible = ref(false)
const ctxPos = ref({ x: 0, y: 0 })
const ctxItems = ref([])
let ctxTarget = null

// 发送名片弹窗
const showCardDialog = ref(false)
const cardUser = ref(null)
function onSendCard(user) {
  cardUser.value = user
  showCardDialog.value = true
}
async function doSendCard(conv) {
  showCardDialog.value = false
  const c = { targetType: conv.targetType, targetId: conv.targetId, targetName: conv.targetName, targetAvatar: conv.targetAvatar, unreadCount: 0 }
  chatStore.openChat(c)
  await chatStore.fetchMessages(50)
  try { await chatStore.sendContactCard(cardUser.value) } catch {}
}

function onFriendCtx(e, friend) {
  ctxTarget = { type: 'friend', data: friend }
  ctxItems.value = [
    { label: '发消息', action: 'chat' },
    { label: '发送名片', action: 'sendCard' },
    { label: '设置备注', action: 'remark' },
    { divider: true },
    { label: '删除好友', action: 'delete', danger: true }
  ]
  ctxPos.value = { x: e.clientX, y: e.clientY }
  ctxVisible.value = true
}

function onGroupCtx(e, group) {
  ctxTarget = { type: 'group', data: group }
  ctxItems.value = [
    { label: '进入群聊', action: 'chat' },
    { label: '查看群信息', action: 'info' },
    { divider: true },
    { label: '退出群聊', action: 'quit', danger: true }
  ]
  ctxPos.value = { x: e.clientX, y: e.clientY }
  ctxVisible.value = true
}

async function onCtxAction(item) {
  ctxVisible.value = false
  if (!ctxTarget) return
  if (ctxTarget.type === 'friend') {
    const f = ctxTarget.data
    if (item.action === 'chat') emit('select', toConv(f))
    else if (item.action === 'sendCard') onSendCard(f)
    else if (item.action === 'remark') {
      const val = await cfm.prompt('设置备注', { inputPlaceholder: '输入备注名', inputDefault: f.remark || '' })
      if (val !== false && val !== undefined) await contactStore.updateRemark(f.userId, val)
    } else if (item.action === 'delete') {
      if (await cfm.danger('确定删除好友？', { confirmText: '删除' })) await contactStore.deleteFriend(f.userId)
    }
  } else if (ctxTarget.type === 'group') {
    const g = ctxTarget.data
    if (item.action === 'chat') emit('select', toGroupConv(g))
    else if (item.action === 'info') emit('showGroupInfo', g.id)
    else if (item.action === 'quit') {
      if (await cfm.danger('确定退出群聊？', { confirmText: '退出' })) await groupStore.quitGroup(g.id)
    }
  }
}

function toConv(f) {
  return {
    targetType: 1, targetId: f.userId,
    targetName: f.remark || f.nickname || f.username,
    targetAvatar: f.avatar || '',
    targetIsOnline: !!(f.isOnline ?? f.online),
    unreadCount: 0, lastMessagePreview: '', lastMessageAt: null, lastMessageId: null
  }
}

function toGroupConv(g) {
  return {
    targetType: 2, targetId: g.id,
    targetName: g.name, targetAvatar: g.avatar || '',
    unreadCount: 0, lastMessagePreview: '', lastMessageAt: null, lastMessageId: null
  }
}
</script>

<style scoped>
.ctl-root { flex: 1; overflow-y: auto; }
.ctl-actions { padding: 8px 12px; display: flex; flex-direction: column; gap: 2px; }
.ctl-act-btn {
  display: flex; align-items: center; gap: 12px; width: 100%;
  padding: 10px 12px; border: none; background: transparent;
  color: var(--text-primary, #e8e8ea); font-size: 14px;
  cursor: pointer; border-radius: 6px; transition: background 0.12s;
}
.ctl-act-btn:hover { background: var(--bg-hover, rgba(255,255,255,0.04)); }
.ctl-act-icon { width: 36px; height: 36px; border-radius: 4px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.ctl-section-hd { font-size: 12px; color: var(--text-muted, #999); padding: 10px 16px 4px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
.ctl-letter-hd { font-size: 11px; color: var(--text-muted, #999); padding: 6px 16px 2px; font-weight: 600; }
.cti-root { display: flex; align-items: center; gap: 12px; padding: 10px 16px; cursor: pointer; transition: background 0.12s; }
.cti-root:hover { background: var(--bg-hover, rgba(255,255,255,0.04)); }
.cti-name { font-size: 14px; color: var(--text-primary, #e8e8ea); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.cti-req-badge { background: #07C160; color: #fff; font-size: 10px; min-width: 16px; height: 16px; line-height: 16px; text-align: center; border-radius: 8px; padding: 0 5px; margin-left: 6px; flex-shrink: 0; }
.cti-fh-avatar { width: 40px; height: 40px; border-radius: 8px; background: rgba(247,147,30,0.15); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.cd-overlay { position: fixed; inset: 0; z-index: 300; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; }
.cd-dialog { width: 320px; max-height: 400px; border-radius: 12px; background: var(--bg-dialog, #252529); overflow: hidden; display: flex; flex-direction: column; }
.cd-hd { padding: 14px 18px; font-size: 14px; font-weight: 600; color: var(--text-primary, #e8e8ea); border-bottom: 1px solid var(--border, #3a3c44); }
.cd-list { flex: 1; overflow-y: auto; padding: 4px 0; }
.cd-item { display: flex; align-items: center; gap: 10px; padding: 10px 18px; cursor: pointer; color: var(--text-primary, #e8e8ea); font-size: 13px; }
.cd-item:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); }
.cd-empty { text-align: center; padding: 24px; color: var(--text-muted, #999); font-size: 13px; }
.cd-search { padding: 8px 14px; }
.ctl-add-btn { padding: 4px 12px; border: none; border-radius: 4px; background: var(--accent, #f7931e); color: #fff; font-size: 12px; cursor: pointer; margin-left: auto; }
</style>
