<template>
  <div class="ml-root" ref="rootRef" @scroll="onScroll">
    <LoadingSpinner :visible="loading" />
    <div class="ml-inner" ref="innerRef">
      <template v-for="(msg, idx) in messages" :key="msg.id">
        <!-- 日期分隔 -->
        <div v-if="showDateSep(idx)" class="ml-date-sep">
          <span>{{ formatFullTime(msg.createdAt) }}</span>
        </div>
        <div :id="'msg-' + msg.id" @contextmenu.prevent="onMsgCtx($event, msg)" :class="{ 'ml-msg-sel': chatStore.selectedIds.has(msg.id) }" @click="chatStore.multiSelect && chatStore.toggleSelect(msg.id)">
          <div v-if="chatStore.multiSelect" class="ml-check">
            <svg v-if="chatStore.selectedIds.has(msg.id)" viewBox="0 0 24 24" width="18" height="18" fill="#07C160"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
            <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="currentColor" opacity="0.3"><path d="M19 5v14H5V5h14m0-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"/></svg>
          </div>
          <MessageItem :msg="msg" :isGroup="isGroup" @userClick="$emit('userClick', $event)" />
        </div>
      </template>
      <div ref="bottomRef"></div>
    </div>
    <!-- 多选底栏 -->
    <div v-if="chatStore.multiSelect" class="ml-ms-bar">
      <button class="ml-ms-cancel" @click="chatStore.toggleMultiSelect()">取消</button>
      <span class="ml-ms-count">已选 {{ chatStore.selectedIds.size }} 条</span>
      <button class="ml-ms-del" :disabled="!chatStore.selectedIds.size" @click="chatStore.deleteSelected()">删除</button>
    </div>

    <!-- 滚动到底部 -->
    <button v-if="showScrollBtn && !chatStore.multiSelect" class="ml-scroll-btn" @click="scrollToBottom">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"/></svg>
    </button>

    <ContextMenu
      v-if="ctxVisible"
      :visible="ctxVisible"
      :items="ctxItems"
      :position="ctxPos"
      @close="ctxVisible = false"
      @action="onMsgCtxAction"
    />
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted } from 'vue'
import MessageItem from './MessageItem.vue'
import LoadingSpinner from '../common/LoadingSpinner.vue'
import ContextMenu from '../common/ContextMenu.vue'
import { useChatStore } from '../../stores/chat'
import { useAuthStore } from '../../stores/auth'
import { messageApi } from '../../api/endpoints'
import { formatFullTime } from '../../utils/format'

const chatStore = useChatStore()
const auth = useAuthStore()

const props = defineProps({
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  isGroup: { type: Boolean, default: false }
})

const emit = defineEmits(['loadOlder', 'userClick'])

const rootRef = ref(null)
const innerRef = ref(null)
const bottomRef = ref(null)
const showScrollBtn = ref(false)

// 消息右键菜单
const ctxVisible = ref(false)
const ctxPos = ref({ x: 0, y: 0 })
const ctxItems = ref([])
let ctxMsg = null

function onMsgCtx(e, msg) {
  e.stopPropagation()
  ctxMsg = msg
  const myId = Number(auth.user?.id)
  const msgFromId = Number(msg.fromUserId)
  const isMyMsg = myId > 0 && msgFromId === myId
  const isRecalled = msg.isRecalled === 1 || msg.isRecalled === true
  const items = []
  // 多选
  items.push({ label: '多选', action: 'multiSelect' })
  // 引用回复（任何非撤回消息都可回复）
  if (!isRecalled) {
    items.push({ label: '回复', action: 'reply' })
  }
  // 自己的文字消息可编辑
  const isText = msg.msgType === 1 || msg.msgType === 4
  if (isMyMsg && !isRecalled && isText) {
    items.push({ label: '编辑', action: 'edit' })
  }
  // 自己的消息且未撤回：显示撤回按钮
  if (isMyMsg && !isRecalled) {
    items.push({ label: '撤回', action: 'recall' })
  }
  items.push({ label: '删除', action: 'delete', danger: true })
  ctxItems.value = items
  ctxPos.value = { x: e.clientX, y: e.clientY }
  ctxVisible.value = true
}

async function onMsgCtxAction(item) {
  ctxVisible.value = false
  if (!ctxMsg) return
  if (item.action === 'delete') {
    try {
      await messageApi.hide(ctxMsg.id)
    } catch { /* 后端调用失败不影响前端移除 */ }
    chatStore.removeMessageLocal(ctxMsg.id)
  } else if (item.action === 'recall') {
    try {
      await chatStore.recallMessage(ctxMsg.id)
    } catch { /* 后端已校验 */ }
  } else if (item.action === 'reply') {
    chatStore.setReplyTo(ctxMsg)
  } else if (item.action === 'multiSelect') {
    chatStore.toggleMultiSelect()
  } else if (item.action === 'edit') {
    chatStore.startEdit(ctxMsg.id, ctxMsg.content)
  }
}

function showDateSep(idx) {
  if (idx === 0) return true
  const prev = new Date(props.messages[idx - 1].createdAt)
  const cur = new Date(props.messages[idx].createdAt)
  return prev.toDateString() !== cur.toDateString()
}

function scrollToBottom() {
  nextTick(() => {
    bottomRef.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

function onScroll() {
  if (!rootRef.value) return
  const { scrollTop, scrollHeight, clientHeight } = rootRef.value
  showScrollBtn.value = scrollHeight - scrollTop - clientHeight > 150
}

// 新消息自动滚到底部（用户需在底部附近，且忽略初始空 DOM）
watch(() => props.messages.length, () => {
  if (rootRef.value) {
    const { scrollTop, scrollHeight, clientHeight } = rootRef.value
    if (scrollHeight > 0 && scrollHeight - scrollTop - clientHeight < 200) {
      scrollToBottom()
    }
  }
})

onMounted(() => scrollToBottom())

// 跳转到指定消息（来自搜索点击）
watch(() => chatStore.jumpMsgId, (msgId) => {
  if (!msgId) return
  const tryScroll = (retries = 0) => {
    const el = document.getElementById('msg-' + msgId)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      el.classList.add('msg-highlight')
      setTimeout(() => el.classList.remove('msg-highlight'), 2000)
      chatStore.jumpMsgId = null
    } else if (retries < 5) {
      setTimeout(() => tryScroll(retries + 1), 300)
    }
  }
  setTimeout(() => tryScroll(), 200)
})
</script>

<style scoped>
.ml-root {
  flex: 1; overflow-y: auto; position: relative;
  padding: 8px 0;
}
.ml-inner { padding: 0 20px; }
.ml-date-sep {
  text-align: center; margin: 16px 0;
}
.ml-date-sep span {
  font-size: 11px; color: var(--text-muted, #999);
  background: var(--bg-date, rgba(0,0,0,0.2)); padding: 4px 12px;
  border-radius: 4px;
}
.ml-scroll-btn {
  position: absolute; bottom: 12px; right: 20px;
  width: 36px; height: 36px; border-radius: 50%;
  border: 1px solid var(--border, #3a3c44);
  background: var(--bg-dialog, #252529);
  color: var(--text-secondary, #bbb);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  transition: all 0.15s; z-index: 5;
}
.ml-scroll-btn:hover { background: var(--bg-hover, rgba(255,255,255,0.08)); }
:deep(.msg-highlight) {
  animation: msgFlash 0.6s ease 3;
}
@keyframes msgFlash {
  0%, 100% { background: transparent; }
  50% { background: rgba(247,147,30,0.15); border-radius: 6px; }
}
.ml-check { width: 24px; flex-shrink: 0; display: flex; align-items: center; cursor: pointer; }
.ml-msg-sel { background: rgba(7,193,96,0.08); border-radius: 6px; }
.ml-ms-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; background: var(--bg-dialog, #252529);
  border-top: 1px solid var(--border, #3a3c44);
  flex-shrink: 0;
}
.ml-ms-cancel { background: none; border: none; color: var(--text-muted, #888); font-size: 13px; cursor: pointer; }
.ml-ms-count { font-size: 13px; color: var(--text-primary, #e8e8ea); }
.ml-ms-del { padding: 6px 20px; border: none; border-radius: 4px; background: #e74c3c; color: #fff; font-size: 13px; cursor: pointer; }
.ml-ms-del:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
