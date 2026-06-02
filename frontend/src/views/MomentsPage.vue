<template>
  <div v-if="visible" class="mp-root">
    <!-- 导航栏 -->
    <div class="mp-nav">
      <button class="mp-nav-back" @click="$emit('close')">←</button>
      <span class="mp-nav-title">朋友圈</span>
      <button class="mp-nav-cam" @click="showPost=true">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="#fff"><path d="M12 15.2c1.98 0 3.6-1.62 3.6-3.6s-1.62-3.6-3.6-3.6-3.6 1.62-3.6 3.6 1.62 3.6 3.6 3.6z"/><path d="M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"/></svg>
      </button>
    </div>
    <!-- 内容区 -->
    <div class="mp-scroll" ref="scrollRef">
      <!-- 封面占位 -->
      <div class="mp-cover">
        <!-- 个人区 -->
        <div class="mp-me">
          <div class="mp-me-name">{{ auth.user?.nickname || auth.user?.username }}</div>
          <Avatar :src="auth.user?.avatar" :name="auth.user?.nickname || auth.user?.username" :size="64" class="mp-me-av" />
        </div>
      </div>
      <!-- 时间线 -->
      <LoadingSpinner :visible="loading" />
      <div v-if="!loading && !items.length" class="mp-empty">暂无动态</div>
      <div v-for="m in items" :key="m.id" class="mp-card">
        <div class="mp-card-left">
          <Avatar :src="m.userAvatar" :name="m.userName" :size="40" />
        </div>
        <div class="mp-card-right">
          <div class="mp-card-name">{{ m.userName }}</div>
          <div class="mp-card-body" v-if="m.content">{{ m.content }}</div>
          <div class="mp-imgs" v-if="imgList(m).length">
            <img v-for="(url,i) in imgList(m).slice(0,9)" :key="i" :src="url" class="mp-img" @click="preview(imgList(m),i)" />
          </div>
          <div class="mp-card-ft">
            <span class="mp-card-time">{{ fmt(m.createdAt) }}</span>
            <span class="mp-card-del" v-if="m.userId === auth.user?.id" @click="delMoment(m.id)">删除</span>
            <span class="mp-dot" v-if="auth.user?.id ? m.userId===auth.user.id : false"></span>
            <span class="mp-actions">
              <span class="mp-act" :class="{ on: m.liked }" @click="toggleLike(m)">{{ m.liked ? '取消' : '' }}赞</span>
              <span class="mp-act" @click="m._cmt=!m._cmt">评论</span>
            </span>
          </div>
          <div v-if="m.likes?.length || m.comments?.length" class="mp-interact">
            <div v-if="m.likes?.length" class="mp-likes">
              <span class="mp-like-icon">❤️</span>
              <span v-for="(l,i) in m.likes" :key="l.userId">{{ l.name }}<span v-if="i < m.likes.length-1">, </span></span>
            </div>
            <div v-for="c in m.comments" :key="c.id" class="mp-cmt">
              <b>{{ c.userName }}</b>: {{ c.content }}
            </div>
          </div>
          <div v-if="m._cmt" class="mp-cmt-input">
            <input v-model="m._txt" @keydown.enter="sendCmt(m)" placeholder="评论" class="mp-inp" />
          </div>
        </div>
      </div>
    </div>

    <!-- 发表页(全屏) -->
    <div v-if="showPost" class="mp-post-root">
      <div class="mp-post-nav">
        <span @click="showPost=false">取消</span>
        <b>发表文字</b>
        <span class="mp-post-send" @click="doPost">发表</span>
      </div>
      <textarea v-model="postText" class="mp-post-ta" placeholder="这一刻的想法..." rows="5" autofocus></textarea>
      <div class="mp-post-imgs" v-if="postImgs.length">
        <div v-for="(url,i) in postImgs" :key="i" class="mp-post-iw"><img :src="url" class="mp-post-img" /><span class="mp-post-del" @click="postImgs.splice(i,1)">×</span></div>
      </div>
      <div class="mp-post-bar">
        <label class="mp-post-bar-item">📷<span>从相册选择</span><input type="file" accept="image/*" multiple @change="onImg" style="display:none" /></label>
        <span class="mp-post-bar-item">📍<span>所在位置</span></span>
      </div>
    </div>
  </div>
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
async function load(){ loading.value=true; try{items.value=(await momentApi.timeline())||[]}catch{}; loading.value=false }
function imgList(m){ try{return JSON.parse(m.images||'[]')}catch{return[]} }
function fmt(t){ if(!t)return''; const d=new Date(t),n=new Date(),diff=n-d; if(diff<6e4)return'刚刚'; if(diff<36e5)return Math.floor(diff/6e4)+'分钟前'; if(diff<864e5)return Math.floor(diff/36e5)+'小时前'; return (d.getMonth()+1)+'月'+d.getDate()+'日' }
async function toggleLike(m){ try{await momentApi.like(m.id); await load()}catch{} }
async function sendCmt(m){ if(!m._txt?.trim())return; try{await momentApi.comment(m.id,m._txt); m._txt=''; await load()}catch{} }
async function delMoment(id){ try{await http.delete('/moments/'+id); await load()}catch{} }
function preview(imgs,i){ const w=window.open('','_blank'); if(w){w.document.write(`<img src="${imgs[i]}" style="max-width:100vw;max-height:100vh">`);w.document.title=i+1+'/'+imgs.length}}
async function onImg(e){ for(const f of e.target.files){ if(postImgs.value.length>=9)break; const fd=new FormData();fd.append('file',f); try{const r=await http.post('/files/upload',fd);const u=r?.url||r;if(u)postImgs.value.push(u)}catch{}} e.target.value='' }
async function doPost(){ if(!postText.value.trim()&&!postImgs.value.length)return; try{await momentApi.create({content:postText.value,images:JSON.stringify(postImgs.value)}); showPost.value=false; postText.value=''; postImgs.value=[]; await load()}catch{} }
</script>

<style scoped>
.mp-root{ position:fixed;inset:0;z-index:300;background:#fff;display:flex;flex-direction:column; }
.mp-nav{ display:flex;align-items:center;justify-content:space-between;padding:12px 16px;background:#2c2c2c;color:#fff;flex-shrink:0; }
.mp-nav-back{ background:none;border:none;color:#fff;font-size:18px;cursor:pointer; }
.mp-nav-title{ font-size:17px;font-weight:600; }
.mp-nav-cam{ background:none;border:none;color:#fff;cursor:pointer; }
.mp-scroll{ flex:1;overflow-y:auto;-webkit-overflow-scrolling:touch; }
.mp-cover{ height:280px;background:linear-gradient(135deg,#3a3a3a,#1a1a1a);position:relative;display:flex;align-items:flex-end;justify-content:flex-end;padding:16px; }
.mp-me{ display:flex;align-items:center;gap:12px; }
.mp-me-name{ font-size:16px;font-weight:600;color:#fff;text-shadow:0 1px 2px rgba(0,0,0,0.5); }
.mp-me-av{ border-radius:8px;border:2px solid rgba(255,255,255,0.3); }
.mp-empty{ text-align:center;padding:80px 0;color:#ccc;font-size:14px; }
.mp-card{ display:flex;gap:10px;padding:12px 14px;border-bottom:1px solid #f0f0f0; }
.mp-card-left{ flex-shrink:0; }
.mp-card-right{ flex:1;min-width:0; }
.mp-card-name{ font-size:15px;color:#576b95;font-weight:600;margin-bottom:4px; }
.mp-card-body{ font-size:15px;color:#333;line-height:1.5;margin-bottom:6px;white-space:pre-wrap;word-break:break-word; }
.mp-imgs{ display:grid;grid-template-columns:repeat(3,1fr);gap:3px;margin-bottom:6px;max-width:250px; }
.mp-img{ width:100%;aspect-ratio:1;object-fit:cover; }
.mp-card-ft{ display:flex;align-items:center;gap:8px; }
.mp-card-time{ font-size:12px;color:#999; }
.mp-card-del{ font-size:12px;color:#576b95;cursor:pointer; }
.mp-dot{ width:3px;height:3px;border-radius:50%;background:#d0d0d0; }
.mp-actions{ display:flex;gap:12px; }
.mp-act{ font-size:12px;color:#576b95;cursor:pointer; }
.mp-act.on{ color:#e74c3c; }
.mp-interact{ background:#f5f5f5;border-radius:2px;padding:6px 8px;margin-top:4px; }
.mp-likes{ font-size:13px;color:#576b95;padding-bottom:4px;border-bottom:1px solid #e5e5e5;margin-bottom:4px;display:flex;flex-wrap:wrap; }
.mp-like-icon{ margin-right:4px; }
.mp-cmt{ font-size:13px;color:#333;padding:1px 0;line-height:1.4; }
.mp-cmt b{ color:#576b95; }
.mp-cmt-input{ margin-top:4px; }
.mp-inp{ width:100%;border:1px solid #eee;border-radius:4px;padding:4px 8px;font-size:13px;outline:none;background:#f5f5f5; }

.mp-post-root{ position:fixed;inset:0;z-index:500;background:#f5f5f5;display:flex;flex-direction:column; }
.mp-post-nav{ display:flex;justify-content:space-between;align-items:center;padding:14px 16px;background:#2c2c2c;color:#fff;font-size:15px; }
.mp-post-nav span{ cursor:pointer; }
.mp-post-send{ color:#07C160;font-weight:600;background:#222;padding:4px 12px;border-radius:4px; }
.mp-post-ta{ width:100%;border:none;padding:16px;font-size:16px;background:#fff;outline:none;resize:none;font-family:inherit;border-bottom:1px solid #eee; }
.mp-post-ta::placeholder{ color:#bbb; }
.mp-post-imgs{ display:flex;gap:8px;flex-wrap:wrap;padding:12px 16px;background:#fff; }
.mp-post-iw{ position:relative; }
.mp-post-img{ width:72px;height:72px;object-fit:cover; }
.mp-post-del{ position:absolute;top:-6px;right:-6px;width:18px;height:18px;border-radius:50%;background:#e74c3c;color:#fff;font-size:12px;display:flex;align-items:center;justify-content:center;cursor:pointer; }
.mp-post-bar{ display:flex;flex-direction:column;background:#fff;margin-top:12px;border-top:1px solid #eee; }
.mp-post-bar-item{ display:flex;align-items:center;gap:12px;padding:14px 16px;font-size:15px;color:#333;cursor:pointer;border-bottom:1px solid #f0f0f0; }
.mp-post-bar-item span{ font-size:14px;color:#555; }
</style>
