package com.smartticket.common.ratelimit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 滑动窗口限流器（B9，基于 Redis ZSet）。
 * tryAcquire 在 windowSeconds 窗口内最多放行 limit 次。
 */
@Component
public class RateLimiter {

    private final RedisTemplate<String, Object> redis;

    public RateLimiter(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    /**
     * @return true 放行；false 触发限流
     */
    public boolean tryAcquire(String bizKey, int limit, int windowSeconds) {
        String key = "rl:" + bizKey;
        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000L;

        // 移除窗口外的记录
        redis.opsForZSet().removeRangeByScore(key, 0, windowStart);
        Long count = redis.opsForZSet().zCard(key);
        if (count != null && count >= limit) {
            return false;
        }
        // 记录本次（member 唯一）
        redis.opsForZSet().add(key, now + ":" + UUID.randomUUID(), now);
        redis.expire(key, windowSeconds, TimeUnit.SECONDS);
        return true;
    }
}
