<template>
  <div class="mi-root" :class="{ self: isSelf, system: msg.msgType === 5 || msg.isRecalled }">
    <!-- 系统消息居中 -->
    <div v-if="msg.msgType === 5 || msg.isRecalled" class="mi-system">
      <span class="mi-system-text">{{ systemText }}</span>
    </div>

    <template v-else>
      <!-- 左侧：对方头像(群聊显示) 或 占位 -->
      <Avatar v-if="!isSelf && isGroup" :src="msg.fromUserAvatar" :name="senderName" :size="32" class="mi-av clickable" @click.stop="$emit('userClick', msg.fromUserId)" />
      <div v-else-if="!isSelf && !isGroup" class="mi-spacer"></div>

      <!-- 消息体 -->
      <div class="mi-body" :class="{ right: isSelf }">
        <div v-if="!isSelf && isGroup" class="mi-sender">{{ senderName }}</div>
        <div class="mi-row" :class="{ right: isSelf }">
          <span v-if="msg._failed" class="mi-fail" title="发送失败">!</span>
          <MessageBubble :msg="msg" :isSelf="isSelf" />
        </div>
      </div>

      <!-- 右侧：自己的头像 -->
      <Avatar v-if="isSelf" :src="auth.user?.avatar" :name="auth.user?.nickname || auth.user?.username" :size="32" class="mi-av" />
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Avatar from '../common/Avatar.vue'
import MessageBubble from './MessageBubble.vue'
import { useAuthStore } from '../../stores/auth'
import { useContactStore } from '../../stores/contacts'
import { MSG_TYPE } from '../../utils/constants'

const auth = useAuthStore()
const contactStore = useContactStore()
const props = defineProps({
  msg: { type: Object, required: true },
  isGroup: { type: Boolean, default: false }
})

defineEmits(['userClick'])
const isSelf = computed(() => props.msg.fromUserId === auth.user?.id)
const systemText = computed(() => {
  if (props.msg.isRecalled) return '消息已撤回'
  if (props.msg.msgType === MSG_TYPE.SYSTEM) return props.msg.content || ''
  return props.msg.content || ''
})

// 群聊中显示名优先级：好友备注 > 好友自己昵称 > 消息原始名
const senderName = computed(() => {
  if (!props.msg.fromUserId || isSelf.value) return props.msg.fromUserName
  const friend = contactStore.friends.find(f => f.userId === props.msg.fromUserId)
  if (!friend) return props.msg.fromUserName
  return friend.remark || friend.nickname || props.msg.fromUserName
})
</script>

<style scoped>
.mi-root {
  display: flex; align-items: flex-start; gap: 8px; margin-bottom: 12px;
}
/* 自己发的消息：靠右对齐 */
.mi-root.self {
  justify-content: flex-end;
}
.mi-root.system { justify-content: center; }
.mi-system {
  text-align: center; padding: 4px 0;
}
.mi-system-text {
  font-size: 11px; color: var(--text-muted, #999);
  background: var(--bg-date, rgba(255,255,255,0.05)); padding: 4px 12px;
  border-radius: 4px; font-style: italic;
}
.mi-av { flex-shrink: 0; }
.mi-av.clickable { cursor: pointer; }
.mi-av.clickable:hover { opacity: 0.8; }
.mi-spacer { width: 32px; flex-shrink: 0; } /* 单聊非自己消息的左边占位 */
.mi-body { max-width: 60%; min-width: 0; }
.mi-body.right { display: flex; flex-direction: column; align-items: flex-end; }
.mi-sender { font-size: 11px; color: var(--text-muted, #999); margin-bottom: 2px; }
.mi-row { display: flex; align-items: center; gap: 6px; }
.mi-row.right { flex-direction: row-reverse; }
.mi-fail {
  width: 18px; height: 18px; border-radius: 50%; background: #e74c3c;
  color: #fff; font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
</style>
