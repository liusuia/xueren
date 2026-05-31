<template>
  <div class="empty">
    <div class="empty-icon" v-html="icon"></div>
    <div class="empty-title">{{ title }}</div>
    <div v-if="desc" class="empty-desc">{{ desc }}</div>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '' },
  title: { type: String, default: '暂无数据' },
  desc: { type: String, default: '' }
})
</script>

<style scoped>
.empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 100%; min-height: 200px; padding: 40px 20px;
  color: var(--text-muted, #999); user-select: none;
}
.empty-icon { opacity: 0.3; margin-bottom: 16px; }
.empty-title { font-size: 14px; margin-bottom: 4px; }
.empty-desc { font-size: 12px; opacity: 0.7; }
</style>
