package com.xueren.security;

import com.xueren.common.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthHolder {

    private AuthHolder() {
    }

    public static Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        throw new BusinessException("登录已过期，请重新登录");
    }
}
