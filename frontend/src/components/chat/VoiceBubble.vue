<template>
  <div class="vb-root" :class="{ self: isSelf }" @click="togglePlay">
    <svg v-if="!playing" viewBox="0 0 24 24" width="18" height="18" fill="currentColor" class="vb-icon"><path d="M8 5v14l11-7z"/></svg>
    <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="currentColor" class="vb-icon"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
    <span class="vb-dur">{{ durText }}</span>
    <div class="vb-wave" :style="{ width: waveWidth + 'px' }">
      <span v-for="i in 5" :key="i" class="vb-bar" :class="{ playing: playing }" :style="{ animationDelay: (i * 0.15) + 's', height: (8 + i * 3) + 'px' }"></span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({ msg: Object, isSelf: Boolean })
const playing = ref(false)
let audio = null

const dur = computed(() => {
  if (props.msg._voiceDuration) return props.msg._voiceDuration
  const parts = (props.msg.content || '').split('|')
  return parseInt(parts[1]) || 0
})
const audioUrl = computed(() => (props.msg.content || '').split('|')[0])
const durText = computed(() => {
  const d = dur.value
  if (!d) return ''
  if (d < 60) return d + "''"
  return Math.floor(d / 60) + "'" + (d % 60) + "''"
})
const waveWidth = computed(() => Math.min(30 + dur.value * 2, 120))

function togglePlay() {
  if (playing.value) { stop(); return }
  const url = audioUrl.value || props.msg.fileUrl
  if (!url) return
  audio = new Audio(url)
  audio.onended = () => { playing.value = false; audio = null }
  audio.onerror = () => { playing.value = false; audio = null }
  audio.play().catch(() => { playing.value = false })
  playing.value = true
}
function stop() {
  if (audio) { audio.pause(); audio = null }
  playing.value = false
}
</script>

<style scoped>
.vb-root {
  display: flex; align-items: center; gap: 6px; cursor: pointer;
  padding: 8px 12px; border-radius: 8px; min-width: 80px;
  background: var(--bubble-other, #fff); color: #333;
  user-select: none;
}
.vb-root.self { background: #95eb6b; }
.vb-icon { flex-shrink: 0; }
.vb-dur { font-size: 12px; min-width: 24px; }
.vb-wave { display: flex; align-items: center; gap: 2px; }
.vb-bar {
  width: 3px; border-radius: 2px; background: currentColor; opacity: 0.5;
}
.vb-bar.playing {
  animation: vb-wave 0.8s ease-in-out infinite alternate; opacity: 1;
}
@keyframes vb-wave { 0% { opacity: 0.3; } 100% { opacity: 1; } }
</style>
