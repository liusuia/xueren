<template>
  <div class="bpl-overlay" @click.self="$emit('close')">
    <div class="bpl-panel">
      <div class="bpl-hd">
        <h3>拉黑列表</h3>
        <button class="bpl-close" @click="$emit('close')">&times;</button>
      </div>
      <div class="bpl-list">
        <div v-if="!blockedUsers.length" class="bpl-empty">暂无拉黑用户</div>
        <div v-for="id in contactStore.blockedIds" :key="id" class="bpl-item">
          <span>用户 ID: {{ id }}</span>
          <button @click="contactStore.unblockFriend(id)">解除拉黑</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useContactStore } from '../../stores/contacts'

const contactStore = useContactStore()
defineEmits(['close'])
const blockedUsers = computed(() => contactStore.blockedIds)
</script>

<style scoped>
.bpl-overlay { position: fixed; inset: 0; z-index: 100; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; }
.bpl-panel { width: 380px; max-height: 400px; border-radius: 12px; background: var(--bg-dialog, #252529); box-shadow: 0 12px 40px rgba(0,0,0,0.3); overflow: hidden; display: flex; flex-direction: column; }
.bpl-hd { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid var(--border, #3a3c44); }
.bpl-hd h3 { font-size: 16px; font-weight: 600; color: var(--text-primary, #e8e8ea); }
.bpl-close { background: none; border: none; color: var(--text-muted, #888); font-size: 20px; cursor: pointer; }
.bpl-list { flex: 1; overflow-y: auto; padding: 8px 0; }
.bpl-empty { text-align: center; padding: 40px; color: var(--text-muted, #999); font-size: 14px; }
.bpl-item { display: flex; align-items: center; justify-content: space-between; padding: 10px 20px; color: var(--text-primary, #e8e8ea); font-size: 13px; }
.bpl-item button { padding: 4px 10px; border-radius: 4px; font-size: 11px; background: var(--bg-input, #2e3038); color: var(--text-secondary, #bbb); border: none; cursor: pointer; }
</style>
