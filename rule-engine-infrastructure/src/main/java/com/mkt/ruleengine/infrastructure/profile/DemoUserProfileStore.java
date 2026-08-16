package com.mkt.ruleengine.infrastructure.profile;

import com.mkt.ruleengine.core.spi.UserProfileResolver;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示用户画像（确定性生成）：基于 userId 哈希产出稳定画像，供示例规则与前端演示使用。
 * 生产环境替换为真实用户中心实现。
 */
@Component
public class DemoUserProfileStore implements UserProfileResolver {

    private static final List<String> LTV_TIERS = List.of("A", "B", "C");
    private static final List<String> REGION_TAGS = List.of("BEIJING", "SHANGHAI", "GUANGZHOU", "SHENZHEN", "HANGZHOU");

    @Override
    public Map<String, Object> resolve(String userId) {
        Map<String, Object> profile = new LinkedHashMap<>();
        if (userId == null || userId.isBlank()) {
            return profile;
        }
        int h = Math.floorMod(userId.hashCode(), 100_000);
        int registeredDays = h % 365;
        int orderCount = h % 30;
        int checkinStreak = h % 10;
        String ltvTier = LTV_TIERS.get(h % LTV_TIERS.size());
        String region = REGION_TAGS.get(h % REGION_TAGS.size());

        List<String> tags = new java.util.ArrayList<>();
        if (registeredDays < 7) {
            tags.add("NEW_USER");
        } else if (registeredDays < 180) {
            tags.add("ACTIVE");
        } else {
            tags.add("OLD_USER");
        }
        if (orderCount >= 10) {
            tags.add("HIGH_FREQ");
        }
        if ("A".equals(ltvTier)) {
            tags.add("VIP");
        }

        profile.put("tags", tags);
        // 规则画布约定字段：用户标签（与 tags 同值，别名兼容）
        profile.put("userTag", tags);
        profile.put("registeredDays", registeredDays);
        profile.put("orderCount", orderCount);
        profile.put("checkinStreak", checkinStreak);
        profile.put("ltvTier", ltvTier);
        profile.put("region", region);
        profile.put("points", h % 10_000);
        return profile;
    }
}
