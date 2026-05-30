-- GroupMember 新增字段
ALTER TABLE group_member
  ADD COLUMN is_muted     INT DEFAULT 0 COMMENT '是否禁言 0-否 1-是',
  ADD COLUMN muted_until  DATETIME     NULL COMMENT '禁言截止时间';

-- GroupMember 群聊备注
ALTER TABLE group_member
  ADD COLUMN remark VARCHAR(50) NULL COMMENT '用户对群聊的个人备注';

-- ChatGroup 群公告
ALTER TABLE `group`
  ADD COLUMN notice VARCHAR(1000) NULL COMMENT '群公告',
  ADD COLUMN notice_updated_at DATETIME NULL COMMENT '公告更新时间';

-- Message 新增字段
ALTER TABLE message
  ADD COLUMN mention_user_ids VARCHAR(500) NULL COMMENT '@提及的用户ID，逗号分隔';

-- 群文件表
CREATE TABLE IF NOT EXISTS group_file (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_id    BIGINT   NOT NULL COMMENT '群ID',
  uploader_id BIGINT   NOT NULL COMMENT '上传者ID',
  file_id     BIGINT   NOT NULL COMMENT '文件ID',
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群文件';
