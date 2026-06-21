package com.smartticket.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等注解（B9）。基于请求头幂等 token + Redis setnx，防重复提交。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 幂等 token 所在请求头名。 */
    String header() default "Idempotency-Key";

    /** token 有效期（秒）。 */
    int ttlSeconds() default 60;
}
