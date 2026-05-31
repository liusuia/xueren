import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { friendApi, userApi } from '../api/endpoints'
import { pinyin } from 'pinyin-pro'

// FriendVO 字段: { id, userId, username, nickname, avatar, remark, status, requesterId, isOnline }
export const useContactStore = defineStore('contacts', () => {
  const friends = ref([])
  const requests = ref([])
  const blockedIds = ref([])
  const blockedByIds = ref([])
  const loading = ref(false)

  async function fetchFriends() {
    loading.value = true
    try {
      friends.value = await friendApi.list()
    } finally {
      loading.value = false
    }
  }

  async function fetchRequests() {
    requests.value = await friendApi.requests()
  }

  async function fetchBlocked() {
    blockedIds.value = await friendApi.blocked()
  }

  async function fetchBlockedBy() {
    blockedByIds.value = await friendApi.blockedBy()
  }

  async function sendFriendRequest(userId) {
    await friendApi.sendRequest(userId)
  }

  async function acceptRequest(requesterId) {
    await friendApi.accept(requesterId)
    requests.value = requests.value.filter(r => r.requesterId !== requesterId)
    await fetchFriends()
  }

  async function rejectRequest(requesterId) {
    await friendApi.reject(requesterId)
    requests.value = requests.value.filter(r => r.requesterId !== requesterId)
  }

  async function blockFriend(friendId) {
    await friendApi.block(friendId)
    friends.value = friends.value.filter(f => f.userId !== friendId)
    blockedIds.value.push(friendId)
  }

  async function unblockFriend(friendId) {
    await friendApi.unblock(friendId)
    blockedIds.value = blockedIds.value.filter(id => id !== friendId)
    await fetchFriends()
  }

  async function deleteFriend(friendId) {
    await friendApi.delete(friendId)
    friends.value = friends.value.filter(f => f.userId !== friendId)
  }

  async function updateRemark(friendId, remark) {
    await friendApi.updateRemark(friendId, remark)
    const f = friends.value.find(x => x.userId === friendId)
    if (f) f.remark = remark
  }

  async function searchUsers(keyword) {
    if (!keyword || !keyword.trim()) return []
    return await userApi.search(keyword.trim())
  }

  // 拼音分组好友列表
  const friendSections = computed(() => {
    const groups = {}
    for (const f of friends.value) {
      const name = f.remark || f.nickname || f.username || ''
      let letter = '#'
      if (name) {
        const first = name.charAt(0)
        if (/[a-zA-Z]/.test(first)) {
          letter = first.toUpperCase()
        } else if (/[一-鿿]/.test(first)) {
          try {
            const py = pinyin(first, { pattern: 'first', toneType: 'none' })
            if (py && /[a-zA-Z]/.test(py[0])) letter = py[0].toUpperCase()
          } catch { letter = '#' }
        }
      }
      if (!groups[letter]) groups[letter] = []
      groups[letter].push(f)
    }
    const sorted = Object.keys(groups).sort((a, b) => {
      if (a === '#') return 1
      if (b === '#') return -1
      return a.localeCompare(b)
    })
    return sorted.map(letter => ({
      letter,
      items: groups[letter].sort((a, b) => {
        const na = a.remark || a.nickname || a.username || ''
        const nb = b.remark || b.nickname || b.username || ''
        return na.localeCompare(nb)
      })
    }))
  })

  const pendingRequestCount = computed(() => requests.value.length)

  function isBlocked(userId) {
    return blockedIds.value.includes(userId)
  }

  function isBlockedBy(userId) {
    return blockedByIds.value.includes(userId)
  }

  return {
    friends, requests, blockedIds, blockedByIds, loading,
    friendSections, pendingRequestCount,
    fetchFriends, fetchRequests, fetchBlocked, fetchBlockedBy,
    sendFriendRequest, acceptRequest, rejectRequest,
    blockFriend, unblockFriend, deleteFriend, updateRemark,
    searchUsers, isBlocked, isBlockedBy
  }
})
