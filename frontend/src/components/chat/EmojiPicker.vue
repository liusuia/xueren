<template>
  <div class="ep-root">
    <!-- 标签页 -->
    <div class="ep-tabs">
      <button class="ep-tab" :class="{ active: tab === 'default' }" @click="tab = 'default'">默认</button>
      <button class="ep-tab" :class="{ active: tab === 'custom' }" @click="tab = 'custom'; loadCustom()">自定义</button>
    </div>
    <!-- 默认表情 -->
    <div v-if="tab === 'default'" class="ep-grid">
      <button v-for="e in emojis" :key="e" class="ep-item" @click="$emit('select', e)" :title="e">{{ e }}</button>
    </div>
    <!-- 自定义表情 -->
    <div v-else class="ep-custom">
      <div class="ep-custom-grid">
        <div v-for="e in customEmojis" :key="e.id" class="ep-c-item" @click="$emit('selectEmoji', e)" @contextmenu.prevent="deleteEmoji(e)" :title="e.name + ' (右键删除)'">
          <img :src="e.url" :alt="e.name" class="ep-c-img" />
        </div>
        <label class="ep-c-add" title="上传表情">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>
          <input type="file" accept="image/*" @change="onUpload" style="display:none" />
        </label>
      </div>
      <div v-if="customEmojis.length === 0 && !customLoading" class="ep-empty">点击 + 上传自定义表情</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import http from '../../api/http'
import { useConfirm } from '../../composables/useConfirm'

defineEmits(['select', 'selectEmoji'])
const cfm = useConfirm()
const tab = ref('default')
const customEmojis = ref([])
const customLoading = ref(false)

async function loadCustom() {
  if (customEmojis.value.length || customLoading.value) return
  customLoading.value = true
  try {
    const res = await http.get('/emoji')
    customEmojis.value = Array.isArray(res) ? res : (res?.data || [])
  } catch {}
  finally { customLoading.value = false }
}

async function deleteEmoji(e) {
  if (!await cfm.info('删除表情 "' + e.name + '"？')) return
  try { await http.delete('/emoji/' + e.id); customEmojis.value = customEmojis.value.filter(x => x.id !== e.id) } catch {}
}

async function onUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  try {
    const form = new FormData()
    form.append('file', file)
    form.append('name', file.name.replace(/\.[^.]+$/, ''))
    const res = await http.post('/emoji', form)
    const emoji = res?.data || res
    if (emoji) customEmojis.value.unshift(emoji)
  } catch (e) { console.error('表情上传失败', e) }
  e.target.value = ''
}

const emojis = [
  '😀','😃','😄','😁','😅','😂','🤣','😊',
  '😇','🙂','😉','😌','😍','🥰','😘','😗',
  '😋','😛','😜','🤪','😝','🤑','🤗','🤭',
  '🤔','🤐','😐','😑','😶','😏','😒','🙄',
  '😬','😮','😯','😲','😳','🥺','😢','😭',
  '😤','😡','🤬','😈','👿','💀','☠️','💩',
  '👍','👎','👏','🙌','💪','🤝','👋','✌️',
  '❤️','🧡','💛','💚','💙','💜','🖤','🤍',
  '🔥','⭐','🌟','✨','💯','🎉','🎊','🌸',
  '🐶','🐱','🐼','🐨','🦊','🐰','🐸','🐵',
  '🍎','🍊','🍋','🍉','🍇','🍓','🍒','🍑',
  '☕','🍵','🍺','🍻','🎂','🍰','🍕','🍔',
  '⚽','🏀','🎮','🎵','🎶','📷','💻','📱'
]
</script>

<style scoped>
.ep-root {
  border-top: 1px solid var(--border, #2e3038);
  padding: 0; max-height: 280px; overflow-y: auto;
}
.ep-tabs {
  display: flex; gap: 0; padding: 6px 12px 0; border-bottom: 1px solid var(--border, #2e3038);
}
.ep-tab {
  padding: 6px 14px; font-size: 12px; border: none; background: transparent;
  color: var(--text-muted, #888); cursor: pointer; border-bottom: 2px solid transparent;
  transition: all 0.15s;
}
.ep-tab.active { color: var(--accent, #f7931e); border-bottom-color: var(--accent, #f7931e); }
.ep-grid {
  display: grid; grid-template-columns: repeat(12, 1fr); gap: 4px;
  padding: 8px 12px;
}
.ep-item {
  width: 100%; aspect-ratio: 1; font-size: 18px;
  border: none; background: transparent; cursor: pointer;
  border-radius: 4px; display: flex; align-items: center; justify-content: center;
  transition: background 0.12s;
}
.ep-item:hover { background: var(--bg-hover, rgba(255,255,255,0.08)); }
.ep-custom { padding: 8px 12px; }
.ep-custom-grid {
  display: flex; flex-wrap: wrap; gap: 8px;
}
.ep-c-item {
  width: 68px; cursor: pointer; border-radius: 6px; overflow: hidden;
  border: 1px solid var(--border, #3a3c44); transition: border-color 0.12s;
  background: var(--bg-input, #2e3038);
}
.ep-c-item:hover { border-color: var(--accent, #f7931e); }
.ep-c-img { width: 68px; height: 68px; object-fit: contain; display: block; }
.ep-c-add {
  width: 68px; height: 68px; cursor: pointer; border-radius: 6px;
  border: 2px dashed var(--border, #3a3c44); display: flex; align-items: center;
  justify-content: center; color: var(--text-muted, #888); transition: all 0.12s;
  background: transparent;
}
.ep-c-add:hover { border-color: var(--accent, #f7931e); color: var(--accent, #f7931e); }
.ep-empty { text-align: center; color: var(--text-muted, #888); font-size: 12px; padding: 20px 0; }
</style>
