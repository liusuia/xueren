<template>
  <span v-if="count > 0" class="badge" :style="badgeStyle">{{ display }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  count: { type: Number, default: 0 },
  max: { type: Number, default: 99 },
  size: { type: Number, default: 18 }
})

const display = computed(() => props.count > props.max ? `${props.max}+` : String(props.count))

const badgeStyle = computed(() => ({
  minWidth: props.size + 'px',
  height: props.size + 'px',
  lineHeight: props.size + 'px',
  fontSize: Math.max(10, props.size * 0.6) + 'px',
  padding: props.count > 9 ? '0 4px' : '0'
}))
</script>

<style scoped>
.badge {
  display: inline-flex; align-items: center; justify-content: center;
  background: #e74c3c; color: #fff; border-radius: 10px;
  font-weight: 600; text-align: center; flex-shrink: 0;
}
</style>
