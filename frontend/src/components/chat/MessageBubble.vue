<template>
  <div class="mb-root" :class="[msgTypeClass, { self: isSelf, optimistic: msg._optimistic }]">
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

    <!-- 图片消息 -->
    <img
      v-else-if="msg.msgType === 2"
      :src="msg.content || msg.fileUrl"
      class="mb-img"
      @click="$emit('preview', msg)"
      loading="lazy"
      alt="图片"
    />

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
const chat = useChatStore()

const props = defineProps({
  msg: { type: Object, required: true },
  isSelf: { type: Boolean, default: false }
})

const msgTypeClass = computed(() => {
  const map = { 1: 'type-text', 2: 'type-image', 3: 'type-file', 4: 'type-emoji' }
  return map[props.msg.msgType] || 'type-text'
})
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
</style>
