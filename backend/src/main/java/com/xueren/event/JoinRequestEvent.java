package com.xueren.event;

/** 有人申请入群事件 — 通知群主更新审批数 */
public record JoinRequestEvent(Long groupId, Long ownerId) {}
