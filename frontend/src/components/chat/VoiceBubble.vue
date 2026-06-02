<template>
  <div class="vb-root" :class="{ self: isSelf }" @click="togglePlay">
    <svg v-if="!playing" viewBox="0 0 24 24" width="18" height="18" fill="currentColor" class="vb-icon"><path d="M8 5v14l11-7z"/></svg>
    <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="currentColor" class="vb-icon"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
    <div class="vb-body">
      <div class="vb-progress" v-if="playing">
        <div class="vb-bar-fill" :style="{ width: progressPct + '%' }"></div>
      </div>
      <div class="vb-wave" v-else :style="{ width: waveWidth + 'px' }">
        <span v-for="i in 5" :key="i" class="vb-bar" :style="{ animationDelay: (i * 0.15) + 's', height: (8 + i * 3) + 'px' }"></span>
      </div>
      <span class="vb-dur">{{ playing ? currentText : durText }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'

const props = defineProps({ msg: Object, isSelf: Boolean })
const playing = ref(false)
const currentTime = ref(0)
let audio = null
let progressTimer = null

const dur = computed(() => {
  if (props.msg._voiceDuration) return props.msg._voiceDuration
  const parts = (props.msg.content || '').split('|')
  return parseInt(parts[1]) || 0
})
const audioUrl = computed(() => (props.msg.content || '').split('|')[0])
const durText = computed(() => {
  const d = dur.value
  if (!d) return ''
  return d + "''"
})
const currentText = computed(() => {
  const t = Math.floor(currentTime.value)
  return t + "''"
})
const progressPct = computed(() => dur.value > 0 ? (currentTime.value / dur.value) * 100 : 0)
const waveWidth = computed(() => Math.min(30 + dur.value * 2, 120))

onUnmounted(() => { clearInterval(progressTimer); if (audio) { audio.pause(); audio = null } })

function togglePlay() {
  if (playing.value) { stop(); return }
  const url = audioUrl.value || props.msg.fileUrl
  if (!url) return
  audio = new Audio(url)
  audio.ontimeupdate = () => { currentTime.value = audio.currentTime }
  audio.onended = () => { stop() }
  audio.onerror = () => { stop() }
  audio.play().catch(() => { stop() })
  playing.value = true
  currentTime.value = 0
}
function stop() {
  if (audio) { audio.pause(); audio = null }
  playing.value = false
  currentTime.value = 0
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
.vb-body { display: flex; flex-direction: column; gap: 3px; flex: 1; min-width: 0; }
.vb-dur { font-size: 11px; min-width: 24px; }
.vb-wave { display: flex; align-items: center; gap: 2px; }
.vb-bar { width: 3px; border-radius: 2px; background: currentColor; opacity: 0.5; }
.vb-bar.playing { animation: vb-wave 0.8s ease-in-out infinite alternate; opacity: 1; }
@keyframes vb-wave { 0% { opacity: 0.3; } 100% { opacity: 1; } }
.vb-progress { height: 3px; background: rgba(0,0,0,0.1); border-radius: 2px; overflow: hidden; }
.vb-bar-fill { height: 100%; background: currentColor; opacity: 0.6; border-radius: 2px; transition: width 0.2s; }
</style>
