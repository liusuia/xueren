import { defineStore } from 'pinia'
import { ref } from 'vue'
import { convApi } from '../api/endpoints'

export const useConversationStore = defineStore('conversations', () => {
  const list = ref([])
  const loading = ref(false)

  // 免打扰集合 — 用 ref+version 解决 reactive(Set) 的响应式缺陷
  const mutedSet = new Set(JSON.parse(localStorage.getItem('xr_muted') || '[]'))
  const mutedVersion = ref(0)
  function saveMuted() { localStorage.setItem('xr_muted', JSON.stringify([...mutedSet])) }
  function setMuted(targetType, targetId, muted) {
    const key = targetType + '_' + targetId
    if (muted) mutedSet.add(key)
    else mutedSet.delete(key)
    saveMuted()
    mutedVersion.value++  // 触发 computed 重新计算
  }
  function isMuted(targetType, targetId) {
    mutedVersion.value  // 建立响应式依赖
    return mutedSet.has(targetType + '_' + targetId)
  }

  // 置顶集合 — 同上
  const pinnedSet = new Set(JSON.parse(localStorage.getItem('xr_pinned') || '[]'))
  const pinnedVersion = ref(0)
  function savePinned() { localStorage.setItem('xr_pinned', JSON.stringify([...pinnedSet])) }
  function togglePinned(targetType, targetId) {
    const key = targetType + '_' + targetId
    if (pinnedSet.has(key)) pinnedSet.delete(key)
    else pinnedSet.add(key)
    savePinned()
    pinnedVersion.value++
    return pinnedSet.has(key)
  }
  function isPinned(targetType, targetId) {
    pinnedVersion.value
    return pinnedSet.has(targetType + '_' + targetId)
  }

  async function fetchConversations() {
    loading.value = true
    try {
      const raw = await convApi.list()
      raw.sort((a, b) => {
        // 文件助手永远在最前
        const aFH = a.targetType === 1 && a.targetId === 1
        const bFH = b.targetType === 1 && b.targetId === 1
        if (aFH !== bFH) return aFH ? -1 : 1
        // 置顶的排前面
        const pa = pinnedSet.has(a.targetType + '_' + a.targetId) ? 1 : 0
        const pb = pinnedSet.has(b.targetType + '_' + b.targetId) ? 1 : 0
        if (pa !== pb) return pb - pa
        return new Date(b.lastMessageAt || 0) - new Date(a.lastMessageAt || 0)
      })
      list.value = raw
    } finally {
      loading.value = false
    }
  }

  async function deleteConversation(convId) {
    await convApi.delete(convId)
    list.value = list.value.filter(c => c.id !== convId)
  }

  function markReadLocal(targetType, targetId, lastMessageId) {
    const conv = list.value.find(c => c.targetType === targetType && c.targetId === targetId)
    if (conv) {
      conv.unreadCount = 0
      if (lastMessageId) conv.lastReadMessageId = lastMessageId
    }
  }

  // 消息撤回后：会话列表预览改为 [消息已撤回]
  function updatePreviewToRecall(targetType, targetId, messageId) {
    const conv = list.value.find(c => c.targetType === targetType && c.targetId === targetId)
    if (conv && conv.lastMessageId === messageId) {
      conv.lastMessagePreview = '[消息已撤回]'
    }
  }

  function clearPreview(targetType, targetId) {
    const conv = list.value.find(c => c.targetType === targetType && c.targetId === targetId)
    if (conv) {
      conv.lastMessagePreview = ''
      conv.unreadCount = 0
      conv.lastMessageAt = null
      conv.lastMessageId = null
    }
  }

  async function markRead(targetType, targetId, lastMessageId) {
    markReadLocal(targetType, targetId, lastMessageId)
    try { await convApi.markRead(targetType, targetId, lastMessageId) } catch {}
  }

  // WebSocket 推送 NEW_MESSAGE 时更新会话列表
  function updateFromPush(data, currentUserId) {
    const msg = data.data || data
    if (!msg || !msg.id) return

    const targetId = msg.chatType === 1 ? msg.fromUserId : msg.groupId
    const existing = list.value.find(c =>
      c.targetType === msg.chatType && c.targetId === targetId
    )

    // 判断是否跳过未读：自己发的消息 或 该会话已免打扰
    const isSelf = currentUserId && msg.fromUserId === currentUserId
    const muted = mutedSet.has(msg.chatType + '_' + targetId)
    const skipUnread = isSelf || muted

    if (existing) {
      existing.lastMessageId = msg.id
      existing.lastMessagePreview = buildPreview(msg)
      existing.lastMessageAt = msg.createdAt
      if (!skipUnread) {
        existing.unreadCount = (existing.unreadCount || 0) + 1
      }
      list.value.sort((a, b) => {
        const pa = pinnedSet.has(a.targetType + '_' + a.targetId) ? 1 : 0
        const pb = pinnedSet.has(b.targetType + '_' + b.targetId) ? 1 : 0
        if (pa !== pb) return pb - pa
        return new Date(b.lastMessageAt) - new Date(a.lastMessageAt)
      })
    } else {
      fetchConversations()
    }
  }

  function buildPreview(msg) {
    if (msg.isRecalled) return '[消息已撤回]'
    let prefix = ''
    if (msg.chatType === 2) {
      const name = msg.fromNickname || msg.fromUserName || ''
      if (name) prefix = name + ': '
    }
    let content
    if (msg.msgType === 2) content = '[图片]'
    else if (msg.msgType === 7) content = '[表情]'
    else if (msg.msgType === 3) content = '[文件]'
    else if (msg.msgType === 5) content = '[系统消息]'
    else content = msg.content || ''
    const text = prefix + content
    return text.length > 50 ? text.slice(0, 50) + '...' : text
  }

  return {
    list, loading,
    fetchConversations, deleteConversation, markRead, markReadLocal,
    updateFromPush, clearPreview, updatePreviewToRecall, setMuted, isMuted, togglePinned, isPinned
  }
})
