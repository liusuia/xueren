<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="show" class="so-overlay" @click.self="show = false; $emit('close')">
        <div class="so-panel" @click.stop>
          <!-- 搜索框 -->
          <div class="so-hd">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" class="so-hd-icon"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
            <input ref="inp" v-model="kw" class="so-inp" placeholder="搜索联系人、群组、聊天记录..." @input="onInput" @keydown="onKey" autofocus />
            <button class="so-close-btn" @click="show = false; $emit('close')">取消</button>
          </div>

          <!-- 结果区域 -->
          <div class="so-body">
            <div v-if="!kw.trim()" class="so-empty">输入关键词开始搜索</div>
            <div v-else-if="searching" class="so-empty">搜索中...</div>
            <div v-else-if="!allItems.length" class="so-empty">未找到结果</div>

            <template v-else>
              <div class="so-count">共找到 {{ allItems.length }} 条结果</div>
              <div v-for="(item, idx) in allItems" :key="item.id" class="so-item" :class="{ sel: idx === selIdx }" @click="onClick(item)" @mouseenter="selIdx = idx">
                <Avatar :src="item.avatar" :name="item.name" :size="40" />
                <div class="so-info">
                  <div class="so-name">
                    <span>{{ item.name }}</span>
                    <span class="so-type">{{ item.typeLabel }}</span>
                  </div>
                  <div class="so-detail" v-if="item.detail">{{ item.detail }}</div>
                  <div class="so-sub" v-else-if="item.sub">{{ item.sub }}</div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import Avatar from '../common/Avatar.vue'
import { useContactStore } from '../../stores/contacts'
import { useGroupStore } from '../../stores/groups'
import { useConversationStore } from '../../stores/conversations'
import { messageApi } from '../../api/endpoints'

const contactStore = useContactStore()
const groupStore = useGroupStore()
const convStore = useConversationStore()
const emit = defineEmits(['close', 'select'])

const show = ref(true)
const kw = ref('')
const selIdx = ref(0)
const searching = ref(false)
const results = ref({ contacts: [], groups: [], messages: [] })
const inp = ref(null)
let timer = null

onMounted(() => nextTick(() => inp.value?.focus()))

// 平坦结果
const allItems = computed(() => {
  const items = []
  results.value.contacts.forEach(c => items.push({ id: 'c'+c.id, name: c.nickname || c.username, avatar: c.avatar, sub: '@'+c.username, typeLabel: '联系人', conv: { targetType:1, targetId:c.id, targetName:c.nickname||c.username, targetAvatar:c.avatar } }))
  results.value.groups.forEach(g => items.push({ id: 'g'+g.id, name: g.name, avatar: g.avatar, typeLabel: '群组', conv: { targetType:2, targetId:g.id, targetName:g.name, targetAvatar:g.avatar } }))
  results.value.messages.forEach(m => items.push({
    id: 'm'+m.id,
    name: m._conv?.targetName || '',
    avatar: m._conv?.targetAvatar || '',
    detail: (m.fromUserName || '') + ': ' + (m.content || '[非文本消息]'),
    typeLabel: '聊天记录',
    conv: m._conv,
    msgId: m.id
  }))
  return items
})

function onInput() {
  clearTimeout(timer)
  const q = kw.value.trim()
  if (!q) { results.value = { contacts:[], groups:[], messages:[] }; selIdx.value = 0; return }

  timer = setTimeout(async () => {
    searching.value = true
    selIdx.value = 0

    // 联系人
    const friendMatches = contactStore.friends.filter(f => {
      const name = f.remark || f.nickname || f.username || ''
      return name.includes(q) || (f.username||'').includes(q)
    })
    results.value.contacts = friendMatches.map(f => ({ id: f.userId, username: f.username, nickname: f.nickname, avatar: f.avatar }))

    // 群组
    try { results.value.groups = await groupStore.searchGroups(q) } catch { results.value.groups = [] }

    // 聊天记录
    let msgMatches = []
    try {
      const convIds = await messageApi.search(q)
      if (convIds?.length) {
        const matchPromises = convIds.slice(0, 8).map(async cid => {
          const conv = convStore.list.find(c => c.id === cid)
          if (!conv) return []
          try {
            const msgs = conv.targetType === 1
              ? await messageApi.singleHistory(conv.targetId, 30)
              : await messageApi.groupHistory(conv.targetId, 30)
            return msgs.filter(m => !m.isRecalled && (m.content||'').includes(q))
              .map(m => ({ ...m, _conv: { targetType:conv.targetType, targetId:conv.targetId, targetName:conv.targetName, targetAvatar:conv.targetAvatar } }))
          } catch { return [] }
        })
        msgMatches = (await Promise.all(matchPromises)).flat().slice(0, 20)
      }
    } catch {}
    results.value.messages = msgMatches
    searching.value = false
  }, 300)
}

function onKey(e) {
  const total = allItems.value.length
  if (!total) return
  if (e.key === 'ArrowDown') { e.preventDefault(); selIdx.value = Math.min(selIdx.value + 1, total - 1) }
  else if (e.key === 'ArrowUp') { e.preventDefault(); selIdx.value = Math.max(selIdx.value - 1, 0) }
  else if (e.key === 'Enter') {
    e.preventDefault()
    const item = allItems.value[selIdx.value]
    if (item) onClick(item)
  }
}

function onClick(item) {
  show.value = false
  emit('close')
  emit('select', {
    ...item.conv,
    unreadCount: 0, lastMessagePreview: '', lastMessageAt: null, lastMessageId: null,
    _jumpToMsgId: item.msgId || undefined
  })
}
</script>

<style scoped>
.so-overlay { position: fixed; inset: 0; z-index: 200; background: rgba(0,0,0,0.45); display: flex; justify-content: center; padding-top: 80px; }
.so-panel {
  width: 540px; max-height: 520px; border-radius: 12px;
  background: var(--bg-dialog, #252529); box-shadow: 0 16px 48px rgba(0,0,0,0.4);
  display: flex; flex-direction: column; overflow: hidden;
}
.so-hd { display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-bottom: 1px solid var(--border, #3a3c44); }
.so-hd-icon { color: var(--text-muted, #888); flex-shrink: 0; }
.so-inp { flex: 1; border: none; outline: none; background: transparent; font-size: 15px; color: var(--text-primary, #e8e8ea); }
.so-inp::placeholder { color: var(--text-placeholder, #555); }
.so-close-btn { background: none; border: none; color: var(--text-secondary, #bbb); font-size: 13px; cursor: pointer; flex-shrink: 0; }
.so-body { flex: 1; overflow-y: auto; padding: 8px 0; }
.so-empty { text-align: center; padding: 50px 20px; color: var(--text-muted, #999); font-size: 14px; }
.so-count { font-size: 11px; color: var(--text-muted, #999); padding: 6px 18px 4px; }
.so-item {
  display: flex; align-items: center; gap: 12px; padding: 10px 18px;
  cursor: pointer; transition: background 0.1s;
}
.so-item:hover, .so-item.sel { background: var(--bg-hover, rgba(255,255,255,0.06)); }
.so-info { flex: 1; min-width: 0; }
.so-name { font-size: 14px; color: var(--text-primary, #e8e8ea); display: flex; align-items: center; gap: 8px; }
.so-type { font-size: 10px; color: var(--accent, #f7931e); background: rgba(247,147,30,0.15); padding: 1px 6px; border-radius: 3px; }
.so-detail { font-size: 12px; color: var(--text-secondary, #bbb); margin-top: 3px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.so-sub { font-size: 12px; color: var(--text-muted, #999); margin-top: 2px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
