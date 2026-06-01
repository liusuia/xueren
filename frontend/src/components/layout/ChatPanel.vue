<template>
  <div class="chat-panel" v-if="chat.currentConv" :style="{ backgroundColor: 'var(--chat-bg, #1a1d23)' }" @dragover.prevent @drop.prevent="onDrop">
    <div v-if="chatBg" class="cp-bg-blur" :style="{ backgroundImage: 'url(' + chatBg + ')' }"></div>
    <div v-if="chatBg" class="cp-bg-main" :style="{ backgroundImage: 'url(' + chatBg + ')' }"></div>
    <!-- 头部 -->
    <div class="cp-header">
      <!-- 移动端返回按钮 -->
      <button class="cp-back-btn" @click="$emit('close')" title="返回">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>
      </button>
      <div class="cp-h-left">
        <div v-if="chat.currentConv.targetType === 1 && chat.currentConv.targetId === 1" class="cp-fh-avatar">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="#f7931e"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 15h8v2H8zm0-4h8v2H8z"/></svg>
        </div>
        <Avatar v-else :src="chat.currentConv.targetAvatar" :name="headerName" :size="34" class="cp-h-avatar" @click.stop="onAvatarClick" />
        <div class="cp-h-info">
          <div class="cp-h-name">{{ headerName }}</div>
          <div class="cp-h-typing" v-if="chat.typingUser">对方正在输入...</div>
          <div class="cp-h-status" v-else-if="chat.currentConv.targetType === 1 && chat.currentConv.targetId !== 1">
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
        <button class="cp-h-btn" @click="triggerBg" @contextmenu.prevent="resetBg" :title="chatBg ? '右键恢复默认背景' : '设置背景'">
          <svg viewBox="0 0 24 24" width="18" height="18" :fill="chatBg ? '#f7931e' : 'currentColor'"><path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/></svg>
        </button>
        <input type="file" ref="bgInput" accept="image/*" @change="onBgChange" style="display:none" />
        <button class="cp-h-btn" @click="$emit('close')" title="关闭">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
        </button>
      </div>
    </div>

    <!-- 消息列表 -->
    <MessageList
      ref="msgListRef"
      :messages="chat.sortedMessages"
      :loading="chat.loading"
      :isGroup="chat.currentConv.targetType === 2"
      @loadOlder="loadOlder"
      @userClick="(uid) => $emit('userInfo', uid)"
      @preview="onPreview"
    />
    <ImagePreview :visible="previewVisible" :images="previewImages" :index="previewIndex" @close="previewVisible = false" />

    <!-- 回复指示条 -->
    <div v-if="chat.replyTo" class="cp-reply-bar">
      <div class="cp-reply-info">
        <span class="cp-reply-label">回复 {{ chat.replyTo.senderName }}</span>
        <span class="cp-reply-preview">{{ chat.replyTo.preview }}</span>
      </div>
      <button class="cp-reply-close" @click="chat.clearReply()">&times;</button>
    </div>

    <!-- 输入栏 -->
    <InputBar
      ref="inputBarRef"
      :isGroup="chat.currentConv.targetType === 2"
      :members="groupMembers"
      @sendText="onSendText"
      @sendImage="onSendImage"
      @sendFile="onSendFile"
      @sendEmoji="onSendEmoji"
      @sendImageUrl="onSendImageUrl"
      @sendVoice="onSendVoice"
    />
  </div>

  <!-- 空状态 -->
  <div v-else class="cp-empty">
    <div class="cp-empty-logo">
      <img src="/logo.png" alt="logo" class="cp-empty-img" />
    </div>
    <div class="cp-empty-title">轻语</div>
    <div class="cp-empty-sub">选择一个会话开始聊天</div>
  </div>
</template>

<script setup>
import Avatar from '../common/Avatar.vue'
import OnlineDot from '../common/OnlineDot.vue'
import MessageList from '../chat/MessageList.vue'
import InputBar from '../chat/InputBar.vue'
import ImagePreview from '../chat/ImagePreview.vue'
import { useChatStore } from '../../stores/chat'
import { useGroupStore } from '../../stores/groups'
import { useContactStore } from '../../stores/contacts'
import { fileApi } from '../../api/endpoints'
import { MSG_TYPE } from '../../utils/constants'
import http from '../../api/http'
import { computed, ref, watch, nextTick, onMounted } from 'vue'

const chat = useChatStore()
const groupStore = useGroupStore()
const contactStore = useContactStore()
const inputBarRef = ref(null)
const bgInput = ref(null)
const chatBg = ref('')

function getBgKey() {
  const cv = chat.currentConv
  return cv ? 'xr-bg-' + cv.targetType + '-' + cv.targetId : ''
}
function loadBg() { chatBg.value = localStorage.getItem(getBgKey()) || '' }
function triggerBg() { bgInput.value?.click() }
function resetBg() { localStorage.removeItem(getBgKey()); chatBg.value = '' }
function onBgChange(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => { localStorage.setItem(getBgKey(), reader.result); chatBg.value = reader.result }
  reader.readAsDataURL(file)
  e.target.value = ''
}
watch(() => chat.currentConv?.targetId, loadBg)
onMounted(loadBg)

// 点击回复后自动聚焦输入框
watch(() => chat.replyTo, (val) => {
  if (val) nextTick(() => inputBarRef.value?.focus())
})

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
  if (chat.currentConv.targetId === 1) return // 文件助手
  if (chat.currentConv.targetType === 2) {
    emit('groupInfo')
  } else {
    emit('userInfo', chat.currentConv.targetId)
  }
}

const emit = defineEmits(['close', 'groupInfo', 'userInfo', 'friendInfo'])
const previewVisible = ref(false)
const previewImages = ref([])
const previewIndex = ref(0)
function onPreview({ images, index }) {
  previewImages.value = images
  previewIndex.value = index
  previewVisible.value = true
}

async function loadOlder() {
  await chat.loadOlderMessages()
  msgListRef.value?.onLoadOlderDone()
}

async function onSendText(content, mentions) {
  await chat.sendMessage({ content, msgType: 1, mentionedUserIds: mentions?.length ? mentions : undefined })
  nextTick(() => msgListRef.value?.scrollToBottom())
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
async function onSendImageUrl({ url, fileId }) {
  await chat.sendMessage({ content: url, msgType: MSG_TYPE.STICKER, fileId })
}
async function onSendVoice({ blob, duration }) {
  const form = new FormData()
  form.append('file', blob, 'voice.webm')
  const res = await http.post('/files/upload', form)
  const fileVO = res?.data || res
  if (fileVO) {
    await chat.sendMessage({ content: fileVO.url, msgType: MSG_TYPE.VOICE, fileId: fileVO.id, _voiceDuration: duration })
  }
}
async function onSendLocation({ url, latitude, longitude }) {
  await chat.sendMessage({ content: url, msgType: MSG_TYPE.IMAGE, _location: { lat: latitude, lon: longitude } })
}

async function onDrop(e) {
  const files = e.dataTransfer?.files
  if (!files || !files.length) return
  for (const file of files) {
    try {
      const fileVO = await fileApi.upload(file)
      if (file.type.startsWith('image/')) {
        await chat.sendMessage({ content: fileVO.url, msgType: 2, fileId: fileVO.id })
      } else {
        await chat.sendMessage({ content: fileVO.originalName, msgType: 3, fileId: fileVO.id })
      }
    } catch {}
  }
}
</script>

<style scoped>
.chat-panel {
  flex: 1; display: flex; flex-direction: column; height: 100%;
  background: var(--chat-bg, #1a1d23); min-width: 0;
  position: relative; overflow: hidden;
}
.cp-bg-blur {
  position: absolute; inset: 0; z-index: -1;
  background-size: cover; background-position: center;
  filter: blur(20px); opacity: 0.6; transform: scale(1.1);
}
.cp-bg-main {
  position: absolute; inset: 0; z-index: -1;
  background-size: contain; background-repeat: no-repeat; background-position: center;
}
.cp-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; border-bottom: 1px solid var(--border, #2e3038);
  flex-shrink: 0; position: relative; z-index: 1;
}
.cp-h-left { display: flex; align-items: center; gap: 10px; }
.cp-h-info { min-width: 0; }
.cp-h-name { font-size: 15px; font-weight: 500; color: var(--text-primary, #e8e8ea); }
.cp-h-status { display: flex; align-items: center; gap: 4px; font-size: 11px; color: var(--text-muted, #999); margin-top: 1px; }
.cp-h-typing { font-size: 11px; color: var(--accent, #f7931e); margin-top: 1px; animation: typingBlink 1s ease-in-out infinite; }
@keyframes typingBlink { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.cp-h-right { display: flex; gap: 4px; }
.cp-h-btn {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; color: var(--text-muted, #888);
  border-radius: 4px; cursor: pointer; transition: all 0.15s;
}
.cp-h-btn:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); color: var(--text-secondary, #bbb); }
/* 移动端返回按钮 */
.cp-back-btn {
  display: none; width: 32px; height: 32px; align-items: center; justify-content: center;
  border: none; background: transparent; color: var(--text-muted, #888);
  border-radius: 4px; cursor: pointer; margin-right: 6px; flex-shrink: 0;
}
.cp-back-btn:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); }
/* 非桌面端显示返回按钮 */
.ml-root.mobile .cp-back-btn,
.ml-root.tablet .cp-back-btn { display: flex; }
.cp-fh-avatar { width: 34px; height: 34px; border-radius: 8px; background: rgba(247,147,30,0.15); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }

.cp-reply-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 16px; background: var(--bg-input, #2e3038);
  border-top: 1px solid var(--border, #3a3c44);
  font-size: 12px;
}
.cp-reply-label { color: var(--accent, #f7931e); font-weight: 500; margin-right: 8px; }
.cp-reply-preview { color: var(--text-muted, #999); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.cp-reply-close { background: none; border: none; color: var(--text-muted, #888); cursor: pointer; font-size: 18px; padding: 0 4px; }

.cp-empty {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%; background: var(--chat-bg, #1a1d23); user-select: none;
  color: var(--text-muted, #777);
}
.cp-empty-logo {
  width: 80px; height: 80px; border-radius: 50%;
  background: linear-gradient(135deg, #f7931e 0%, #e67e22 100%);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 16px; overflow: hidden;
}
.cp-empty-img { width: 100%; height: 100%; object-fit: cover; }
.cp-empty-title { font-size: 18px; color: var(--text-secondary, #bbb); margin-bottom: 4px; }
.cp-empty-sub { font-size: 13px; }
</style>
