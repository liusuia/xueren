<template>
  <div class="spinner-wrap" v-if="visible">
    <span class="spinner"></span>
  </div>
</template>

<script setup>
defineProps({ visible: { type: Boolean, default: true } })
</script>

<style scoped>
.spinner-wrap {
  display: flex; align-items: center; justify-content: center; padding: 20px;
}
.spinner {
  width: 24px; height: 24px; border-radius: 50%;
  border: 3px solid var(--border, #e0e0e0);
  border-top-color: var(--accent, #f7931e);
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
