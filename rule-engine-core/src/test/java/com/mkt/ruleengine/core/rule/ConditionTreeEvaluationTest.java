package com.mkt.ruleengine.core.rule;

import com.mkt.ruleengine.core.event.MarketingEvent;
import com.mkt.ruleengine.core.spi.ExpressionEvaluator;
import com.mkt.ruleengine.core.testutil.FakeExpressionEvaluator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 条件树求值测试：嵌套 AND/OR/NOT + 各操作符 + EXPRESSION。
 */
class ConditionTreeEvaluationTest {

    private final ExpressionEvaluator evaluator = new FakeExpressionEvaluator();
    private final DefaultConditionEvaluator conditionEvaluator = new DefaultConditionEvaluator(evaluator);

    private EvaluationContext ctx(Map<String, Object> params, Map<String, Object> attrs, Map<String, Object> profile) {
        MarketingEvent event = new MarketingEvent("AD_CLICK", "u1", "AD-ZHITONG",
                System.currentTimeMillis(), params);
        return new EvaluationContext(event, attrs, profile);
    }

    @Test
    void leafEqualsAndNumberGte() {
        // orderAmount >= 100
        LeafConditionNode node = new LeafConditionNode("orderAmount", CompareOp.GTE, 100, ValueType.NUMBER);
        assertTrue(conditionEvaluator.evaluate(node, ctx(Map.of("orderAmount", 150), Map.of(), Map.of())));
        assertFalse(conditionEvaluator.evaluate(node, ctx(Map.of("orderAmount", 50), Map.of(), Map.of())));
    }

    @Test
    void leafInListOnProfile() {
        LeafConditionNode node = new LeafConditionNode("userTag", CompareOp.IN, List.of("NEW_USER", "ACTIVE"), ValueType.STRING);
        assertTrue(conditionEvaluator.evaluate(node, ctx(Map.of(), Map.of(),
                Map.of("userTag", List.of("NEW_USER", "VIP")))));
        assertFalse(conditionEvaluator.evaluate(node, ctx(Map.of(), Map.of(),
                Map.of("userTag", List.of("OLD_USER")))));
    }

    @Test
    void betweenOnDatetime() {
        LeafConditionNode node = new LeafConditionNode("eventTime", CompareOp.BETWEEN,
                List.of("2024-01-01 00:00:00", "2024-12-31 23:59:59"), ValueType.DATETIME);
        MarketingEvent event = new MarketingEvent("ORDER_CREATE", "u1", "APP",
                java.time.LocalDateTime.parse("2024-06-01T12:00:00").atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli(), Map.of());
        assertTrue(conditionEvaluator.evaluate(node, new EvaluationContext(event, Map.of(), Map.of())));
    }

    @Test
    void nestedAndOrNot() {
        // (userTag IN [NEW_USER] OR orderCount >= 10) AND NOT (region == 'SHANGHAI')
        ConditionNode tree = new LogicConditionNode(LogicOp.AND, List.of(
                new LogicConditionNode(LogicOp.OR, List.of(
                        new LeafConditionNode("userTag", CompareOp.IN, List.of("NEW_USER"), ValueType.STRING),
                        new LeafConditionNode("orderCount", CompareOp.GTE, 10, ValueType.NUMBER))),
                new LogicConditionNode(LogicOp.NOT, List.of(
                        new LeafConditionNode("region", CompareOp.EQUALS, "SHANGHAI", ValueType.STRING)))));

        assertTrue(conditionEvaluator.evaluate(tree, ctx(Map.of("region", "BEIJING"), Map.of(),
                Map.of("userTag", List.of("OLD_USER"), "orderCount", 12))));
        assertFalse(conditionEvaluator.evaluate(tree, ctx(Map.of("region", "SHANGHAI"), Map.of(),
                Map.of("userTag", List.of("NEW_USER")))));
        assertFalse(conditionEvaluator.evaluate(tree, ctx(Map.of("region", "BEIJING"), Map.of(),
                Map.of("userTag", List.of("OLD_USER"), "orderCount", 3))));
    }

    @Test
    void expressionOperatorWithEnhanceAttribute() {
        // 连续打卡 >= 3（由前置函数写入属性）→ 表达式条件
        LeafConditionNode node = LeafConditionNode.expression("checkinStreak >= 3");
        assertTrue(conditionEvaluator.evaluate(node, ctx(Map.of(), Map.of("checkinStreak", 5), Map.of())));
        assertFalse(conditionEvaluator.evaluate(node, ctx(Map.of(), Map.of("checkinStreak", 2), Map.of())));
    }

    @Test
    void containsOnListStringified() {
        LeafConditionNode node = new LeafConditionNode("userTag", CompareOp.CONTAINS, "VIP", ValueType.STRING);
        assertTrue(conditionEvaluator.evaluate(node, ctx(Map.of(), Map.of(),
                Map.of("userTag", List.of("ACTIVE", "VIP")))));
    }

    @Test
    void leafNotFlag() {
        LeafConditionNode node = new LeafConditionNode("channelId", CompareOp.EQUALS, "AD-ZHITONG", ValueType.STRING);
        node.setNot(true);
        assertFalse(conditionEvaluator.evaluate(node, ctx(Map.of(), Map.of(), Map.of())));
    }
}
