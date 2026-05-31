<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="modelValue" class="cd-overlay" @click.self="onCancel">
        <div class="cd-card">
          <div class="cd-body">
            <div class="cd-icon" v-html="iconType[type]"></div>
            <div class="cd-msg">{{ message }}</div>
            <input v-if="type === 'prompt'" v-model="inputVal" class="cd-inp" :placeholder="inputPlaceholder" @keydown.enter="onConfirm" autofocus />
          </div>
          <div class="cd-actions">
            <button class="cd-btn cancel" @click="onCancel">{{ cancelText }}</button>
            <button class="cd-btn confirm" :class="{ danger: type === 'danger' }" @click="onConfirm">{{ confirmText }}</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  message: { type: String, default: '' },
  type: { type: String, default: 'info' }, // info | danger | prompt
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  inputPlaceholder: { type: String, default: '' },
  inputDefault: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const inputVal = ref('')

watch(() => props.modelValue, (v) => {
  if (v) inputVal.value = props.inputDefault
})

const iconType = {
  info: '<svg viewBox="0 0 24 24" width="36" height="36" fill="#f7931e"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/></svg>',
  danger: '<svg viewBox="0 0 24 24" width="36" height="36" fill="#e74c3c"><path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>',
  prompt: '<svg viewBox="0 0 24 24" width="36" height="36" fill="#1485EE"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>'
}

function onConfirm() {
  if (props.type === 'prompt') emit('confirm', inputVal.value)
  else emit('confirm')
  emit('update:modelValue', false)
}
function onCancel() {
  emit('cancel')
  emit('update:modelValue', false)
}
</script>

<style scoped>
.cd-overlay {
  position: fixed; inset: 0; z-index: 300; background: rgba(0,0,0,0.45);
  display: flex; align-items: center; justify-content: center;
}
.cd-card {
  width: 340px; background: var(--bg-dialog, #252529);
  border-radius: 12px; box-shadow: 0 16px 48px rgba(0,0,0,0.4);
  overflow: hidden;
}
.cd-body {
  padding: 28px 24px 20px; display: flex; flex-direction: column; align-items: center; gap: 12px;
}
.cd-icon { }
.cd-msg { font-size: 15px; color: var(--text-primary, #e8e8ea); text-align: center; line-height: 1.5; }
.cd-inp {
  width: 100%; border: 1px solid var(--border, #3a3c44); border-radius: 6px;
  padding: 8px 12px; font-size: 14px; color: var(--text-primary, #e8e8ea);
  background: var(--bg-input, #2e3038); outline: none; margin-top: 4px;
}
.cd-inp:focus { border-color: var(--accent, #f7931e); }
.cd-actions { display: flex; border-top: 1px solid var(--border, #3a3c44); }
.cd-btn {
  flex: 1; padding: 12px; border: none; background: transparent; font-size: 14px;
  cursor: pointer; transition: background 0.12s; color: var(--text-secondary, #bbb);
}
.cd-btn.cancel { border-right: 1px solid var(--border, #3a3c44); }
.cd-btn:hover { background: var(--bg-hover, rgba(255,255,255,0.04)); }
.cd-btn.confirm { color: var(--accent, #f7931e); font-weight: 600; }
.cd-btn.confirm.danger { color: #e74c3c; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
