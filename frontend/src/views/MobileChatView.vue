<template>
<div class="m-app" :class="theme">
    <div v-if="!currentChat" class="m-list-view">
      <header class="m-header">
        <h1 class="m-header-title" @click="onHeaderTitleClick">{{ headerTitle }}</h1>
        <button class="m-header-btn" @click="showSearch = true">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
        </button>
      </header>

      <div class="m-tab-content">
        <div v-show="bottomTab === 'chats'" class="m-panel m-chats-panel">
          <div class="m-chats-list" ref="chatsListRef" @touchstart="convTouchStart" @touchend="convTouchEnd">
            <div v-if="foldedConversations.length > 0" class="m-fold-bar" @click="toggleFold">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M16.59 8.59L12 13.17 7.41 8.59 6 10l6 6 6-6z"/></svg>
              <span>{{ foldedLabel }}</span>
            </div>
            <div v-for="conv in displayedConversations" :key="conv.id" class="m-conv-item"
                 :class="{ 'm-conv-pinned': conv.pinned }"
                 @click="selectConversation(conv)">
              <div class="m-conv-avatar">
                <img v-if="conv.targetAvatar" :src="conv.targetAvatar" />
                <span v-else class="m-avatar-fallback">{{ (conv.targetName || '?')[0] }}</span>
                <span v-if="conv.targetIsOnline" class="m-online-dot"></span>
              </div>
              <div class="m-conv-info">
                <div class="m-conv-top">
                  <span class="m-conv-name">{{ conv.targetName }}</span>
                  <span class="m-conv-time">{{ convTime(conv.lastMessageAt) }}</span>
                </div>
                <div class="m-conv-bottom">
                  <span class="m-conv-preview">{{ conv.lastMessagePreview || '' }}</span>
                  <span v-if="conv.unreadCount > 0" class="m-unread-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
                </div>
              </div>
              <button v-if="convSwipedId === conv.id" class="m-conv-delete" @click.stop="deleteConversation(conv)">删除</button>
            </div>
            <div v-if="loading" class="m-loading-hint">加载中...</div>
          </div>
        </div>

        <div v-show="bottomTab === 'contacts'" class="m-panel m-contacts-panel">
          <div class="m-contacts-search">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>
            <input v-model="contactsSearch" placeholder="搜索好友" @input="onContactsSearch" />
          </div>
          <div class="m-contacts-actions">
            <div class="m-action-item" @click="openNewFriends">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              <span>新的朋友</span>
              <span v-if="friendRequests.length > 0" class="m-action-badge">{{ friendRequests.length }}</span>
            </div>
            <div class="m-action-item" @click="openCreateGroup">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 11h-4v4h-2v-4H7v-2h4V7h2v4h4v2z"/></svg>
              <span>创建群聊</span>
            </div>
            <div class="m-action-item" @click="openAddFriend">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              <span>添加好友</span>
            </div>
          </div>
          <div class="m-contacts-list" ref="contactsListRef">
            <div v-for="section in friendSections" :key="section.key" class="m-contact-section">
              <div class="m-section-header">{{ section.key }}</div>
              <div v-for="friend in section.items" :key="friend.id" class="m-contact-item" @click="openFriendChat(friend)">
                <div class="m-contact-avatar">
                  <img v-if="friend.avatar" :src="friend.avatar" />
                  <span v-else class="m-avatar-fallback">{{ (friend.displayName || friend.nickname || friend.username || '?')[0] }}</span>
                </div>
                <span class="m-contact-name">{{ friend.displayName || friend.nickname || friend.username }}</span>
              </div>
            </div>
            <div v-if="friendSections.length === 0" class="m-empty-hint">暂无好友</div>
          </div>
          <div class="m-index-bar" v-if="friendIndexKeys.length > 0">
            <div v-for="key in friendIndexKeys" :key="key" class="m-index-item" @click="scrollToFriendSection(key)">{{ key }}</div>
          </div>
        </div>

        <div v-show="bottomTab === 'me'" class="m-panel m-me-panel">
          <div class="m-me-profile" @click="showProfile = true">
            <div class="m-me-avatar">
              <img v-if="auth.user?.avatar" :src="auth.user.avatar" />
              <span v-else class="m-avatar-fallback">{{ (auth.user?.nickname || auth.user?.username || '我')[0] }}</span>
            </div>
            <div class="m-me-info">
              <div class="m-me-name">{{ auth.user?.nickname || auth.user?.username }}</div>
              <div class="m-me-desc">{{ auth.user?.username || '' }}</div>
            </div>
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor" class="m-me-arrow"><path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>
          </div>
          <div class="m-me-menu">
            <div class="m-me-menu-item" @click="showProfile = true">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              <span>个人信息</span>
            </div>
            <div class="m-me-menu-item" @click="toggleTheme">
              <svg v-if="theme === 'dark'" viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0 .39-.39.39-1.03 0-1.41l-1.06-1.06zm1.06-10.96c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"/></svg>
              <svg v-else viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-3.03 0-5.5-2.47-5.5-5.5 0-1.82.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z"/></svg>
              <span>{{ theme === 'dark' ? '浅色模式' : '深色模式' }}</span>
            </div>
            <div class="m-me-menu-item" @click="logout">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/></svg>
              <span>退出登录</span>
            </div>
          </div>
        </div>
      </div>

      <nav class="m-tabbar">
        <button class="m-tabbar-item" :class="{ active: bottomTab === 'chats' }" @click="bottomTab = 'chats'">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/></svg>
          <span class="m-tabbar-label">消息</span>
        </button>
        <button class="m-tabbar-item" :class="{ active: bottomTab === 'contacts' }" @click="bottomTab = 'contacts'">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>
          <span class="m-tabbar-label">通讯录</span>
        </button>
        <button class="m-tabbar-item" :class="{ active: bottomTab === 'me' }" @click="bottomTab = 'me'">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
          <span class="m-tabbar-label">我的</span>
        </button>
      </nav>
    </div>

    <div v-if="currentChat" class="m-chat-window">
      <div class="m-chat-header">
        <button class="m-chat-back" @click="closeChat">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </button>
        <div class="m-chat-title" @click="chatTargetType === 2 ? openGroupInfo() : (currentChat?.targetId ? showUserInfo(currentChat.targetId) : null)">
          <span>{{ chatTargetName }}</span>
          <span v-if="chatTargetType !== 2 && chatTargetOnline" class="m-chat-online"></span>
        </div>
        <button class="m-chat-more" @click="chatTargetType === 2 ? openGroupInfo() : null">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
        </button>
      </div>

      <div class="m-msg-list" ref="msgListRef" @scroll="tryAutoRead">
        <div v-for="msg in messages" :key="msg.id" class="m-msg-item" :class="{ 'm-msg-self': msg.senderId === auth.user?.id, 'm-msg-optimistic': msg._optimistic }">
          <div v-if="chatTargetType === 2 && msg.senderId !== auth.user?.id" class="m-msg-sender">{{ groupSenderName(msg) }}</div>
          <div class="m-msg-bubble-row">
            <img v-if="chatTargetType === 2 && msg.senderId !== auth.user?.id" :src="msg.senderAvatar" class="m-msg-avatar" @click="showUserInfo(msg.senderId)" />
            <div class="m-msg-bubble" :class="{ 'm-msg-bubble-img': msg.type === 'image', 'm-msg-bubble-file': msg.type === 'file' }">
              <template v-if="msg.type === 'text'">{{ msg.content }}</template>
              <img v-else-if="msg.type === 'image'" :src="msg.content" class="m-msg-image" @click="previewImage(msg.content)" />
              <div v-else-if="msg.type === 'file'" class="m-msg-file" @click="downloadFile(msg.content)">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
                <span>文件</span>
              </div>
              <div v-else-if="msg.type === 'recall'" class="m-msg-recall-notice">{{ msg.content }}</div>
            </div>
          </div>
          <div v-if="msg._optimistic" class="m-msg-status m-msg-sending">发送中...</div>
        </div>
      </div>

      <div class="m-input-bar">
        <div class="m-input-toolbar">
          <button class="m-tool-btn" @click="toggleEmoji">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/></svg>
          </button>
          <button class="m-tool-btn" @click="triggerImageSelect">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"/></svg>
          </button>
          <button class="m-tool-btn" @click="triggerFileSelect">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M6 2c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6H6zm0 18V4h7v5h5v11H6z"/></svg>
          </button>
          <button class="m-tool-btn" @click="selectAtMember">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10h5v-2h-5c-4.34 0-8-3.66-8-8s3.66-8 8-8 8 3.66 8 8v1.43c0 .79-.71 1.57-1.5 1.57s-1.5-.78-1.5-1.57V12c0-2.76-2.24-5-5-5s-5 2.24-5 5 2.24 5 5 5c1.38 0 2.64-.56 3.54-1.47.65.89 1.77 1.47 2.96 1.47 1.97 0 3.5-1.6 3.5-3.57V12c0-5.52-4.48-10-10-10zm0 13c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3z"/></svg>
          </button>
        </div>
        <div class="m-input-row">
          <input ref="msgInputRef" v-model="inputText" class="m-msg-input" placeholder="输入消息..."
                 @keyup="onInputKeyup" @keydown.enter.prevent="sendMessage" />
          <button class="m-send-btn" :disabled="!inputText.trim()" @click="sendMessage">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/></svg>
          </button>
        </div>
        <div v-if="showEmoji" class="m-emoji-panel">
          <span v-for="e in emojis" :key="e" class="m-emoji-item" @click="insertEmoji(e)">{{ e }}</span>
        </div>
      </div>
    </div>

    <div v-if="showGroupInfo" class="m-overlay" @click.self="showGroupInfo = false">
      <div class="m-dialog m-group-info-dialog">
        <div class="m-dialog-header">
          <span>群信息</span>
          <button class="m-dialog-close" @click="showGroupInfo = false"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></button>
        </div>
        <div class="m-group-info-body">
          <div class="m-group-info-avatar">
            <img v-if="groupDetail?.avatar" :src="groupDetail.avatar" />
            <span v-else class="m-avatar-fallback">{{ (groupDetail?.name || '群')[0] }}</span>
          </div>
          <div class="m-group-info-name">{{ groupDetail?.name }}</div>
          <div v-if="groupDetail?.notice" class="m-group-info-notice">{{ groupDetail.notice }}</div>
          <div class="m-group-members">
            <div v-for="m in groupDetail?.members || []" :key="m.id" class="m-group-member-item">
              <img :src="m.avatar" class="m-group-member-avatar" />
              <span class="m-group-member-name">{{ m.nickname || m.username }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showCreateGroup" class="m-overlay" @click.self="showCreateGroup = false">
      <div class="m-dialog m-create-group-dialog">
        <div class="m-dialog-header">
          <span>创建群聊</span>
          <button class="m-dialog-close" @click="showCreateGroup = false"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></button>
        </div>
        <div class="m-dialog-body">
          <input v-model="createGroupForm.name" placeholder="群名称" class="m-dialog-input" />
          <div class="m-dialog-label">选择好友</div>
          <div class="m-dialog-friend-list">
            <div v-for="f in friends" :key="f.id" class="m-dialog-friend-item"
                 :class="{ selected: createGroupForm.memberIds.includes(f.id) }"
                 @click="toggleGroupMember(f.id)">
              <img :src="f.avatar" class="m-dialog-friend-avatar" />
              <span>{{ f.displayName || f.nickname || f.username }}</span>
            </div>
          </div>
          <button class="m-dialog-btn" @click="createGroup">创建</button>
        </div>
      </div>
    </div>

    <div v-if="showInvite" class="m-overlay" @click.self="showInvite = false">
      <div class="m-dialog m-invite-dialog">
        <div class="m-dialog-header">
          <span>邀请好友</span>
          <button class="m-dialog-close" @click="showInvite = false"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></button>
        </div>
        <div class="m-dialog-body">
          <div v-for="f in friends" :key="f.id" class="m-dialog-friend-item"
               :class="{ selected: inviteMemberIds.includes(f.id) }"
               @click="toggleInviteMember(f.id)">
            <img :src="f.avatar" class="m-dialog-friend-avatar" />
            <span>{{ f.displayName || f.nickname || f.username }}</span>
          </div>
          <button class="m-dialog-btn" @click="doInvite">邀请</button>
        </div>
      </div>
    </div>

    <div v-if="showProfile" class="m-overlay" @click.self="showProfile = false">
      <div class="m-dialog m-profile-dialog">
        <div class="m-dialog-header">
          <span>个人信息</span>
          <button class="m-dialog-close" @click="showProfile = false"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></button>
        </div>
        <div class="m-dialog-body m-profile-body">
          <div class="m-profile-avatar-wrap" @click="onSelfAvatarSelected">
            <img v-if="auth.user?.avatar" :src="auth.user.avatar" class="m-profile-avatar" />
            <span v-else class="m-avatar-fallback m-profile-avatar-fallback">{{ (auth.user?.nickname || auth.user?.username || '我')[0] }}</span>
            <div class="m-profile-avatar-overlay">更换头像</div>
          </div>
          <div class="m-profile-field">
            <label>用户名</label>
            <span>{{ auth.user?.username }}</span>
          </div>
          <div class="m-profile-field">
            <label>昵称</label>
            <input v-model="profileForm.nickname" class="m-profile-input" />
          </div>
          <button class="m-dialog-btn" @click="saveProfile">保存</button>
        </div>
      </div>
    </div>

    <div v-if="showUserInfoModal" class="m-overlay" @click.self="showUserInfoModal = false">
      <div class="m-dialog m-user-info-dialog">
        <div class="m-dialog-header">
          <span>用户信息</span>
          <button class="m-dialog-close" @click="showUserInfoModal = false"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></button>
        </div>
        <div class="m-dialog-body">
          <div class="m-user-info-avatar">
            <img v-if="userInfoModal?.avatar" :src="userInfoModal.avatar" />
            <span v-else class="m-avatar-fallback">{{ (userInfoModal?.nickname || userInfoModal?.username || '?')[0] }}</span>
          </div>
          <div class="m-user-info-name">{{ userInfoModal?.nickname || userInfoModal?.username }}</div>
          <div class="m-user-info-uname">{{ userInfoModal?.username }}</div>
          <button class="m-dialog-btn" @click="openFriendChat(userInfoModal)">发送消息</button>
        </div>
      </div>
    </div>

    <div v-if="showAddFriend" class="m-overlay" @click.self="showAddFriend = false">
      <div class="m-dialog m-add-friend-dialog">
        <div class="m-dialog-header">
          <span>添加好友</span>
          <button class="m-dialog-close" @click="showAddFriend = false"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></button>
        </div>
        <div class="m-dialog-body">
          <input v-model="addFriendKeyword" placeholder="搜索用户名" class="m-dialog-input" @input="searchUsers" />
          <div v-if="searchResults.length > 0" class="m-search-results">
            <div v-for="u in searchResults" :key="u.id" class="m-search-result-item">
              <img :src="u.avatar" class="m-search-result-avatar" />
              <div class="m-search-result-info">
                <div class="m-search-result-name">{{ u.nickname || u.username }}</div>
                <div class="m-search-result-uname">{{ u.username }}</div>
              </div>
              <button class="m-small-btn" @click="sendFriendRequest(u.id)">添加</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showNewFriends" class="m-overlay" @click.self="showNewFriends = false">
      <div class="m-dialog m-new-friends-dialog">
        <div class="m-dialog-header">
          <span>好友申请</span>
          <button class="m-dialog-close" @click="showNewFriends = false"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></button>
        </div>
        <div class="m-dialog-body">
          <div v-for="req in friendRequests" :key="req.id" class="m-request-item">
            <img :src="req.avatar" class="m-request-avatar" />
            <div class="m-request-info">
              <div class="m-request-name">{{ req.nickname || req.username }}</div>
              <div class="m-request-uname">{{ req.username }}</div>
            </div>
            <div class="m-request-actions">
              <button class="m-small-btn m-accept-btn" @click="acceptRequest(req)">同意</button>
              <button class="m-small-btn m-reject-btn" @click="rejectRequest(req)">拒绝</button>
            </div>
          </div>
          <div v-if="friendRequests.length === 0" class="m-empty-hint">暂无申请</div>
        </div>
      </div>
    </div>

    <div v-if="showSearch" class="m-overlay" @click.self="showSearch = false">
      <div class="m-dialog m-search-dialog">
        <div class="m-dialog-header">
          <span>搜索</span>
          <button class="m-dialog-close" @click="showSearch = false"><svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg></button>
        </div>
        <div class="m-dialog-body">
          <input v-model="searchKeyword" placeholder="搜索会话、好友、群组" class="m-dialog-input" @input="onSearchInput" />
          <div v-if="searchResults.length > 0" class="m-search-results">
            <div v-for="item in searchResults" :key="item.id" class="m-search-result-item" @click="selectConversation(item)">
              <img :src="item.targetAvatar" class="m-search-result-avatar" />
              <div class="m-search-result-info">
                <div class="m-search-result-name">{{ item.targetName }}</div>
              </div>
            </div>
          </div>
          <div v-if="searchKeyword && searchResults.length === 0" class="m-empty-hint">无结果</div>
        </div>
      </div>
    </div>

    <input ref="imageInputRef" type="file" accept="image/*" style="display:none" @change="onImageSelected" />
    <input ref="fileInputRef" type="file" style="display:none" @change="onFileSelected" />
    <input ref="avatarInputRef" type="file" accept="image/*" style="display:none" @change="onAvatarSelected" />
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElNotification, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { addWsListener, sendChat } from '../api/ws'
import { useAuthStore } from '../stores/auth'
import { pinyin } from 'pinyin-pro'

const router = useRouter()
const auth = useAuthStore()

const theme = ref(localStorage.getItem('xr-theme') || 'dark')
function applyTheme(val) {
  document.documentElement.setAttribute('data-theme', val)
}
function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('xr-theme', theme.value)
  applyTheme(theme.value)
}

onMounted(() => {
  applyTheme(theme.value)
  loadAll()
  const unsub = addWsListener(onWsMessage)
  onBeforeUnmount(() => unsub())
})

const bottomTab = ref('chats')
const loading = ref(false)
const conversations = ref([])
const friends = ref([])
const groups = ref([])
const friendRequests = ref([])
const blockedUserIds = ref([])
const blockedByUserIds = ref([])

const headerTitle = computed(() => {
  const titles = { chats: '消息', contacts: '通讯录', me: '我的' }
  return titles[bottomTab.value] || '消息'
})

const folded = ref(true)
const foldedConversations = computed(() => conversations.value.filter(c => c.folded))
const activeConversations = computed(() => conversations.value.filter(c => !c.folded))
const displayedConversations = computed(() => folded.value ? activeConversations.value : conversations.value)
const foldedLabel = computed(() => folded.value
  ? '折叠的会话 (' + foldedConversations.value.length + ')'
  : '收起')

function toggleFold() { folded.value = !folded.value }

function mapMessage(m) {
  return {
    id: m.id,
    senderId: m.fromUserId,
    senderName: m.fromNickname,
    senderAvatar: m.fromUserAvatar,
    chatType: m.chatType,
    type: ['', 'text', 'image', 'file'][m.msgType || 1] || 'text',
    content: m.content,
    createdAt: m.createdAt,
    fileUrl: m.fileUrl
  }
}
function avatarBg(name) {
  var colors = ['#f7931e', '#e67e22', '#d35400', '#e74c3c', '#9b59b6', '#3498db', '#1abc9c', '#2ecc71']
  var hash = 0
  for (var i = 0; i < (name || '').length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

function notify(msg, type) {
  ElNotification({ title: '', message: msg, type: type, duration: type === 'success' ? 2000 : 3000, offset: 60, customClass: 'xr-notify' })
}

function getErrorMsg(e) {
  return (e && e.response && e.response.data && e.response.data.message) || (e && e.message) || '操作失败'
}

function convTime(t) {
  if (!t) return ''
  var d = new Date(t)
  var now = new Date()
  function pad(n) { return String(n).padStart(2, '0') }
  if (d.toDateString() === now.toDateString()) return pad(d.getHours()) + ':' + pad(d.getMinutes())
  var yesterday = new Date(now); yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return '昨天'
  if (d.getFullYear() === now.getFullYear()) return (d.getMonth() + 1) + '/' + d.getDate()
  return d.getFullYear() + '/' + (d.getMonth() + 1) + '/' + d.getDate()
}

async function loadAll() {
  loading.value = true
  await Promise.all([
    loadConversations(),
    loadFriends(),
    loadGroups(),
    loadRequests(),
    loadBlocked()
  ])
  loading.value = false
}

async function loadConversations() {
  try { conversations.value = (await http.get('/conversations')) || [] } catch (e) { console.error(e) }
}

async function loadFriends() {
  try {
    var data = await http.get('/friends') || []
    var blockedSet = new Set(blockedByUserIds.value || [])
    friends.value = data.filter(function(f) { return !blockedSet.has(f.id) })
  } catch (e) { console.error(e) }
}

async function loadGroups() {
  try { groups.value = await http.get('/groups') || [] } catch (e) { console.error(e) }
}

async function loadRequests() {
  try { friendRequests.value = await http.get('/friends/requests') || [] } catch (e) { console.error(e) }
}

async function loadBlocked() {
  try {
    blockedUserIds.value = await http.get('/friends/blocked') || []
    blockedByUserIds.value = await http.get('/friends/blocked-by') || []
  } catch (e) { console.error(e) }
}

function searchConversations(keyword) {
  return conversations.value.filter(function(c) {
    return c.targetName && c.targetName.toLowerCase().includes(keyword.toLowerCase())
  })
}

function onWsMessage(packet) {
  if (packet.type === 'NEW_MESSAGE') {
    var msg = mapMessage(packet.data)
    var raw = packet.data
    var isMyChat = false
    if (currentChat.value) {
      if (chatTargetType.value === 2) {
        isMyChat = raw.chatType === 2 && chatTargetId.value === raw.groupId
      } else {
        isMyChat = raw.chatType === 1 && (chatTargetId.value === raw.fromUserId || chatTargetId.value === raw.toUserId)
      }
    }
    if (isMyChat) {
      messages.value.push(msg)
      nextTick(function() { scrollBottom() })
      tryMarkRead()
    }
    loadConversations()
  } else if (packet.type === 'FRIEND_REQUEST') {
    loadRequests()
  } else if (packet.type === 'FRIEND_ACCEPTED') {
    notify('好友请求已通过', 'success')
    loadFriends()
    loadConversations()
  } else if (packet.type === 'GROUP_UPDATED') {
    loadGroups()
  }
}

var contactsSearch = ref('')
function onContactsSearch() {}

function friendDisplayName(f) {
  return f.displayName || f.nickname || f.username
}

function indexKeyOf(name) {
  if (!name) return '#'
  var first = name[0]
  if (/[a-zA-Z]/.test(first)) return first.toUpperCase()
  try {
    var py = pinyin(first, { pattern: 'first', toneType: 'none' })
    if (py && py[0]) return py[0][0].toUpperCase()
  } catch (e) {}
  var code = first.charCodeAt(0)
  if (code >= 0x4e00 && code <= 0x9fff) return '#'
  return '#'
}

var friendSections = computed(function() {
  var list = contactsSearch.value
    ? friends.value.filter(function(f) { return (f.displayName || f.nickname || f.username || '').includes(contactsSearch.value) })
    : friends.value
  var map = {}
  list.forEach(function(f) {
    var key = indexKeyOf(f.displayName || f.nickname || f.username)
    if (!map[key]) map[key] = []
    map[key].push(f)
  })
  var sorted = Object.keys(map).sort(function(a, b) {
    if (a === '#') return 1
    if (b === '#') return -1
    return a.localeCompare(b)
  })
  return sorted.map(function(key) { return { key: key, items: map[key] } })
})

var friendIndexKeys = computed(function() { return friendSections.value.map(function(s) { return s.key }) })

function scrollToFriendSection(key) {
  var el = document.querySelector('.m-section-header')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

var currentChat = ref(null)
var chatTargetType = ref(0)
var chatTargetId = ref(0)
var chatTargetName = ref('')
var chatTargetOnline = ref(false)
var messages = ref([])
var inputText = ref('')
var showEmoji = ref(false)
var msgListRef = ref(null)
var msgInputRef = ref(null)
var chatsListRef = ref(null)
var contactsListRef = ref(null)
var imageInputRef = ref(null)
var fileInputRef = ref(null)
var avatarInputRef = ref(null)

var emojis = ['😀','😁','😂','🤣','😃','😄','😅','😆','😉','😊','😋','😎','😍','😘','🥰','😗','😙','😚','🤗','🤩','🤔','🤨','😐','😑','😶','🙄','😏','😣','😥','😮','🤐','😯','😪','😫','😴','😌','😛','😜','😝','🤤','😒','😓','😔','😕','🙃','🤑','😲','☹️','🙁','😖','😞','😟','😤','😢','😭','😦','😧','😨','😩','🤯','😬','😰','😱','🥵','🥶','😳','🤪','😵','😡','😠','🤬','👍','👎','👊','✊','🤛','🤜','👏','🙌','👐','🤲','🤝','🙏','✌️','🤟','🤘','👌','💪','❤️','🧡','💛','💚','💙','💜','🖤','💕','💞','💗','💖','✨','🌟','⭐','🔥','💯','🎉']

function toggleEmoji() { showEmoji.value = !showEmoji.value }
function insertEmoji(e) {
  inputText.value += e
  showEmoji.value = false
  nextTick(function() { if (msgInputRef.value) msgInputRef.value.focus() })
}

function triggerImageSelect() { if (imageInputRef.value) imageInputRef.value.click() }
function triggerFileSelect() { if (fileInputRef.value) fileInputRef.value.click() }

async function onImageSelected(e) {
  var file = e.target.files[0]
  if (!file) return
  try {
    var fd = new FormData()
    fd.append('file', file)
    var url = await http.post('/files/upload', fd)
    await sendMessageContent('image', url)
  } catch (err) {
    notify(getErrorMsg(err), 'error')
  }
  e.target.value = ''
}

async function onFileSelected(e) {
  var file = e.target.files[0]
  if (!file) return
  try {
    var fd = new FormData()
    fd.append('file', file)
    var url = await http.post('/files/upload', fd)
    await sendMessageContent('file', url)
  } catch (err) {
    notify(getErrorMsg(err), 'error')
  }
  e.target.value = ''
}

async function onAvatarSelected(e) {
  var file = e.target.files[0]
  if (!file) return
  try {
    var fd = new FormData()
    fd.append('file', file)
    await http.post('/users/avatar', fd)
    notify('头像已更新', 'success')
    var userData = await http.get('/users/me')
    auth.user = userData
    localStorage.setItem('user', JSON.stringify(userData))
  } catch (err) {
    notify(getErrorMsg(err), 'error')
  }
  e.target.value = ''
}

function onSelfAvatarSelected() { if (avatarInputRef.value) avatarInputRef.value.click() }

function onInputKeyup(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

async function sendMessage() {
  var text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  showEmoji.value = false
  await sendMessageContent('text', text)
}

async function sendMessageContent(type, content) {
  if (!currentChat.value) return
  var isGroup = chatTargetType.value === 2
  var payload = { chatType: chatTargetType.value, msgType: 1, content: content }
  if (isGroup) payload.groupId = chatTargetId.value
  else payload.toUserId = chatTargetId.value
  var optimistic = { id: Date.now(), senderId: auth.user ? auth.user.id : null, type: type, content: content, _optimistic: true, createdAt: new Date().toISOString() }
  messages.value.push(optimistic)
  nextTick(function() { scrollBottom() })
  try {
    var real = await http.post('/messages', payload)
    real = mapMessage(real)
    var idx = messages.value.indexOf(optimistic)
    if (idx !== -1) messages.value.splice(idx, 1, real)
    loadConversations()
  } catch (err) {
    var idx = messages.value.indexOf(optimistic)
    if (idx !== -1) messages.value.splice(idx, 1)
    notify(getErrorMsg(err), 'error')
  }
}

async function loadMessages() {
  if (!currentChat.value) return
  try {
    var endpoint = chatTargetType.value === 2 ? '/messages/group/' + chatTargetId.value : '/messages/single/' + chatTargetId.value
    var data = await http.get(endpoint)
    messages.value = (data || []).map(mapMessage)
    nextTick(function() { scrollBottom() })
  } catch (e) {
    notify(getErrorMsg(e), 'error')
  }
}

function scrollBottom() {
  nextTick(function() {
    if (msgListRef.value) {
      msgListRef.value.scrollTop = msgListRef.value.scrollHeight
    }
  })
}

function tryAutoRead() { tryMarkRead() }

async function tryMarkRead() {
  if (!currentChat.value || messages.value.length === 0) return
  var last = messages.value[messages.value.length - 1]
  if (last._optimistic) return
  try {
    await http.post('/messages/' + last.id + '/read')
  } catch (e) {}
}

function groupSenderName(msg) {
  return msg.senderNickname || msg.senderName || '未知'
}

function selectConversation(conv) {
  if (!conv) return
  chatTargetType.value = conv.targetType
  chatTargetId.value = conv.targetId
  chatTargetName.value = conv.targetName
  chatTargetOnline.value = conv.targetIsOnline || false
  currentChat.value = conv
  loadMessages()
  showSearch.value = false
}

function openFriendChat(friend) {
  if (!friend) return
  chatTargetType.value = 1
  chatTargetId.value = friend.id
  chatTargetName.value = friend.displayName || friend.nickname || friend.username
  chatTargetOnline.value = friend.online || false
  currentChat.value = { targetType: 1, targetId: friend.id, targetName: chatTargetName.value }
  showUserInfoModal.value = false
  loadMessages()
}

function openGroupChat(group) {
  if (!group) return
  chatTargetType.value = 2
  chatTargetId.value = group.id
  chatTargetName.value = group.name || group.targetName
  currentChat.value = { targetType: 2, targetId: group.id, targetName: chatTargetName.value }
  loadMessages()
}

function closeChat() {
  currentChat.value = null
  messages.value = []
  inputText.value = ''
  showEmoji.value = false
}

function onHeaderTitleClick() {
  scrollBottom()
}

var convSwipedId = ref(null)
function convTouchStart(e) {
  var item = e.target.closest('.m-conv-item')
  if (!item) return
  item._touchStartX = e.touches[0].clientX
  convSwipedId.value = null
}
function convTouchEnd(e) {
  var item = e.target.closest('.m-conv-item')
  if (!item) return
  var touch = e.changedTouches[0]
  var dx = touch.clientX - (item._touchStartX || 0)
  if (dx < -40) {
    var children = Array.from(item.parentNode.children)
    var idx = children.indexOf(item)
    var convIdx = idx - (foldedConversations.value.length > 0 ? 1 : 0)
    var conv = displayedConversations.value[convIdx]
    if (conv) convSwipedId.value = conv.id
  } else {
    convSwipedId.value = null
  }
}

function msgTouchStart() {}
function msgTouchEnd() {}

function selectAtMember() {
  inputText.value += '@'
  nextTick(function() { if (msgInputRef.value) msgInputRef.value.focus() })
}

function previewImage(url) { window.open(url, '_blank') }
function downloadFile(url) { window.open(url, '_blank') }

async function deleteConversation(conv) {
  try {
    await ElMessageBox.confirm('确定删除该会话？', '提示', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    await http.delete('/conversations/' + conv.id)
    notify('已删除', 'success')
    loadConversations()
    convSwipedId.value = null
  } catch (e) {
    if (e !== 'cancel') notify(getErrorMsg(e), 'error')
  }
}

var showGroupInfo = ref(false)
var groupDetail = ref(null)
async function openGroupInfo() {
  if (chatTargetType.value !== 2) return
  showGroupInfo.value = true
  try {
    groupDetail.value = await http.get('/groups/' + chatTargetId.value)
  } catch (e) {
    notify(getErrorMsg(e), 'error')
  }
}

var showCreateGroup = ref(false)
var createGroupForm = ref({ name: '', memberIds: [] })
function openCreateGroup() {
  showCreateGroup.value = true
  createGroupForm.value = { name: '', memberIds: [] }
}

function toggleGroupMember(id) {
  var idx = createGroupForm.value.memberIds.indexOf(id)
  if (idx === -1) createGroupForm.value.memberIds.push(id)
  else createGroupForm.value.memberIds.splice(idx, 1)
}

async function createGroup() {
  if (!createGroupForm.value.name.trim()) { notify('请输入群名称', 'error'); return }
  if (createGroupForm.value.memberIds.length === 0) { notify('请选择好友', 'error'); return }
  try {
    await http.post('/groups', createGroupForm.value)
    notify('创建成功', 'success')
    showCreateGroup.value = false
    loadGroups()
  } catch (e) { notify(getErrorMsg(e), 'error') }
}

var showInvite = ref(false)
var inviteGroupId = ref(null)
var inviteMemberIds = ref([])
function openInvite(groupId) {
  showInvite.value = true
  inviteGroupId.value = groupId
  inviteMemberIds.value = []
}

function toggleInviteMember(id) {
  var idx = inviteMemberIds.value.indexOf(id)
  if (idx === -1) inviteMemberIds.value.push(id)
  else inviteMemberIds.value.splice(idx, 1)
}

async function doInvite() {
  if (inviteMemberIds.value.length === 0) { notify('请选择好友', 'error'); return }
  try {
    await http.post('/groups/' + inviteGroupId.value + '/members', inviteMemberIds.value)
    notify('邀请成功', 'success')
    showInvite.value = false
  } catch (e) { notify(getErrorMsg(e), 'error') }
}

var showProfile = ref(false)
var profileForm = ref({ nickname: '' })

async function saveProfile() {
  try {
    var data = await http.put('/users/profile', profileForm.value)
    auth.user = data
    localStorage.setItem('user', JSON.stringify(data))
    notify('保存成功', 'success')
    showProfile.value = false
  } catch (e) { notify(getErrorMsg(e), 'error') }
}

var showUserInfoModal = ref(false)
var userInfoModal = ref(null)
async function showUserInfo(userId) {
  try {
    var data = await http.get('/users/' + userId)
    userInfoModal.value = data
    showUserInfoModal.value = true
  } catch (e) { notify(getErrorMsg(e), 'error') }
}

var showAddFriend = ref(false)
var addFriendKeyword = ref('')
var searchResults = ref([])
function openAddFriend() {
  showAddFriend.value = true
  addFriendKeyword.value = ''
  searchResults.value = []
}

async function searchUsers() {
  if (!addFriendKeyword.value.trim()) { searchResults.value = []; return }
  try {
    var users = await http.get('/users/search?keyword=' + encodeURIComponent(addFriendKeyword.value))
    searchResults.value = (users || []).filter(function(u) { return u.id !== (auth.user ? auth.user.id : null) })
  } catch (e) {}
}

async function sendFriendRequest(friendId) {
  try {
    await http.post('/friends/request', { friendId: friendId })
    notify('好友申请已发送', 'success')
  } catch (e) { notify(getErrorMsg(e), 'error') }
}

var showNewFriends = ref(false)
function openNewFriends() {
  showNewFriends.value = true
  loadRequests()
}

async function acceptRequest(req) {
  try {
    await http.post('/friends/accept/' + req.id)
    notify('已同意', 'success')
    loadRequests()
    loadFriends()
  } catch (e) { notify(getErrorMsg(e), 'error') }
}

async function rejectRequest(req) {
  try {
    await http.post('/friends/reject/' + req.id)
    notify('已拒绝', 'success')
    loadRequests()
  } catch (e) { notify(getErrorMsg(e), 'error') }
}

async function deleteFriend(friend) {
  try {
    await ElMessageBox.confirm('确定删除该好友？', '提示', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    await http.delete('/friends/' + friend.id)
    notify('已删除', 'success')
    loadFriends()
  } catch (e) { if (e !== 'cancel') notify(getErrorMsg(e), 'error') }
}

async function toggleBlockFriend(friend) {
  var isBlocked = blockedUserIds.value.includes(friend.id)
  try {
    if (isBlocked) await http.post('/friends/unblock/' + friend.id)
    else await http.post('/friends/block/' + friend.id)
    notify(isBlocked ? '已解除拉黑' : '已拉黑', 'success')
    loadBlocked()
  } catch (e) { notify(getErrorMsg(e), 'error') }
}

var showSearch = ref(false)
var searchKeyword = ref('')
function onSearchInput() {
  if (!searchKeyword.value.trim()) { searchResults.value = []; return }
  searchResults.value = searchConversations(searchKeyword.value)
}

async function logout() {
  try {
    await ElMessageBox.confirm('确定退出登录？', '提示', { confirmButtonText: '退出', cancelButtonText: '取消', type: 'warning' })
    auth.logout()
    router.push('/login')
  } catch (e) {}
}
</script>

<style scoped>
.m-app {
  height: var(--app-height, 100vh); height: 100dvh;
  display: flex; flex-direction: column; overflow: hidden;
  background: #0f1219; color: #e8e8ea;
  user-select: none; -webkit-user-select: none;
  --accent: #f7931e; --accent-dark: #e67e22;
  --bg-primary: #0f1219; --bg-secondary: #151a24; --bg-tertiary: #1e2432;
  --bg-hover: rgba(255,255,255,0.06);
  --text-primary: #e8e8ea; --text-secondary: #9ca3af; --text-muted: #6b7280;
  --border: rgba(255,255,255,0.08);
}
.m-app.light {
  --bg-primary: #f0f2f5; --bg-secondary: #ffffff; --bg-tertiary: #e8ecf0;
  --bg-hover: rgba(0,0,0,0.04);
  --text-primary: #1a1a2e; --text-secondary: #6b7280; --text-muted: #9ca3af;
  --border: rgba(0,0,0,0.08);
  background: var(--bg-primary); color: var(--text-primary);
}

.m-list-view { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.m-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: max(12px, env(safe-area-inset-top, 12px)) 16px 8px;
  background: var(--bg-secondary); border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.m-header-title { font-size: 20px; font-weight: 600; cursor: pointer; -webkit-tap-highlight-color: transparent; }
.m-header-btn {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: var(--bg-tertiary); color: var(--text-primary);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
}

.m-tab-content { flex: 1; overflow: hidden; position: relative; }
.m-panel { height: 100%; overflow-y: auto; overflow-x: hidden; -webkit-overflow-scrolling: touch; }

.m-chats-list { padding: 4px 0; }
.m-fold-bar {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 16px; font-size: 13px; color: var(--text-muted);
  cursor: pointer; -webkit-tap-highlight-color: transparent;
}
.m-conv-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; cursor: pointer; position: relative;
  transition: background 0.15s; -webkit-tap-highlight-color: transparent;
}
.m-conv-item:active { background: var(--bg-hover); }
.m-conv-pinned { background: rgba(247,147,30,0.04); }
.m-conv-avatar { width: 48px; height: 48px; border-radius: 50%; overflow: hidden; flex-shrink: 0; position: relative; }
.m-conv-avatar img { width: 100%; height: 100%; object-fit: cover; }
.m-online-dot {
  position: absolute; bottom: 1px; right: 1px;
  width: 10px; height: 10px; border-radius: 50%;
  background: #07c160; border: 2px solid var(--bg-secondary);
}
.m-conv-info { flex: 1; min-width: 0; }
.m-conv-top { display: flex; justify-content: space-between; align-items: center; }
.m-conv-name { font-size: 15px; font-weight: 500; }
.m-conv-time { font-size: 11px; color: var(--text-muted); flex-shrink: 0; }
.m-conv-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 2px; }
.m-conv-preview { font-size: 13px; color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.m-unread-badge {
  background: var(--accent); color: #fff; font-size: 11px; font-weight: 600;
  min-width: 18px; height: 18px; border-radius: 9px;
  display: flex; align-items: center; justify-content: center;
  padding: 0 5px; flex-shrink: 0;
}
.m-conv-delete {
  position: absolute; right: 12px; top: 50%; transform: translateY(-50%);
  background: #e74c3c; color: #fff; border: none; border-radius: 6px;
  padding: 6px 12px; font-size: 13px; cursor: pointer;
}

.m-contacts-panel { position: relative; }
.m-contacts-search {
  display: flex; align-items: center; gap: 8px;
  margin: 10px 16px; padding: 8px 12px;
  background: var(--bg-tertiary); border-radius: 10px;
}
.m-contacts-search input {
  flex: 1; border: none; outline: none; font-size: 14px;
  background: transparent; color: var(--text-primary);
}
.m-contacts-search input::placeholder { color: var(--text-muted); }
.m-contacts-actions { display: flex; padding: 4px 16px 12px; gap: 8px; }
.m-action-item {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 12px 0; border-radius: 12px; background: var(--bg-secondary);
  cursor: pointer; position: relative; font-size: 12px; color: var(--text-secondary);
  -webkit-tap-highlight-color: transparent;
}
.m-action-item:active { background: var(--bg-hover); }
.m-action-badge {
  position: absolute; top: 4px; right: 12px;
  background: var(--accent); color: #fff; font-size: 10px; font-weight: 600;
  min-width: 16px; height: 16px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; padding: 0 4px;
}

.m-section-header {
  padding: 6px 16px; font-size: 12px; color: var(--text-muted);
  background: var(--bg-primary); font-weight: 600;
}
.m-contact-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 16px; cursor: pointer; -webkit-tap-highlight-color: transparent;
}
.m-contact-item:active { background: var(--bg-hover); }
.m-contact-avatar { width: 40px; height: 40px; border-radius: 50%; overflow: hidden; flex-shrink: 0; }
.m-contact-avatar img { width: 100%; height: 100%; object-fit: cover; }
.m-contact-name { font-size: 15px; }

.m-index-bar {
  position: fixed; right: 2px; top: 50%; transform: translateY(-50%);
  display: flex; flex-direction: column; align-items: center;
  gap: 0; z-index: 10; pointer-events: none;
}
.m-index-item {
  width: 20px; height: 16px; font-size: 10px; font-weight: 500;
  color: var(--accent); display: flex; align-items: center; justify-content: center;
  cursor: pointer; pointer-events: auto; -webkit-tap-highlight-color: transparent;
}

.m-me-panel { padding: 0; }
.m-me-profile {
  display: flex; align-items: center; gap: 14px;
  padding: 20px 16px; cursor: pointer; -webkit-tap-highlight-color: transparent;
}
.m-me-profile:active { background: var(--bg-hover); }
.m-me-avatar { width: 56px; height: 56px; border-radius: 50%; overflow: hidden; flex-shrink: 0; }
.m-me-avatar img { width: 100%; height: 100%; object-fit: cover; }
.m-me-info { flex: 1; }
.m-me-name { font-size: 17px; font-weight: 600; }
.m-me-desc { font-size: 13px; color: var(--text-muted); margin-top: 2px; }
.m-me-arrow { color: var(--text-muted); }
.m-me-menu { margin-top: 12px; border-top: 1px solid var(--border); }
.m-me-menu-item {
  display: flex; align-items: center; gap: 14px;
  padding: 16px; cursor: pointer; -webkit-tap-highlight-color: transparent;
}
.m-me-menu-item:active { background: var(--bg-hover); }
.m-me-menu-item span { font-size: 15px; }

.m-tabbar {
  display: flex; border-top: 1px solid var(--border);
  background: var(--bg-secondary); flex-shrink: 0;
  padding-bottom: env(safe-area-inset-bottom, 0);
}
.m-tabbar-item {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  padding: 6px 0 4px; gap: 2px; border: none;
  background: transparent; color: var(--text-muted);
  cursor: pointer; -webkit-tap-highlight-color: transparent;
}
.m-tabbar-item.active { color: var(--accent); }
.m-tabbar-label { font-size: 11px; }

.m-chat-window {
  flex: 1; display: flex; flex-direction: column; overflow: hidden;
  background: var(--bg-primary);
}
.m-chat-header {
  display: flex; align-items: center; gap: 8px;
  padding: max(12px, env(safe-area-inset-top, 12px)) 12px 8px;
  background: var(--bg-secondary); border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.m-chat-back {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: transparent; color: var(--text-primary);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
}
.m-chat-title { flex: 1; font-size: 16px; font-weight: 600; display: flex; align-items: center; gap: 6px; cursor: pointer; }
.m-chat-online { width: 8px; height: 8px; border-radius: 50%; background: #07c160; }
.m-chat-more {
  width: 36px; height: 36px; border-radius: 50%; border: none;
  background: transparent; color: var(--text-primary);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
}

.m-msg-list {
  flex: 1; overflow-y: auto; overflow-x: hidden; padding: 12px 12px 8px;
  display: flex; flex-direction: column; gap: 8px;
  -webkit-overflow-scrolling: touch;
}
.m-msg-item { display: flex; flex-direction: column; gap: 2px; }
.m-msg-self { align-items: flex-end; }
.m-msg-sender { font-size: 11px; color: var(--text-muted); margin-left: 44px; margin-bottom: 2px; }
.m-msg-bubble-row { display: flex; align-items: flex-end; gap: 6px; max-width: 85%; }
.m-msg-self .m-msg-bubble-row { flex-direction: row-reverse; }
.m-msg-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; flex-shrink: 0; cursor: pointer; }
.m-msg-bubble {
  padding: 10px 14px; border-radius: 18px;
  font-size: 15px; line-height: 1.45; word-break: break-word;
  background: var(--bg-tertiary); color: var(--text-primary);
}
.m-msg-self .m-msg-bubble { background: var(--accent); color: #fff; border-bottom-right-radius: 6px; }
.m-msg-self .m-msg-bubble.m-msg-bubble-img { background: transparent; }
.m-msg-bubble-img { padding: 4px; background: transparent !important; }
.m-msg-bubble-file { padding: 8px 14px; }
.m-msg-image { max-width: 200px; max-height: 240px; border-radius: 12px; cursor: pointer; display: block; }
.m-msg-file { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.m-msg-recall-notice { font-size: 12px; color: var(--text-muted); text-align: center; padding: 4px 12px; }
.m-msg-optimistic { opacity: 0.7; }
.m-msg-status { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
.m-msg-sending { color: var(--accent); }

.m-input-bar {
  flex-shrink: 0; border-top: 1px solid var(--border);
  background: var(--bg-secondary);
  padding-bottom: env(safe-area-inset-bottom, 0);
}
.m-input-toolbar { display: flex; align-items: center; gap: 4px; padding: 6px 12px 4px; }
.m-tool-btn {
  width: 34px; height: 34px; border-radius: 50%; border: none;
  background: transparent; color: var(--text-secondary);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
}
.m-tool-btn:active { background: var(--bg-hover); }
.m-input-row { display: flex; align-items: center; gap: 8px; padding: 0 12px 8px; }
.m-msg-input {
  flex: 1; border: none; outline: none; padding: 10px 14px;
  border-radius: 22px; font-size: 15px; line-height: 1.4;
  background: var(--bg-tertiary); color: var(--text-primary);
  min-height: 40px; max-height: 120px;
}
.m-msg-input::placeholder { color: var(--text-muted); }
.m-send-btn {
  width: 40px; height: 40px; border-radius: 50%; border: none;
  background: var(--accent); color: #fff;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; flex-shrink: 0; -webkit-tap-highlight-color: transparent;
}
.m-send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.m-send-btn:active:not(:disabled) { transform: scale(0.92); }

.m-emoji-panel {
  display: flex; flex-wrap: wrap; gap: 6px; padding: 10px 12px 8px;
  border-top: 1px solid var(--border); max-height: 180px; overflow-y: auto;
}
.m-emoji-item { font-size: 26px; cursor: pointer; transition: transform 0.1s; -webkit-tap-highlight-color: transparent; }
.m-emoji-item:active { transform: scale(1.2); }

.m-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.6);
  display: flex; align-items: flex-end; justify-content: center;
  z-index: 1000; animation: mFadeIn 0.2s;
}
@keyframes mFadeIn { from { opacity: 0; } to { opacity: 1; } }

.m-dialog {
  width: 100%; max-height: 80vh; overflow-y: auto;
  background: var(--bg-secondary); border-radius: 20px 20px 0 0;
  animation: mSlideUp 0.25s ease-out;
}
@keyframes mSlideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }

.m-dialog-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px 12px; font-size: 17px; font-weight: 600;
  border-bottom: 1px solid var(--border);
}
.m-dialog-close {
  width: 32px; height: 32px; border-radius: 50%; border: none;
  background: var(--bg-tertiary); color: var(--text-muted);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
}
.m-dialog-body { padding: 16px 20px 24px; }
.m-dialog-input {
  width: 100%; padding: 12px 16px; border-radius: 12px; border: 1px solid var(--border);
  font-size: 15px; background: var(--bg-tertiary); color: var(--text-primary);
  outline: none;
}
.m-dialog-input:focus { border-color: var(--accent); }
.m-dialog-input::placeholder { color: var(--text-muted); }
.m-dialog-btn {
  width: 100%; padding: 12px; border-radius: 12px; border: none;
  background: var(--accent); color: #fff; font-size: 16px; font-weight: 600;
  cursor: pointer; margin-top: 16px; -webkit-tap-highlight-color: transparent;
}
.m-dialog-btn:active { opacity: 0.85; }
.m-dialog-label { font-size: 14px; font-weight: 500; margin: 12px 0 8px; color: var(--text-secondary); }
.m-dialog-friend-list { max-height: 240px; overflow-y: auto; }
.m-dialog-friend-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 0;
  cursor: pointer; -webkit-tap-highlight-color: transparent;
}
.m-dialog-friend-item.selected { color: var(--accent); }
.m-dialog-friend-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; }

.m-group-info-body { padding: 20px; display: flex; flex-direction: column; align-items: center; }
.m-group-info-avatar { width: 64px; height: 64px; border-radius: 50%; overflow: hidden; margin-bottom: 12px; }
.m-group-info-avatar img { width: 100%; height: 100%; object-fit: cover; }
.m-group-info-name { font-size: 17px; font-weight: 600; margin-bottom: 8px; }
.m-group-info-notice { font-size: 13px; color: var(--text-secondary); padding: 10px 16px; background: var(--bg-tertiary); border-radius: 10px; width: 100%; margin-bottom: 16px; }
.m-group-members { display: flex; flex-wrap: wrap; gap: 12px; width: 100%; }
.m-group-member-item { display: flex; flex-direction: column; align-items: center; gap: 4px; width: 60px; }
.m-group-member-avatar { width: 44px; height: 44px; border-radius: 50%; object-fit: cover; }
.m-group-member-name { font-size: 11px; color: var(--text-secondary); text-align: center; overflow: hidden; text-overflow: ellipsis; width: 100%; }

.m-avatar-fallback { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: var(--accent); color: #fff; font-weight: 600; font-size: 18px; }

.m-profile-body { display: flex; flex-direction: column; align-items: center; }
.m-profile-avatar-wrap { width: 80px; height: 80px; border-radius: 50%; overflow: hidden; position: relative; cursor: pointer; margin-bottom: 20px; }
.m-profile-avatar { width: 100%; height: 100%; object-fit: cover; }
.m-profile-avatar-fallback { font-size: 28px; }
.m-profile-avatar-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; color: #fff; font-size: 12px; opacity: 0; transition: opacity 0.2s; }
.m-profile-avatar-wrap:hover .m-profile-avatar-overlay { opacity: 1; }
.m-profile-field { width: 100%; display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--border); }
.m-profile-field label { font-size: 14px; color: var(--text-muted); width: 60px; flex-shrink: 0; }
.m-profile-field span { font-size: 15px; }
.m-profile-input { flex: 1; border: none; outline: none; font-size: 15px; background: transparent; color: var(--text-primary); }

.m-user-info-avatar { width: 72px; height: 72px; border-radius: 50%; overflow: hidden; margin-bottom: 12px; }
.m-user-info-avatar img { width: 100%; height: 100%; object-fit: cover; }
.m-user-info-name { font-size: 18px; font-weight: 600; }
.m-user-info-uname { font-size: 13px; color: var(--text-muted); margin-top: 2px; }

.m-search-result-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; cursor: pointer; -webkit-tap-highlight-color: transparent; }
.m-search-result-item:active { background: var(--bg-hover); }
.m-search-result-avatar { width: 36px; height: 36px; border-radius: 50%; object-fit: cover; }
.m-search-result-info { flex: 1; }
.m-search-result-name { font-size: 15px; }
.m-search-result-uname { font-size: 12px; color: var(--text-muted); }

.m-small-btn { padding: 6px 14px; border-radius: 8px; border: none; background: var(--accent); color: #fff; font-size: 13px; font-weight: 500; cursor: pointer; flex-shrink: 0; -webkit-tap-highlight-color: transparent; }
.m-accept-btn { background: var(--accent); }
.m-reject-btn { background: #6b7280; }

.m-request-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid var(--border); }
.m-request-avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; }
.m-request-info { flex: 1; }
.m-request-name { font-size: 15px; }
.m-request-uname { font-size: 12px; color: var(--text-muted); }
.m-request-actions { display: flex; gap: 6px; }

.m-empty-hint { text-align: center; padding: 40px 0; color: var(--text-muted); font-size: 14px; }
.m-loading-hint { text-align: center; padding: 16px 0; color: var(--text-muted); font-size: 13px; }
</style>
