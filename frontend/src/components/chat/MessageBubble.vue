<template>
  <div class="mb-root" :class="[msgTypeClass, { self: isSelf, optimistic: msg._optimistic, mentioned: isMentioned }]">
    <!-- 引用消息 -->
    <div v-if="msg.replyToId" class="mb-reply">
      <span class="mb-reply-preview">{{ msg.replyToPreview || '[消息]' }}</span>
    </div>
    <!-- 编辑模式 -->
    <div v-if="chat.editMsgId === msg.id" class="mb-edit-wrap">
      <textarea v-model="chat.editContent" class="mb-edit-input" @keydown.enter.exact.prevent="chat.submitEdit()" @keydown.escape="chat.cancelEdit()" rows="2"></textarea>
      <div class="mb-edit-hint">Enter 保存 · Esc 取消</div>
    </div>
    <!-- 文字消息 -->
    <div v-else-if="msg.msgType === 1 || msg.msgType === 4" class="mb-text">
      {{ msg.content }}
      <span v-if="msg.editedAt" class="mb-edited">已编辑</span>
    </div>

    <!-- 图片 / 表情 -->
    <img
      v-else-if="msg.msgType === 2 || msg.msgType === 7"
      :src="msg.content || msg.fileUrl"
      class="mb-img"
      :class="{ 'mb-sticker': msg.msgType === 7 }"
      @click.stop="$emit('preview', msg)"
      loading="lazy"
      alt="图片"
    />

    <!-- 语音消息 -->
    <VoiceBubble v-else-if="msg.msgType === 8" :msg="msg" :isSelf="isSelf" />
    <!-- 名片消息 -->
    <div v-else-if="msg.msgType == 6 && cardData" class="mb-card" @click="openCard">
      <Avatar :src="cardData.avatar" :name="cardData.nickname || cardData.username" :size="36" />
      <div class="mb-card-info">
        <div class="mb-card-name">{{ cardData.nickname || cardData.username }}</div>
        <div class="mb-card-sub">个人名片</div>
      </div>
    </div>
    <!-- 文件消息 -->
    <a v-else-if="msg.msgType === 3" class="mb-file" :href="msg.content || msg.fileUrl" target="_blank" download>
      <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11z"/></svg>
      <span class="mb-file-name">{{ msg.content || msg.fileName || '未知文件' }}</span>
    </a>
  </div>
</template>

<script setup>
import { computed } from 'vue'

import { useChatStore } from '../../stores/chat'
import { useAuthStore } from '../../stores/auth'
import { useUiStore } from '../../stores/ui'
import VoiceBubble from './VoiceBubble.vue'
const chat = useChatStore()
const authStore = useAuthStore()
const ui = useUiStore()

const props = defineProps({
  msg: { type: Object, required: true },
  isSelf: { type: Boolean, default: false }
})

const msgTypeClass = computed(() => {
  const map = { 1: 'type-text', 2: 'type-image', 3: 'type-file', 4: 'type-emoji', 6: 'type-card', 7: 'type-image' }
  return map[props.msg.msgType] || 'type-text'
})
const isMentioned = computed(() => {
  const ids = props.msg.mentionedUserIds
  if (!ids || !ids.length) return false
  const myId = authStore.user?.id
  if (!myId) return false
  return ids.some(id => Number(id) === Number(myId))
})
const cardData = computed(() => {
  if (props.msg.msgType != 6 || !props.msg.content) return null
  try { return JSON.parse(props.msg.content) } catch { return null }
})
function openCard() {
  if (cardData.value?.userId) {
    ui.openUserCard(cardData.value.userId)
  }
}
</script>

<style scoped>
.mb-root {
  position: relative; word-break: break-word;
}
.mb-text {
  padding: 10px 14px; font-size: 14px; line-height: 1.5;
  color: var(--text-primary, #e8e8ea);
  background: var(--bubble-other, #2e3038);
  border-radius: 4px 16px 16px 16px;
}
.mb-root.self .mb-text {
  background: var(--accent, #f7931e); color: #fff;
  border-radius: 16px 4px 16px 16px;
}
.mb-root.type-emoji .mb-text {
  font-size: 24px; line-height: 1.3; padding: 4px 8px;
  background: transparent !important;
}
.mb-root.optimistic .mb-text { opacity: 0.7; }
.mb-root.mentioned .mb-text { background: rgba(247,147,30,0.2) !important; border-left: 3px solid #f7931e; }
.mb-reply {
  padding: 6px 10px; margin-bottom: 2px; font-size: 12px; color: var(--text-muted, #999);
  background: rgba(0,0,0,0.2); border-left: 3px solid var(--accent, #f7931e);
  border-radius: 4px; max-width: 300px;
}
.mb-reply-preview { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.mb-img {
  max-width: 240px; max-height: 320px; border-radius: 8px;
  cursor: pointer; display: block; object-fit: cover;
}
.mb-sticker {
  max-width: 200px; max-height: 200px;
  width: auto; height: auto;
  object-fit: contain;
}
.mb-file {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; border-radius: 8px;
  background: var(--bubble-other, #2e3038);
  color: var(--text-primary, #e8e8ea); text-decoration: none;
  font-size: 13px;
}
.mb-file:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); }
.mb-file-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 160px; }
.mb-edited { font-size: 10px; color: var(--text-muted, #999); margin-left: 6px; opacity: 0.7; }
.mb-edit-wrap { min-width: 200px; }
.mb-edit-input {
  width: 100%; padding: 8px 12px; border: 2px solid var(--accent, #f7931e);
  border-radius: 8px; background: var(--bg-dialog, #252529);
  color: var(--text-primary, #e8e8ea); font-size: 14px; resize: none; outline: none;
  font-family: inherit; line-height: 1.5;
}
.mb-edit-hint { font-size: 10px; color: var(--text-muted, #888); margin-top: 4px; text-align: right; }
.mb-card {
  display: flex; align-items: center; gap: 10px; padding: 10px 14px;
  background: var(--bubble-other, #2e3038); border-radius: 8px;
  cursor: pointer; min-width: 180px; transition: background 0.12s;
}
.mb-card:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); }
.mb-card-info { min-width: 0; }
.mb-card-name { font-size: 14px; color: var(--text-primary, #e8e8ea); font-weight: 500; }
.mb-card-sub { font-size: 11px; color: var(--text-muted, #999); margin-top: 2px; }
</style>
