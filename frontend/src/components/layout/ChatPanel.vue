<template>
  <div class="chat-panel" v-if="chat.currentConv">
    <!-- 头部 -->
    <div class="cp-header">
      <div class="cp-h-left">
        <Avatar :src="chat.currentConv.targetAvatar" :name="headerName" :size="34" class="cp-h-avatar" @click.stop="onAvatarClick" />
        <div class="cp-h-info">
          <div class="cp-h-name">{{ headerName }}</div>
          <div class="cp-h-status" v-if="chat.currentConv.targetType === 1">
            <OnlineDot :active="chat.currentConv.online" :s="8" />
            <span>{{ chat.currentConv.online ? '在线' : '离线' }}</span>
          </div>
        </div>
      </div>
      <div class="cp-h-right">
        <button v-if="chat.currentConv.targetType === 2" class="cp-h-btn" @click="$emit('groupInfo')" title="群信息">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
        </button>
        <button v-if="chat.currentConv.targetType === 1" class="cp-h-btn" @click="$emit('friendInfo', chat.currentConv.targetId)" title="聊天信息">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
        </button>
        <button class="cp-h-btn" @click="$emit('close')" title="关闭">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
        </button>
      </div>
    </div>

    <!-- 消息列表 -->
    <MessageList
      :messages="chat.sortedMessages"
      :loading="chat.loading"
      :isGroup="chat.currentConv.targetType === 2"
      @loadOlder="loadOlder"
      @userClick="(uid) => $emit('userInfo', uid)"
    />

    <!-- 输入栏 -->
    <InputBar
      :isGroup="chat.currentConv.targetType === 2"
      :members="groupMembers"
      @sendText="onSendText"
      @sendImage="onSendImage"
      @sendFile="onSendFile"
      @sendEmoji="onSendEmoji"
    />
  </div>

  <!-- 空状态 -->
  <div v-else class="cp-empty">
    <div class="cp-empty-logo">
      <svg viewBox="0 0 100 100" width="48" height="48" fill="#fff">
        <path d="M50 8C35 8 22 18 18 32c-2 7-1 14 2 20l-8 20c-1 2 0 4 2 5l14 6c3 1 6 0 8-2l2-3c5 3 10 4 16 4s10-1 15-4l2 3c2 2 5 3 8 2l14-6c2-1 3-3 2-5l-8-20c3-6 4-13 2-20C78 18 65 8 50 8z"/>
        <path d="M50 20c-8 0-14 6-14 14 0 3 1 5 2 8l-6 15c-1 2 0 3 1 4l8 3c2 1 4 0 5-1l1-2c3 2 7 3 11 3s7-1 10-3l1 2c1 1 3 2 5 1l8-3c1-1 2-2 1-4l-6-15c1-3 2-5 2-8 0-8-6-14-14-14z" fill="#f7931e" opacity="0.85"/>
      </svg>
    </div>
    <div class="cp-empty-title">雪人 Xueren</div>
    <div class="cp-empty-sub">选择一个会话开始聊天</div>
  </div>
</template>

<script setup>
import Avatar from '../common/Avatar.vue'
import OnlineDot from '../common/OnlineDot.vue'
import MessageList from '../chat/MessageList.vue'
import InputBar from '../chat/InputBar.vue'
import { useChatStore } from '../../stores/chat'
import { useGroupStore } from '../../stores/groups'
import { useContactStore } from '../../stores/contacts'
import { fileApi } from '../../api/endpoints'
import { computed } from 'vue'

const chat = useChatStore()
const groupStore = useGroupStore()
const contactStore = useContactStore()

const groupMembers = computed(() => groupStore.currentGroupMembers || [])

const headerName = computed(() => {
  if (!chat.currentConv) return ''
  if (chat.currentConv.targetType === 1) {
    const f = contactStore.friends.find(x => x.userId === chat.currentConv.targetId)
    if (f) return f.remark || f.nickname || chat.currentConv.targetName
  } else {
    const g = groupStore.list.find(x => x.id === chat.currentConv.targetId)
    let r = g?.remark; if (r) { try { const p = JSON.parse(r); r = p.remark || r } catch {} }
    if (r) return r
  }
  return chat.currentConv.targetName
})

function onAvatarClick() {
  if (!chat.currentConv) return
  if (chat.currentConv.targetType === 2) {
    // 群聊：打开群信息
    emit('groupInfo')
  } else {
    // 单聊：打开用户资料
    emit('userInfo', chat.currentConv.targetId)
  }
}

const emit = defineEmits(['close', 'groupInfo', 'userInfo', 'friendInfo'])

function loadOlder() {
  // 暂不实现分页加载，后续可扩展
}

async function onSendText(content, mentions) {
  await chat.sendMessage({ content, msgType: 1, mentionedUserIds: mentions?.length ? mentions : undefined })
}

async function onSendImage(file) {
  const fileVO = await fileApi.upload(file)
  await chat.sendMessage({ content: fileVO.url, msgType: 2, fileId: fileVO.id })
}

async function onSendFile(file) {
  const fileVO = await fileApi.upload(file)
  await chat.sendMessage({ content: fileVO.originalName, msgType: 3, fileId: fileVO.id })
}

async function onSendEmoji(emoji) {
  await chat.sendMessage({ content: emoji, msgType: 4 })
}
</script>

<style scoped>
.chat-panel {
  flex: 1; display: flex; flex-direction: column; height: 100%;
  background: var(--chat-bg, #1a1d23); min-width: 0;
}
.cp-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; border-bottom: 1px solid var(--border, #2e3038);
  flex-shrink: 0;
}
.cp-h-left { display: flex; align-items: center; gap: 10px; }
.cp-h-info { min-width: 0; }
.cp-h-name { font-size: 15px; font-weight: 500; color: var(--text-primary, #e8e8ea); }
.cp-h-status { display: flex; align-items: center; gap: 4px; font-size: 11px; color: var(--text-muted, #999); margin-top: 1px; }
.cp-h-right { display: flex; gap: 4px; }
.cp-h-btn {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; color: var(--text-muted, #888);
  border-radius: 4px; cursor: pointer; transition: all 0.15s;
}
.cp-h-btn:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); color: var(--text-secondary, #bbb); }

.cp-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%; background: var(--chat-bg, #1a1d23); user-select: none;
  color: var(--text-muted, #777);
}
.cp-empty-logo {
  width: 80px; height: 80px; border-radius: 50%;
  background: linear-gradient(135deg, #f7931e 0%, #e67e22 100%);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 16px; opacity: 0.7;
}
.cp-empty-title { font-size: 18px; color: var(--text-secondary, #bbb); margin-bottom: 4px; }
.cp-empty-sub { font-size: 13px; }
</style>
