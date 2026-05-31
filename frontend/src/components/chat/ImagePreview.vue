<template>
  <Teleport to="body">
    <Transition name="ip-fade">
      <div v-if="visible" class="ip-overlay" @click="$emit('close')">
        <img :src="src" class="ip-img" @click.stop />
        <button class="ip-close" @click="$emit('close')">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="#fff"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
        </button>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
defineProps({ visible: Boolean, src: String })
defineEmits(['close'])
</script>

<style scoped>
.ip-overlay {
  position: fixed; inset: 0; z-index: 10000; background: rgba(0,0,0,0.9);
  display: flex; align-items: center; justify-content: center;
}
.ip-img { max-width: 90vw; max-height: 90vh; object-fit: contain; border-radius: 4px; }
.ip-close { position: absolute; top: 20px; right: 20px; background: rgba(255,255,255,0.1); border: none; width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: background 0.15s; }
.ip-close:hover { background: rgba(255,255,255,0.2); }
.ip-fade-enter-active, .ip-fade-leave-active { transition: opacity 0.2s; }
.ip-fade-enter-from, .ip-fade-leave-to { opacity: 0; }
</style>
