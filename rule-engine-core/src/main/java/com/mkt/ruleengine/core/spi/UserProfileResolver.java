package com.mkt.ruleengine.core.spi;

import java.util.Map;

/**
 * 用户画像解析 SPI：业务侧接入用户标签 / 行为次数 / LTV 等画像数据，
 * 供条件（如 userTag IN (...)）与自定义函数引用。默认返回空画像。
 */
public interface UserProfileResolver {

    /**
     * 解析用户画像字段。
     *
     * @param userId 用户 ID
     * @return 画像字段 Map（可空实现返回空 Map）
     */
    Map<String, Object> resolve(String userId);

    /**
     * 空实现。
     */
    UserProfileResolver NOOP = userId -> Map.of();
}
