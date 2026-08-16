package com.mkt.ruleengine.core.gray;

/**
 * 百分比分桶灰度：按分桶键一致性哈希，前 percent% 的桶放行。
 */
public class PercentGrayStrategy implements GrayStrategy {

    @Override
    public boolean hit(GrayConfig config, GrayContext ctx) {
        if (!config.isEnabled() || config.getPercent() <= 0) {
            return false;
        }
        if (config.getPercent() >= 100) {
            return true;
        }
        String bucketValue = ctx.bucketValue(config.getBucketKey());
        return GrayContext.hashBucketHit(bucketValue, ctx.getRuleCode(), config.getPercent());
    }
}
