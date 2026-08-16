package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.spi.IdempotencyStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认幂等存储：JVM 内存实现（TTL 自动清理），适合单机演示；
 * 生产可替换为 Redis / DB 唯一键实现。
 */
public class DefaultIdempotencyStore implements IdempotencyStore {

    private static final long DEFAULT_TTL_MS = 24 * 60 * 60 * 1000L;

    private final Map<String, Long> acquired = new ConcurrentHashMap<>();
    private final long ttlMs;

    public DefaultIdempotencyStore() {
        this(DEFAULT_TTL_MS);
    }

    public DefaultIdempotencyStore(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    @Override
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Long prev = acquired.putIfAbsent(key, now);
        if (prev == null) {
            return true;
        }
        // 过期键允许重试
        if (now - prev > ttlMs) {
            if (acquired.replace(key, prev, now)) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public void release(String key) {
        acquired.remove(key);
    }

    public int size() {
        return acquired.size();
    }
}
