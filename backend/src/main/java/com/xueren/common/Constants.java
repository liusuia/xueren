package com.xueren.common;

public final class Constants {

    private Constants() {
    }

    public static final int FRIEND_PENDING = 0;
    public static final int FRIEND_ACCEPTED = 1;
    public static final int FRIEND_REJECTED = 2;
    public static final int FRIEND_BLOCKED = 3;

    public static final int CHAT_SINGLE = 1;
    public static final int CHAT_GROUP = 2;

    public static final int TARGET_USER = 1;
    public static final int TARGET_GROUP = 2;

    public static final int MSG_TEXT = 1;
    public static final int MSG_IMAGE = 2;
    public static final int MSG_FILE = 3;
    public static final int MSG_EMOJI = 4;
    public static final int MSG_SYSTEM = 5;

    public static final int GROUP_ROLE_OWNER = 1;
    public static final int GROUP_ROLE_ADMIN = 2;
    public static final int GROUP_ROLE_MEMBER = 3;
}
