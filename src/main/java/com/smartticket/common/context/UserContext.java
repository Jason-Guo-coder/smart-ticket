package com.smartticket.common.context;

import lombok.Getter;

/**
 * 当前登录用户上下文（ThreadLocal）。
 * 由 JWT 过滤器在请求开始时写入（Phase 1），审计/业务读取，请求结束清理。
 */
public final class UserContext {

    @Getter
    public static class CurrentUser {
        private final Long userId;
        private final String username;
        private final String role; // EMPLOYEE/ENGINEER/ADMIN

        public CurrentUser(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static String getRole() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
