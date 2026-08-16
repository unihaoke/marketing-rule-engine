package com.mkt.ruleengine.core.spi;

/**
 * 幂等存储 SPI：动作执行去重（同事件 + 同规则 + 同动作只执行一次）。
 */
public interface IdempotencyStore {

    /**
     * 尝试占用幂等键。
     *
     * @param key 幂等键（eventId:ruleCode:actionCode）
     * @return true = 首次（允许执行）；false = 已占用（幂等跳过）
     */
    boolean tryAcquire(String key);

    /**
     * 释放幂等键。
     */
    void release(String key);
}
