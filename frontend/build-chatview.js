const fs = require('fs')
const path = require('path')

const parts = []

parts.push(`<template>
  <div class="chat-layout" :class="theme">
    <div class="sidebar">
      <div class="sidebar-top">
        <div class="side-tabs">
          <span class="side-tab" :class="{ active: sideTab === 'chat' }" @click="sideTab = 'chat'">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/></svg>
            <span>聊天</span>
          </span>
          <span class="side-tab" :class="{ active: sideTab === 'contacts' }" @click="sideTab = 'contacts'">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
            <span>通讯录</span>
          </span>
          <span class="side-tab" :class="{ active: sideTab === 'me' }" @click="sideTab = 'me'">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            <span>我</span>
          </span>
        </div>
      </div>

      <div v-show="sideTab === 'chat'" class="side-panel chat-panel">
        <div class="search-box">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" class="search-icon"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
          <input v-model="searchText" placeholder="搜索" @input="searchConversations" />
        </div>
        <div class="conv-list" ref="convListRef">
          <div v-for="item in filteredConvs" :key="item.id"
            class="conv-item"
            :class="{ active: selectedConv && selectedConv.id === item.id, pinned: item.isPinned }"
            @click="selectConversation(item)">
            <div class="conv-avatar" :style="avatarStyle(item.targetName, item.targetAvatar)">
              <span v-if="!item.targetAvatar">{{ item.targetName ? item.targetName.charAt(0).toUpperCase() : '?' }}</span>
              <img v-else :src="item.targetAvatar" />
            </div>
            <div class="conv-info">
              <div class="conv-top">
                <span class="conv-name">{{ item.targetName || '未知' }}</span>
                <span v-if="item.lastMessageAt" class="conv-time">{{ formatTime(item.lastMessageAt) }}</span>
              </div>
              <div class="conv-bottom">
                <span class="conv-preview">{{ item.lastMessagePreview || '' }}</span>
                <div class="conv-badges">
                  <span v-if="item.unreadCount > 0" class="unread-badge">{{ item.unreadCount > 99 ? '99+' : item.unreadCount }}</span>
                  <span v-if="item.isPinned" class="pin-badge">
                    <svg viewBox="0 0 24 24" width="12" height="12" fill="currentColor"><path d="M16 12V4h1V2H7v2h1v8l-2 2v2h5.2v6h1.6v-6H18v-2l-2-2z"/></svg>
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div v-if="filteredConvs.length === 0" class="empty-hint">暂无会话</div>
        </div>
      </div>

      <div v-show="sideTab === 'contacts'" class="side-panel contacts-panel">
        <div class="contacts-search">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" class="search-icon"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
          <input v-model="contactsSearchText" placeholder="搜索联系人" @input="onContactsSearch" />
        </div>

        <div v-if="contactsMode === 'list'" class="contacts-quick-actions">
          <div class="quick-action" @click="contactsMode = 'addFriend'">
            <div class="qa-icon add-friend">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
            </div>
            <span>添加好友</span>
          </div>
          <div class="quick-action" @click="contactsMode = 'newFriends'">
            <div class="qa-icon new-friend">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
            </div>
            <span>好友申请 <em v-if="pendingRequests.length > 0" class="badge-dot">{{ pendingRequests.length }}</em></span>
          </div>
          <div class="quick-action" @click="contactsMode = 'createGroup'">
            <div class="qa-icon create-group">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 12.75c1.63 0 3.07.39 4.24.9 1.08.48 1.76 1.56 1.76 2.73V18H6v-1.61c0-1.18.68-2.26 1.76-2.73 1.17-.52 2.61-.91 4.24-.91zM4 13c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm1.13 1.1c-.37-.06-.74-.1-1.13-.1-.99 0-1.93.21-2.78.58C.48 14.9 0 15.62 0 16.43V18h4.5v-1.61c0-.83.23-1.61.63-2.29zM20 13c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-4-3c0-2.21-1.79-4-4-4S8 7.79 8 10s1.79 4 4 4 4-1.79 4-4zm-4-2c1.1 0 2 .9 2 2s-.9 2-2 2-2-.9-2-2 .9-2 2-2z"/></svg>
            </div>
            <span>创建群聊</span>
          </div>
        </div>

        <div v-if="contactsMode === 'list'" class="contacts-body" ref="contactsBodyRef">
          <div v-for="section in friendSections" :key="section.key" class="friend-section" :data-key="section.key">
            <div class="section-header">{{ section.key }}</div>
            <div v-for="f in section.items" :key="f.id" class="contact-item" @click="openFriendChat(f)">
              <div class="contact-avatar" :style="avatarStyle(f.nickname || f.username, f.avatar)">
                <span v-if="!f.avatar">{{ (f.nickname || f.username).charAt(0).toUpperCase() }}</span>
                <img v-else :src="f.avatar" />
              </div>
              <div class="contact-name">{{ f.nickname || f.username }}</div>
            </div>
          </div>
          <div v-if="friendSections.length === 0 && !contactsSearchText" class="empty-hint">暂无好友</div>
          <div v-if="friends.length > 0 && filteredFriends.length === 0 && contactsSearchText" class="empty-hint">未找到匹配的好友</div>
          <div v-if="groups.length > 0" class="group-section">
            <div class="section-header">群聊</div>
            <div v-for="g in groups" :key="g.id" class="contact-item" @click="openGroupChat(g)">
              <div class="contact-avatar group-avatar" :style="avatarStyle(g.name, g.avatar)">
                <span v-if="!g.avatar">{{ g.name.charAt(0).toUpperCase() }}</span>
                <img v-else :src="g.avatar" />
              </div>
              <div class="contact-name">{{ g.name }}</div>
            </div>
          </div>
        </div>

        <div v-if="contactsMode === 'list'" class="contacts-index" ref="contactsIndexRef">
          <span v-for="key in friendIndexKeys" :key="key" class="index-letter" @click="scrollToFriendSection(key)">{{ key }}</span>
        </div>

        <div v-if="contactsMode === 'addFriend'" class="contacts-sub-panel">
          <div class="sub-panel-header">
            <button class="back-btn" @click="contactsMode = 'list'">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
            </button>
            <span>添加好友</span>
          </div>
          <div class="add-friend-box">
            <input v-model="addFriendInput" placeholder="搜索用户名" @keyup.enter="sendFriendRequest()" />
            <button @click="sendFriendRequest()">搜索</button>
          </div>
          <div v-if="searchedUser" class="searched-user-card">
            <div class="searched-user-avatar" :style="avatarStyle(searchedUser.nickname || searchedUser.username, searchedUser.avatar)">
              <span v-if="!searchedUser.avatar">{{ (searchedUser.nickname || searchedUser.username).charAt(0).toUpperCase() }}</span>
              <img v-else :src="searchedUser.avatar" />
            </div>
            <div class="searched-user-info">
              <div class="searched-user-name">{{ searchedUser.nickname || searchedUser.username }}</div>
              <div class="searched-user-id">ID: {{ searchedUser.id }}</div>
            </div>
            <button class="add-btn" @click="sendFriendRequest(searchedUser.id)">添加</button>
          </div>
          <div v-if="!searchedUser && addFriendInput" class="empty-hint">未找到该用户</div>
        </div>

        <div v-if="contactsMode === 'newFriends'" class="contacts-sub-panel">
          <div class="sub-panel-header">
            <button class="back-btn" @click="contactsMode = 'list'">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
            </button>
            <span>好友申请</span>
          </div>
          <div class="friend-requests">
            <div v-for="req in pendingRequests" :key="req.id" class="request-item">
              <div class="request-avatar" :style="avatarStyle(req.nickname || req.username, req.avatar)">
                <span v-if="!req.avatar">{{ (req.nickname || req.username).charAt(0).toUpperCase() }}</span>
                <img v-else :src="req.avatar" />
              </div>
              <div class="request-info">
                <div class="request-name">{{ req.nickname || req.username }}</div>
                <div class="request-msg">{{ req.message || '请求添加你为好友' }}</div>
              </div>
              <div class="request-actions">
                <button class="accept-btn" @click="acceptRequest(req)">同意</button>
                <button class="reject-btn" @click="rejectRequest(req)">拒绝</button>
              </div>
            </div>
            <div v-if="pendingRequests.length === 0" class="empty-hint">暂无好友申请</div>
          </div>
          <div v-if="blockedUsers.length > 0" class="blocked-section">
            <div class="section-header" @click="showBlocked = !showBlocked">
              黑名单 ({{ blockedUsers.length }})
              <span :class="{ rotated: showBlocked }">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6 1.41-1.41z"/></svg>
              </span>
            </div>
            <div v-show="showBlocked">
              <div v-for="u in blockedUsers" :key="u.id" class="blocked-item">
                <span>{{ u.nickname || u.username }}</span>
                <button @click="toggleBlockFriend(u)">移出</button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="contactsMode === 'createGroup'" class="contacts-sub-panel">
          <div class="sub-panel-header">
            <button class="back-btn" @click="contactsMode = 'list'">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
            </button>
            <span>创建群聊</span>
          </div>
          <div class="create-group-form">
            <input v-model="createGroupName" placeholder="群聊名称" />
            <input v-model="createGroupDesc" placeholder="群聊简介（可选）" />
            <div class="create-group-members">
              <div class="cgm-hint">选择成员</div>
              <div v-for="f in friends" :key="f.id" class="cgm-item" @click="toggleGroupMember(f)">
                <div class="contact-avatar" :style="avatarStyle(f.nickname || f.username, f.avatar)">
                  <span v-if="!f.avatar">{{ (f.nickname || f.username).charAt(0).toUpperCase() }}</span>
                  <img v-else :src="f.avatar" />
                </div>
                <span>{{ f.nickname || f.username }}</span>
                <span class="checkmark" :class="{ checked: selectedMembers.includes(f.id) }">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
                </span>
              </div>
            </div>
            <button class="create-btn" @click="createGroup" :disabled="!createGroupName || selectedMembers.length === 0">
              创建群聊 ({{ selectedMembers.length }})
            </button>
          </div>
        </div>
      </div>

      <div v-show="sideTab === 'me'" class="side-panel me-panel">
        <div class="me-profile-card">
          <div class="me-avatar-wrap" @click="onSelfAvatarSelected">
            <div class="me-avatar" :style="avatarStyle(auth.user?.nickname || auth.user?.username, auth.user?.avatar)">
              <span v-if="!auth.user?.avatar">{{ (auth.user?.nickname || auth.user?.username || '?').charAt(0).toUpperCase() }}</span>
              <img v-else :src="auth.user?.avatar" />
            </div>
            <div class="me-avatar-overlay">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/></svg>
            </div>
            <input ref="selfAvatarInput" type="file" accept="image/*" style="display:none" @change="onSelfAvatarSelected" />
          </div>
          <div class="me-name">{{ auth.user?.nickname || auth.user?.username || '用户' }}</div>
          <div class="me-desc">ID: {{ auth.user?.id }}</div>
        </div>
        <div class="me-info-list">
          <div class="me-info-item">
            <span class="me-label">用户名</span>
            <span class="me-value">{{ auth.user?.username }}</span>
          </div>
          <div class="me-info-item">
            <span class="me-label">昵称</span>
            <span class="me-value">{{ auth.user?.nickname || '-' }}</span>
          </div>
          <div class="me-info-item">
            <span class="me-label">邮箱</span>
            <span class="me-value">{{ auth.user?.email || '-' }}</span>
          </div>
        </div>
        <div class="me-theme-section">
          <span>主题模式</span>
          <div class="theme-switch" :class="{ active: theme === 'dark' }" @click="toggleTheme">
            <div class="theme-switch-knob"></div>
          </div>
        </div>
        <div class="me-footer">
          <button class="logout-btn" @click="logout">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
            退出登录
          </button>
        </div>
      </div>

      <div class="sidebar-footer">
        <div class="theme-toggle-btn" @click="toggleTheme" :title="theme === 'dark' ? '白天模式' : '夜间模式'">
          <svg v-if="theme === 'dark'" viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-3.03 0-5.5-2.47-5.5-5.5 0-1.82.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z"/></svg>
          <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0 .39-.39.39-1.03 0-1.41l-1.06-1.06zm1.06-10.96c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"/></svg>
        </div>
      </div>
    </div>
`)

parts.push(`
    <div class="main-panel">
      <template v-if="selectedConv">
        <div class="chat-header">
          <div class="chat-header-left" @click="onHeaderTitleClick">
            <div class="chat-header-avatar" :style="avatarStyle(selectedConv.targetName, selectedConv.targetAvatar)">
              <span v-if="!selectedConv.targetAvatar">{{ selectedConv.targetName ? selectedConv.targetName.charAt(0).toUpperCase() : '?' }}</span>
              <img v-else :src="selectedConv.targetAvatar" />
            </div>
            <div class="chat-header-info">
              <div class="chat-header-name">{{ selectedConv.targetName }}</div>
              <div v-if="selectedConv.targetType === 1" class="chat-header-online">
                <span class="online-dot" :class="{ offline: !selectedConv.targetIsOnline }"></span>
                {{ selectedConv.targetIsOnline ? '在线' : '离线' }}
              </div>
              <div v-else class="chat-header-online">群聊</div>
            </div>
          </div>
          <div class="chat-header-actions">
            <button class="header-action-btn" @click="openGroupInfo" v-if="selectedConv.targetType === 2" title="群信息">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/></svg>
            </button>
          </div>
        </div>

        <div class="message-list" ref="msgListRef" @scroll="onMsgScroll">
          <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="{ self: msg.senderId === auth.user?.id }">
            <template v-if="msg.type === 'system' || msg.type === 'topic_change'">
              <div class="system-msg">{{ msg.content }}</div>
            </template>
            <template v-else>
              <div class="msg-avatar" :style="avatarStyle(getSenderAvatar(msg), '', getSenderAvatar(msg))">
                <img v-if="getSenderAvatar(msg) && getSenderAvatar(msg).startsWith('http')" :src="getSenderAvatar(msg)" />
                <span v-else>{{ (getSenderAvatar(msg) || '?').charAt(0).toUpperCase() }}</span>
              </div>
              <div class="msg-body">
                <div class="msg-sender" v-if="selectedConv.targetType === 2 && msg.senderId !== auth.user?.id">
                  {{ groupSenderName(msg) }}
                </div>
                <div class="msg-content" :class="{ 'msg-image': msg.msgType === 'image' || msg.content?.startsWith?.('http'), 'msg-file': msg.msgType === 'file' }">
                  <template v-if="msg.msgType === 'image' || msg.content?.startsWith?.('http')">
                    <img :src="msg.content" class="msg-image-content" @click="previewImage(msg.content)" />
                  </template>
                  <template v-else-if="msg.msgType === 'file'">
                    <div class="file-attachment">
                      <svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm-3.06 16L7.4 14.46l1.41-1.41 2.12 2.12 4.24-4.24 1.41 1.41L10.94 18zM13 9V3.5L18.5 9H13z"/></svg>
                      <div class="file-info">
                        <div class="file-name">{{ msg.fileName || '文件' }}</div>
                        <div class="file-size">{{ msg.fileSize ? formatFileSize(msg.fileSize) : '' }}</div>
                      </div>
                    </div>
                  </template>
                  <template v-else>
                    <span v-if="msg.content" v-html="renderMsgContent(msg.content)"></span>
                    <span v-else class="empty-msg">[空消息]</span>
                  </template>
                </div>
                <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
              </div>
            </template>
          </div>
          <div v-if="messages.length === 0" class="empty-hint msg-empty-hint">暂无消息，开始聊天吧</div>
        </div>

        <div class="msg-input-bar">
          <div class="input-toolbar">
            <button class="toolbar-btn" @click="toggleEmoji" title="表情">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 3c-2.33 0-4.31 1.46-5.11 3.5h10.22c-.8-2.04-2.78-3.5-5.11-3.5z"/></svg>
            </button>
            <button class="toolbar-btn" @click="onImageSelected" title="图片">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/></svg>
              <input ref="imageInput" type="file" accept="image/*" style="display:none" @change="onImageSelected" />
            </button>
            <button class="toolbar-btn" @click="onFileSelected" title="文件">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm-3.06 16L7.4 14.46l1.41-1.41 2.12 2.12 4.24-4.24 1.41 1.41L10.94 18zM13 9V3.5L18.5 9H13z"/></svg>
              <input ref="fileInput" type="file" style="display:none" @change="onFileSelected" />
            </button>
            <button class="toolbar-btn" @click="toggleAtMember" title="@提醒" v-if="selectedConv?.targetType === 2">
              <span style="font-weight:700;font-size:16px">@</span>
            </button>
          </div>
          <div v-if="showEmoji" class="emoji-picker">
            <span v-for="e in emojis" :key="e" class="emoji-item" @click="insertEmoji(e)">{{ e }}</span>
          </div>
          <div v-if="showAtMember && selectedConv?.targetType === 2" class="at-member-picker">
            <div v-for="m in groupMembers" :key="m.id" class="at-member-item" @click="selectAtMember(m)">
              <div class="contact-avatar" :style="avatarStyle(m.nickname || m.username, m.avatar)">
                <span v-if="!m.avatar">{{ (m.nickname || m.username).charAt(0).toUpperCase() }}</span>
                <img v-else :src="m.avatar" />
              </div>
              <span>{{ m.nickname || m.username }}</span>
            </div>
          </div>
          <div class="input-area">
            <div ref="inputRef" class="msg-input" contenteditable="true" @input="onInputInput" @keydown="onInputKeydown" data-placeholder="输入消息..."></div>
            <button class="send-btn" @click="sendMessage" :disabled="!currentInput.trim()">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
            </button>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="no-conv-selected">
          <div class="no-conv-content">
            <div class="no-conv-logo">
              <svg viewBox="0 0 100 100" width="64" height="64" fill="none">
                <path d="M50 8C35 8 22 18 18 32c-2 7-1 14 2 20l-8 20c-1 2 0 4 2 5l14 6c3 1 6 0 8-2l2-3c5 3 10 4 16 4 5 0 10-1 15-4l2 3c2 2 5 3 8 2l14-6c2-1 3-3 2-5l-8-20c3-6 4-13 2-20C78 18 65 8 50 8z" fill="currentColor" opacity="0.08"/>
                <path d="M50 20c-8 0-14 6-14 14 0 3 1 5 2 8l-6 15c-1 2 0 3 1 4l8 3c2 1 4 0 5-1l1-2c3 2 7 3 11 3s7-1 10-3l1 2c1 1 3 2 5 1l8-3c1-1 2-2 1-4l-6-15c1-3 2-5 2-8 0-8-6-14-14-14z" fill="#f7931e" opacity="0.6"/>
                <circle cx="50" cy="34" r="8" fill="currentColor" opacity="0.15"/>
                <path d="M46 42c0 0 2 2 4 2s4-2 4-2" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" opacity="0.3"/>
              </svg>
            </div>
            <div class="no-conv-title">NARUTO</div>
            <div class="no-conv-subtitle">选择一个会话开始聊天</div>
          </div>
        </div>
      </template>
    </div>

    <div v-if="showSearchDialog" class="search-dialog-overlay" @click.self="showSearchDialog = false">
      <div class="search-dialog">
        <div class="search-dialog-header">
          <input v-model="searchDialogQuery" placeholder="搜索聊天记录" ref="searchDialogInput" @input="onSearchDialogInput" />
          <button @click="showSearchDialog = false">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
          </button>
        </div>
        <div class="search-dialog-results">
          <div v-for="r in searchResults" :key="r.id" class="search-result-item" @click="jumpToMessage(r)">
            <div class="search-result-sender">{{ r.senderName }}</div>
            <div class="search-result-preview" v-html="highlightText(r.content || '', searchDialogQuery)"></div>
            <div class="search-result-time">{{ formatTime(r.createdAt) }}</div>
          </div>
          <div v-if="searchDialogQuery && searchResults.length === 0" class="empty-hint">无搜索结果</div>
        </div>
      </div>
    </div>

    <div v-if="showGroupInfo" class="dialog-overlay" @click.self="showGroupInfo = false">
      <div class="dialog group-info-dialog">
        <div class="dialog-header">
          <span>群信息</span>
          <button @click="showGroupInfo = false">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
          </button>
        </div>
        <div class="dialog-body">
          <div class="group-info-header">
            <div class="group-info-avatar" :style="avatarStyle(groupDetail?.name, groupDetail?.avatar)">
              <span v-if="!groupDetail?.avatar">{{ groupDetail?.name?.charAt(0).toUpperCase() || 'G' }}</span>
              <img v-else :src="groupDetail?.avatar" />
            </div>
            <div class="group-info-name">{{ groupDetail?.name }}</div>
            <div class="group-info-desc">{{ groupDetail?.description || '暂无简介' }}</div>
          </div>
          <div class="group-member-section">
            <div class="group-member-title">群成员 ({{ groupDetail?.members?.length || 0 }})</div>
            <div class="group-member-list">
              <div v-for="m in groupDetail?.members || []" :key="m.id" class="group-member-item">
                <div class="contact-avatar" :style="avatarStyle(m.nickname || m.username, m.avatar)">
                  <span v-if="!m.avatar">{{ (m.nickname || m.username).charAt(0).toUpperCase() }}</span>
                  <img v-else :src="m.avatar" />
                </div>
                <span>{{ m.nickname || m.username }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showUserProfile" class="dialog-overlay" @click.self="showUserProfile = false">
      <div class="dialog user-profile-dialog">
        <div class="dialog-header">
          <span>好友资料</span>
          <button @click="showUserProfile = false">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
          </button>
        </div>
        <div class="dialog-body">
          <div class="profile-header">
            <div class="profile-avatar" :style="avatarStyle(profileUser?.nickname || profileUser?.username, profileUser?.avatar)">
              <span v-if="!profileUser?.avatar">{{ (profileUser?.nickname || profileUser?.username || '?').charAt(0).toUpperCase() }}</span>
              <img v-else :src="profileUser?.avatar" />
            </div>
            <div class="profile-name">{{ profileUser?.nickname || profileUser?.username }}</div>
            <div class="profile-id">ID: {{ profileUser?.id }}</div>
          </div>
          <div class="profile-actions">
            <button class="profile-action-btn" @click="openFriendChat(profileUser)">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/></svg>
              发送消息
            </button>
            <button class="profile-action-btn danger" @click="deleteFriend(profileUser)">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/></svg>
              删除好友
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showSelfProfile" class="dialog-overlay" @click.self="showSelfProfile = false">
      <div class="dialog self-profile-dialog">
        <div class="dialog-header">
          <span>个人资料</span>
          <button @click="showSelfProfile = false">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>
          </button>
        </div>
        <div class="dialog-body">
          <div class="profile-header">
            <div class="profile-avatar" :style="avatarStyle(auth.user?.nickname || auth.user?.username, auth.user?.avatar)">
              <span v-if="!auth.user?.avatar">{{ (auth.user?.nickname || auth.user?.username || '?').charAt(0).toUpperCase() }}</span>
              <img v-else :src="auth.user?.avatar" />
            </div>
            <div class="profile-name">{{ auth.user?.nickname || auth.user?.username }}</div>
            <div class="profile-id">ID: {{ auth.user?.id }}</div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="previewImageUrl" class="dialog-overlay image-preview-overlay" @click.self="previewImageUrl = ''">
      <img :src="previewImageUrl" class="preview-image" />
    </div>
  </div>
</template>
`)

fs.writeFileSync(path.join(__dirname, 'src', 'views', 'ChatView.vue'), parts.join('\n'), 'utf-8')
console.log('Template written successfully')
