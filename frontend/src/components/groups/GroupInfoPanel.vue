<template>
  <Teleport to="body">
    <Transition name="slide">
      <div class="gip-root">
        <div class="gip-backdrop" @click="$emit('close')"></div>
        <div class="gip-panel">
          <div class="gip-hd">
            <button class="gip-back-btn" @click="$emit('close')">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>
            </button>
            <span class="gip-hd-title">群聊信息</span>
          </div>
          <div class="gip-body" v-if="groupStore.currentGroup">
            <div class="gip-top">
              <div class="gip-avatar-wrap" @click="triggerGroupAvatar">
                <Avatar :src="groupStore.currentGroup.avatar" :name="groupStore.currentGroup.name" :size="56" />
                <div v-if="canEditAvatar" class="gip-avatar-cam">📷</div>
              </div>
              <input type="file" ref="groupAvInput" accept="image/*" @change="onGroupAvatarChange" style="display:none" />
              <div v-if="!editingName" class="gip-name" :class="{ editable: canEditName }" @click="startEditName">{{ groupStore.currentGroup.name }}</div>
              <input v-else ref="nameInputRef" class="gip-inp gip-name-input" :value="editNameVal" @blur="saveName" @keydown.enter="$event.target.blur()" @keydown.escape="editingName=false" />
              <div class="gip-code" v-if="groupStore.currentGroup.groupCode">
                群号: {{ groupStore.currentGroup.groupCode }}
                <button class="gip-copy-btn" @click="copyGroupCode">复制</button>
              </div>
            </div>

            <!-- 群公告 -->
            <div class="gip-card">
              <div class="gip-card-hd">
                <span>群公告</span>
                <button v-if="groupStore.isOwner(groupStore.currentGroup.id) && !editingNotice" class="gip-link" @click="startEditNotice">编辑</button>
                <button v-if="groupStore.isOwner(groupStore.currentGroup.id) && editingNotice" class="gip-link" style="color:#07C160" @click="saveNotice">保存</button>
                <button v-if="editingNotice" class="gip-link" style="color:#e74c3c" @click="editingNotice=false">取消</button>
              </div>
              <div v-if="!editingNotice" class="gip-card-body" v-html="renderNotice(groupStore.currentGroup.notice) || '<span style=color:var(--text-muted)>暂无公告</span>'"></div>
              <textarea v-else ref="noticeInputRef" v-model="noticeText" class="gip-textarea" placeholder="输入群公告（支持换行和链接）" rows="4"></textarea>
            </div>

            <!-- 入群方式（仅群主可见） -->
            <div v-if="groupStore.isOwner(groupStore.currentGroup.id)" class="gip-card">
              <div class="gip-card-hd"><span>入群方式</span></div>
              <select v-model.number="joinMode" class="gip-select" @change="onJoinModeChange">
                <option :value="0">自由加入</option>
                <option :value="1">需群主审批</option>
                <option :value="2">禁止加入</option>
              </select>
            </div>

            <!-- 入群申请（仅群主可见） -->
            <div v-if="groupStore.isOwner(groupStore.currentGroup.id) && pendingRequests.length" class="gip-card">
              <div class="gip-card-hd"><span>入群申请 ({{ pendingRequests.length }})</span></div>
              <div v-for="r in pendingRequests" :key="r.id" class="gip-req">
                <Avatar :src="r.avatar" :name="r.nickname || r.username" :size="32" />
                <span class="gip-req-name">{{ r.nickname || r.username }}</span>
                <button class="gip-req-accept" @click="onApproveRequest(r.id, true)">通过</button>
                <button class="gip-req-reject" @click="onApproveRequest(r.id, false)">拒绝</button>
              </div>
            </div>

            <!-- 刷新审批按钮 -->
            <div v-if="groupStore.isOwner(groupStore.currentGroup.id)" class="gip-card" style="text-align:center">
              <button class="gip-link" @click="loadRequests">刷新入群申请</button>
            </div>

            <!-- 群备注（个人可见） -->
            <div class="gip-card">
              <div class="gip-card-hd"><span>群备注</span></div>
              <input class="gip-inp" :value="myRemark" placeholder="设置个人备注" @blur="onSaveRemark" @keydown.enter="$event.target.blur()" />
            </div>
            <!-- 我的群昵称（群内可见） -->
            <div class="gip-card">
              <div class="gip-card-hd"><span>我的群昵称</span></div>
              <input class="gip-inp" :value="myNickname" placeholder="设置我在本群的昵称" @blur="onSaveNickname" @keydown.enter="$event.target.blur()" />
            </div>

            <!-- 群成员 + 邀请 -->
            <div class="gip-card">
              <div class="gip-card-hd"><span>群成员 ({{ groupStore.currentGroupMembers.length }})</span></div>
              <div class="gip-members">
                <div v-for="m in groupStore.currentGroupMembers" :key="m.userId" class="gip-mem" @click="onMemberClick(m)" @contextmenu.prevent="onMemberCtx($event, m)">
                  <Avatar :src="m.userAvatar || m.avatar" :name="m.nickname || m.userName || m.username" :size="40" />
                  <span class="gip-mem-name">{{ m.nickname || m.userName || m.username }}</span>
                  <span v-if="m.role === 1" class="gip-role owner">群主</span>
                  <span v-else-if="m.role === 2" class="gip-role admin">管理</span>
                </div>
                <!-- 邀请按钮：微信风格 + 号 -->
                <div class="gip-mem gip-invite-btn" @click="showInvite = !showInvite">
                  <div class="gip-inv-icon">+</div>
                  <span class="gip-mem-name">邀请</span>
                </div>
              </div>
              <div v-if="showInvite" class="gip-invite">
                <div class="gip-inv-hd">选择好友邀请入群</div>
                <div class="gip-inv-list">
                  <div v-for="f in contactStore.friends" :key="f.userId" class="gip-inv-item" :class="{ sel: inviteList.includes(f.userId) }" @click="toggleInvite(f.userId)">
                    <Avatar :src="f.avatar" :name="f.remark || f.nickname || f.username" :size="32" />
                    <span>{{ f.remark || f.nickname || f.username }}</span>
                    <svg v-if="inviteList.includes(f.userId)" viewBox="0 0 24 24" width="18" height="18" fill="#07C160"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                  </div>
                </div>
                <button v-if="inviteList.length" class="gip-btn primary" @click="doInvite">邀请 {{ inviteList.length }} 人</button>
              </div>
            </div>

            <!-- 操作列表 -->
            <div class="gip-card">
              <div class="gip-ops">
                <button class="gip-op" @click="toggleMute">
                  <span>消息免打扰</span>
                  <span class="gip-toggle" :class="{ on: myMuted }"></span>
                </button>
                <button class="gip-op" @click="onClearHistory"><span>清空聊天记录</span><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" opacity="0.3"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6z"/></svg></button>
                <button class="gip-op" @click="onSearchChat"><span>查找聊天记录</span><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" opacity="0.3"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6z"/></svg></button>
                <button class="gip-op" @click="onTopConv">
                  <span>置顶聊天</span>
                  <span class="gip-toggle" :class="{ on: isTopped }"></span>
                </button>
              </div>
            </div>

            <div class="gip-footer">
              <button v-if="!groupStore.isOwner(groupStore.currentGroup.id)" class="gip-btn danger" @click="onQuit">退出群聊</button>
              <button v-if="groupStore.isOwner(groupStore.currentGroup.id)" class="gip-btn danger" @click="onDismiss">解散群聊</button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <ContextMenu :visible="ctxVisible" :items="ctxItems" :position="ctxPos" @close="ctxVisible = false" @action="onCtxAction" />
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import Avatar from '../common/Avatar.vue'
import ContextMenu from '../common/ContextMenu.vue'
import { useGroupStore } from '../../stores/groups'
import { useContactStore } from '../../stores/contacts'
import { useAuthStore } from '../../stores/auth'
import { useConversationStore } from '../../stores/conversations'
import { useNotification } from '../../composables/useNotification'
import { useConfirm } from '../../composables/useConfirm'
import { groupApi } from '../../api/endpoints'
import http from '../../api/http'

const groupStore = useGroupStore()
const contactStore = useContactStore()
const auth = useAuthStore()
const convStore = useConversationStore()
const { success, error } = useNotification()
const cfm = useConfirm()
const emit = defineEmits(['close', 'searchChat', 'clearChat', 'showUserInfo', 'chatWith'])

const ctxVisible = ref(false)
const ctxPos = ref({ x: 0, y: 0 })
const ctxItems = ref([])
let ctxMember = null
const showInvite = ref(false)
const inviteList = ref([])
const joinMode = ref(0)
watch(() => groupStore.currentGroup?.joinMode, (v) => { joinMode.value = v ?? 0 }, { immediate: true })
async function onJoinModeChange() {
  try { await groupApi.setJoinMode(groupStore.currentGroup.id, joinMode.value) } catch {}
}
const pendingRequests = ref([])
async function loadRequests() {
  if (!groupStore.isOwner(groupStore.currentGroup?.id)) return
  try { pendingRequests.value = await http.get('/groups/' + groupStore.currentGroup.id + '/requests') } catch {}
}
async function onApproveRequest(reqId, approve) {
  try {
    await http.put('/groups/' + groupStore.currentGroup.id + '/requests/' + reqId, { approve })
    loadRequests()
    // 立即更新本地计数
    const gid = groupStore.currentGroup.id
    const cur = groupStore.pendingCounts[gid] || 0
    if (cur > 0) groupStore.pendingCounts = { ...groupStore.pendingCounts, [gid]: cur - 1 }
  } catch {}
}
let reqTimer = null
watch(() => groupStore.currentGroup?.id, (id) => {
  if (id) { clearTimeout(reqTimer); reqTimer = setTimeout(loadRequests, 300) }
}, { immediate: true })
onUnmounted(() => clearTimeout(reqTimer))

// 清洗旧版 JSON 脏数据：{"remark":"xxx"} → xxx
function clean(v) {
  if (!v) return ''
  const s = String(v)
  // 匹配 {"remark":"..."} 或 {"nickname":"..."}
  try { const p = JSON.parse(s); if (p && typeof p === 'object') return p.remark || p.nickname || p.notice || s } catch {}
  return s
}
function cleanNotice(v) {
  if (!v) return ''
  const s = String(v)
  try { const p = JSON.parse(s); if (p && typeof p === 'object') return p.notice || s } catch {}
  return s
}
function renderNotice(v) {
  const text = cleanNotice(v)
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/(https?:\/\/[^\s]+)/g, '<a href="$1" target="_blank" rel="noopener">$1</a>')
    .replace(/\n/g, '<br>')
}

const editingNotice = ref(false)
const noticeText = ref('')
const noticeInputRef = ref(null)
const groupAvInput = ref(null)
function copyGroupCode() {
  navigator.clipboard.writeText(groupStore.currentGroup.groupCode).then(() => success('已复制')).catch(() => {})
}
function triggerGroupAvatar() { if (canEditAvatar.value) groupAvInput.value?.click(); else error('仅群主可修改') }
async function onGroupAvatarChange(e) {
  const file = e.target.files[0]; if (!file) return
  try {
    const form = new FormData(); form.append('file', file)
    const res = await http.post('/files/upload', form)
    const fileVO = res?.data || res
    if (fileVO) { await groupStore.updateAvatar(groupStore.currentGroup.id, fileVO.url); groupStore.currentGroup.avatar = fileVO.url }
  } catch {}
  e.target.value = ''
}
function startEditNotice() {
  noticeText.value = cleanNotice(groupStore.currentGroup?.notice) || ''
  editingNotice.value = true
  nextTick(() => noticeInputRef.value?.focus())
}
async function saveNotice() {
  try {
    await groupStore.updateNotice(groupStore.currentGroup.id, noticeText.value.trim())
    groupStore.currentGroup.notice = noticeText.value.trim()
    editingNotice.value = false
  } catch (e) { error(e.message || '修改失败') }
}

const me = computed(() => groupStore.currentGroupMembers.find(m => m.userId === auth.user?.id))
const myNickname = computed(() => clean(me.value?.nickname || ''))
const myRemark = computed(() => {
  const v = me.value?.remark || groupStore.list.find(x => x.id === groupStore.currentGroup?.id)?.remark
  return clean(v)
})
const myMuted = computed(() => !!(me.value?.isNotificationMuted))

// 群名称编辑
const editingName = ref(false)
const editNameVal = ref('')
const nameInputRef = ref(null)
const canEditName = computed(() => {
  return groupStore.isOwner(groupStore.currentGroup?.id) || groupStore.isAdmin(groupStore.currentGroup?.id)
})
const canEditAvatar = computed(() => groupStore.isOwner(groupStore.currentGroup?.id))
function startEditName() {
  if (!canEditName.value) return
  editNameVal.value = groupStore.currentGroup.name
  editingName.value = true
  nextTick(() => nameInputRef.value?.focus())
}
async function saveName(e) {
  const val = e.target.value.trim()
  editingName.value = false
  if (val && val !== groupStore.currentGroup.name) {
    try {
      await groupStore.updateName(groupStore.currentGroup.id, val)
    } catch (e) { error(e.message || '修改失败') }
  }
}

async function onSaveNickname(e) {
  const val = e.target.value.trim()
  if (val !== myNickname.value) {
    await groupStore.updateGroupNickname(groupStore.currentGroup.id, val)
  }
}
async function onSaveRemark(e) {
  const val = e.target.value.trim()
  if (val !== myRemark.value) {
    await groupStore.updateGroupRemark(groupStore.currentGroup.id, val)
  }
}
async function toggleMute() {
  try {
    const gid = groupStore.currentGroup.id
    const newMuted = !myMuted.value
    // 1. 立即更新本地 UI
    if (me.value) me.value.isNotificationMuted = newMuted ? 1 : 0
    // 2. 更新会话列表免打扰标志
    convStore.setMuted(2, gid, newMuted)
    // 3. 异步同步到后端
    await groupStore.toggleMuteNotification(gid, newMuted)
  } catch (e) {
    // 回滚本地状态
    if (me.value) me.value.isNotificationMuted = myMuted.value ? 1 : 0
    convStore.setMuted(2, groupStore.currentGroup.id, myMuted.value)
    error(e.message || '操作失败')
  }
}
async function onClearHistory() {
  const ok = await cfm.info('确定清空聊天记录？清空后将无法恢复。')
  if (!ok) return
  const conv = convStore.list.find(c => c.targetType === 2 && c.targetId === groupStore.currentGroup.id)
  if (conv) { conv.lastMessagePreview = ''; conv.unreadCount = 0; conv.lastMessageAt = null }
  emit('clearChat', { targetType: 2, targetId: groupStore.currentGroup.id })
}
function onSearchChat() {
  emit('close')
  emit('searchChat')
}
const isTopped = computed(() => convStore.isPinned(2, groupStore.currentGroup?.id))
function onTopConv() {
  convStore.togglePinned(2, groupStore.currentGroup.id)
  convStore.fetchConversations() // 刷新排序
}
function onMemberClick(m) {
  if (m.userId !== auth.user?.id) {
    emit('showUserInfo', m.userId)
  }
}
function toggleInvite(uid) {
  const idx = inviteList.value.indexOf(uid)
  if (idx >= 0) inviteList.value.splice(idx, 1); else inviteList.value.push(uid)
}
async function doInvite() {
  try { await groupStore.addMembers(groupStore.currentGroup.id, inviteList.value); success('邀请成功'); inviteList.value = []; showInvite.value = false } catch (e) { error(e.message) }
}
function onMemberCtx(e, member) {
  ctxMember = member; const items = []
  // 基础操作：所有人可见
  if (member.userId !== auth.user?.id) {
    items.push({ label: '发消息', action: 'chatWith' })
    items.push({ label: '查看资料', action: 'viewProfile' })
  }
  // 群主专有
  if (groupStore.currentGroup.ownerId === auth.user?.id) {
    items.push({ label: member.role === 2 ? '取消管理员' : '设为管理员', action: 'toggleAdmin' })
    items.push({ label: '转让群主', action: 'transferOwner' })
  }
  // 管理员专有（操作普通成员）
  if (groupStore.isAdmin(groupStore.currentGroup.id) && member.role === 3) {
    const muted = !!(member.isMuted)
    items.push({ label: muted ? '解除禁言' : '禁言', action: 'toggleMute' })
    items.push({ label: '移出群聊', action: 'remove', danger: true })
  }
  if (!items.length) return
  ctxItems.value = items; ctxPos.value = { x: e.clientX, y: e.clientY }; ctxVisible.value = true
}
async function onCtxAction(item) {
  ctxVisible.value = false; if (!ctxMember) return; const gid = groupStore.currentGroup.id
  if (item.action === 'toggleAdmin') { await groupStore.setAdmin(gid, ctxMember.userId, ctxMember.role !== 2); success('已更新') }
  else if (item.action === 'transferOwner') { await groupStore.transferOwner(gid, ctxMember.userId); success('已转让'); emit('close') }
  else if (item.action === 'toggleMute') { await groupStore.muteMember(gid, ctxMember.userId, !ctxMember.isMuted); success('已更新') }
  else if (item.action === 'remove') { await groupStore.removeMember(gid, ctxMember.userId); success('已移出') }
  else if (item.action === 'viewProfile') { emit('showUserInfo', ctxMember.userId) }
  else if (item.action === 'chatWith') { emit('chatWith', ctxMember.userId) }
}
async function onQuit() { if (await cfm.danger('确定退出群聊？', { confirmText: '退出' })) { await groupStore.quitGroup(groupStore.currentGroup.id); emit('close') } }
async function onDismiss() { if (await cfm.danger('确定解散群聊？此操作不可撤销。', { confirmText: '解散' })) { await groupStore.dismissGroup(groupStore.currentGroup.id); emit('close') } }
</script>

<style scoped>
.gip-root { position: fixed; inset: 0; z-index: 150; display: flex; }
.gip-backdrop { position: absolute; inset: 0; background: rgba(0,0,0,0.4); }
.gip-panel {
  position: relative; z-index: 1; margin-left: auto;
  width: 340px; height: 100%; background: var(--bg-dialog, #1e2028);
  box-shadow: -4px 0 24px rgba(0,0,0,0.3); display: flex; flex-direction: column;
}
.gip-hd { display: flex; align-items: center; gap: 8px; padding: 14px 16px; border-bottom: 1px solid var(--border, #2e3038); flex-shrink: 0; }
.gip-back-btn { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; border: none; background: transparent; color: var(--text-secondary, #bbb); cursor: pointer; border-radius: 4px; }
.gip-back-btn:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); }
.gip-hd-title { font-size: 15px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.gip-body { flex: 1; overflow-y: auto; padding: 16px; }

.gip-top { display: flex; flex-direction: column; align-items: center; margin-bottom: 16px; }
.gip-name { font-size: 17px; font-weight: 600; color: var(--text-primary, #e8e8ea); margin-top: 8px; }
.gip-name.editable { cursor: pointer; border-bottom: 1px dashed transparent; }
.gip-name.editable:hover { border-bottom-color: var(--accent, #f7931e); }
.gip-code { font-size: 12px; color: var(--text-muted, #999); margin-top: 4px; user-select: all; display: flex; align-items: center; gap: 6px; }
.gip-copy-btn { background: none; border: 1px solid var(--border, #3a3c44); color: var(--text-muted, #888); font-size: 10px; padding: 1px 6px; border-radius: 3px; cursor: pointer; }
.gip-copy-btn:hover { color: var(--accent, #f7931e); border-color: var(--accent, #f7931e); }
.gip-avatar-wrap { position: relative; cursor: pointer; }
.gip-avatar-wrap:hover { opacity: 0.85; }
.gip-avatar-cam { position: absolute; bottom: 0; right: 0; width: 22px; height: 22px; border-radius: 50%; background: var(--bg-input, #3a3c44); font-size: 12px; display: flex; align-items: center; justify-content: center; }
.gip-name-input { font-size: 17px; font-weight: 600; text-align: center; margin-top: 8px; }

.gip-card { background: var(--bg-input, #22252d); border-radius: 8px; padding: 12px 14px; margin-bottom: 12px; }
.gip-card-hd { display: flex; justify-content: space-between; align-items: center; font-size: 12px; font-weight: 600; color: var(--text-muted, #999); margin-bottom: 8px; }
.gip-card-body { font-size: 13px; color: var(--text-secondary, #bbb); line-height: 1.5; }
.gip-select { width: 100%; padding: 6px 10px; border: 1px solid var(--border, #3a3c44); border-radius: 4px; background: var(--bg-input, #2e3038); color: var(--text-primary, #e8e8ea); font-size: 13px; outline: none; }
.gip-req { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid rgba(255,255,255,0.04); }
.gip-req:last-child { border-bottom: none; }
.gip-req-name { flex: 1; font-size: 13px; color: var(--text-primary, #e8e8ea); }
.gip-req-accept { padding: 3px 10px; border: none; border-radius: 3px; background: #07C160; color: #fff; font-size: 11px; cursor: pointer; }
.gip-req-reject { padding: 3px 10px; border: none; border-radius: 3px; background: #e74c3c; color: #fff; font-size: 11px; cursor: pointer; }
.gip-link { background: none; border: none; color: var(--accent, #f7931e); font-size: 12px; cursor: pointer; }

.gip-inp { width: 100%; border: none; border-radius: 4px; padding: 6px 10px; font-size: 13px; color: var(--text-primary, #e8e8ea); background: transparent; outline: none; }
.gip-textarea { width: 100%; border: 1px solid var(--border, #3a3c44); border-radius: 6px; padding: 8px 10px; font-size: 13px; color: var(--text-primary, #e8e8ea); background: var(--bg-input, #2e3038); outline: none; resize: vertical; font-family: inherit; line-height: 1.5; }
.gip-textarea:focus { border-color: var(--accent, #f7931e); }
.gip-card-body :deep(a) { color: var(--accent, #f7931e); text-decoration: underline; }

.gip-members { display: flex; flex-wrap: wrap; gap: 6px; }
.gip-mem { display: flex; flex-direction: column; align-items: center; gap: 2px; cursor: pointer; padding: 4px; border-radius: 6px; transition: background 0.12s; width: 56px; }
.gip-mem:hover { background: var(--bg-hover, rgba(255,255,255,0.04)); }
.gip-mem-name { font-size: 10px; color: var(--text-secondary, #bbb); max-width: 52px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; text-align: center; }
.gip-role { font-size: 8px; padding: 0px 3px; border-radius: 2px; }
.gip-role.owner { background: #f7931e; color: #fff; }
.gip-role.admin { background: #1485EE; color: #fff; }
.gip-invite-btn { }
.gip-inv-icon {
  width: 40px; height: 40px; border-radius: 4px; border: 1.5px dashed var(--border, #3a3c44);
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; color: var(--text-muted, #888); font-weight: 300;
  transition: border-color 0.15s;
}
.gip-invite-btn:hover .gip-inv-icon { border-color: var(--accent, #f7931e); color: var(--accent, #f7931e); }

.gip-invite { margin-top: 10px; padding: 8px; border-radius: 6px; background: var(--bg-dialog, #1a1d23); }
.gip-inv-hd { font-size: 12px; color: var(--text-muted, #999); margin-bottom: 6px; }
.gip-inv-list { max-height: 180px; overflow-y: auto; display: flex; flex-direction: column; gap: 2px; }
.gip-inv-item { display: flex; align-items: center; gap: 10px; padding: 6px 8px; cursor: pointer; border-radius: 4px; font-size: 13px; color: var(--text-primary, #e8e8ea); }
.gip-inv-item:hover { background: var(--bg-hover, rgba(255,255,255,0.04)); }
.gip-inv-item.sel { background: rgba(247,147,30,0.1); }
.gip-inv-item svg { margin-left: auto; flex-shrink: 0; }

.gip-ops { display: flex; flex-direction: column; }
.gip-op {
  display: flex; justify-content: space-between; align-items: center;
  width: 100%; padding: 10px 0; border: none; background: transparent;
  color: var(--text-primary, #e8e8ea); font-size: 13px; cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.04); text-align: left;
}
.gip-op:last-child { border-bottom: none; }
.gip-op:hover { opacity: 0.8; }
.gip-toggle {
  width: 40px; height: 22px; border-radius: 11px; background: #555;
  position: relative; transition: background .2s; flex-shrink: 0;
}
.gip-toggle::after {
  content: ''; position: absolute; top: 2px; left: 2px;
  width: 18px; height: 18px; border-radius: 50%; background: #fff;
  transition: transform .2s;
}
.gip-toggle.on { background: var(--accent, #f7931e); }
.gip-toggle.on::after { transform: translateX(18px); }

.gip-footer { margin-top: 12px; padding-bottom: 20px; }
.gip-btn { width: 100%; padding: 10px; border: none; border-radius: 6px; font-size: 13px; cursor: pointer; font-weight: 500; }
.gip-btn.primary { background: var(--accent, #f7931e); color: #fff; }
.gip-btn.danger { background: transparent; color: #e74c3c; }
.gip-btn.danger:hover { background: rgba(231,76,60,0.08); }

.slide-enter-active, .slide-leave-active { transition: all 0.25s ease; }
.slide-enter-from .gip-panel { transform: translateX(100%); }
.slide-enter-to .gip-panel { transform: translateX(0); }
.slide-leave-to .gip-panel { transform: translateX(100%); }
.slide-enter-from .gip-backdrop, .slide-leave-to .gip-backdrop { opacity: 0; }
.slide-enter-to .gip-backdrop, .slide-leave-from .gip-backdrop { opacity: 1; }
</style>
