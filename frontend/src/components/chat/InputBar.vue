<template>
  <div class="ib-root">
    <!-- @提及下拉 -->
    <div v-if="showMention" class="ib-mention">
      <div class="ib-m-list">
        <div v-if="canAtAll" class="ib-m-item" @click="insertMention('所有人', [])">
          <span class="ib-m-name">@所有人</span>
          <span class="ib-m-tag">群主/管理员</span>
        </div>
        <div v-for="m in mentionMembers" :key="m.userId" class="ib-m-item" @click="insertMention(m.nickname || m.userName || m.username, [m.userId])">
          <Avatar :src="m.userAvatar || m.avatar" :name="m.nickname || m.userName || m.username" :size="28" />
          <span class="ib-m-name">{{ m.nickname || m.userName || m.username }}</span>
        </div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="ib-tools">
      <button class="ib-tool-btn" :class="{ recording: isRecording }" @pointerdown="startRecord" @pointerup="stopRecord" @pointerleave="cancelRecord" title="语音">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/><path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/></svg>
      </button>
      <button class="ib-tool-btn" @click="showEmoji = !showEmoji" title="表情">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/></svg>
      </button>
      <button class="ib-tool-btn" @click="triggerImage" title="图片">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/></svg>
      </button>
      <button class="ib-tool-btn" @click="triggerFile" title="文件">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5c0-1.38 1.12-2.5 2.5-2.5s2.5 1.12 2.5 2.5v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5c0 1.38 1.12 2.5 2.5 2.5s2.5-1.12 2.5-2.5V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z"/></svg>
      </button>
    </div>

    <!-- 输入区 -->
    <div class="ib-input-area">
      <textarea ref="inputRef" v-model="text" class="ib-textarea" :placeholder="placeholder" rows="1" @keydown.enter.exact="onEnter" @input="onInput" @paste="onPaste"></textarea>
      <button class="ib-send" :class="{ ready: text.trim() }" @click="onSend" :disabled="!text.trim()">发送</button>
    </div>

    <EmojiPicker v-if="showEmoji" @select="onEmoji" @selectEmoji="onCustomEmoji" />

    <input type="file" ref="imageInput" accept="image/*" @change="onImageChange" style="display:none" />
    <input type="file" ref="fileInput" @change="onFileChange" style="display:none" />
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onUnmounted, watch } from 'vue'
import EmojiPicker from './EmojiPicker.vue'
import Avatar from '../common/Avatar.vue'
import { useAuthStore } from '../../stores/auth'
import { useChatStore } from '../../stores/chat'
import { useConversationStore } from '../../stores/conversations'
import { sendTyping } from '../../api/ws'
import { fileApi } from '../../api/endpoints'
import http from '../../api/http'
import { GROUP_ROLE } from '../../utils/constants'

const auth = useAuthStore()
const chat = useChatStore()
const convStore = useConversationStore()
const props = defineProps({
  isGroup: { type: Boolean, default: false },
  members: { type: Array, default: () => [] }
})
const emit = defineEmits(['sendText', 'sendImage', 'sendFile', 'sendEmoji', 'sendImageUrl', 'sendVoice'])

// 录音
const isRecording = ref(false)
let mediaRecorder = null
let recordChunks = []
let recordStartTime = 0

async function startRecord(e) {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })
    recordChunks = []
    mediaRecorder.ondataavailable = (e) => recordChunks.push(e.data)
    mediaRecorder.onstop = async () => {
      stream.getTracks().forEach(t => t.stop())
      if (!recordChunks.length) return
      const blob = new Blob(recordChunks, { type: 'audio/webm' })
      const duration = Math.round((Date.now() - recordStartTime) / 1000)
      emit('sendVoice', { blob, duration })
    }
    recordStartTime = Date.now()
    mediaRecorder.start()
    isRecording.value = true
  } catch (e) { /* 麦克风不可用 */ }
}

function stopRecord() {
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop()
    isRecording.value = false
  }
}

function cancelRecord() {
  if (mediaRecorder && isRecording.value) {
    mediaRecorder.stop()
    recordChunks = []
    isRecording.value = false
  }
}

const text = ref('')
const showEmoji = ref(false)
const showMention = ref(false)
const inputRef = ref(null)
const imageInput = ref(null)
const fileInput = ref(null)

// 草稿：恢复 + 自动保存
let draftTimer = null
const saveDraft = (targetType, targetId, content) => {
  http.put('/conversations/draft', { draft: content || '' }, {
    params: { targetType, targetId }
  }).catch(() => {})
}
watch(text, (val) => {
  if (!chat.currentConv) return
  const cv = chat.currentConv
  cv.draft = val
  const item = convStore.list.find(c => c.targetType === cv.targetType && c.targetId === cv.targetId)
  if (item) item.draft = val
  clearTimeout(draftTimer)
  draftTimer = setTimeout(() => saveDraft(cv.targetType, cv.targetId, val), 1000)
})
let lastConv = null
watch(() => chat.currentConv?.targetId, (newId, oldId) => {
  // 切出前立即保存上一个会话的草稿
  if (lastConv) {
    clearTimeout(draftTimer)
    saveDraft(lastConv.targetType, lastConv.targetId, lastConv.text)
  }
  lastConv = chat.currentConv ? { targetType: chat.currentConv.targetType, targetId: chat.currentConv.targetId, text: '' } : null
  if (newId) {
    text.value = chat.currentConv?.draft || ''
    lastConv.text = text.value
    nextTick(() => inputRef.value?.focus())
  }
}, { immediate: true })
const placeholder = props.isGroup ? '输入消息，@ 提及成员' : '输入消息，Enter发送'

defineExpose({ focus: () => nextTick(() => inputRef.value?.focus()) })

const currentMentions = ref([]) // 当前输入中 @ 的用户ID列表

// 是否是群主或管理员
const canAtAll = computed(() => {
  if (!props.isGroup || !props.members.length) return false
  const me = props.members.find(m => m.userId === auth.user?.id)
  return me && (me.role === GROUP_ROLE.OWNER || me.role === GROUP_ROLE.ADMIN)
})

// 过滤掉自己，用于 @ 提及
const mentionMembers = computed(() => {
  return props.members.filter(m => m.userId !== auth.user?.id)
})

let typingTimer = null
function notifyTyping() {
  if (!chat.currentConv) return
  const data = { chatType: chat.currentConv.targetType, typing: true }
  if (chat.currentConv.targetType === 1) data.toUserId = chat.currentConv.targetId
  else data.groupId = chat.currentConv.targetId
  sendTyping(data)
  clearTimeout(typingTimer)
  typingTimer = setTimeout(() => {
    sendTyping({ ...data, typing: false })
  }, 2000)
}
onUnmounted(() => clearTimeout(typingTimer))

function onInput() {
  notifyTyping()
  // 检测 @ 符号触发提及
  const val = text.value
  const cursorPos = inputRef.value?.selectionStart || val.length
  const beforeCursor = val.slice(0, cursorPos)
  const lastAt = beforeCursor.lastIndexOf('@')
  if (lastAt >= 0 && (lastAt === 0 || beforeCursor[lastAt - 1] === ' ' || beforeCursor[lastAt - 1] === '\n')) {
    const afterAt = beforeCursor.slice(lastAt + 1)
    if (!afterAt.includes(' ')) {
      showMention.value = true
    } else {
      showMention.value = false
    }
  } else {
    showMention.value = false
  }
}

function insertMention(name, userIds) {
  const val = text.value
  const cursorPos = inputRef.value?.selectionStart || val.length
  const beforeCursor = val.slice(0, cursorPos)
  const lastAt = beforeCursor.lastIndexOf('@')
  const before = val.slice(0, lastAt)
  const after = val.slice(cursorPos)
  text.value = before + '@' + name + ' ' + after
  // @所有人：传所有成员 ID；@单个：传指定 ID
  if (userIds.length === 0) {
    currentMentions.value = props.members.map(m => m.userId)
  } else {
    currentMentions.value.push(...userIds)
  }
  showMention.value = false
  nextTick(() => {
    inputRef.value?.focus()
    const pos = before.length + name.length + 2
    inputRef.value?.setSelectionRange(pos, pos)
  })
}

async function onPaste(e) {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (file) {
        try {
          emit('sendImage', file)
          clearDraft()
        } catch { /* ignore */ }
      }
      return
    }
  }
}

function clearDraft() {
  if (!chat.currentConv) return
  const cv = chat.currentConv
  cv.draft = ''
  const item = convStore.list.find(c => c.targetType === cv.targetType && c.targetId === cv.targetId)
  if (item) item.draft = ''
  // 同步 lastConv 防止切会话时把旧草稿写回
  if (lastConv) lastConv.text = ''
  clearTimeout(draftTimer)
  saveDraft(cv.targetType, cv.targetId, '')
}

function onEnter(e) {
  if (e.shiftKey) return
  e.preventDefault()
  if (showMention.value) { showMention.value = false; return }
  onSend()
}

function onSend() {
  const content = text.value.trim()
  if (!content) return
  emit('sendText', content, currentMentions.value.length ? currentMentions.value : [])
  text.value = ''
  currentMentions.value = []
  // 立即清除草稿
  if (chat.currentConv) {
    const cv = chat.currentConv
    cv.draft = ''
    const item = convStore.list.find(c => c.targetType === cv.targetType && c.targetId === cv.targetId)
    if (item) item.draft = ''
    clearTimeout(draftTimer)
    saveDraft(cv.targetType, cv.targetId, '')
  }
  nextTick(() => { if (inputRef.value) inputRef.value.style.height = 'auto' })
}

function onEmoji(emoji) {
  text.value += emoji
  showEmoji.value = false
  nextTick(() => inputRef.value?.focus())
}
function onCustomEmoji(emoji) {
  showEmoji.value = false
  if (!emoji.fileId) {
    console.error('表情缺少 fileId，请重新上传', emoji)
  }
  emit('sendImageUrl', { url: emoji.url, fileId: emoji.fileId })
}
function triggerImage() { imageInput.value?.click() }
function triggerFile() { fileInput.value?.click() }
function onImageChange(e) { const f = e.target.files[0]; if (f) { emit('sendImage', f); e.target.value = '' } }
function onFileChange(e) { const f = e.target.files[0]; if (f) { emit('sendFile', f); e.target.value = '' } }
</script>

<style scoped>
.ib-root { border-top: 1px solid var(--border, #2e3038); flex-shrink: 0; background: var(--chat-bg, #1a1d23); position: relative; }
.ib-mention {
  position: absolute; bottom: 100%; left: 0; right: 0; z-index: 20;
  background: var(--bg-dialog, #1e2028); border: 1px solid var(--border, #2e3038);
  border-radius: 8px 8px 0 0; max-height: 200px; overflow-y: auto;
}
.ib-m-list { padding: 4px 0; }
.ib-m-item {
  display: flex; align-items: center; gap: 10px; padding: 8px 16px;
  cursor: pointer; font-size: 13px; color: var(--text-primary, #e8e8ea);
  transition: background 0.1s;
}
.ib-m-item:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); }
.ib-m-name { flex: 1; }
.ib-m-tag { font-size: 10px; color: var(--accent, #f7931e); }

.ib-tools { display: flex; gap: 2px; padding: 6px 16px 0; }
.ib-tool-btn {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; color: var(--text-muted, #888);
  border-radius: 4px; cursor: pointer; transition: all 0.15s;
}
.ib-tool-btn:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); color: var(--text-secondary, #bbb); }
.ib-tool-btn.recording { color: #e74c3c; animation: ib-pulse 1s infinite; }
@keyframes ib-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.ib-input-area { display: flex; align-items: flex-end; gap: 10px; padding: 8px 16px 12px; }
.ib-textarea {
  flex: 1; border: none; outline: none; background: transparent;
  font-size: 14px; color: var(--text-primary, #e8e8ea);
  resize: none; max-height: 120px; line-height: 1.5; min-height: 24px; font-family: inherit;
}
.ib-textarea::placeholder { color: var(--text-placeholder, #555); }
.ib-send {
  padding: 6px 20px; border: none; border-radius: 4px;
  background: var(--bg-input, #2e3038); color: var(--text-muted, #888);
  font-size: 13px; cursor: pointer; transition: all 0.15s; flex-shrink: 0;
}
.ib-send.ready { background: var(--accent, #f7931e); color: #fff; }
.ib-send:hover:not(:disabled) { opacity: 0.9; }
</style>
