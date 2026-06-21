package com.smartticket.common.result;

import lombok.Getter;

/** 统一响应码。 */
@Getter
public enum ResultCode {
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "操作冲突，请重试"),
    TOO_MANY_REQUESTS(429, "操作过于频繁，请稍后再试"),
    FAIL(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
