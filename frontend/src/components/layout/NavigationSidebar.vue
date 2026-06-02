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
        <span v-if="pendingCount > 0" class="nav-badge nav-badge-friend">{{ pendingCount > 99 ? '99+' : pendingCount }}</span>
      </button>
      <button class="nav-btn" :class="{ active: ui.activeTab === 'moments' }" @click="onMomentsClick" title="朋友圈">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
        <span v-if="momentBadge" class="nav-badge nav-badge-friend">{{ momentBadge > 99 ? '99+' : momentBadge }}</span>
      </button>
      <button class="nav-btn" :class="{ active: ui.activeTab === 'favorites' }" @click="ui.setActiveTab('favorites')" title="收藏">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
      </button>
    </div>

    <!-- 底部 -->
    <div class="nav-bottom">
      <button class="nav-btn" @click="ui.toggleTheme()" :title="ui.theme === 'dark' ? '切换白天模式' : '切换夜间模式'">
        <!-- 夜间模式 → 显示月亮（点击切白天） -->
        <svg v-if="ui.theme === 'dark'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          <circle cx="17" cy="5" r="1" fill="currentColor" stroke="none"/>
          <circle cx="6" cy="18" r="1" fill="currentColor" stroke="none"/>
        </svg>
        <!-- 白天模式 → 显示太阳（点击切夜间） -->
        <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="5"/>
          <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
        </svg>
      </button>
      <button class="nav-btn" :class="{ 'nav-notify-off': !deskNotify.enabled.value }" @click="deskNotify.toggle()" title="桌面通知">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>
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
import { useContactStore } from '../../stores/contacts'
import { useGroupStore } from '../../stores/groups'
import { useDesktopNotify } from '../../composables/useDesktopNotify'
import { momentApi } from '../../api/endpoints'
import { onMounted, onUnmounted, ref } from 'vue'

const auth = useAuthStore()
const groupStore = useGroupStore()
const ui = useUiStore()
const deskNotify = useDesktopNotify()
const convStore = useConversationStore()
const contactStore = useContactStore()

const totalUnread = computed(() =>
  convStore.list.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
)
const pendingCount = computed(() => {
  let count = contactStore.requests?.length || 0
  // 加上群审批数
  const pcs = groupStore.pendingCounts || {}
  for (const k in pcs) count += pcs[k] || 0
  return count
})

defineEmits(['profile'])

// 朋友圈红点
const momentBadge = ref(0)
let momentTimer = null
async function checkMoments() {
  const since = localStorage.getItem('xr-moment-view') || ''
  try { const c = await momentApi.newCount(since ? Number(since) : undefined); momentBadge.value = Number(c) || 0 } catch {}
}
function onMomentsClick() {
  localStorage.setItem('xr-moment-view', String(Date.now()))
  momentBadge.value = 0
  ui.setActiveTab('moments')
}
onMounted(() => { checkMoments(); momentTimer = setInterval(checkMoments, 30000) })
onUnmounted(() => clearInterval(momentTimer))
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
.nav-notify-off { opacity: 0.4; }
.nav-badge {
  position: absolute; top: -1px; right: -2px;
  min-width: 16px; height: 16px; line-height: 16px;
  padding: 0 4px; font-size: 10px; font-weight: 700;
  background: #e74c3c; color: #fff; border-radius: 8px;
  text-align: center;
}
.nav-badge-friend { top: 2px; right: 4px; min-width: 14px; height: 14px; line-height: 14px; font-size: 9px; }
.nav-badge.sm {
  min-width: 14px; height: 14px; line-height: 14px;
  font-size: 9px;
}
</style>
