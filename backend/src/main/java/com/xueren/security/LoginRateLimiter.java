package com.xueren.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 简单内存限流：同一 IP/用户名 连续失败 5 次后锁定 15 分钟
 */
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;

    private final ConcurrentMap<String, int[]> attempts = new ConcurrentHashMap<>();

    /** 记录一次失败，返回 true 表示已被锁定 */
    public boolean recordFailure(String key) {
        long now = System.currentTimeMillis();
        int[] record = attempts.compute(key, (k, v) -> {
            if (v == null) return new int[]{1, 0}; // [attempts, lockUntilMs]
            if (v[1] > 0 && now < v[1]) {
                v[0]++; // 锁定期间继续计数
                return v;
            }
            if (v[1] > 0 && now >= v[1]) {
                return new int[]{1, 0}; // 锁过期，重置
            }
            v[0]++;
            if (v[0] >= MAX_ATTEMPTS) {
                v[1] = (int) (now + LOCK_MINUTES * 60 * 1000);
            }
            return v;
        });
        return record[1] > 0 && now < record[1];
    }

    /** 检查是否被锁定 */
    public boolean isLocked(String key) {
        int[] record = attempts.get(key);
        if (record == null) return false;
        if (record[1] == 0) return false;
        return System.currentTimeMillis() < record[1];
    }

    /** 登录成功后清除记录 */
    public void clear(String key) {
        attempts.remove(key);
    }
}
