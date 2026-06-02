<template>
  <div class="rpb-root" :class="{ opened: detail.opened, self: isSelf }" @click="onClick">
    <div class="rpb-icon">🧧</div>
    <div class="rpb-info">
      <div class="rpb-title">{{ detail.message || '恭喜发财，大吉大利' }}</div>
      <div class="rpb-sub">{{ detail.opened ? (myAmount ? '已领取 ¥' + (myAmount/100).toFixed(2) : '已被领完') : '领取红包' }}</div>
    </div>
    <!-- 拆红包弹窗 -->
    <Teleport to="body">
      <div v-if="showOpen" class="rpo-overlay" @click.self="showOpen=false">
        <div class="rpo-card">
          <div class="rpo-top">🧧</div>
          <div class="rpo-msg">{{ detail.message }}</div>
          <div v-if="!detail.opened && !opened" class="rpo-btn" @click="doOpen">开</div>
          <div v-else class="rpo-result">
            <div class="rpo-amount">¥{{ (myAmount/100).toFixed(2) }}</div>
            <div v-if="detail.receives" class="rpo-list">
              <div v-for="r in detail.receives" :key="r.userId" class="rpo-item">{{ r.userId }}: ¥{{ (r.amount/100).toFixed(2) }}</div>
            </div>
          </div>
          <button class="rpo-close" @click="showOpen=false">关闭</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../../api/http'

const props = defineProps({ msg: Object, isSelf: Boolean })
const detail = ref({})
const showOpen = ref(false)
const opened = ref(false)
const myAmount = ref(null)

onMounted(async () => {
  try { detail.value = await http.get('/red-packet/' + props.msg.content) || {} } catch {}
})

async function onClick() {
  showOpen.value = true
  if (!detail.value.opened && !props.isSelf) {
    try {
      const res = await http.post('/red-packet/' + props.msg.content + '/open')
      myAmount.value = res?.amount
      detail.value.opened = true
      opened.value = true
    } catch {}
  } else {
    myAmount.value = detail.value.receives?.find(r => r.userId === props.msg.fromUserId)?.amount
  }
}
</script>

<style scoped>
.rpb-root { display: flex; align-items: center; gap: 10px; padding: 12px 14px; border-radius: 8px; background: #f85b3a; color: #fff; cursor: pointer; max-width: 260px; }
.rpb-root.self { background: #f85b3a; }
.rpb-root.opened { opacity: 0.6; }
.rpb-icon { font-size: 36px; flex-shrink: 0; }
.rpb-title { font-size: 14px; font-weight: 600; }
.rpb-sub { font-size: 11px; opacity: 0.8; margin-top: 2px; }
.rpo-overlay { position: fixed; inset: 0; z-index: 1000; background: rgba(0,0,0,0.6); display: flex; align-items: center; justify-content: center; }
.rpo-card { width: 280px; text-align: center; background: #fff; border-radius: 12px; padding: 32px 24px; }
.rpo-top { font-size: 64px; }
.rpo-msg { font-size: 16px; color: #333; margin: 12px 0; }
.rpo-btn { width: 60px; height: 60px; margin: 16px auto; border-radius: 50%; background: radial-gradient(circle, #f7d34e, #e8a82c); color: #fff; font-size: 24px; font-weight: 700; display: flex; align-items: center; justify-content: center; cursor: pointer; border: 4px solid #f9e177; }
.rpo-btn:hover { transform: scale(1.05); }
.rpo-amount { font-size: 28px; color: #e74c3c; font-weight: 700; margin: 8px 0; }
.rpo-list { font-size: 12px; color: #888; text-align: left; max-height: 120px; overflow-y: auto; margin: 8px 0; }
.rpo-item { padding: 3px 0; }
.rpo-close { margin-top: 12px; background: #eee; border: none; padding: 8px 32px; border-radius: 4px; cursor: pointer; font-size: 13px; }
</style>
