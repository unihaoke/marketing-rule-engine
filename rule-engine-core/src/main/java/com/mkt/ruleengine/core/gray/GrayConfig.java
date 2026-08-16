package com.mkt.ruleengine.core.gray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则灰度配置：按渠道 / 用户分桶百分比灰度开关。
 */
public class GrayConfig {

    /** 是否开启灰度 */
    private boolean enabled = false;

    /** 灰度策略 */
    private GrayStrategyType strategy = GrayStrategyType.OFF;

    /** 灰度百分比 0-100（PERCENT 策略） */
    private int percent = 0;

    /** 渠道白名单（CHANNEL 策略） */
    private List<String> channels = new ArrayList<>();

    /** 分桶键字段（默认 userId；也可用 channelId 等） */
    private String bucketKey = "userId";

    public GrayConfig() {
    }

    public GrayConfig(boolean enabled, GrayStrategyType strategy, int percent,
                      List<String> channels, String bucketKey) {
        this.enabled = enabled;
        this.strategy = strategy == null ? GrayStrategyType.OFF : strategy;
        this.percent = percent;
        this.channels = channels == null ? new ArrayList<>() : new ArrayList<>(channels);
        this.bucketKey = bucketKey == null ? "userId" : bucketKey;
    }

    /** 全量上线（关闭灰度） */
    public static GrayConfig fullRelease() {
        return new GrayConfig(false, GrayStrategyType.OFF, 0, List.of(), "userId");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public GrayStrategyType getStrategy() {
        return strategy;
    }

    public void setStrategy(GrayStrategyType strategy) {
        this.strategy = strategy == null ? GrayStrategyType.OFF : strategy;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public List<String> getChannels() {
        return Collections.unmodifiableList(channels);
    }

    public void setChannels(List<String> channels) {
        this.channels = channels == null ? new ArrayList<>() : new ArrayList<>(channels);
    }

    public String getBucketKey() {
        return bucketKey;
    }

    public void setBucketKey(String bucketKey) {
        this.bucketKey = bucketKey == null ? "userId" : bucketKey;
    }
}
