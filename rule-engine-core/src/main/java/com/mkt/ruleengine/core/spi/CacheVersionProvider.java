package com.mkt.ruleengine.core.spi;

/**
 * 缓存版本提供者 SPI：返回规则配置的全局版本号，供轮询热更新检测变更。
 */
public interface CacheVersionProvider {

    /**
     * 当前全局版本号（配置变更时递增）。
     */
    long currentVersion();
}
