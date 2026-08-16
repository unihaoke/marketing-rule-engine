package com.mkt.ruleengine.core.gray;

/**
 * 组合灰度：渠道白名单 + 百分比分桶（先渠道后分桶）。
 */
public class PercentAndChannelGrayStrategy implements GrayStrategy {

    private final ChannelGrayStrategy channelStrategy = new ChannelGrayStrategy();
    private final PercentGrayStrategy percentStrategy = new PercentGrayStrategy();

    @Override
    public boolean hit(GrayConfig config, GrayContext ctx) {
        if (!config.isEnabled()) {
            return false;
        }
        return channelStrategy.hit(config, ctx) && percentStrategy.hit(config, ctx);
    }
}
