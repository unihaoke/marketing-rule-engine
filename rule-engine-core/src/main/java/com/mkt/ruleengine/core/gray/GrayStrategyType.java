package com.mkt.ruleengine.core.gray;

/**
 * 灰度策略类型。
 */
public enum GrayStrategyType {
    /** 关闭灰度 */
    OFF,
    /** 按百分比分桶（默认按 userId 哈希分桶） */
    PERCENT,
    /** 渠道白名单 */
    CHANNEL,
    /** 渠道白名单 + 百分比分桶（先渠道后分桶） */
    PERCENT_AND_CHANNEL
}
