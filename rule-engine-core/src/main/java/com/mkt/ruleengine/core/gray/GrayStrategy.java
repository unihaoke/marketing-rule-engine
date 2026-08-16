package com.mkt.ruleengine.core.gray;

/**
 * 灰度判定策略 SPI（策略模式）。
 */
public interface GrayStrategy {

    /**
     * 判定是否命中灰度放行。
     *
     * @param config 灰度配置
     * @param ctx    灰度上下文
     * @return true = 放行（执行规则）
     */
    boolean hit(GrayConfig config, GrayContext ctx);
}
