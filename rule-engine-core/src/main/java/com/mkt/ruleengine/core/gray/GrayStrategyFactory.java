package com.mkt.ruleengine.core.gray;

import com.mkt.ruleengine.core.exception.RuleConfigException;

import java.util.EnumMap;
import java.util.Map;

/**
 * 灰度策略工厂（工厂模式）：按策略类型返回对应实现。
 */
public class GrayStrategyFactory {

    private static final Map<GrayStrategyType, GrayStrategy> STRATEGIES = new EnumMap<>(GrayStrategyType.class);

    static {
        STRATEGIES.put(GrayStrategyType.OFF, (config, ctx) -> false);
        STRATEGIES.put(GrayStrategyType.PERCENT, new PercentGrayStrategy());
        STRATEGIES.put(GrayStrategyType.CHANNEL, new ChannelGrayStrategy());
        STRATEGIES.put(GrayStrategyType.PERCENT_AND_CHANNEL, new PercentAndChannelGrayStrategy());
    }

    public static GrayStrategy of(GrayStrategyType type) {
        GrayStrategy strategy = STRATEGIES.get(type);
        if (strategy == null) {
            throw new RuleConfigException("unsupported gray strategy: " + type);
        }
        return strategy;
    }

    /**
     * 灰度放行判定。关闭灰度时视为全量放行。
     */
    public static boolean decide(GrayConfig config, GrayContext ctx) {
        if (config == null || !config.isEnabled()) {
            return true;
        }
        return of(config.getStrategy()).hit(config, ctx);
    }
}
