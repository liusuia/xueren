<template>
  <div class="lb-root" :class="{ self: isSelf }">
    <div class="lb-map">
      <img v-if="data.map" :src="data.map" class="lb-map-img" @error="imgErr=true" alt="位置" :style="{display:imgErr?'none':''}" />
      <div v-if="!data.map || imgErr" class="lb-map-fb">📍</div>
      <div v-if="data.map && !imgErr" class="lb-pin-dot"></div>
    </div>
    <div class="lb-info">
      <div class="lb-title">位置</div>
      <div class="lb-coords">{{ data.lat }}, {{ data.lng }}</div>
    </div>
    <a class="lb-link" :href="data.link" target="_blank" @click.stop>查看地图 →</a>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
const props = defineProps({ msg: Object, isSelf: Boolean })
const imgErr = ref(false)
const data = computed(() => {
  try { return JSON.parse(props.msg.content || '{}') } catch { return {} }
})
</script>

<style scoped>
.lb-root {
  width: 240px; border-radius: 10px; overflow: hidden;
  background: var(--bubble-other, #fff); color: #333;
  cursor: pointer;
}
.lb-root.self { background: #95eb6b; }
.lb-map { position: relative; height: 140px; overflow: hidden; background: linear-gradient(135deg, #a8d8ea, #c9f0d9); display: flex; align-items: center; justify-content: center; }
.lb-map-img { width: 100%; height: 100%; object-fit: cover; }
.lb-map-fb { font-size: 48px; opacity: 0.6; }
.lb-pin-dot { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 12px; height: 12px; border-radius: 50%; background: #e74c3c; border: 2px solid #fff; box-shadow: 0 2px 6px rgba(0,0,0,0.3); }
.lb-info { padding: 10px 12px 4px; }
.lb-title { font-size: 14px; font-weight: 600; }
.lb-coords { font-size: 11px; color: #888; margin-top: 2px; }
.lb-link { display: block; padding: 6px 12px 10px; font-size: 12px; color: #576b95; text-decoration: none; border-top: 1px solid rgba(0,0,0,0.06); }
.lb-link:hover { text-decoration: underline; }
</style>
