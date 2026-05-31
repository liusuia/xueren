package com.xueren.event;

/** 群内系统消息事件 — 解耦 GroupService 和 MessagePushService 的循环依赖 */
public record GroupEvent(Long groupId, String sysMessage) {}
