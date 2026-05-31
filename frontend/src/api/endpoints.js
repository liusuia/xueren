import http from './http'

// ==================== Auth ====================
export const authApi = {
  login: (data) => http.post('/auth/login', data),
  register: (data) => http.post('/auth/register', data),
  refresh: (data) => http.post('/auth/refresh', data),
  logout: () => http.post('/auth/logout')
}

// ==================== User ====================
export const userApi = {
  getMe: () => http.get('/users/me'),
  getUser: (id) => http.get(`/users/${id}`),
  search: (keyword) => http.get('/users/search', { params: { keyword } }),
  uploadAvatar: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/users/avatar', fd)
  },
  updateProfile: (data) => http.put('/users/profile', data)
}

// ==================== Friend ====================
export const friendApi = {
  list: () => http.get('/friends'),
  requests: () => http.get('/friends/requests'),
  sendRequest: (friendId, verificationMsg) => http.post('/friends/request', { friendId, verificationMsg }),
  accept: (requesterId) => http.post(`/friends/accept/${requesterId}`),
  reject: (requesterId) => http.post(`/friends/reject/${requesterId}`),
  block: (friendId) => http.post(`/friends/block/${friendId}`),
  unblock: (friendId) => http.post(`/friends/unblock/${friendId}`),
  blocked: () => http.get('/friends/blocked'),
  blockedBy: () => http.get('/friends/blocked-by'),
  updateRemark: (friendId, remark) => http.put(`/friends/${friendId}/remark`, { remark }),
  delete: (friendId) => http.delete(`/friends/${friendId}`)
}

// ==================== Group ====================
export const groupApi = {
  list: () => http.get('/groups'),
  create: (data) => http.post('/groups', data),
  detail: (groupId) => http.get(`/groups/${groupId}`),
  addMember: (groupId, userId) => http.post(`/groups/${groupId}/members/${userId}`),
  addMembers: (groupId, userIds) => http.post(`/groups/${groupId}/members`, userIds),
  quit: (groupId) => http.delete(`/groups/${groupId}/members/me`),
  removeMember: (groupId, userId) => http.delete(`/groups/${groupId}/members/${userId}`),
  dismiss: (groupId) => http.delete(`/groups/${groupId}`),
  transferOwner: (groupId, newOwnerId) => http.put(`/groups/${groupId}/owner`, newOwnerId),
  updateRemark: (groupId, remark) => http.put(`/groups/${groupId}/remark`, { remark }),
  updateMyRemark: (groupId, remark) => http.put(`/groups/${groupId}/my-remark`, { remark }),
  updateNotice: (groupId, notice) => http.put(`/groups/${groupId}/notice`, { notice }),
  search: (keyword) => http.get('/groups/search', { params: { keyword } }),
  join: (groupId) => http.post(`/groups/${groupId}/join`),
  setAdmin: (groupId, userId, admin) => http.put(`/groups/${groupId}/admin/${userId}`, { admin }),
  mute: (groupId, userId, mute) => http.put(`/groups/${groupId}/mute/${userId}`, { mute }),
  muteNotification: (groupId, mute) => http.put(`/groups/${groupId}/mute-notification`, { mute }),
  uploadFile: (groupId, fileId) => http.post(`/groups/${groupId}/files`, { fileId }),
  listFiles: (groupId) => http.get(`/groups/${groupId}/files`),
  updateAvatar: (groupId, avatar) => http.put(`/groups/${groupId}/avatar`, { avatar }),
  updateName: (groupId, name) => http.put(`/groups/${groupId}/name`, { name })
}

// ==================== Message ====================
export const messageApi = {
  send: (data) => http.post('/messages', data),
  singleHistory: (peerId, limit = 50, beforeId) => http.get(`/messages/single/${peerId}`, { params: beforeId ? { limit, beforeId } : { limit } }),
  groupHistory: (groupId, limit = 50, beforeId) => http.get(`/messages/group/${groupId}`, { params: beforeId ? { limit, beforeId } : { limit } }),
  recall: (messageId) => http.post(`/messages/${messageId}/recall`),
  markRead: (messageId) => http.post(`/messages/${messageId}/read`),
  search: (keyword) => http.get('/messages/search', { params: { keyword } }),
  clear: (chatType, targetId) => http.delete('/messages/clear', { params: { chatType, targetId } }),
  hide: (messageId) => http.post(`/messages/${messageId}/hide`),
  edit: (messageId, content) => http.put(`/messages/${messageId}`, { content })
}

// ==================== Conversation ====================
export const convApi = {
  list: () => http.get('/conversations'),
  markRead: (targetType, targetId, lastMessageId) =>
    http.post('/conversations/read', null, { params: { targetType, targetId, lastMessageId } }),
  delete: (convId) => http.delete(`/conversations/${convId}`),
  togglePin: (targetType, targetId) =>
    http.put('/conversations/pin', null, { params: { targetType, targetId } })
}

// ==================== File ====================
export const fileApi = {
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/files/upload', fd)
  }
}
