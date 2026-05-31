<template>
  <div class="cti-root">
    <Avatar :src="friend.avatar" :name="displayName" :size="40" />
    <span class="cti-name">{{ displayName }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Avatar from '../common/Avatar.vue'

const props = defineProps({ friend: { type: Object, required: true } })
const displayName = computed(() => props.friend.remark || props.friend.nickname || props.friend.username || '')
</script>

<style scoped>
.cti-root {
  display: flex; align-items: center; gap: 12px; padding: 10px 16px;
  cursor: pointer; transition: background 0.12s;
}
.cti-root:hover { background: var(--bg-hover, rgba(255,255,255,0.04)); }
.cti-name { font-size: 14px; color: var(--text-primary, #e8e8ea); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
