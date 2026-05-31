<template>
  <div class="ml-root" :class="[ui.theme]">
    <NavigationSidebar @profile="showProfile = true" />

    <ListPanel
      @selectConv="onSelectConv"
      @showFriendInfo="onShowFriendInfo"
      @showGroupInfo="onShowGroupInfoFromList"
      @openSearch="showSearch = true"
    />

    <!-- 拖拽手柄 -->
    <div class="ml-resize" @mousedown="startResize"></div>

    <ChatPanel
      @close="onCloseChat"
      @groupInfo="onOpenGroupInfo"
      @userInfo="onShowUserInfo"
      @friendInfo="onShowFriendInfo"
    />

    <!-- 各种弹窗/Slide-out -->
    <ProfilePanel v-if="showProfile" @close="showProfile = false" />
    <GroupInfoPanel v-if="showGroupInfo" @close="showGroupInfo = false" @searchChat="showChatSearch = true" @clearChat="onClearCurrentChat" @showUserInfo="onShowUserInfo" />
    <UserInfoCard v-if="showUserInfo" :userId="userInfoUserId" @close="showUserInfo = false" @chat="onSelectConv" />
    <FriendChatInfo v-if="showFriendInfo" :userId="friendInfoUserId" @close="showFriendInfo = false" @searchChat="showChatSearch = true" @clearChat="onClearCurrentChat" />
    <ChatSearch
      v-if="showChatSearch"
      :chatType="chat.currentConv?.targetType"
      :targetId="chat.currentConv?.targetId"
      :chatName="chat.currentConv?.targetName || ''"
      @close="showChatSearch = false"
      @jump="onJumpToMessage"
    />

    <!-- 全局确认弹窗 -->
    <ConfirmDialog v-model="cfm.visible.value" v-bind="cfm.opts.value" @confirm="cfm.confirm" @cancel="cfm.cancel" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useUiStore } from '../../stores/ui'
import { useAuthStore } from '../../stores/auth'
import { useChatStore } from '../../stores/chat'
import { useConversationStore } from '../../stores/conversations'
import { useContactStore } from '../../stores/contacts'
import { useGroupStore } from '../../stores/groups'
import { useWebSocket } from '../../composables/useWebSocket'
import { addWsListener } from '../../api/ws'
import { useKeyboard } from '../../composables/useKeyboard'

import NavigationSidebar from './NavigationSidebar.vue'
import ListPanel from './ListPanel.vue'
import ChatPanel from './ChatPanel.vue'

// 懒加载引入弹窗组件
import ProfilePanel from '../profile/ProfilePanel.vue'
import GroupInfoPanel from '../groups/GroupInfoPanel.vue'
import UserInfoCard from '../profile/UserInfoCard.vue'
import FriendChatInfo from '../contacts/FriendChatInfo.vue'
import ChatSearch from '../chat/ChatSearch.vue'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import { useConfirm } from '../../composables/useConfirm'
import { playMessageSound } from '../../utils/sound'

const cfm = useConfirm()
const ui = useUiStore()
const auth = useAuthStore()
const chat = useChatStore()
const convStore = useConversationStore()
const contactStore = useContactStore()
const groupStore = useGroupStore()

const showProfile = ref(false)
const showGroupInfo = ref(false)
const showUserInfo = ref(false)
const userInfoUserId = ref(null)
const showFriendInfo = ref(false)
const friendInfoUserId = ref(null)
const showChatSearch = ref(false)

// WebSocket 消息监听
const { onNewMessage, onMessageRecalled } = useWebSocket()

onNewMessage((msg) => {
  // 处理可能被包装的数据
  const message = msg.data || msg
  if (!message.id) return
  const isSelf = message.fromUserId === auth.user?.id
  // 如果当前聊天打开，追加消息
  const isCurrent = chat.appendFromPush(message)
  // 更新会话列表
  convStore.updateFromPush(message, auth.user?.id)
  // 如果当前聊天打开且不是自己发的，标记已读
  if (isCurrent && !isSelf) {
    convStore.markRead(message.chatType, message.chatType === 1 ? message.fromUserId : message.groupId, message.id)
  }
  // 声音提示：不是自己发的 且 会话未开启免打扰
  if (!isSelf) {
    const targetId = message.chatType === 1 ? message.fromUserId : message.groupId
    if (!convStore.isMuted(message.chatType, targetId)) {
      playMessageSound()
    }
  }
})

onMessageRecalled((msg) => {
  const message = msg.data || msg
  if (message.id) {
    chat.markRecalledFromPush(message.id)
    // 同步更新会话列表预览为「消息已撤回」
    // 单聊：每个参与者的会话 target 是对方；群聊：target 是群 ID
    let convTargetId
    if (message.chatType === 1) {
      convTargetId = message.fromUserId === auth.user?.id ? message.toUserId : message.fromUserId
    } else {
      convTargetId = message.groupId
    }
    convStore.updatePreviewToRecall(message.chatType, convTargetId, message.id)
  }
})

// 正在输入指示
let typingTimer = null
addWsListener((packet) => {
  if (packet.type === 'MESSAGE_EDITED') {
    const m = packet.data
    const existing = chat.messages.find(x => x.id === m.id)
    if (existing) { existing.content = m.content; existing.editedAt = m.editedAt }
    const tid = m.chatType === 1 ? m.fromUserId : m.groupId
    const conv = convStore.list.find(c => c.targetType === m.chatType && c.targetId === tid)
    if (conv && conv.lastMessageId === m.id) {
      conv.lastMessagePreview = (m.content || '').length > 50 ? m.content.slice(0, 50) + '...' : (m.content || '')
    }
    playMessageSound()
    return
  }
  if (packet.type === 'TYPING') {
    const d = packet.data
    if (!d || !chat.currentConv) return
    const isCurrent =
      (d.chatType === 1 && d.fromUserId === chat.currentConv.targetId) ||
      (d.chatType === 2 && d.groupId === chat.currentConv.targetId)
    if (isCurrent) {
      if (d.typing) {
        clearTimeout(typingTimer)
        chat.setTypingUser({ userId: d.fromUserId })
      } else {
        typingTimer = setTimeout(() => chat.setTypingUser(null), 500)
      }
    }
  }
})

// 名片点击 → 打开用户资料
watch(() => ui.showUserCardId, (uid) => {
  if (uid) { userInfoUserId.value = uid; showUserInfo.value = true; ui.closeUserCard() }
})

// 初始化数据
onMounted(async () => {
  await convStore.fetchConversations()
  contactStore.fetchFriends()
  contactStore.fetchRequests()
  groupStore.fetchGroups()
  // 默认打开文件助手
  const fh = convStore.list.find(c => c.targetType === 1 && c.targetId === 1)
  if (fh && !chat.currentConv) {
    chat.openChat(fh)
    await chat.fetchMessages(50)
    ui.openChat()
  }
})

// 选会话
async function onSelectConv(conv) {
  chat.openChat(conv)
  await chat.fetchMessages(conv._jumpToMsgId ? 200 : 50)
  ui.openChat()
  if (conv.unreadCount > 0) {
    convStore.markRead(conv.targetType, conv.targetId, conv.lastMessageId)
  }
  if (conv.targetType === 2) {
    groupStore.fetchGroupDetail(conv.targetId)
  }
}

function onCloseChat() {
  chat.closeChat()
  ui.closeChat()
}

function onShowUserInfo(userId) {
  userInfoUserId.value = userId
  showUserInfo.value = true
}

function onShowFriendInfo(userId) {
  friendInfoUserId.value = userId
  showFriendInfo.value = true
}

async function onOpenGroupInfo() {
  if (chat.currentConv?.targetId) {
    await groupStore.fetchGroupDetail(chat.currentConv.targetId)
  }
  showGroupInfo.value = true
}

async function onShowGroupInfoFromList(groupId) {
  await groupStore.fetchGroupDetail(groupId)
  showGroupInfo.value = true
}

function onClearCurrentChat(payload) {
  const type = payload?.targetType || chat.currentConv?.targetType
  const id = payload?.targetId || chat.currentConv?.targetId
  if (!type || !id) return
  // 清空消息（记录时间戳 + 清空当前列表）
  chat.clearHistory(type, id)
  // 同步清除会话预览
  convStore.clearPreview(type, id)
}

function onJumpToMessage(msgId) {
  chat.jumpMsgId = null
  setTimeout(() => { chat.jumpMsgId = msgId }, 100)
}

// 栏宽拖拽
const isResizing = ref(false)
function startResize() {
  isResizing.value = true
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', stopResize)
}
function onMouseMove(e) {
  if (!isResizing.value) return
  ui.resizeColumn2(e.clientX - 56)
}
function stopResize() {
  isResizing.value = false
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', stopResize)
}

// 快捷键
useKeyboard([
  { key: 'Escape', handler: () => { if (chat.currentConv) onCloseChat() } },
  { key: '1', ctrl: true, handler: () => { ui.setActiveTab('chat') }, allowInInput: true },
  { key: '2', ctrl: true, handler: () => { ui.setActiveTab('contacts') }, allowInInput: true }
])
</script>

<style scoped>
.ml-root {
  display: flex; width: 100%; height: 100%; overflow: hidden;
  background: var(--bg-main, #e8e8e8);
}
.ml-resize {
  width: 4px; cursor: col-resize; background: transparent;
  transition: background 0.15s; flex-shrink: 0; z-index: 5;
}
.ml-resize:hover, .ml-resize:active { background: var(--accent, #f7931e); }
</style>
