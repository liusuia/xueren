<template>
  <div class="mpg-root">
    <!-- 头部 -->
    <div class="mpg-header">
      <button class="mpg-back" @click="$emit('back')">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="#fff"><path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/></svg>
      </button>
      <span class="mpg-hd-title">朋友圈</span>
      <button class="mpg-camera" @click="showPost=true">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="#fff"><path d="M12 15.2c1.98 0 3.6-1.62 3.6-3.6s-1.62-3.6-3.6-3.6-3.6 1.62-3.6 3.6 1.62 3.6 3.6 3.6z"/><path d="M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"/></svg>
      </button>
    </div>
    <!-- 个人区 -->
    <div class="mpg-me">
      <Avatar :src="auth.user?.avatar" :name="auth.user?.nickname || auth.user?.username" :size="60" class="mpg-me-av" />
      <div class="mpg-me-name">{{ auth.user?.nickname || auth.user?.username }}</div>
    </div>
    <!-- 时间线 -->
    <div class="mpg-body">
      <LoadingSpinner :visible="loading" />
      <div v-if="!loading && !items.length" class="mpg-empty">暂无动态</div>
      <div v-for="m in items" :key="m.id" class="mpg-card">
        <div class="mpg-card-hd">
          <Avatar :src="m.userAvatar" :name="m.userName" :size="40" />
          <div>
            <div class="mpg-card-name">{{ m.userName }}</div>
            <div class="mpg-card-time">{{ formatTime(m.createdAt) }}</div>
          </div>
        </div>
        <div class="mpg-card-body" v-if="m.content">{{ m.content }}</div>
        <div class="mpg-imgs" v-if="imgList(m).length" :class="'mpg-imgs-' + Math.min(imgList(m).length, 9)">
          <img v-for="(url, i) in imgList(m).slice(0,9)" :key="i" :src="url" class="mpg-img" @click="preview(imgList(m), i)" />
        </div>
        <div class="mpg-ft">
          <div class="mpg-ft-left">
            <span class="mpg-likes" v-if="m.likes?.length">❤️ {{ m.likes.map(l=>l.name).join(', ') }}</span>
          </div>
          <div class="mpg-ft-right">
            <span class="mpg-like-btn" :class="{ liked: m.liked }" @click="toggleLike(m)">❤️</span>
            <span class="mpg-cmt-btn" @click="m._show=!m._show">💬</span>
          </div>
        </div>
        <div class="mpg-comments" v-if="m.comments?.length">
          <div v-for="c in m.comments" :key="c.id" class="mpg-cmt"><b>{{ c.userName }}</b>: {{ c.content }}</div>
        </div>
        <div v-if="m._show" class="mpg-cmt-input">
          <input v-model="m._txt" @keydown.enter="sendCmt(m)" placeholder="评论..." class="mpg-inp" />
        </div>
      </div>
    </div>
    <!-- 发布弹窗 -->
    <Teleport to="body">
      <div v-if="showPost" class="mpg-overlay" @click.self="showPost=false">
        <div class="mpg-dlg">
          <div class="mpg-dlg-hd"><span @click="showPost=false">取消</span><b>发表文字</b><span class="mpg-dlg-send" @click="doPost">发表</span></div>
          <textarea v-model="postText" class="mpg-ta" placeholder="这一刻的想法..." rows="4" autofocus></textarea>
          <div class="mpg-dlg-imgs" v-if="postImgs.length">
            <div v-for="(url,i) in postImgs" :key="i" class="mpg-dlg-iw">
              <img :src="url" class="mpg-dlg-img" /><span class="mpg-dlg-del" @click="postImgs.splice(i,1)">×</span>
            </div>
          </div>
          <label class="mpg-dlg-add">📷 添加图片<input type="file" accept="image/*" multiple @change="onImg" style="display:none" /></label>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Avatar from '../components/common/Avatar.vue'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { momentApi } from '../api/endpoints'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
defineEmits(['back'])
const items = ref([])
const loading = ref(false)
const showPost = ref(false)
const postText = ref('')
const postImgs = ref([])

onMounted(load)
async function load() { loading.value = true; try { items.value = (await momentApi.timeline()) || [] } catch {}; loading.value = false }
function imgList(m) { try { return JSON.parse(m.images||'[]') } catch { return [] } }
function formatTime(t) { if(!t) return ''; const d=new Date(t); return (d.getMonth()+1)+'月'+d.getDate()+'日 '+String(d.getHours()).padStart(2,'0')+':'+String(d.getMinutes()).padStart(2,'0') }
async function toggleLike(m) { try { await momentApi.like(m.id); await load() } catch {} }
async function sendCmt(m) { if(!m._txt?.trim()) return; try { await momentApi.comment(m.id, m._txt); m._txt=''; await load() } catch {} }
function preview(imgs, i) { const w=window.open('','_blank'); if(w){ w.document.write(`<img src="${imgs[i]}" style="max-width:100vw;max-height:100vh">`); w.document.title=i+1+'/'+imgs.length } }
async function onImg(e) { for(const f of e.target.files){ if(postImgs.value.length>=9) break; const fd=new FormData(); fd.append('file',f); try { const r=await http.post('/files/upload',fd); const u=r?.url||r; if(u) postImgs.value.push(u) } catch{} } e.target.value='' }
async function doPost() { if(!postText.value.trim()&&!postImgs.value.length) return; try { await momentApi.create({content:postText.value,images:JSON.stringify(postImgs.value)}); showPost.value=false; postText.value=''; postImgs.value=[]; await load() } catch{} }
</script>

<style scoped>
.mpg-root { width:100%;height:100%;display:flex;flex-direction:column;background:var(--list-bg,#22252d);overflow:hidden; }
.mpg-header { display:flex;align-items:center;justify-content:space-between;padding:12px 16px;background:var(--nav-bg,#1e2028);flex-shrink:0;position:sticky;top:0;z-index:10; }
.mpg-back{ background:none;border:none;color:#fff;cursor:pointer;padding:0; }
.mpg-hd-title{ font-size:17px;font-weight:600;color:#fff; }
.mpg-camera{ background:none;border:none;color:#fff;cursor:pointer;padding:0; }
.mpg-me{ display:flex;align-items:center;gap:12px;padding:20px 16px;background:linear-gradient(135deg,#2a2d35,#1e2028);flex-shrink:0; }
.mpg-me-av{ border:2px solid rgba(255,255,255,0.3);border-radius:8px; }
.mpg-me-name{ font-size:18px;font-weight:600;color:#fff;text-shadow:0 1px 2px rgba(0,0,0,0.3); }
.mpg-body{ flex:1;overflow-y:auto; }
.mpg-empty{ text-align:center;padding:64px 0;color:var(--text-muted,#999);font-size:14px; }
.mpg-card{ padding:12px 14px;border-bottom:1px solid rgba(0,0,0,0.06);background:var(--bg-dialog,#1e2028);margin-bottom:6px; }
.mpg-card-hd{ display:flex;align-items:center;gap:10px;margin-bottom:8px; }
.mpg-card-name{ font-size:15px;font-weight:600;color:var(--accent,#f7931e); }
.mpg-card-time{ font-size:11px;color:var(--text-muted,#999); }
.mpg-card-body{ font-size:14px;color:var(--text-primary,#e8e8ea);margin-bottom:8px;line-height:1.6;white-space:pre-wrap; }
.mpg-imgs{ display:grid;gap:3px;margin-bottom:8px;max-width:280px; }
.mpg-imgs-1{ grid-template-columns:1fr;max-width:200px; }
.mpg-imgs-2{ grid-template-columns:1fr 1fr; }
.mpg-imgs-3{ grid-template-columns:1fr 1fr 1fr; }
.mpg-imgs-4{ grid-template-columns:1fr 1fr; }
.mpg-imgs-5,.mpg-imgs-6{ grid-template-columns:1fr 1fr 1fr; }
.mpg-imgs-7,.mpg-imgs-8,.mpg-imgs-9{ grid-template-columns:1fr 1fr 1fr; }
.mpg-img{ width:100%;aspect-ratio:1;object-fit:cover;border-radius:2px;cursor:pointer; }
.mpg-imgs-1 .mpg-img{ aspect-ratio:auto;max-height:260px; }
.mpg-ft{ display:flex;justify-content:space-between;align-items:center;padding-top:6px;border-top:1px solid rgba(255,255,255,0.04); }
.mpg-likes{ font-size:12px;color:var(--accent,#f7931e); }
.mpg-ft-right{ display:flex;gap:16px; }
.mpg-like-btn,.mpg-cmt-btn{ cursor:pointer;font-size:16px; }
.mpg-like-btn.liked{ color:#e74c3c; }
.mpg-comments{ background:rgba(0,0,0,0.1);border-radius:4px;padding:6px 10px;margin-top:6px; }
.mpg-cmt{ font-size:12px;color:var(--text-secondary,#bbb);padding:2px 0; }
.mpg-cmt b{ color:var(--accent,#f7931e); }
.mpg-cmt-input{ margin-top:6px; }
.mpg-inp{ width:100%;border:1px solid var(--border,#3a3c44);border-radius:4px;padding:6px 10px;font-size:13px;background:var(--bg-input,#2e3038);color:var(--text-primary,#e8e8ea);outline:none; }
.mpg-overlay{ position:fixed;inset:0;z-index:500;background:rgba(0,0,0,0.6);display:flex;align-items:flex-start;justify-content:center;padding-top:60px; }
.mpg-dlg{ width:100%;max-width:500px;background:#1e2028;border-radius:0; }
.mpg-dlg-hd{ display:flex;justify-content:space-between;padding:14px 16px;color:#fff;font-size:15px;background:#2a2d35; }
.mpg-dlg-hd span{ cursor:pointer; }
.mpg-dlg-send{ color:#07C160;font-weight:600; }
.mpg-ta{ width:100%;border:none;padding:16px;font-size:16px;background:transparent;color:#fff;outline:none;resize:none;font-family:inherit; }
.mpg-ta::placeholder{ color:#555; }
.mpg-dlg-imgs{ display:flex;gap:6px;flex-wrap:wrap;padding:0 16px 8px; }
.mpg-dlg-iw{ position:relative; }
.mpg-dlg-img{ width:72px;height:72px;object-fit:cover;border-radius:4px; }
.mpg-dlg-del{ position:absolute;top:-6px;right:-6px;width:18px;height:18px;border-radius:50%;background:#e74c3c;color:#fff;font-size:12px;display:flex;align-items:center;justify-content:center;cursor:pointer; }
.mpg-dlg-add{ display:block;padding:12px 16px;color:#aaa;cursor:pointer;font-size:14px;border-top:1px solid rgba(255,255,255,0.04); }
</style>
