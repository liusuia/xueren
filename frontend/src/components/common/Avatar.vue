<template>
  <div class="av-root" :style="rootStyle" :title="name">
    <img v-if="src" :src="src" :alt="name" class="av-img" @error="onErr" />
    <span v-else class="av-text">{{ initial }}</span>
    <span v-if="online" class="av-dot"></span>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { getAvatarColor, getInitial } from '../../utils/avatar'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '' },
  size: { type: Number, default: 40 },
  online: { type: Boolean, default: false }
})

const imgErr = ref(false)

const rootStyle = computed(() => ({
  width: props.size + 'px',
  height: props.size + 'px',
  minWidth: props.size + 'px',
  backgroundColor: (!props.src || imgErr.value) ? getAvatarColor(props.name) : 'transparent',
  fontSize: Math.max(12, props.size * 0.4) + 'px'
}))

const initial = computed(() => getInitial(props.name))

function onErr() { imgErr.value = true }
</script>

<style scoped>
.av-root {
  border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  position: relative; flex-shrink: 0; user-select: none;
  overflow: hidden;
}
.av-img {
  width: 100%; height: 100%; object-fit: cover; border-radius: 4px;
}
.av-text {
  color: #fff; font-weight: 600; line-height: 1;
}
.av-dot {
  position: absolute; bottom: -1px; right: -1px;
  width: 10px; height: 10px; border-radius: 50%;
  background: #07C160; border: 2px solid var(--bg-main, #e8e8e8);
}
</style>
