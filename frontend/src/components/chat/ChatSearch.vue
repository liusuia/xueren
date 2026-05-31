<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="show" class="cs-overlay" @click.self="show = false; $emit('close')">
        <div class="cs-panel" @click.stop>
          <div class="cs-hd">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" class="cs-hd-icon"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
            <input ref="inp" v-model="kw" class="cs-inp" :placeholder="'在 ' + chatName + ' 中搜索'" @input="onInput" @keydown="onKey" autofocus />
            <button class="cs-close" @click="show = false; $emit('close')">取消</button>
          </div>

          <div class="cs-body">
            <div v-if="!kw.trim()" class="cs-empty">输入关键词搜索当前聊天记录</div>
            <div v-else-if="searching" class="cs-empty">搜索中...</div>
            <div v-else-if="!results.length" class="cs-empty">未找到匹配的消息</div>

            <template v-else>
              <div class="cs-count">找到 {{ results.length }} 条匹配消息</div>
              <div v-for="(m, idx) in results" :key="m.id" class="cs-item" :class="{ sel: idx === selIdx }" @click="onJump(m)" @mouseenter="selIdx = idx">
                <Avatar :src="m.fromUserAvatar" :name="m.fromUserName" :size="36" />
                <div class="cs-info">
                  <div class="cs-meta">
                    <span class="cs-sender">{{ m.fromUserName }}</span>
                    <span class="cs-time">{{ formatFullTime(m.createdAt) }}</span>
                  </div>
                  <div class="cs-content" v-html="highlight(m.content || '[非文本消息]')"></div>
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
import { messageApi } from '../../api/endpoints'
import { formatFullTime } from '../../utils/format'

const props = defineProps({
  chatType: { type: Number, required: true },  // 1=single, 2=group
  targetId: { type: Number, required: true },
  chatName: { type: String, default: '' }
})
const emit = defineEmits(['close', 'jump'])

const show = ref(true)
const kw = ref('')
const selIdx = ref(0)
const searching = ref(false)
const results = ref([])
const inp = ref(null)
let timer = null

onMounted(() => nextTick(() => inp.value?.focus()))

function onInput() {
  clearTimeout(timer)
  const q = kw.value.trim()
  if (!q) { results.value = []; selIdx.value = 0; return }
  timer = setTimeout(async () => {
    searching.value = true
    selIdx.value = 0
    try {
      // 拉取更多消息（200条）来做客户端搜索
      const msgs = props.chatType === 1
        ? await messageApi.singleHistory(props.targetId, 200)
        : await messageApi.groupHistory(props.targetId, 200)
      const qLower = q.toLowerCase()
      results.value = msgs.filter(m =>
        !m.isRecalled && (m.content || '').toLowerCase().includes(qLower)
      )
    } catch { results.value = [] }
    searching.value = false
  }, 300)
}

function onKey(e) {
  const total = results.value.length
  if (!total) return
  if (e.key === 'ArrowDown') { e.preventDefault(); selIdx.value = Math.min(selIdx.value + 1, total - 1) }
  else if (e.key === 'ArrowUp') { e.preventDefault(); selIdx.value = Math.max(selIdx.value - 1, 0) }
  else if (e.key === 'Enter') {
    e.preventDefault()
    const m = results.value[selIdx.value]
    if (m) onJump(m)
  }
}

function onJump(msg) {
  emit('jump', msg.id)
  show.value = false
  emit('close')
}

function highlight(text) {
  const q = kw.value.trim()
  if (!q) return text
  const escaped = q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark>$1</mark>')
}
</script>

<style scoped>
.cs-overlay { position: fixed; inset: 0; z-index: 200; background: rgba(0,0,0,0.45); display: flex; justify-content: center; padding-top: 60px; }
.cs-panel { width: 580px; max-height: 560px; border-radius: 12px; background: var(--bg-dialog, #252529); box-shadow: 0 16px 48px rgba(0,0,0,0.4); display: flex; flex-direction: column; overflow: hidden; }
.cs-hd { display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-bottom: 1px solid var(--border, #3a3c44); }
.cs-hd-icon { color: var(--text-muted, #888); flex-shrink: 0; }
.cs-inp { flex: 1; border: none; outline: none; background: transparent; font-size: 15px; color: var(--text-primary, #e8e8ea); }
.cs-inp::placeholder { color: var(--text-placeholder, #555); }
.cs-close { background: none; border: none; color: var(--text-secondary, #bbb); font-size: 13px; cursor: pointer; flex-shrink: 0; }
.cs-body { flex: 1; overflow-y: auto; padding: 8px 0; }
.cs-empty { text-align: center; padding: 60px 20px; color: var(--text-muted, #999); font-size: 14px; }
.cs-count { font-size: 11px; color: var(--text-muted, #999); padding: 6px 20px 4px; }
.cs-item { display: flex; gap: 12px; padding: 10px 20px; cursor: pointer; transition: background 0.1s; }
.cs-item:hover, .cs-item.sel { background: var(--bg-hover, rgba(255,255,255,0.06)); }
.cs-info { flex: 1; min-width: 0; }
.cs-meta { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 4px; }
.cs-sender { font-size: 13px; color: var(--text-primary, #e8e8ea); font-weight: 500; }
.cs-time { font-size: 11px; color: var(--text-muted, #888); flex-shrink: 0; }
.cs-content { font-size: 13px; color: var(--text-secondary, #bbb); line-height: 1.5; word-break: break-word; }
.cs-content :deep(mark) { background: rgba(247,147,30,0.3); color: var(--accent, #f7931e); padding: 0 2px; border-radius: 2px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
