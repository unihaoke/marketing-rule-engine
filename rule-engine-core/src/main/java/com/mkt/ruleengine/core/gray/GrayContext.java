package com.mkt.ruleengine.core.gray;

import com.mkt.ruleengine.core.event.MarketingEvent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * 灰度判定上下文。
 */
public class GrayContext {

    private final MarketingEvent event;
    private final String ruleCode;

    public GrayContext(MarketingEvent event, String ruleCode) {
        this.event = event;
        this.ruleCode = ruleCode;
    }

    public MarketingEvent getEvent() {
        return event;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    /** 取分桶键值：优先 bucketKey 指定字段（事件参数/固定字段），缺省 userId */
    public String bucketValue(String bucketKey) {
        if (bucketKey == null || bucketKey.isBlank()) {
            bucketKey = "userId";
        }
        Object v = event.getParams().get(bucketKey);
        if (v == null) {
            switch (bucketKey) {
                case "userId" -> v = event.getUserId();
                case "channelId" -> v = event.getChannelId();
                case "eventId" -> v = event.getEventId();
                default -> v = null;
            }
        }
        return v == null ? null : String.valueOf(v);
    }

    /**
     * 一致性哈希分桶：hash(bucketKey + "#" + ruleCode) % 100 &lt; percent 则命中。
     * 相同用户 + 相同规则永远落在同一桶，保证灰度一致性。
     */
    public static boolean hashBucketHit(String bucketValue, String ruleCode, int percent) {
        if (bucketValue == null || bucketValue.isBlank()) {
            return false;
        }
        int bucket = consistentBucket(bucketValue + "#" + ruleCode);
        return bucket < Math.max(0, Math.min(100, percent));
    }

    public static int consistentBucket(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            int hash = ((digest[0] & 0xFF) << 24) | ((digest[1] & 0xFF) << 16)
                    | ((digest[2] & 0xFF) << 8) | (digest[3] & 0xFF);
            return Math.floorMod(hash, 100);
        } catch (Exception e) {
            return Math.floorMod(Objects.hashCode(key), 100);
        }
    }
}
