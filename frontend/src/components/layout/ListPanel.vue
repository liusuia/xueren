<template>
  <div class="list-panel" :style="{ width: ui.column2Width + 'px' }">
    <!-- 搜索栏 -->
    <div class="list-search">
      <svg class="search-icon" viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
      <input v-model="searchQ" class="search-input" placeholder="搜索" @input="onSearch" @keydown="onSearchKey" />
      <button v-if="searchQ" class="search-clear" @click="searchQ='';clearResults()">&times;</button>
      <!-- + 按钮 -->
      <div class="plus-wrap">
        <button class="plus-btn" @click="showPlusMenu = !showPlusMenu" title="添加">+</button>
        <Transition name="plus-fade">
          <div v-if="showPlusMenu" class="plus-menu">
            <div class="plus-item" @click="showPlusMenu=false;showAddFriend=true">添加好友</div>
            <div class="plus-item" @click="showPlusMenu=false;showCreateGroup=true">创建群聊</div>
          </div>
        </Transition>
      </div>
      <!-- 点击外部关闭菜单 -->
      <div v-if="showPlusMenu" class="plus-backdrop" @click="showPlusMenu=false"></div>
    </div>

    <!-- 搜索结果 -->
    <div v-if="searchQ" class="search-results">
      <div v-if="!allResults.length && searched" class="sr-empty">未找到结果</div>
      <div v-for="(item, idx) in allResults" :key="item.id" class="sr-item" :class="{ sel: idx===searchIdx }" @click="selectResult(item)" @mouseenter="searchIdx=idx">
        <Avatar :src="item.avatar" :name="item.name" :size="36" />
        <div class="sr-info">
          <div class="sr-name">
            <span>{{ item.name }}</span>
            <span v-if="item.type==='message'" class="sr-conv"> — {{ item.sub }}</span>
            <span class="sr-tag">{{ item.type==='contact'?'联系人':item.type==='group'?'群组':'聊天记录' }}</span>
          </div>
          <div v-if="item.sub && item.type!=='message'" class="sr-sub">{{ item.sub }}</div>
          <div v-if="item.detail" class="sr-detail">{{ trunc(item.detail, 50) }}</div>
        </div>
      </div>
    </div>

    <!-- 正常视图 -->
    <template v-else>
      <ConversationList v-if="ui.activeTab==='chat'" @select="$emit('selectConv',$event)" />
      <ContactList v-else @select="$emit('selectConv',$event)" @addFriend="showAddFriend=true" @friendRequests="showFriendRequests=true" @createGroup="showCreateGroup=true" @joinGroup="showJoinGroup=true" @showFriendInfo="$emit('showFriendInfo',$event)" @showGroupInfo="$emit('showGroupInfo',$event)" @showBlockedList="showBlockedList=true" @showFavorites="showFavorites=true" />
    </template>

    <div v-if="showFriendRequests||showCreateGroup||showAddFriend||showJoinGroup" class="lp-overlay" @click="closeAll"></div>
    <FriendRequestsPanel v-if="showFriendRequests" @close="closeAll" :panelStyle="popStyle" />
    <JoinGroupDialog v-if="showJoinGroup" @close="closeAll" :panelStyle="popStyle" />
    <BlockedListPanel v-if="showBlockedList" @close="showBlockedList=false" />
    <FavoriteList v-if="showFavorites" @close="showFavorites=false" />
    <CreateGroupDialog v-if="showCreateGroup" @close="closeAll" :panelStyle="popStyle" />
    <AddFriendDialog v-if="showAddFriend" @close="closeAll" :panelStyle="popStyle" />
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useUiStore } from '../../stores/ui'
import { useContactStore } from '../../stores/contacts'
import { useGroupStore } from '../../stores/groups'
import { useConversationStore } from '../../stores/conversations'
import { messageApi } from '../../api/endpoints'
import ConversationList from '../conversations/ConversationList.vue'
import ContactList from '../contacts/ContactList.vue'
import BlockedListPanel from '../contacts/BlockedListPanel.vue'
import JoinGroupDialog from '../contacts/JoinGroupDialog.vue'
import FavoriteList from '../contacts/FavoriteList.vue'
import FriendRequestsPanel from '../contacts/FriendRequestsPanel.vue'
import CreateGroupDialog from '../groups/CreateGroupDialog.vue'
import AddFriendDialog from '../contacts/AddFriendDialog.vue'
import Avatar from '../common/Avatar.vue'

const ui = useUiStore()
const contactStore = useContactStore()
const groupStore = useGroupStore()
const convStore = useConversationStore()
const emit = defineEmits(['selectConv','showFriendInfo','showGroupInfo'])

const searchQ = ref('')
const searched = ref(false)
const searchIdx = ref(-1)
const results = reactive({ contacts:[], groups:[], messages:[] })
let timer = null

const showAddFriend = ref(false)
const showFriendRequests = ref(false)
const showBlockedList = ref(false)
const showJoinGroup = ref(false)
const showFavorites = ref(false)
const showCreateGroup = ref(false)
const showPlusMenu = ref(false)
const popStyle = computed(() => ({ left:(ui.column2Width+64)+'px', top:'60px' }))
function closeAll() { showAddFriend.value=false; showFriendRequests.value=false; showCreateGroup.value=false; showJoinGroup.value=false }

const allResults = computed(() => {
  const items = []
  results.contacts.forEach(c => items.push({
    id:'c'+c.id, type:'contact', name:c.nickname||c.username, avatar:c.avatar,
    sub:'@'+c.username,
    conv:{ targetType:1, targetId:c.id, targetName:c.nickname||c.username, targetAvatar:c.avatar, unreadCount:0 }
  }))
  results.groups.forEach(g => items.push({
    id:'g'+g.id, type:'group', name:g.name, avatar:g.avatar,
    conv:{ targetType:2, targetId:g.id, targetName:g.name, targetAvatar:g.avatar, unreadCount:0 }
  }))
  results.messages.forEach(m => items.push({
    id:'m'+m.id, type:'message', name:m.fromUserName||'', avatar:m.fromUserAvatar||'',
    sub:m._conv?.targetName||'', detail:m.content||'',
    conv:m._conv, msgId:m.id
  }))
  return items
})

function clearResults() { Object.assign(results, { contacts:[], groups:[], messages:[] }); searched.value=false }

function onSearch() {
  clearTimeout(timer); searchIdx.value=-1
  const q = searchQ.value.trim()
  if (!q) { clearResults(); return }
  timer = setTimeout(async () => {
    searched.value = true
    const ql = q.toLowerCase()
    // 联系人
    results.contacts = contactStore.friends
      .filter(f => (f.remark||f.nickname||f.username||'').toLowerCase().includes(ql))
      .map(f => ({ id:f.userId, username:f.username, nickname:f.nickname, avatar:f.avatar }))
    // 群组（仅已加入的）
    try {
      const myIds = new Set(groupStore.list.map(g => g.id))
      results.groups = ((await groupStore.searchGroups(q)) || []).filter(g => myIds.has(g.id))
    } catch { results.groups = [] }
    // 聊天记录：服务端全文搜索
    let msgMatches = []
    try {
      const msgs = await messageApi.searchContent(q) || []
      msgMatches = msgs.map(m => ({
        ...m,
        _conv: { targetType:m.chatType, targetId:m.chatType===1 ? m.fromUserId : m.groupId, targetName:'', targetAvatar:'' }
      }))
    } catch {}
    results.messages = msgMatches
  }, 250)
}

function onSearchKey(e) {
  const t = allResults.value.length; if (!t) return
  if (e.key==='ArrowDown') { e.preventDefault(); searchIdx.value=Math.min(searchIdx.value+1, t-1) }
  else if (e.key==='ArrowUp') { e.preventDefault(); searchIdx.value=Math.max(searchIdx.value-1, 0) }
  else if (e.key==='Enter' && searchIdx.value>=0) { e.preventDefault(); const item=allResults.value[searchIdx.value]; if (item) selectResult(item) }
}

function selectResult(item) {
  searchQ.value=''; clearResults()
  if (item.type==='message') {
    emit('selectConv', { ...item.conv, unreadCount:0, lastMessagePreview:'', lastMessageAt:null, lastMessageId:null, _jumpToMsgId:item.msgId })
  } else {
    emit('selectConv', { ...item.conv, lastMessagePreview:'', lastMessageAt:null, lastMessageId:null })
  }
}

function trunc(t,n) { return t&&t.length>n ? t.slice(0,n)+'...' : t }
</script>

<style scoped>
.list-panel { height:100%; display:flex; flex-direction:column; background:var(--list-bg,#22252d); border-right:1px solid var(--border,#2e3038); overflow:hidden; min-width:200px; }
.list-search { display:flex; align-items:center; padding:10px 14px; gap:8px; flex-shrink:0; background:var(--bg-input,#2e3038); border-radius:4px; margin:8px; }
.search-icon { color:var(--text-muted,#888); flex-shrink:0; }
.search-input { flex:1; border:none; outline:none; background:transparent; font-size:13px; color:var(--text-primary,#e8e8ea); padding:2px 0; }
.search-input::placeholder { color:var(--text-placeholder,#555); }
.search-clear { background:none; border:none; color:var(--text-muted,#888); cursor:pointer; font-size:16px; }
.plus-wrap { position:relative; flex-shrink:0; }
.plus-btn {
  width:28px; height:28px; border:none; background:var(--bg-hover,rgba(255,255,255,0.06));
  color:var(--text-secondary,#bbb); font-size:20px; font-weight:300; line-height:1;
  border-radius:6px; cursor:pointer; display:flex; align-items:center; justify-content:center;
  transition:all 0.15s;
}
.plus-btn:hover { background:var(--accent,#f7931e); color:#fff; }
.plus-menu {
  position:absolute; top:100%; right:0; margin-top:6px;
  min-width:120px; background:var(--bg-dialog,#252529);
  border:1px solid var(--border,#3a3c44); border-radius:8px;
  padding:4px 0; box-shadow:0 6px 20px rgba(0,0,0,0.3); z-index:200;
}
.plus-item {
  padding:10px 16px; font-size:13px; color:var(--text-primary,#e8e8ea);
  cursor:pointer; white-space:nowrap; transition:background 0.12s;
}
.plus-item:hover { background:var(--bg-hover,rgba(255,255,255,0.06)); }
.plus-backdrop { position:fixed; inset:0; z-index:199; }
.plus-fade-enter-active,.plus-fade-leave-active { transition:opacity 0.15s,transform 0.15s; }
.plus-fade-enter-from,.plus-fade-leave-to { opacity:0; transform:translateY(-4px); }
.search-results { flex:1; overflow-y:auto; padding:4px 0; }
.sr-empty { text-align:center; padding:30px; color:var(--text-muted,#999); font-size:13px; }
.sr-item { display:flex; align-items:center; gap:10px; padding:8px 14px; cursor:pointer; transition:background .1s; }
.sr-item:hover,.sr-item.sel { background:var(--bg-hover,rgba(255,255,255,.06)); }
.sr-info { flex:1; min-width:0; }
.sr-name { font-size:13px; color:var(--text-primary,#e8e8ea); display:flex; align-items:center; gap:4px; flex-wrap:wrap; }
.sr-conv { color:var(--text-muted,#999); font-size:12px; }
.sr-tag { font-size:9px; color:var(--accent,#f7931e); background:rgba(247,147,30,.15); padding:1px 5px; border-radius:3px; flex-shrink:0; }
.sr-sub { font-size:12px; color:var(--text-muted,#999); margin-top:2px; }
.sr-detail { font-size:12px; color:var(--text-secondary,#bbb); margin-top:2px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.lp-overlay { position:fixed; inset:0; z-index:99; }
</style>
