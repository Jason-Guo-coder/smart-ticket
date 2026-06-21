package com.smartticket.common.aspect;

import com.smartticket.common.annotation.AuditLog;
import com.smartticket.common.audit.AuditLogMapper;
import com.smartticket.common.audit.SysAuditLog;
import com.smartticket.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计日志切面（B7）：标注 @AuditLog 的方法执行后统一写 sys_audit_log，
 * 记录操作人/角色/IP/结果。审计写入失败不影响主流程。
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;

    public AuditLogAspect(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        String result = "SUCCESS";
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            result = "FAIL";
            throw t;
        } finally {
            save(auditLog.value(), result);
        }
    }

    private void save(String actionType, String result) {
        try {
            SysAuditLog row = new SysAuditLog();
            row.setActionType(actionType);
            row.setResult(result);
            row.setOperatorId(UserContext.getUserId());
            row.setRole(UserContext.getRole());
            row.setIp(clientIp());
            auditLogMapper.insert(row);
        } catch (Exception e) {
            log.warn("写审计日志失败: {}", e.getMessage());
        }
    }

    private String clientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
