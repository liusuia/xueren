<template>
  <Teleport to="body">
    <div class="jgd-panel" :style="panelStyle" @click.stop>
      <div class="jgd-hd"><span>加入群聊</span><button class="jgd-close" @click="$emit('close')">&times;</button></div>
      <div class="jgd-search">
        <input v-model="keyword" class="jgd-inp" placeholder="输入群号搜索" @input="onInput" autofocus />
      </div>
      <div class="jgd-list" v-if="results.length">
        <div v-for="g in results" :key="g.id" class="jgd-item">
          <Avatar :src="g.avatar" :name="g.name" :size="36" />
          <div class="jgd-info">
            <div class="jgd-name">{{ g.name }}</div>
            <div class="jgd-sub">群号: {{ g.groupCode }}</div>
          </div>
          <button class="jgd-add" @click="onJoin(g.id)" :disabled="g._joined">{{ g._joined ? '已加入' : '加入' }}</button>
        </div>
      </div>
      <div v-if="searched && !results.length" class="jgd-empty">未找到该群号</div>
      <div v-if="!keyword" class="jgd-empty">输入群号搜索</div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref } from 'vue'
import Avatar from '../common/Avatar.vue'
import { useGroupStore } from '../../stores/groups'
import { useNotification } from '../../composables/useNotification'
const groupStore = useGroupStore()
const { success } = useNotification()
defineEmits(['close'])
defineProps({ panelStyle: { type: Object, default: () => ({}) } })
const keyword = ref('')
const results = ref([])
const searched = ref(false)
let t = null
function onInput() { clearTimeout(t); t = setTimeout(() => onSearch(), 300) }
async function onSearch() {
  const q = keyword.value.trim(); if (!q) { results.value = []; searched.value = false; return }
  searched.value = true
  try {
    const all = await groupStore.searchGroups(q)
    const myIds = new Set(groupStore.list.map(g => g.id))
    results.value = all.filter(g => !myIds.has(g.id))
  } catch { results.value = [] }
}
async function onJoin(gid) {
  try {
    await groupStore.joinGroup(gid)
    const g = results.value.find(x => x.id === gid)
    if (g) g._joined = true
    success('已加入群聊')
  } catch (e) {
    // BusinessException 消息在拦截器中已 toast，此处不再重复
  }
}
</script>

<style scoped>
.jgd-panel { position: fixed; width: 360px; max-height: 400px; border-radius: 10px; background: var(--bg-dialog, #252529); box-shadow: 0 8px 32px rgba(0,0,0,0.35); display: flex; flex-direction: column; overflow: hidden; z-index: 200; }
.jgd-hd { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; border-bottom: 1px solid var(--border, #3a3c44); }
.jgd-hd span { font-size: 14px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.jgd-close { background: none; border: none; color: var(--text-muted, #888); font-size: 20px; cursor: pointer; }
.jgd-search { padding: 10px 14px; }
.jgd-inp { width: 100%; border: 1px solid var(--border, #3a3c44); border-radius: 6px; padding: 8px 12px; font-size: 13px; color: var(--text-primary, #e8e8ea); background: var(--bg-input, #2e3038); outline: none; }
.jgd-inp:focus { border-color: var(--accent, #f7931e); }
.jgd-list { flex: 1; overflow-y: auto; padding: 0 14px; }
.jgd-item { display: flex; align-items: center; gap: 8px; padding: 8px 0; }
.jgd-info { flex: 1; min-width: 0; }
.jgd-name { font-size: 13px; color: var(--text-primary, #e8e8ea); }
.jgd-sub { font-size: 11px; color: var(--text-muted, #999); }
.jgd-add { padding: 4px 14px; border-radius: 4px; font-size: 11px; background: var(--accent, #f7931e); color: #fff; border: none; cursor: pointer; white-space: nowrap; }
.jgd-add:disabled { opacity: 0.4; cursor: default; }
.jgd-empty { text-align: center; padding: 24px; color: var(--text-muted, #999); font-size: 13px; }
</style>
