CREATE DATABASE IF NOT EXISTS xueren DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xueren;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    last_online_at DATETIME COMMENT '最后在线时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 好友关系表（含申请、备注、拉黑）
-- status: 0=待确认 1=已是好友 2=已拒绝 3=已拉黑
CREATE TABLE IF NOT EXISTS friend (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '关系所属用户',
    friend_id BIGINT NOT NULL COMMENT '对方用户',
    requester_id BIGINT NOT NULL COMMENT '发起申请的用户',
    status TINYINT DEFAULT 0,
    remark VARCHAR(50) COMMENT '好友备注名',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_friend_status (friend_id, status)
);

-- 群组表
CREATE TABLE IF NOT EXISTS `group` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_code VARCHAR(10) COMMENT '群号（6位数字）',
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(255),
    owner_id BIGINT NOT NULL COMMENT '群主',
    notice VARCHAR(1000) COMMENT '群公告',
    notice_updated_at DATETIME,
    join_mode TINYINT DEFAULT 0 COMMENT '0=自由加入 1=需审批 2=禁止加入',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_group_code (group_code),
    INDEX idx_owner (owner_id)
);

-- 群成员表
-- role: 1=群主 2=管理员 3=普通成员
CREATE TABLE IF NOT EXISTS group_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role TINYINT DEFAULT 3,
    nickname VARCHAR(50) COMMENT '群内昵称',
    remark VARCHAR(50) COMMENT '个人群备注',
    is_muted TINYINT DEFAULT 0 COMMENT '管理员禁言',
    muted_until DATETIME COMMENT '禁言截止时间',
    is_notification_muted TINYINT DEFAULT 0 COMMENT '消息免打扰（用户自己设置）',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user_id (user_id)
);

-- 文件表（图片、附件）
CREATE TABLE IF NOT EXISTS file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uploader_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_path VARCHAR(500) NOT NULL COMMENT '存储路径或访问URL',
    file_size BIGINT NOT NULL COMMENT '字节数',
    mime_type VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uploader (uploader_id)
);

-- 消息表（单聊 + 群聊，支持撤回）
-- msg_type: 1=文本 2=图片 3=文件
-- chat_type: 1=单聊 2=群聊
CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chat_type TINYINT NOT NULL DEFAULT 1,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT COMMENT '单聊对方，群聊为空',
    group_id BIGINT COMMENT '群聊ID，单聊为空',
    content TEXT COMMENT '文本内容；非文本时可存描述',
    msg_type TINYINT DEFAULT 1,
    file_id BIGINT COMMENT '关联 file 表',
    is_recalled TINYINT DEFAULT 0 COMMENT '0=正常 1=已撤回',
    recalled_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_single_chat (chat_type, from_user_id, to_user_id, created_at),
    INDEX idx_group_chat (chat_type, group_id, created_at),
    INDEX idx_to_user (to_user_id, created_at)
);

-- 会话列表（左侧最近聊天，含未读数）
-- target_type: 1=单聊用户 2=群组
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '会话所属用户',
    target_type TINYINT NOT NULL,
    target_id BIGINT NOT NULL COMMENT '对方用户ID或群组ID',
    last_message_id BIGINT,
    last_message_preview VARCHAR(200) COMMENT '最后一条消息摘要',
    last_message_at DATETIME,
    unread_count INT DEFAULT 0,
    last_read_message_id BIGINT COMMENT '该用户在此会话中读到的最新消息',
    cleared_at DATETIME COMMENT '用户清空聊天记录时间，此时间之前的消息对自己不展示',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_user_updated (user_id, last_message_at DESC)
);

-- 消息已读记录（单聊已读回执、群聊多人已读）
CREATE TABLE IF NOT EXISTS message_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_user (message_id, user_id),
    INDEX idx_user_read (user_id, read_at)
);

-- 用户隐藏的消息（单条软删除）
CREATE TABLE IF NOT EXISTS message_hidden (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    hidden_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_msg (user_id, message_id),
    INDEX idx_user_id (user_id)
);

-- Refresh Token（配合 JWT 续期）
CREATE TABLE IF NOT EXISTS user_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL COMMENT 'Token SHA256',
    expires_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_token_hash (token_hash),
    INDEX idx_expires (expires_at)
);

-- ============ 外键约束 ============
-- 注意：如果已有孤儿数据，需要先清理再添加外键

-- ALTER TABLE friend ADD CONSTRAINT fk_friend_user FOREIGN KEY (user_id) REFERENCES user(id);
-- ALTER TABLE friend ADD CONSTRAINT fk_friend_friend FOREIGN KEY (friend_id) REFERENCES user(id);
-- ALTER TABLE message ADD CONSTRAINT fk_msg_from FOREIGN KEY (from_user_id) REFERENCES user(id);
-- ALTER TABLE message ADD CONSTRAINT fk_msg_file FOREIGN KEY (file_id) REFERENCES file(id);
-- 自定义表情表
CREATE TABLE IF NOT EXISTS emoji (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    url VARCHAR(500) NOT NULL,
    file_id BIGINT,
    file_hash VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_emoji_user (user_id),
    INDEX idx_emoji_hash (user_id, file_hash)
);

-- ALTER TABLE group_member ADD CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES `group`(id);
-- ALTER TABLE group_member ADD CONSTRAINT fk_gm_user FOREIGN KEY (user_id) REFERENCES user(id);
-- ALTER TABLE file ADD CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES user(id);
-- ALTER TABLE conversation ADD CONSTRAINT fk_conv_user FOREIGN KEY (user_id) REFERENCES user(id);
-- ALTER TABLE message_hidden ADD CONSTRAINT fk_mh_user FOREIGN KEY (user_id) REFERENCES user(id);
-- ALTER TABLE message_hidden ADD CONSTRAINT fk_mh_msg FOREIGN KEY (message_id) REFERENCES message(id);
