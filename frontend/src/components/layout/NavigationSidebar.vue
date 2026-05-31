<template>
  <nav class="nav-sidebar">
    <!-- 头像 -->
    <div class="nav-avatar" @click="$emit('profile')" title="个人资料">
      <Avatar :src="auth.user?.avatar" :name="auth.user?.nickname || auth.user?.username" :size="36" />
    </div>

    <!-- 标签页 -->
    <div class="nav-tabs">
      <button class="nav-btn" :class="{ active: ui.activeTab === 'chat' }" @click="ui.setActiveTab('chat')" title="聊天">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/><path d="M7 9h10v2H7zM7 12h7v2H7z"/></svg>
        <span v-if="totalUnread > 0" class="nav-badge">{{ totalUnread > 99 ? '99+' : totalUnread }}</span>
      </button>
      <button class="nav-btn" :class="{ active: ui.activeTab === 'contacts' }" @click="ui.setActiveTab('contacts')" title="通讯录">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
      </button>
    </div>

    <!-- 底部 -->
    <div class="nav-bottom">
      <button class="nav-btn" @click="ui.toggleTheme()" :title="ui.theme === 'dark' ? '白天模式' : '夜间模式'">
        <svg v-if="ui.theme === 'dark'" viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5z"/></svg>
        <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9 0-.46-.04-.92-.1-1.36c-.98 1.37-2.58 2.26-4.4 2.26-3.03 0-5.5-2.47-5.5-5.5 0-1.82.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z"/></svg>
      </button>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import Avatar from '../common/Avatar.vue'
import { useAuthStore } from '../../stores/auth'
import { useUiStore } from '../../stores/ui'
import { useConversationStore } from '../../stores/conversations'

const auth = useAuthStore()
const ui = useUiStore()
const convStore = useConversationStore()

const totalUnread = computed(() =>
  convStore.list.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
)

defineEmits(['profile'])
</script>

<style scoped>
.nav-sidebar {
  width: 56px; min-width: 56px; height: 100%;
  display: flex; flex-direction: column; align-items: center;
  padding: 12px 0 8px; gap: 2px;
  background: var(--nav-bg, #1e2028);
  border-right: 1px solid var(--border, #2e3038);
  user-select: none; z-index: 10;
}
.nav-avatar {
  margin-bottom: 16px; cursor: pointer; border-radius: 4px;
  transition: opacity 0.15s;
}
.nav-avatar:hover { opacity: 0.85; }
.nav-tabs { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.nav-bottom { display: flex; flex-direction: column; gap: 2px; }
.nav-btn {
  width: 40px; height: 40px; display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; color: var(--text-muted, #888);
  border-radius: 6px; cursor: pointer; transition: all 0.15s;
  position: relative;
}
.nav-btn:hover { background: var(--bg-hover, rgba(255,255,255,0.06)); color: var(--text-secondary, #bbb); }
.nav-btn.active { color: var(--accent, #f7931e); }
.nav-badge {
  position: absolute; top: -1px; right: -2px;
  min-width: 16px; height: 16px; line-height: 16px;
  padding: 0 4px; font-size: 10px; font-weight: 700;
  background: #e74c3c; color: #fff; border-radius: 8px;
  text-align: center;
}
.nav-badge.sm {
  min-width: 14px; height: 14px; line-height: 14px;
  font-size: 9px;
}
</style>
