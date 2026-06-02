<template>
  <div class="mp-root">
    <div class="mp-hd">朋友圈</div>
    <div class="mp-body">
      <LoadingSpinner :visible="loading" />
      <div v-if="!loading && !items.length" class="mp-empty">暂无动态</div>
      <div v-for="m in items" :key="m.id" class="mp-card">
        <div class="mp-card-hd">
          <Avatar :src="m.userAvatar" :name="m.userName" :size="40" />
          <div class="mp-card-info">
            <div class="mp-card-name">{{ m.userName }}</div>
            <div class="mp-card-time">{{ formatTime(m.createdAt) }}</div>
          </div>
        </div>
        <div class="mp-card-body" v-if="m.content">{{ m.content }}</div>
        <div class="mp-imgs" v-if="imgList(m).length">
          <img v-for="(url, i) in imgList(m)" :key="i" :src="url" class="mp-img" @click="previewImgs(imgList(m), i)" />
        </div>
        <div class="mp-actions">
          <span class="mp-likes" v-if="m.likes?.length">{{ m.likes.map(l => l.name).join(', ') }}</span>
        </div>
        <div class="mp-bar">
          <span class="mp-like-btn" :class="{ liked: m.liked }" @click="toggleLike(m)">❤️ {{ m.likes?.length || 0 }}</span>
          <span class="mp-cmt-btn" @click="openCmt(m)">💬</span>
        </div>
        <div class="mp-comments" v-if="m.comments?.length">
          <div v-for="c in m.comments" :key="c.id" class="mp-cmt">
            <b>{{ c.userName }}</b>: {{ c.content }}
          </div>
        </div>
        <div v-if="m._showCmt" class="mp-cmt-input">
          <input v-model="m._cmtText" @keydown.enter="sendCmt(m)" placeholder="评论..." />
        </div>
      </div>
    </div>
    <!-- 发布按钮 -->
    <button class="mp-post-btn" @click="showPost=true">＋</button>
    <!-- 发布弹窗 -->
    <Teleport to="body">
      <div v-if="showPost" class="mp-post-overlay" @click.self="showPost=false">
        <div class="mp-post-dlg">
          <div class="mp-post-hd">发表动态</div>
          <textarea v-model="postText" class="mp-post-ta" placeholder="分享新鲜事..." rows="3"></textarea>
          <div class="mp-post-imgs" v-if="postImgs.length">
            <div v-for="(url, i) in postImgs" :key="i" class="mp-post-img-wrap">
              <img :src="url" class="mp-post-img" />
              <span class="mp-post-del" @click="postImgs.splice(i,1)">×</span>
            </div>
          </div>
          <div class="mp-post-btns">
            <label class="mp-post-add">📷<input type="file" accept="image/*" multiple @change="onPostImg" style="display:none" /></label>
            <button class="mp-post-send" @click="doPost" :disabled="!postText.trim() && !postImgs.length">发布</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Avatar from '../common/Avatar.vue'
import LoadingSpinner from '../common/LoadingSpinner.vue'
import { momentApi } from '../../api/endpoints'
import http from '../../api/http'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const items = ref([])
const loading = ref(false)
const showPost = ref(false)
const postText = ref('')
const postImgs = ref([])

onMounted(load)

async function load() {
  loading.value = true
  try { items.value = (await momentApi.timeline()) || [] } catch {}
  finally { loading.value = false }
}

function imgList(m) {
  try { return JSON.parse(m.images || '[]') } catch { return [] }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return (d.getMonth()+1)+'月'+d.getDate()+'日 '+String(d.getHours()).padStart(2,'0')+':'+String(d.getMinutes()).padStart(2,'0')
}

async function toggleLike(m) {
  try { await momentApi.like(m.id); await load() } catch {}
}

function openCmt(m) { m._showCmt = !m._showCmt; if (!m._cmtText) m._cmtText = '' }
async function sendCmt(m) {
  if (!m._cmtText.trim()) return
  try { await momentApi.comment(m.id, m._cmtText); m._cmtText = ''; await load() } catch {}
}

function previewImgs(imgs, i) {
  // 简单全屏预览
  const w = window.open('', '_blank', 'width=800,height=600')
  if (w) { w.document.write(`<img src="${imgs[i]}" style="max-width:100%;max-height:100vh" />`); w.document.title = `${i+1}/${imgs.length}` }
}

async function onPostImg(e) {
  for (const file of e.target.files) {
    if (postImgs.value.length >= 9) break
    const form = new FormData(); form.append('file', file)
    try { const res = await http.post('/files/upload', form); const url = res?.url || res; if (url) postImgs.value.push(url) } catch {}
  }
  e.target.value = ''
}

async function doPost() {
  if (!postText.value.trim() && !postImgs.value.length) return
  try {
    await momentApi.create({ content: postText.value, images: JSON.stringify(postImgs.value) })
    showPost.value = false; postText.value = ''; postImgs.value = []
    await load()
  } catch {}
}
</script>

<style scoped>
.mp-root { height: 100%; display: flex; flex-direction: column; background: var(--list-bg, #22252d); overflow: hidden; }
.mp-hd { padding: 14px 18px; font-size: 15px; font-weight: 600; color: var(--text-primary, #e8e8ea); border-bottom: 1px solid var(--border, #2e3038); flex-shrink: 0; }
.mp-body { flex: 1; overflow-y: auto; padding: 8px 0; }
.mp-empty { text-align: center; padding: 48px; color: var(--text-muted, #999); }
.mp-card { padding: 12px 16px; border-bottom: 1px solid rgba(255,255,255,0.04); }
.mp-card-hd { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.mp-card-name { font-size: 14px; font-weight: 600; color: var(--accent, #f7931e); }
.mp-card-time { font-size: 11px; color: var(--text-muted, #999); }
.mp-card-body { font-size: 14px; color: var(--text-primary, #e8e8ea); margin-bottom: 8px; line-height: 1.5; white-space: pre-wrap; }
.mp-imgs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; margin-bottom: 8px; }
.mp-img { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: 4px; cursor: pointer; }
.mp-actions { margin-bottom: 4px; }
.mp-likes { font-size: 12px; color: var(--accent, #f7931e); }
.mp-bar { display: flex; gap: 16px; padding: 6px 0; border-top: 1px solid rgba(255,255,255,0.04); font-size: 13px; }
.mp-like-btn { cursor: pointer; }
.mp-like-btn.liked { color: #e74c3c; }
.mp-cmt-btn { cursor: pointer; }
.mp-comments { background: rgba(255,255,255,0.03); border-radius: 4px; padding: 6px 10px; margin-top: 4px; }
.mp-cmt { font-size: 12px; color: var(--text-secondary, #bbb); padding: 2px 0; }
.mp-cmt b { color: var(--accent, #f7931e); }
.mp-cmt-input { margin-top: 6px; }
.mp-cmt-input input { width: 100%; border: 1px solid var(--border, #3a3c44); border-radius: 4px; padding: 4px 8px; font-size: 12px; background: var(--bg-input, #2e3038); color: var(--text-primary, #e8e8ea); outline: none; }
.mp-post-btn { position: absolute; bottom: 20px; right: 20px; width: 48px; height: 48px; border-radius: 50%; background: var(--accent, #f7931e); color: #fff; font-size: 24px; border: none; cursor: pointer; box-shadow: 0 4px 12px rgba(0,0,0,0.3); z-index: 10; }
.mp-post-overlay { position: fixed; inset: 0; z-index: 500; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; }
.mp-post-dlg { width: 360px; background: var(--bg-dialog, #252529); border-radius: 12px; padding: 20px; }
.mp-post-hd { font-size: 16px; font-weight: 600; color: var(--text-primary, #e8e8ea); margin-bottom: 12px; }
.mp-post-ta { width: 100%; border: 1px solid var(--border, #3a3c44); border-radius: 6px; padding: 8px; font-size: 13px; background: var(--bg-input, #2e3038); color: var(--text-primary, #e8e8ea); outline: none; resize: none; font-family: inherit; }
.mp-post-imgs { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 8px; }
.mp-post-img-wrap { position: relative; }
.mp-post-img { width: 64px; height: 64px; object-fit: cover; border-radius: 4px; }
.mp-post-del { position: absolute; top: -6px; right: -6px; width: 18px; height: 18px; border-radius: 50%; background: #e74c3c; color: #fff; font-size: 12px; display: flex; align-items: center; justify-content: center; cursor: pointer; }
.mp-post-btns { display: flex; justify-content: space-between; margin-top: 12px; }
.mp-post-add { cursor: pointer; font-size: 24px; }
.mp-post-send { padding: 6px 20px; border: none; border-radius: 4px; background: var(--accent, #f7931e); color: #fff; font-size: 13px; cursor: pointer; }
.mp-post-send:disabled { opacity: 0.4; }
</style>
