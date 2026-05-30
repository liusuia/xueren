<template>
  <div class="chat-layout" :class="theme">
    <div class="sidebar">
      <div class="side-tabs">
        <span class="side-tab" :class="{ active: sideTab === 'chat' }" @click="sideTab = 'chat'">聊天</span>
        <span class="side-tab" :class="{ active: sideTab === 'contacts' }" @click="sideTab = 'contacts'">通讯录</span>
        <span class="side-tab" :class="{ active: sideTab === 'me' }" @click="sideTab = 'me'">我</span>
      </div>
      <div class="side-panel">
        <div v-if="sideTab === 'chat'" class="panel-chat">
          <div class="conv-list">
            <div v-for="c in conversations" :key="c.id" class="conv-item" :class="{ active: selectedConv?.id === c.id }" @click="selectConv(c)">
              <div class="conv-avatar" :style="avatarBg(c.targetName, c.targetAvatar)">{{ c.targetName[0] }}</div>
              <div class="conv-info">
                <div class="conv-name">{{ c.targetName }}</div>
                <div class="conv-preview">{{ c.lastMessagePreview || '' }}</div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="sideTab === 'contacts'" class="panel-contacts">
          <div v-for="f in friends" :key="f.id" class="contact-item" @click="openFriendChat(f)">
            <div class="contact-avatar" :style="avatarBg(f.nickname || f.username, f.avatar)">{{ (f.nickname || f.username)[0] }}</div>
            <span>{{ f.nickname || f.username }}</span>
          </div>
        </div>
        <div v-if="sideTab === 'me'" class="panel-me">
          <div class="me-card">
            <div class="me-avatar" :style="avatarBg(auth.user?.nickname || auth.user?.username, auth.user?.avatar)">{{ (auth.user?.nickname || auth.user?.username || '?')[0] }}</div>
            <div class="me-name">{{ auth.user?.nickname || auth.user?.username }}</div>
            <div class="me-desc">ID: {{ auth.user?.id }}</div>
          </div>
          <button class="logout-btn" @click="logout">退出登录</button>
        </div>
      </div>
    </div>
    <div class="main-panel">
      <template v-if="selectedConv">
        <div class="chat-header">{{ selectedConv.targetName }}</div>
        <div class="message-list" ref="msgListRef">
          <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="{ self: msg.senderId === auth.user?.id }">
            <div class="msg-bubble" :class="{ mine: msg.senderId === auth.user?.id }">{{ msg.content }}</div>
          </div>
        </div>
        <div class="input-bar">
          <input v-model="inputText" class="msg-input" @keyup.enter="sendMsg" placeholder="输入消息..." />
          <button @click="sendMsg">发送</button>
        </div>
      </template>
      <div v-else class="no-conv">
        <div class="no-conv-logo">
          <svg viewBox="0 0 100 100" width="64" height="64">
            <path d="M50 8C35 8 22 18 18 32c-2 7-1 14 2 20l-8 20c-1 2 0 4 2 5l14 6c3 1 6 0 8-2l2-3c5 3 10 4 16 4 5 0 10-1 15-4l2 3c2 2 5 3 8 2l14-6c2-1 3-3 2-5l-8-20c3-6 4-13 2-20C78 18 65 8 50 8z" fill="rgba(255,255,255,0.08)"/>
            <path d="M50 20c-8 0-14 6-14 14 0 3 1 5 2 8l-6 15c-1 2 0 3 1 4l8 3c2 1 4 0 5-1l1-2c3 2 7 3 11 3s7-1 10-3l1 2c1 1 3 2 5 1l8-3c1-1 2-2 1-4l-6-15c1-3 2-5 2-8 0-8-6-14-14-14z" fill="#f7931e" opacity="0.6"/>
          </svg>
        </div>
        <div class="no-conv-title">NARUTO</div>
        <div class="no-conv-sub">选择一个会话开始聊天</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElNotification } from 'element-plus'
import http from '../api/http'
import { addWsListener } from '../api/ws'
import { useAuthStore } from '../stores/auth'
import { pinyin } from 'pinyin-pro'

const auth = useAuthStore()
const router = useRouter()
const theme = ref(localStorage.getItem('xr-theme') || 'dark')
const sideTab = ref('chat')
const conversations = ref([])
const friends = ref([])
const selectedConv = ref(null)
const messages = ref([])
const inputText = ref('')
const msgListRef = ref(null)

const colors = ['#f7931e', '#e67e22', '#d35400', '#e74c3c', '#9b59b6', '#3498db', '#1abc9c', '#2ecc71']
function avatarBg(name, avatar) {
  if (avatar) return { backgroundImage: `url(${avatar})`, backgroundSize: 'cover' }
  let h = 0; const s = name || '?'
  for (let i = 0; i < s.length; i++) h = s.charCodeAt(i) + ((h << 5) - h)
  return { backgroundColor: colors[Math.abs(h) % colors.length] }
}

function mapMessage(m) {
  return {
    id: m.id, senderId: m.fromUserId, senderName: m.fromNickname,
    senderAvatar: m.fromUserAvatar, chatType: m.chatType,
    type: ['', 'text', 'image', 'file'][m.msgType || 1] || 'text',
    content: m.content, createdAt: m.createdAt, fileUrl: m.fileUrl
  }
}

function notify(msg, type) {
  ElNotification({ title: '', message: msg, type, duration: type === 'success' ? 2000 : 3000, offset: 60, customClass: 'xr-notify' })
}

async function loadAll() {
  try {
    const [c, f] = await Promise.all([
      http.get('/conversations').catch(() => []),
      http.get('/friends').catch(() => [])
    ])
    conversations.value = Array.isArray(c) ? c : []
    friends.value = Array.isArray(f) ? f : []
  } catch {}
}

function selectConv(c) {
  selectedConv.value = c
  loadMessages()
}

async function loadMessages() {
  if (!selectedConv.value) return
  try {
    var conv = selectedConv.value
    var endpoint = conv.targetType === 2 ? '/messages/group/' + conv.targetId : '/messages/single/' + conv.targetId
    var data = await http.get(endpoint)
    messages.value = (data || []).map(mapMessage)
    nextTick(scrollBottom)
  } catch { messages.value = [] }
}

function scrollBottom() {
  nextTick(() => {
    const el = msgListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function sendMsg() {
  const text = inputText.value.trim()
  if (!text || !selectedConv.value) return
  inputText.value = ''
  const conv = selectedConv.value
  var isGroup = conv.targetType === 2
  var payload = { chatType: conv.targetType || 1, msgType: 1, content: text }
  if (isGroup) payload.groupId = conv.targetId
  else payload.toUserId = conv.targetId
  var optimistic = { id: Date.now(), content: text, senderId: auth.user?.id, type: 'text', _optimistic: true, createdAt: new Date().toISOString() }
  messages.value.push(optimistic)
  nextTick(scrollBottom)
  try {
    var real = await http.post('/messages', payload)
    real = mapMessage(real)
  } catch { messages.value = messages.value.filter(m => m.id !== msg.id) }
}

function openFriendChat(f) {
  const existing = conversations.value.find(c => c.targetId === f.id && c.targetType === 1)
  if (existing) { selectConv(existing); return }
  const c = { id: Date.now(), targetId: f.id, targetType: 1, targetName: f.nickname || f.username, lastMessagePreview: '' }
  conversations.value.unshift(c)
  selectConv(c)
}

function logout() {
  ElMessageBox.confirm('确定退出登录？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    .then(() => { auth.logout(); router.push('/login') }).catch(() => {})
}

function applyTheme(v) { document.documentElement.setAttribute('data-theme', v) }
onMounted(() => {
  applyTheme(theme.value)
  loadAll()
  addWsListener((pkt) => {
    try { const p = JSON.parse(pkt); if (p.type === 'NEW_MESSAGE') loadAll() } catch {}
  })
})
watch(theme, applyTheme)
</script>

<style scoped>
.chat-layout { height: 100%; display: flex; overflow: hidden; --accent: #f7931e; --accent-dark: #e67e22; --bg-main: #0f1219; --bg-sidebar: #151a24; --bg-panel: #1a1d23; --bg-hover: rgba(255,255,255,0.04); --text-primary: #e8e6e3; --text-secondary: #9899a5; --text-muted: #6b6d7a; --border: #2e3038; }
.chat-layout.light { --bg-main: #e8ecf0; --bg-sidebar: #f0f2f5; --bg-panel: #fff; --text-primary: #1a1a1e; --text-secondary: #5e5f6b; --text-muted: #999; --border: #e8ecf0; }
.sidebar { width: 300px; display: flex; flex-direction: column; background: var(--bg-sidebar); border-right: 1px solid var(--border); }
.side-tabs { display: flex; border-bottom: 1px solid var(--border); }
.side-tab { flex: 1; text-align: center; padding: 14px 0; cursor: pointer; color: var(--text-muted); font-size: 14px; font-weight: 500; transition: all 0.2s; border-bottom: 2px solid transparent; }
.side-tab:hover { color: var(--text-secondary); }
.side-tab.active { color: var(--accent); border-bottom-color: var(--accent); font-weight: 600; }
.side-panel { flex: 1; overflow-y: auto; }
.conv-list { padding: 8px 0; }
.conv-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; cursor: pointer; transition: background 0.15s; }
.conv-item:hover { background: var(--bg-hover); }
.conv-item.active { background: rgba(247,147,30,0.08); }
.conv-avatar { width: 44px; height: 44px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: 600; flex-shrink: 0; }
.conv-info { flex: 1; min-width: 0; }
.conv-name { font-size: 14px; font-weight: 500; color: var(--text-primary); }
.conv-preview { font-size: 12px; color: var(--text-muted); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.contact-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; cursor: pointer; color: var(--text-primary); font-size: 14px; }
.contact-item:hover { background: var(--bg-hover); }
.contact-avatar { width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 16px; font-weight: 600; flex-shrink: 0; }
.panel-me { padding: 20px; }
.me-card { text-align: center; margin-bottom: 20px; }
.me-avatar { width: 68px; height: 68px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 28px; font-weight: 600; margin: 0 auto 10px; }
.me-name { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.me-desc { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.logout-btn { width: 100%; padding: 10px; border-radius: 8px; border: 1px solid var(--border); background: transparent; color: var(--text-secondary); font-size: 14px; cursor: pointer; }
.logout-btn:hover { background: rgba(231,76,60,0.1); color: #e74c3c; }
.main-panel { flex: 1; display: flex; flex-direction: column; background: var(--bg-panel); }
.chat-header { padding: 14px 20px; font-size: 16px; font-weight: 600; color: var(--text-primary); border-bottom: 1px solid var(--border); }
.message-list { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.msg-row { display: flex; }
.msg-row.self { justify-content: flex-end; }
.msg-bubble { background: var(--bg-hover); padding: 8px 14px; border-radius: 8px 8px 8px 0; font-size: 14px; color: var(--text-primary); max-width: 70%; word-break: break-word; }
.msg-bubble.mine { background: var(--accent); color: #fff; border-radius: 8px 8px 0 8px; }
.input-bar { display: flex; gap: 10px; padding: 12px 20px; border-top: 1px solid var(--border); }
.msg-input { flex: 1; height: 40px; padding: 0 14px; border: 1px solid var(--border); border-radius: 8px; background: transparent; color: var(--text-primary); font-size: 14px; outline: none; }
.msg-input:focus { border-color: var(--accent); }
.input-bar button { padding: 0 20px; border: none; border-radius: 8px; background: var(--accent); color: #fff; font-size: 14px; font-weight: 500; cursor: pointer; }
.no-conv { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; }
.no-conv-title { font-size: 24px; font-weight: 400; letter-spacing: 3px; color: var(--text-primary); font-family: 'Russo One', sans-serif; }
.no-conv-sub { font-size: 13px; color: var(--text-muted); }
</style>
