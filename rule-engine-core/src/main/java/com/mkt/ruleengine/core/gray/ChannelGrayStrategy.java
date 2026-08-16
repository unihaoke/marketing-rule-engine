package com.mkt.ruleengine.core.gray;

/**
 * 渠道白名单灰度：事件渠道在配置的白名单内则放行。
 */
public class ChannelGrayStrategy implements GrayStrategy {

    @Override
    public boolean hit(GrayConfig config, GrayContext ctx) {
        if (!config.isEnabled()) {
            return false;
        }
        String channel = ctx.getEvent().getChannelId();
        return channel != null && config.getChannels() != null && config.getChannels().contains(channel);
    }
}
