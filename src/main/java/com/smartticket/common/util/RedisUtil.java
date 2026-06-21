package com.smartticket.common.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具：封装常用操作（缓存、原子自增、ZSet 排行榜、分布式锁、黑名单）。
 * ARCHITECTURE §5.3 的统一入口，避免各模块散落直接操作。
 */
@Component
public class RedisUtil {

    private final RedisTemplate<String, Object> redis;

    public RedisUtil(RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    /* ---------- 通用 KV ---------- */
    public void set(String key, Object value) {
        redis.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redis.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redis.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redis.delete(key);
    }

    public Boolean hasKey(String key) {
        return redis.hasKey(key);
    }

    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redis.expire(key, timeout, unit);
    }

    /* ---------- 原子自增（工单号） ---------- */
    public Long increment(String key, long delta) {
        return redis.opsForValue().increment(key, delta);
    }

    /* ---------- ZSet（绩效排行榜） ---------- */
    public void zAdd(String key, Object member, double score) {
        redis.opsForZSet().add(key, member, score);
    }

    public Double zIncrBy(String key, Object member, double delta) {
        return redis.opsForZSet().incrementScore(key, member, delta);
    }

    public Long zReverseRank(String key, Object member) {
        return redis.opsForZSet().reverseRank(key, member);
    }

    /* ---------- 分布式锁（派单防并发） ---------- */
    public boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        Boolean ok = redis.opsForValue().setIfAbsent(key, value, timeout, unit);
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(String key, String value) {
        Object cur = redis.opsForValue().get(key);
        if (value.equals(cur)) {
            redis.delete(key);
        }
    }

    public RedisTemplate<String, Object> raw() {
        return redis;
    }
}
