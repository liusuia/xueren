<template>
  <Teleport to="body">
    <Transition name="ip-fade">
      <div v-if="visible" class="ip-overlay" @wheel.prevent="onWheel">
        <!-- 工具栏 -->
        <div class="ip-toolbar">
          <span class="ip-counter" v-if="total > 1">{{ idx + 1 }} / {{ total }}</span>
          <button class="ip-btn" @click="rotate = (rotate - 90) % 360" title="旋转">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#fff"><path d="M15.55 5.55L11 1v3.07C7.06 4.56 4 7.92 4 12s3.05 7.44 7 7.93v-2.02c-2.84-.48-5-2.94-5-5.91s2.16-5.43 5-5.91V10l4.55-4.45zM19.93 11c-.34-1.79-1.28-3.32-2.6-4.31l-1.44 1.44c.92.74 1.55 1.82 1.77 3.07h2.27zM17.89 16.87l1.44 1.44c1.32-.99 2.26-2.52 2.6-4.31h-2.27c-.22 1.25-.85 2.33-1.77 3.07z"/></svg>
          </button>
          <button class="ip-btn" @click="zoomIn" title="放大">+</button>
          <button class="ip-btn" @click="zoomOut" title="缩小">−</button>
          <a class="ip-btn" :href="currentSrc" download title="下载">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="#fff"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
          </a>
          <button class="ip-close" @click="$emit('close')">
            <svg viewBox="0 0 24 24" width="24" height="24" fill="#fff"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
          </button>
        </div>
        <!-- 左右箭头 -->
        <button v-if="total > 1" class="ip-arrow ip-left" @click="prev">‹</button>
        <button v-if="total > 1" class="ip-arrow ip-right" @click="next">›</button>
        <!-- 图片 -->
        <img
          :src="currentSrc"
          :style="{ transform: `scale(${scale}) rotate(${rotate}deg)`, transition: 'transform 0.2s' }"
          class="ip-img"
          @click.stop
          draggable="false"
        />
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  visible: Boolean,
  images: { type: Array, default: () => [] },
  index: { type: Number, default: 0 }
})
defineEmits(['close'])

const idx = ref(0)
const scale = ref(1)
const rotate = ref(0)

watch(() => props.visible, (v) => {
  if (v) { idx.value = props.index; scale.value = 1; rotate.value = 0 }
})

const total = computed(() => props.images.length)
const currentSrc = computed(() => props.images[idx.value]?.content || props.images[idx.value]?.fileUrl || '')

function prev() { if (idx.value > 0) { idx.value--; scale.value = 1; rotate.value = 0 } }
function next() { if (idx.value < total.value - 1) { idx.value++; scale.value = 1; rotate.value = 0 } }
function zoomIn() { scale.value = Math.min(scale.value + 0.5, 5) }
function zoomOut() { scale.value = Math.max(scale.value - 0.5, 0.5) }
function onWheel(e) { scale.value = Math.max(0.5, Math.min(5, scale.value + (e.deltaY > 0 ? -0.3 : 0.3))) }
</script>

<style scoped>
.ip-overlay {
  position: fixed; inset: 0; z-index: 10000; background: rgba(0,0,0,0.92);
  display: flex; align-items: center; justify-content: center;
}
.ip-img { max-width: 90vw; max-height: 85vh; object-fit: contain; cursor: grab; }
.ip-toolbar { position: absolute; top: 0; left: 0; right: 0; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 16px; z-index: 10; }
.ip-counter { font-size: 13px; color: #fff; opacity: 0.8; margin-right: 12px; }
.ip-btn {
  background: rgba(255,255,255,0.15); border: none; color: #fff;
  width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  cursor: pointer; font-size: 16px; text-decoration: none; transition: background 0.15s;
}
.ip-btn:hover { background: rgba(255,255,255,0.3); }
.ip-close { background: rgba(255,255,255,0.15); border: none; width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; }
.ip-close:hover { background: rgba(255,255,255,0.3); }
.ip-arrow { position: absolute; top: 50%; transform: translateY(-50%); background: rgba(255,255,255,0.1); border: none; color: #fff; font-size: 36px; width: 48px; height: 48px; border-radius: 50%; cursor: pointer; z-index: 5; display: flex; align-items: center; justify-content: center; }
.ip-arrow:hover { background: rgba(255,255,255,0.2); }
.ip-left { left: 16px; }
.ip-right { right: 16px; }
.ip-fade-enter-active, .ip-fade-leave-active { transition: opacity 0.2s; }
.ip-fade-enter-from, .ip-fade-leave-to { opacity: 0; }
</style>
