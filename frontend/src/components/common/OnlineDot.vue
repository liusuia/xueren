<template>
  <span class="od" :class="{ online: active }" :style="{ width: s + 'px', height: s + 'px' }"></span>
</template>

<script setup>
defineProps({
  active: { type: Boolean, default: false },
  s: { type: Number, default: 8 }
})
</script>

<style scoped>
.od {
  display: inline-block; border-radius: 50%; flex-shrink: 0;
  background: #b0b0b0; transition: background 0.3s;
}
.od.online { background: #07C160; }
</style>
