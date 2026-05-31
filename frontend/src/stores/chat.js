import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { messageApi } from '../api/endpoints'
import { CHAT_TYPE } from '../utils/constants'
import { useAuthStore } from './auth'

export const useChatStore = defineStore('chat', () => {
  const currentConv = ref(null)
  const messages = ref([])
  const loading = ref(false)
  const sending = ref(false)
  const jumpMsgId = ref(null)
  const replyTo = ref(null)
  const typingUser = ref(null) // { userId, nickname } or null

  function setTypingUser(data) {
    typingUser.value = data
  }

  const editMsgId = ref(null)
  const editContent = ref('')
  const multiSelect = ref(false)
  const selectedIds = ref(new Set())

  function toggleMultiSelect() { multiSelect.value = !multiSelect.value; if (!multiSelect.value) selectedIds.value.clear() }
  function toggleSelect(id) {
    const s = selectedIds.value
    if (s.has(id)) s.delete(id); else s.add(id)
    selectedIds.value = new Set(s) // 触发响应式
  }
  async function deleteSelected() {
    const ids = [...selectedIds.value]
    for (const id of ids) {
      try { await messageApi.hide(id) } catch {}
    }
    messages.value = messages.value.filter(m => !selectedIds.value.has(m.id))
    multiSelect.value = false; selectedIds.value.clear()
  }
  function startEdit(msgId, content) { editMsgId.value = msgId; editContent.value = content || '' }
  function cancelEdit() { editMsgId.value = null; editContent.value = '' }
  async function submitEdit() {
    if (!editMsgId.value || !editContent.value.trim()) return
    try {
      const updated = await messageApi.edit(editMsgId.value, editContent.value.trim())
      const idx = messages.value.findIndex(m => m.id === editMsgId.value)
      if (idx !== -1) messages.value[idx] = { ...messages.value[idx], ...updated }
    } catch {}
    cancelEdit()
  }

  function setReplyTo(msg) {
    replyTo.value = msg ? {
      id: msg.id,
      preview: msg.content || (msg.msgType === 2 ? '[图片]' : msg.msgType === 3 ? '[文件]' : '[消息]'),
      senderName: msg.fromUserName || msg.fromNickname || ''
    } : null
  }
  function clearReply() { replyTo.value = null }

  function openChat(conversation) {
    currentConv.value = {
      targetType: conversation.targetType,
      targetId: conversation.targetId,
      targetName: conversation.targetName || conversation.name || '',
      targetAvatar: conversation.targetAvatar || conversation.avatar || '',
      online: conversation.targetIsOnline ?? conversation.online ?? false,
      draft: conversation.draft || ''
    }
    messages.value = []
    jumpMsgId.value = conversation._jumpToMsgId || null
  }

  function closeChat() {
    currentConv.value = null
    messages.value = []
  }

  async function fetchMessages(limit = 50) {
    if (!currentConv.value) return
    loading.value = true
    try {
      const { targetType, targetId } = currentConv.value
      let msgs = []
      if (targetType === 1) {
        msgs = await messageApi.singleHistory(targetId, limit)
      } else {
        msgs = await messageApi.groupHistory(targetId, limit)
      }
      // 后端已通过 conversation.cleared_at 过滤，前端不再重复过滤
      messages.value = msgs
    } finally {
      loading.value = false
    }
  }

  // 加载更早的消息
  async function loadOlderMessages(limit = 30) {
    if (!currentConv.value || messages.value.length === 0) return []
    const { targetType, targetId } = currentConv.value
    // 取当前列表中最小的 id（最早的消息）
    const oldestId = messages.value.reduce((min, m) => {
      const id = typeof m.id === 'number' ? m.id : 0
      return id > 0 && id < min ? id : min
    }, Number.MAX_SAFE_INTEGER)
    if (oldestId === Number.MAX_SAFE_INTEGER) return []
    let older = []
    if (targetType === 1) {
      older = await messageApi.singleHistory(targetId, limit, oldestId)
    } else {
      older = await messageApi.groupHistory(targetId, limit, oldestId)
    }
    // 服务器返回的是倒序（最新在前），需要 reverse 后加到列表头部
    older.reverse()
    messages.value = [...older, ...messages.value]
    return older
  }

  // 清空聊天记录：调用后端软删除 + 清空本地列表
  async function clearHistory(targetType, targetId) {
    try {
      await messageApi.clear(targetType, targetId)
    } catch {}
    if (currentConv.value && currentConv.value.targetType === targetType && currentConv.value.targetId === targetId) {
      messages.value = []
    }
  }

  async function sendContactCard(user) {
    if (!currentConv.value) return
    const card = JSON.stringify({
      userId: user.id || user.userId,
      username: user.username,
      nickname: user.nickname || user.username,
      avatar: user.avatar || ''
    })
    return sendMessage({ content: card, msgType: 6 })
  }

  async function sendMessage({ content, msgType, fileId, mentionedUserIds }) {
    if (!currentConv.value) return
    const { targetType, targetId } = currentConv.value
    const auth = useAuthStore()
    const payload = {
      chatType: targetType,
      msgType: msgType || 1,
      content: content || ''
    }
    if (targetType === CHAT_TYPE.SINGLE) {
      payload.toUserId = targetId
    } else {
      payload.groupId = targetId
    }
    if (fileId) payload.fileId = fileId
    if (replyTo.value) payload.replyToId = replyTo.value.id
    if (mentionedUserIds) payload.mentionedUserIds = mentionedUserIds

    // 乐观更新
    const tempId = 'temp_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
    const optimistic = {
      id: tempId,
      chatType: targetType,
      fromUserId: auth.user.id,
      fromUserName: auth.user.nickname || auth.user.username,
      fromUserAvatar: auth.user.avatar,
      msgType: msgType || 1,
      content: content || '',
      fileId: fileId || null,
      isRecalled: 0,
      createdAt: new Date().toISOString(),
      _optimistic: true
    }
    messages.value.push(optimistic)

    const hasReply = !!replyTo.value
    if (hasReply) clearReply()
    sending.value = true
    try {
      const real = await messageApi.send(payload)
      const idx = messages.value.findIndex(m => m.id === tempId)
      if (idx !== -1) {
        messages.value.splice(idx, 1, { ...real, _optimistic: false })
      }
      return real
    } catch (e) {
      const idx = messages.value.findIndex(m => m.id === tempId)
      if (idx !== -1) {
        messages.value[idx] = { ...messages.value[idx], _failed: true }
      }
      throw e
    } finally {
      sending.value = false
    }
  }

  async function recallMessage(messageId) {
    await messageApi.recall(messageId)
    const msg = messages.value.find(m => m.id === messageId)
    if (msg) {
      msg.isRecalled = 1
      msg.content = null
    }
  }

  // 本地删除单条消息（软删除后从 UI 移除）
  function removeMessageLocal(messageId) {
    const idx = messages.value.findIndex(m => m.id === messageId)
    if (idx !== -1) messages.value.splice(idx, 1)
  }

  // WebSocket 推送新消息
  function appendFromPush(message) {
    if (!currentConv.value) return false
    const { targetType, targetId } = currentConv.value
    const isMatch =
      (targetType === 1 && message.chatType === 1 &&
        (message.fromUserId === targetId || message.toUserId === targetId))
      || (targetType === 2 && message.chatType === 2 && message.groupId === targetId)
    if (isMatch) {
      // 1. 精确 ID 去重
      if (messages.value.find(m => m.id === message.id)) return true
      // 2. 替换乐观消息：自己发的消息，WS 推送可能先于 HTTP 响应到达
      const auth = useAuthStore()
      if (message.fromUserId === auth.user?.id) {
        const tempIdx = messages.value.findIndex(m =>
          String(m.id).startsWith('temp_') && m.content === message.content
        )
        if (tempIdx !== -1) {
          messages.value.splice(tempIdx, 1, { ...message, _optimistic: false })
          return true
        }
      }
      messages.value.push(message)
      return true
    }
    return false
  }

  // WebSocket 推送撤回
  function markRecalledFromPush(messageId) {
    const msg = messages.value.find(m => m.id === messageId)
    if (msg) {
      msg.isRecalled = 1
      msg.content = null
      msg.msgType = 5
    }
  }

  const sortedMessages = computed(() => {
    return [...messages.value].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
  })

  return {
    currentConv, messages, loading, sending, jumpMsgId, replyTo, typingUser,
    openChat, closeChat, fetchMessages, loadOlderMessages, sendMessage, sendContactCard, recallMessage, clearHistory,
    setReplyTo, clearReply, setTypingUser, editMsgId, editContent, startEdit, cancelEdit, submitEdit,
    multiSelect, selectedIds, toggleMultiSelect, toggleSelect, deleteSelected,
    removeMessageLocal, appendFromPush, markRecalledFromPush, sortedMessages
  }
})
