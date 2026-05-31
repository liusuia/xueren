<template>
  <Teleport to="body">
    <Transition name="cm-fade">
      <div v-if="visible" class="cm-backdrop" @click="onClose" @contextmenu.prevent="onClose">
        <div class="cm-menu" :style="menuStyle" @click.stop>
          <template v-for="(item, idx) in items" :key="idx">
            <div v-if="item.divider" class="cm-divider"></div>
            <div v-else class="cm-item" :class="{ danger: item.danger, disabled: item.disabled }" @click="onClick(item)">
              <span v-if="item.icon" class="cm-item-icon" v-html="item.icon"></span>
              <span class="cm-item-label">{{ item.label }}</span>
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  visible: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
  position: { type: Object, default: () => ({ x: 0, y: 0 }) }
})
const emit = defineEmits(['close', 'action'])

import { computed } from 'vue'
const menuStyle = computed(() => ({ left: props.position.x + 'px', top: props.position.y + 'px' }))

function onClick(item) {
  if (item.disabled) return
  emit('action', item)
}

function onClose() { emit('close') }
</script>

<style scoped>
.cm-backdrop {
  position: fixed; inset: 0; z-index: 9999;
}
.cm-menu {
  position: fixed; min-width: 140px; max-width: 200px;
  background: var(--bg-dialog, #252529);
  border: 1px solid var(--border, #3a3c44);
  border-radius: 8px; padding: 4px 0;
  box-shadow: 0 6px 24px rgba(0,0,0,0.3);
  z-index: 10000;
}
.cm-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 14px; font-size: 13px;
  color: var(--text-primary, #e8e8ea); cursor: pointer;
  transition: background 0.12s; white-space: nowrap;
}
.cm-item:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); }
.cm-item.danger { color: #e74c3c; }
.cm-item.danger:hover { background: rgba(231,76,60,0.1); }
.cm-item.disabled { opacity: 0.4; pointer-events: none; }
.cm-item-icon { width: 16px; text-align: center; flex-shrink: 0; }
.cm-divider { height: 1px; background: var(--border, #3a3c44); margin: 4px 8px; }
.cm-fade-enter-active, .cm-fade-leave-active { transition: opacity 0.15s; }
.cm-fade-enter-from, .cm-fade-leave-to { opacity: 0; }
</style>
