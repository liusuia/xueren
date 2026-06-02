<template>
  <Teleport to="body">
    <Transition name="mp-slide">
      <div v-if="visible" class="mpg-overlay" @click.self="$emit('close')">
        <div class="mpg-panel">
          <!-- 顶部导航 -->
          <div class="mpg-nav">
            <button class="mpg-nav-back" @click="$emit('close')">←</button>
            <span class="mpg-nav-title">朋友圈</span>
            <button class="mpg-nav-cam" @click="showPost=true">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="#fff"><path d="M12 15.2c1.98 0 3.6-1.62 3.6-3.6s-1.62-3.6-3.6-3.6-3.6 1.62-3.6 3.6 1.62 3.6 3.6 3.6z"/><path d="M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"/></svg>
            </button>
          </div>
          <!-- 个人区 -->
          <div class="mpg-me">
            <Avatar :src="auth.user?.avatar" :name="auth.user?.nickname || auth.user?.username" :size="56" class="mpg-me-av" />
            <div class="mpg-me-right">
              <div class="mpg-me-name">{{ auth.user?.nickname || auth.user?.username }}</div>
              <div class="mpg-me-sub">轻语号: {{ auth.user?.username }}</div>
            </div>
          </div>
          <!-- 时间线 -->
          <div class="mpg-body">
            <LoadingSpinner :visible="loading" />
            <div v-if="!loading && !items.length" class="mpg-empty">— 暂无动态 —</div>
            <div v-for="m in items" :key="m.id" class="mpg-card">
              <div class="mpg-card-hd">
                <Avatar :src="m.userAvatar" :name="m.userName" :size="36" />
                <div class="mpg-card-meta">
                  <div class="mpg-card-name">{{ m.userName }}</div>
                  <div class="mpg-card-time">{{ formatTime(m.createdAt) }}</div>
                </div>
              </div>
              <div class="mpg-card-body" v-if="m.content">{{ m.content }}</div>
              <div class="mpg-imgs" v-if="imgList(m).length" :class="'mpg-imgs-' + Math.min(imgList(m).length, 9)">
                <img v-for="(url, i) in imgList(m).slice(0,9)" :key="i" :src="url" class="mpg-img" @click="preview(imgList(m), i)" />
              </div>
              <div class="mpg-bar">
                <span class="mpg-bar-time">{{ formatTime(m.createdAt) }}</span>
                <span class="mpg-bar-btns">
                  <span class="mpg-lb" :class="{ on: m.liked }" @click="toggleLike(m)">❤️<span v-if="m.likes?.length">{{ m.likes.length }}</span></span>
                  <span class="mpg-cb" @click="m._cmt=!m._cmt">💬<span v-if="m.comments?.length">{{ m.comments.length }}</span></span>
                </span>
              </div>
              <div v-if="m.likes?.length" class="mpg-likes">{{ m.likes.map(l=>l.name).join(', ') }}</div>
              <div class="mpg-comments" v-if="m.comments?.length">
                <div v-for="c in m.comments" :key="c.id" class="mpg-cmt"><b>{{ c.userName }}</b>: {{ c.content }}</div>
              </div>
              <div v-if="m._cmt" class="mpg-cmt-input">
                <input v-model="m._txt" @keydown.enter="sendCmt(m)" placeholder="评论" class="mpg-inp" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
  <!-- 发布弹窗 -->
  <Teleport to="body">
    <div v-if="showPost" class="mpg-post-overlay">
      <div class="mpg-post-dlg">
        <div class="mpg-post-hd"><span @click="showPost=false">取消</span><b>发表文字</b><span class="mpg-post-send" @click="doPost">发表</span></div>
        <textarea v-model="postText" class="mpg-post-ta" placeholder="这一刻的想法..." rows="4"></textarea>
        <div class="mpg-post-imgs" v-if="postImgs.length">
          <div v-for="(url,i) in postImgs" :key="i" class="mpg-post-iw"><img :src="url" class="mpg-post-img" /><span class="mpg-post-del" @click="postImgs.splice(i,1)">×</span></div>
        </div>
        <label class="mpg-post-add">📷 从相册选择<input type="file" accept="image/*" multiple @change="onImg" style="display:none" /></label>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Avatar from '../components/common/Avatar.vue'
import LoadingSpinner from '../components/common/LoadingSpinner.vue'
import { momentApi } from '../api/endpoints'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

defineProps({ visible: Boolean })
defineEmits(['close'])
const auth = useAuthStore()
const items = ref([])
const loading = ref(false)
const showPost = ref(false)
const postText = ref('')
const postImgs = ref([])

onMounted(load)
async function load() { loading.value=true; try{items.value=(await momentApi.timeline())||[]}catch{}; loading.value=false }
function imgList(m){ try{return JSON.parse(m.images||'[]')}catch{return[]} }
function formatTime(t){ if(!t)return''; const d=new Date(t); const now=new Date(); const diff=now-d; if(diff<3600000) return Math.floor(diff/60000)+'分钟前'; if(diff<86400000) return String(d.getHours()).padStart(2,'0')+':'+String(d.getMinutes()).padStart(2,'0'); return (d.getMonth()+1)+'月'+d.getDate()+'日' }
async function toggleLike(m){ try{await momentApi.like(m.id); await load()}catch{} }
async function sendCmt(m){ if(!m._txt?.trim())return; try{await momentApi.comment(m.id,m._txt); m._txt=''; await load()}catch{} }
function preview(imgs,i){ const w=window.open('','_blank'); if(w){w.document.write(`<img src="${imgs[i]}" style="max-width:100vw;max-height:100vh">`);w.document.title=i+1+'/'+imgs.length}}
async function onImg(e){ for(const f of e.target.files){ if(postImgs.value.length>=9)break; const fd=new FormData();fd.append('file',f); try{const r=await http.post('/files/upload',fd);const u=r?.url||r;if(u)postImgs.value.push(u)}catch{}} e.target.value='' }
async function doPost(){ if(!postText.value.trim()&&!postImgs.value.length)return; try{await momentApi.create({content:postText.value,images:JSON.stringify(postImgs.value)}); showPost.value=false; postText.value=''; postImgs.value=[]; await load()}catch{} }
</script>

<style scoped>
.mpg-overlay{ position:fixed;inset:0;z-index:300;background:rgba(0,0,0,0.3);display:flex;justify-content:flex-end; }
.mpg-panel{ width:480px;max-width:100vw;height:100%;background:var(--bg-primary,#111);display:flex;flex-direction:column;overflow:hidden; }
.mpg-nav{ display:flex;align-items:center;justify-content:space-between;padding:12px 16px;background:#1a1a1a;flex-shrink:0; }
.mpg-nav-back{ background:none;border:none;color:#fff;font-size:20px;cursor:pointer;padding:0; }
.mpg-nav-title{ font-size:17px;font-weight:600;color:#fff; }
.mpg-nav-cam{ background:none;border:none;color:#fff;cursor:pointer;padding:0; }
.mpg-me{ display:flex;align-items:center;gap:14px;padding:24px 16px;background:linear-gradient(180deg,#2a2a2a,#1a1a1a);flex-shrink:0;border-bottom:1px solid rgba(255,255,255,0.05); }
.mpg-me-av{ border-radius:8px; }
.mpg-me-name{ font-size:18px;font-weight:700;color:#fff; }
.mpg-me-sub{ font-size:12px;color:rgba(255,255,255,0.4);margin-top:2px; }
.mpg-body{ flex:1;overflow-y:auto;padding-bottom:20px; }
.mpg-empty{ text-align:center;padding:80px 0;color:rgba(255,255,255,0.2);font-size:14px; }
.mpg-card{ padding:14px 16px;border-bottom:1px solid rgba(255,255,255,0.04); }
.mpg-card-hd{ display:flex;align-items:center;gap:10px;margin-bottom:8px; }
.mpg-card-meta{ line-height:1.3; }
.mpg-card-name{ font-size:15px;font-weight:600;color:#576b95; }
.mpg-card-time{ font-size:11px;color:rgba(255,255,255,0.3); }
.mpg-card-body{ font-size:14px;color:rgba(255,255,255,0.85);margin-bottom:8px;line-height:1.6;white-space:pre-wrap;word-break:break-word; }
.mpg-imgs{ display:grid;gap:3px;margin-bottom:6px;max-width:260px; }
.mpg-imgs-1{ grid-template-columns:1fr;max-width:200px; }
.mpg-imgs-2{ grid-template-columns:1fr 1fr; }
.mpg-imgs-3,.mpg-imgs-4,.mpg-imgs-5,.mpg-imgs-6{ grid-template-columns:1fr 1fr 1fr; }
.mpg-imgs-7,.mpg-imgs-8,.mpg-imgs-9{ grid-template-columns:1fr 1fr 1fr; }
.mpg-img{ width:100%;aspect-ratio:1;object-fit:cover;border-radius:2px;cursor:pointer; }
.mpg-imgs-1 .mpg-img{ aspect-ratio:auto;max-height:220px; }
.mpg-bar{ display:flex;align-items:center;justify-content:space-between;padding:4px 0; }
.mpg-bar-time{ font-size:11px;color:rgba(255,255,255,0.25); }
.mpg-bar-btns{ display:flex;gap:20px; }
.mpg-lb,.mpg-cb{ cursor:pointer;font-size:13px;color:rgba(255,255,255,0.5);display:flex;align-items:center;gap:3px; }
.mpg-lb.on{ color:#e74c3c; }
.mpg-likes{ font-size:12px;color:rgba(255,255,255,0.3);padding:2px 0; }
.mpg-comments{ background:rgba(255,255,255,0.03);padding:4px 8px;border-radius:2px;margin-top:4px; }
.mpg-cmt{ font-size:12px;color:rgba(255,255,255,0.6);padding:1px 0; }
.mpg-cmt b{ color:#576b95; }
.mpg-cmt-input{ margin-top:4px; }
.mpg-inp{ width:100%;border:1px solid rgba(255,255,255,0.1);border-radius:4px;padding:4px 8px;font-size:12px;background:rgba(255,255,255,0.05);color:#fff;outline:none; }
.mpg-inp:focus{ border-color:rgba(255,255,255,0.2); }

.mpg-post-overlay{ position:fixed;inset:0;z-index:600;background:rgba(0,0,0,0.8);display:flex;align-items:flex-start;justify-content:center;padding-top:40px; }
.mpg-post-dlg{ width:100%;max-width:500px;background:#1a1a1a; }
.mpg-post-hd{ display:flex;justify-content:space-between;align-items:center;padding:14px 16px;color:#fff;font-size:15px;border-bottom:1px solid rgba(255,255,255,0.06); }
.mpg-post-hd span{ cursor:pointer; }
.mpg-post-send{ color:#07C160;font-weight:600; }
.mpg-post-ta{ width:100%;border:none;padding:16px;font-size:16px;background:transparent;color:#fff;outline:none;resize:none;font-family:inherit; }
.mpg-post-ta::placeholder{ color:#444; }
.mpg-post-imgs{ display:flex;gap:6px;flex-wrap:wrap;padding:0 16px 8px; }
.mpg-post-iw{ position:relative; }
.mpg-post-img{ width:72px;height:72px;object-fit:cover;border-radius:4px; }
.mpg-post-del{ position:absolute;top:-6px;right:-6px;width:18px;height:18px;border-radius:50%;background:#e74c3c;color:#fff;font-size:12px;display:flex;align-items:center;justify-content:center;cursor:pointer; }
.mpg-post-add{ display:block;padding:14px 16px;color:#aaa;cursor:pointer;font-size:14px;border-top:1px solid rgba(255,255,255,0.04); }

.mp-slide-enter-active,.mp-slide-leave-active{ transition:all 0.25s ease; }
.mp-slide-enter-from .mpg-panel{ transform:translateX(100%); }
.mp-slide-enter-to .mpg-panel{ transform:translateX(0); }
.mp-slide-leave-to .mpg-panel{ transform:translateX(100%); }
.mp-slide-enter-from,.mp-slide-leave-to{ opacity:0; }
</style>
