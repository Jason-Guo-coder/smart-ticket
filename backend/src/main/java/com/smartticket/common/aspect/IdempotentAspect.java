package com.smartticket.common.aspect;

import com.smartticket.common.annotation.Idempotent;
import com.smartticket.common.exception.BizException;
import com.smartticket.common.result.ResultCode;
import com.smartticket.common.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 幂等切面（B9）：请求头携带幂等 token，首次放行并占位，重复 token 直接拒绝。
 */
@Aspect
@Component
public class IdempotentAspect {

    private final RedisUtil redis;

    public IdempotentAspect(RedisUtil redis) {
        this.redis = redis;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return pjp.proceed();
        }
        HttpServletRequest req = attrs.getRequest();
        String token = req.getHeader(idempotent.header());
        if (token == null || token.isBlank()) {
            // 无 token 不拦截（前端可选携带）；如需强制可改为抛异常
            return pjp.proceed();
        }
        String key = "idem:" + token;
        boolean first = redis.tryLock(key, "1", idempotent.ttlSeconds(), TimeUnit.SECONDS);
        if (!first) {
            throw new BizException(ResultCode.CONFLICT, "请勿重复提交");
        }
        return pjp.proceed();
    }
}
