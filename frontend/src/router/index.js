import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { isMobile } from '../utils/device'
import LoginView from '../views/LoginView.vue'
import ChatView from '../views/ChatView.vue'
import MobileLoginView from '../views/MobileLoginView.vue'
import MobileChatView from '../views/MobileChatView.vue'

const mobile = isMobile()

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: mobile ? MobileLoginView : LoginView },
    { path: '/', component: mobile ? MobileChatView : ChatView, meta: { requiresAuth: true } }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) {
    return '/login'
  }
  if (to.path === '/login' && auth.token) {
    return '/'
  }
})

export default router
