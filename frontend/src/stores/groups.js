import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { groupApi } from '../api/endpoints'
import { useAuthStore } from './auth'

export const useGroupStore = defineStore('groups', () => {
  const list = ref([])
  const currentGroup = ref(null)
  const currentGroupMembers = ref([])
  const currentGroupFiles = ref([])
  const loading = ref(false)

  async function fetchGroups() {
    loading.value = true
    try {
      list.value = await groupApi.list()
    } finally {
      loading.value = false
    }
  }

  async function fetchGroupDetail(groupId) {
    currentGroup.value = await groupApi.detail(groupId)
    if (currentGroup.value) {
      currentGroupMembers.value = currentGroup.value.memberVOs || currentGroup.value.members || []
    }
    return currentGroup.value
  }

  async function createGroup(data) {
    const g = await groupApi.create(data)
    await fetchGroups()
    return g
  }

  async function addMembers(groupId, userIds) {
    await groupApi.addMembers(groupId, userIds)
    await fetchGroupDetail(groupId)
  }

  async function removeMember(groupId, userId) {
    await groupApi.removeMember(groupId, userId)
    currentGroupMembers.value = currentGroupMembers.value.filter(m => m.userId !== userId)
  }

  async function quitGroup(groupId) {
    await groupApi.quit(groupId)
    list.value = list.value.filter(g => g.id !== groupId)
    currentGroup.value = null
    currentGroupMembers.value = []
  }

  async function dismissGroup(groupId) {
    await groupApi.dismiss(groupId)
    list.value = list.value.filter(g => g.id !== groupId)
    currentGroup.value = null
    currentGroupMembers.value = []
  }

  async function transferOwner(groupId, newOwnerId) {
    await groupApi.transferOwner(groupId, newOwnerId)
    await fetchGroupDetail(groupId)
  }

  async function setAdmin(groupId, userId, isAdmin) {
    await groupApi.setAdmin(groupId, userId, isAdmin)
    const m = currentGroupMembers.value.find(x => x.userId === userId)
    if (m) m.role = isAdmin ? 2 : 3
  }

  async function muteMember(groupId, userId, doMute) {
    await groupApi.mute(groupId, userId, doMute)
    const m = currentGroupMembers.value.find(x => x.userId === userId)
    if (m) m.isMuted = doMute
  }
  // 消息免打扰
  async function toggleMuteNotification(groupId, mute) {
    await groupApi.muteNotification(groupId, mute)
    const uid = currentUserId()
    const m = currentGroupMembers.value.find(x => x.userId === uid)
    if (m) m.isMuted = mute
  }

  async function updateNotice(groupId, notice) {
    await groupApi.updateNotice(groupId, notice)
    if (currentGroup.value) currentGroup.value.notice = notice
  }

  // 群备注（个人可见的备注名，替代群名显示）
  async function updateGroupRemark(groupId, remark) {
    await groupApi.updateMyRemark(groupId, remark)
    const uid = currentUserId()
    // 更新成员列表
    const m = currentGroupMembers.value.find(x => x.userId === uid)
    if (m) m.remark = remark
    // 同步更新群列表中的 remark（用于 ConversationItem 显示）
    const g = list.value.find(x => x.id === groupId)
    if (g) g.remark = remark
  }
  // 群昵称（群内显示的昵称，群友可见）
  async function updateGroupNickname(groupId, nickname) {
    await groupApi.updateRemark(groupId, nickname)
    const uid = currentUserId()
    const m = currentGroupMembers.value.find(x => x.userId === uid)
    if (m) m.nickname = nickname
  }

  async function fetchGroupFiles(groupId) {
    currentGroupFiles.value = await groupApi.listFiles(groupId)
  }

  async function uploadGroupFile(groupId, fileId) {
    await groupApi.uploadFile(groupId, fileId)
    await fetchGroupFiles(groupId)
  }

  async function searchGroups(keyword) {
    if (!keyword || !keyword.trim()) return []
    return await groupApi.search(keyword.trim())
  }

  async function joinGroup(groupId) {
    await groupApi.join(groupId)
    await fetchGroups()
  }

  async function updateGroupAvatar(groupId, avatarUrl) {
    await groupApi.updateAvatar(groupId, avatarUrl)
    if (currentGroup.value) currentGroup.value.avatar = avatarUrl
  }

  const currentUserId = () => useAuthStore().user?.id
  const isOwner = (groupId) => {
    const g = list.value.find(x => x.id === groupId) || currentGroup.value
    return g?.ownerId === currentUserId()
  }
  const isAdmin = (groupId) => {
    const members = currentGroupMembers.value
    const me = members.find(m => m.userId === currentUserId())
    return me && (me.role === 1 || me.role === 2)
  }
  const currentGroupRole = (groupId) => {
    const members = currentGroupMembers.value
    const me = members.find(m => m.userId === currentUserId())
    return me?.role || 0
  }

  return {
    list, currentGroup, currentGroupMembers, currentGroupFiles, loading,
    fetchGroups, fetchGroupDetail, createGroup, addMembers,
    removeMember, quitGroup, dismissGroup, transferOwner,
    setAdmin, muteMember, toggleMuteNotification, updateNotice, updateGroupRemark, updateGroupNickname,
    fetchGroupFiles, uploadGroupFile, searchGroups, joinGroup,
    updateGroupAvatar, isOwner, isAdmin, currentGroupRole
  }
})
