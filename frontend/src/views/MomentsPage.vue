<template>
  <Teleport to="body">
    <Transition name="mp-fade">
      <div v-if="visible" class="mp-overlay" @click.self="$emit('close')">
        <div class="mp-dialog" :style="{ transform: `translate(${pos.x}px, ${pos.y}px)` }">
          <!-- 可拖拽标题栏 -->
          <div class="mp-nav" @mousedown="onDragStart">
            <button class="mp-nav-btn" @click="goBack">{{ viewing ? '← 返回' : '← 关闭' }}</button>
            <span class="mp-nav-title">{{ viewing ? (viewName+'的朋友圈') : '朋友圈' }}</span>
            <button class="mp-nav-btn" @click="refresh" :disabled="refreshing">{{ refreshing ? '↻' : '↻' }}</button>
          </div>
          <!-- 封面 -->
          <div class="mp-cover" @click="!viewing && triggerCover()">
            <img v-if="coverUrl" :src="coverUrl" class="mp-cover-img" />
            <div class="mp-me" v-if="!viewing">
              <div class="mp-me-name">{{ auth.user?.nickname || auth.user?.username }}</div>
              <Avatar :src="auth.user?.avatar" :name="auth.user?.nickname || auth.user?.username" :size="60" class="mp-me-av" />
            </div>
            <input type="file" ref="coverInput" accept="image/*" @change="onCoverChange" style="display:none" />
          </div>
          <!-- 时间线 -->
          <div class="mp-body">
            <LoadingSpinner :visible="loading" />
            <div v-if="!loading && !items.length" class="mp-empty">— 暂无动态 —</div>
            <template v-for="(m,i) in items" :key="m.id">
              <div v-if="i===0 || !sameDay(items[i-1].createdAt, m.createdAt)" class="mp-day-label">{{ dayLabel(m.createdAt) }}</div>
              <div class="mp-card">
                <Avatar :src="m.userAvatar" :name="m.userName" :size="40" class="mp-card-av" @click="goUser(m)" />
                <div class="mp-card-main">
                  <div class="mp-card-name" @click="goUser(m)">{{ m.userName }}</div>
                  <div class="mp-card-body" v-if="m.content">{{ m.content }}</div>
                  <div class="mp-imgs" v-if="imgList(m).length">
                    <img v-for="(url,j) in imgList(m).slice(0,9)" :key="j" :src="url" class="mp-img" @click="preview(imgList(m),j)" />
                  </div>
                  <div class="mp-card-bar">
                    <span class="mp-card-time">{{ fmt(m.createdAt) }}</span>
                    <span class="mp-card-del" v-if="m.userId===auth.user?.id" @click="delMoment(m.id)">删除</span>
                    <span class="mp-card-actions">
                      <span class="mp-act" :class="{on:m.liked}" @click="toggleLike(m)">赞</span>
                      <span class="mp-act" @click="m._cmt=!m._cmt">评论</span>
                    </span>
                  </div>
                  <div class="mp-interact" v-if="m.likes?.length||m.comments?.length">
                    <div v-if="m.likes?.length" class="mp-i-likes">❤️ {{ m.likes.map(l=>l.name).join(', ') }}</div>
                    <div v-if="m.comments?.length" class="mp-i-cmts"><div v-for="c in m.comments" :key="c.id" class="mp-i-cmt"><b>{{ c.userName }}</b>: {{ c.content }}</div></div>
                  </div>
                  <div v-if="m._cmt" class="mp-cmt-input"><input v-model="m._txt" @keydown.enter="sendCmt(m)" placeholder="评论" class="mp-inp" /></div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
  <!-- 发表 -->
  <Teleport to="body">
    <div v-if="showPost" class="mp-post-overlay">
      <div class="mp-post-dlg">
        <div class="mp-post-nav"><span @click="showPost=false">取消</span><b>发表文字</b><span class="mp-post-send" @click="doPost">发表</span></div>
        <textarea v-model="postText" class="mp-post-ta" placeholder="这一刻的想法..." rows="5" autofocus></textarea>
        <div class="mp-post-imgs" v-if="postImgs.length"><div v-for="(url,i) in postImgs" :key="i" class="mp-post-iw"><img :src="url" class="mp-post-img" /><span class="mp-post-del" @click="postImgs.splice(i,1)">×</span></div></div>
        <div class="mp-post-bar"><label class="mp-post-item">📷 从相册选择<input type="file" accept="image/*" multiple @change="onImg" style="display:none" /></label><span class="mp-post-item">📍 所在位置</span></div>
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
const emit = defineEmits(['close'])
const auth = useAuthStore()
const items = ref([])
const loading = ref(false)
const refreshing = ref(false)
const showPost = ref(false)
const postText = ref('')
const postImgs = ref([])
const coverUrl = ref(localStorage.getItem('xr-cover')||'')
const coverInput = ref(null)
const viewing = ref(false)
const viewId = ref(null)
const viewName = ref('')

// 拖拽
const pos = ref({x:0,y:0})
let drag = null
function onDragStart(e){ if(e.target.closest('button'))return; drag={x:e.clientX-pos.value.x,y:e.clientY-pos.value.y}; document.addEventListener('mousemove',onDrag); document.addEventListener('mouseup',onDragEnd) }
function onDrag(e){ pos.value={x:e.clientX-drag.x,y:e.clientY-drag.y} }
function onDragEnd(){ document.removeEventListener('mousemove',onDrag); document.removeEventListener('mouseup',onDragEnd); drag=null }

async function refresh(){ refreshing.value=true; await load(); refreshing.value=false }
onMounted(load)
async function load(){
  loading.value=true
  try { items.value = viewing.value ? (await momentApi.userTimeline(viewId.value)||[]) : (await momentApi.timeline()||[]) }
  catch {}; loading.value=false
}
function goUser(m){ if(m.userId===auth.user?.id) return; viewing.value=true; viewId.value=m.userId; viewName.value=m.userName; load() }
function goBack(){ if(viewing.value){ viewing.value=false; viewId.value=null; viewName.value=''; load() } else emit('close') }
function sameDay(a,b){ return new Date(a).toDateString()===new Date(b).toDateString() }
function dayLabel(t){ const d=new Date(t),n=new Date(),diff=n-d; if(diff<864e5&&d.getDate()===n.getDate())return'今天'; const y=new Date(n.getFullYear(),n.getMonth(),n.getDate()-1); if(d.toDateString()===y.toDateString())return'昨天'; return d.getFullYear()+'年'+(d.getMonth()+1)+'月'+d.getDate()+'日' }
function imgList(m){ try{return JSON.parse(m.images||'[]')}catch{return[]} }
function fmt(t){ if(!t)return''; const d=new Date(t),n=new Date(); const diff=n-d; if(diff<6e4)return'刚刚'; if(diff<36e5)return Math.floor(diff/6e4)+'分钟前'; if(diff<864e5)return Math.floor(diff/36e5)+'小时前'; return String(d.getHours()).padStart(2,'0')+':'+String(d.getMinutes()).padStart(2,'0') }
async function toggleLike(m){ try{await momentApi.like(m.id); await load()}catch{} }
async function sendCmt(m){ if(!m._txt?.trim())return; try{await momentApi.comment(m.id,m._txt); m._txt=''; await load()}catch{} }
async function delMoment(id){ try{await http.delete('/moments/'+id); await load()}catch{} }
function triggerCover(){ coverInput.value?.click() }
function onCoverChange(e){ const f=e.target.files[0]; if(!f)return; const r=new FileReader(); r.onload=()=>{coverUrl.value=r.result;localStorage.setItem('xr-cover',r.result)}; r.readAsDataURL(f); e.target.value='' }
function preview(imgs,i){ const w=window.open('','_blank'); if(w){w.document.write(`<img src="${imgs[i]}" style="max-width:100vw;max-height:100vh">`);w.document.title=i+1+'/'+imgs.length}}
async function onImg(e){ for(const f of e.target.files){ if(postImgs.value.length>=9)break; const fd=new FormData();fd.append('file',f); try{const r=await http.post('/files/upload',fd);const u=r?.url||r;if(u)postImgs.value.push(u)}catch{}} e.target.value='' }
async function doPost(){ if(!postText.value.trim()&&!postImgs.value.length)return; try{await momentApi.create({content:postText.value,images:JSON.stringify(postImgs.value)}); showPost.value=false; postText.value=''; postImgs.value=[]; await load()}catch{} }
</script>

<style scoped>
.mp-overlay{ position:fixed;inset:0;z-index:300;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center; }
.mp-dialog{ width:560px;max-width:95vw;max-height:88vh;background:#fff;border-radius:12px;overflow:hidden;display:flex;flex-direction:column;box-shadow:0 8px 40px rgba(0,0,0,0.25);transition:box-shadow 0.2s; }
.mp-nav{ display:flex;align-items:center;justify-content:space-between;padding:12px 18px;background:#1a1a1a;color:#fff;flex-shrink:0;cursor:move;user-select:none; }
.mp-nav-btn{ background:none;border:none;color:#bbb;font-size:14px;cursor:pointer;padding:2px 6px;border-radius:4px;transition:all 0.15s; }
.mp-nav-btn:hover{ color:#fff;background:rgba(255,255,255,0.1); }
.mp-nav-btn:disabled{ opacity:0.5; }
.mp-nav-title{ font-size:16px;font-weight:600;color:#fff; }
.mp-cover{ height:200px;background:linear-gradient(135deg,#4a4a4a,#2a2a2a);position:relative;flex-shrink:0;cursor:pointer;overflow:hidden; }
.mp-cover-img{ width:100%;height:100%;object-fit:cover;position:absolute;inset:0; }
.mp-me{ position:absolute;bottom:16px;right:20px;display:flex;align-items:center;gap:10px; }
.mp-me-name{ font-size:15px;font-weight:600;color:#fff;text-shadow:0 1px 3px rgba(0,0,0,0.5); }
.mp-me-av{ border:2px solid rgba(255,255,255,0.25);border-radius:8px; }
.mp-body{ flex:1;overflow-y:auto; }
.mp-empty{ text-align:center;padding:80px 0;color:#ccc;font-size:14px; }
.mp-day-label{ text-align:center;padding:8px;font-size:12px;color:#b0b0b0;background:#f8f8f8;border-bottom:1px solid #eee;position:sticky;top:0;z-index:2; }
.mp-card{ display:flex;gap:12px;padding:12px 20px;border-bottom:1px solid #f0f0f0;transition:background 0.1s; }
.mp-card:hover{ background:#fafafa; }
.mp-card-av{ cursor:pointer;flex-shrink:0;border-radius:4px; }
.mp-card-main{ flex:1;min-width:0; }
.mp-card-name{ font-size:15px;color:#576b95;font-weight:600;cursor:pointer;margin-bottom:4px;display:inline-block; }
.mp-card-name:hover{ text-decoration:underline; }
.mp-card-body{ font-size:15px;color:#222;line-height:1.55;margin-bottom:6px;white-space:pre-wrap;word-break:break-word; }
.mp-imgs{ display:grid;grid-template-columns:repeat(3,1fr);gap:3px;margin-bottom:6px;max-width:250px; }
.mp-img{ width:100%;aspect-ratio:1;object-fit:cover;border-radius:2px;cursor:pointer; }
.mp-card-bar{ display:flex;align-items:center;gap:8px; }
.mp-card-time{ font-size:12px;color:#b0b0b0; }
.mp-card-del{ font-size:12px;color:#576b95;cursor:pointer; }
.mp-card-actions{ display:flex;gap:14px;margin-left:auto; }
.mp-act{ font-size:13px;color:#576b95;cursor:pointer;padding:2px 0;transition:color 0.1s; }
.mp-act:hover{ color:#3a5a8c; }
.mp-act.on{ color:#e74c3c;font-weight:600; }
.mp-interact{ background:#f5f5f5;border-radius:2px;padding:6px 10px;margin-top:4px; }
.mp-i-likes{ font-size:13px;color:#576b95;padding-bottom:4px;margin-bottom:4px;border-bottom:1px solid #e5e5e5; }
.mp-i-cmts{ }
.mp-i-cmt{ font-size:13px;color:#333;padding:1px 0;line-height:1.45; }
.mp-i-cmt b{ color:#576b95; }
.mp-cmt-input{ margin-top:4px; }
.mp-inp{ width:100%;border:1px solid #eee;border-radius:4px;padding:4px 8px;font-size:13px;outline:none;background:#f5f5f5; }
.mp-inp:focus{ border-color:#ccc;background:#fff; }

.mp-post-overlay{ position:fixed;inset:0;z-index:500;background:rgba(0,0,0,0.6);display:flex;align-items:flex-start;justify-content:center;padding-top:40px; }
.mp-post-dlg{ width:100%;max-width:500px;background:#f5f5f5;border-radius:8px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.3); }
.mp-post-nav{ display:flex;justify-content:space-between;padding:14px 16px;background:#2c2c2c;color:#fff;font-size:15px; }
.mp-post-nav span{ cursor:pointer; }
.mp-post-send{ color:#07C160;font-weight:600;background:#222;padding:4px 12px;border-radius:4px; }
.mp-post-ta{ width:100%;border:none;padding:16px;font-size:16px;background:#fff;outline:none;resize:none;font-family:inherit;border-bottom:1px solid #eee; }
.mp-post-ta::placeholder{ color:#bbb; }
.mp-post-imgs{ display:flex;gap:8px;flex-wrap:wrap;padding:12px 16px;background:#fff; }
.mp-post-iw{ position:relative; }
.mp-post-img{ width:68px;height:68px;object-fit:cover;border-radius:2px; }
.mp-post-del{ position:absolute;top:-6px;right:-6px;width:18px;height:18px;border-radius:50%;background:#e74c3c;color:#fff;font-size:12px;display:flex;align-items:center;justify-content:center;cursor:pointer; }
.mp-post-bar{ display:flex;flex-direction:column;background:#fff;margin-top:8px;border-top:1px solid #eee; }
.mp-post-item{ display:flex;align-items:center;gap:12px;padding:14px 16px;font-size:15px;color:#333;cursor:pointer;border-bottom:1px solid #f0f0f0;transition:background 0.1s; }
.mp-post-item:hover{ background:#fafafa; }

.mp-fade-enter-active,.mp-fade-leave-active{ transition:opacity 0.2s ease; }
.mp-fade-enter-from,.mp-fade-leave-to{ opacity:0; }
</style>
