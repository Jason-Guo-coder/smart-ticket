package com.smartticket.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解（B7）。标注在需要留痕的接口方法上，由 AuditLogAspect 统一写 sys_audit_log。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 操作类型：登录/派单/状态流转/评价/用户管理/权限变更 等。 */
    String value();
}
