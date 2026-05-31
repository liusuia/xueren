// 聊天类型
export const CHAT_TYPE = { SINGLE: 1, GROUP: 2 }
// 目标类型
export const TARGET_TYPE = { USER: 1, GROUP: 2 }
// 消息类型
export const MSG_TYPE = { TEXT: 1, IMAGE: 2, FILE: 3, EMOJI: 4, SYSTEM: 5 }
// 好友状态
export const FRIEND_STATUS = { PENDING: 0, ACCEPTED: 1, REJECTED: 2, BLOCKED: 3 }
// 群成员角色
export const GROUP_ROLE = { OWNER: 1, ADMIN: 2, MEMBER: 3 }

export const MSG_TYPE_LABEL = { 1: '文本', 2: '图片', 3: '文件', 4: '表情', 5: '系统' }
export const FRIEND_STATUS_LABEL = { 0: '待验证', 1: '已通过', 2: '已拒绝', 3: '已拉黑' }
export const GROUP_ROLE_LABEL = { 1: '群主', 2: '管理员', 3: '成员' }
