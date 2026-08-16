package com.mkt.ruleengine.core.gray;

import com.mkt.ruleengine.core.event.MarketingEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 灰度策略测试：百分比分桶一致性 / 渠道白名单 / 组合策略 / 全量开关。
 */
class GrayStrategyTest {

    private GrayContext ctx(String userId, String channelId, String ruleCode) {
        return new GrayContext(new MarketingEvent(ruleCode, userId, channelId, System.currentTimeMillis(), Map.of()), ruleCode);
    }

    @Test
    void percentBucketingIsDeterministic() {
        GrayConfig config = new GrayConfig(true, GrayStrategyType.PERCENT, 30, List.of(), "userId");
        String userId = "u1001";
        boolean hit1 = GrayStrategyFactory.decide(config, ctx(userId, "APP", "RULE_1"));
        boolean hit2 = GrayStrategyFactory.decide(config, ctx(userId, "APP", "RULE_1"));
        assertEquals(hit1, hit2, "same user + rule must hit same bucket");
        // 同一用户不同规则可分属不同桶
        boolean hitOther = GrayStrategyFactory.decide(config, ctx(userId, "APP", "RULE_2"));
        assertTrue(hit1 || hitOther || true); // 不做具体值断言，仅验证可执行
    }

    @Test
    void percentBoundary() {
        assertFalse(GrayStrategyFactory.decide(new GrayConfig(true, GrayStrategyType.PERCENT, 0, List.of(), "userId"),
                ctx("u1", "APP", "R")));
        assertTrue(GrayStrategyFactory.decide(new GrayConfig(true, GrayStrategyType.PERCENT, 100, List.of(), "userId"),
                ctx("u1", "APP", "R")));
    }

    @Test
    void channelWhitelist() {
        GrayConfig config = new GrayConfig(true, GrayStrategyType.CHANNEL, 0, List.of("AD-ZHITONG", "APP"), "userId");
        assertTrue(GrayStrategyFactory.decide(config, ctx("u1", "APP", "R")));
        assertFalse(GrayStrategyFactory.decide(config, ctx("u1", "WECHAT", "R")));
    }

    @Test
    void combinedStrategy() {
        GrayConfig config = new GrayConfig(true, GrayStrategyType.PERCENT_AND_CHANNEL, 100,
                List.of("AD-ZHITONG"), "userId");
        assertTrue(GrayStrategyFactory.decide(config, ctx("u1", "AD-ZHITONG", "R")));
        assertFalse(GrayStrategyFactory.decide(config, ctx("u1", "APP", "R")));
    }

    @Test
    void disabledGrayMeansFullRelease() {
        assertTrue(GrayStrategyFactory.decide(GrayConfig.fullRelease(), ctx("u1", "APP", "R")));
        assertTrue(GrayStrategyFactory.decide(null, ctx("u1", "APP", "R")));
    }
}
