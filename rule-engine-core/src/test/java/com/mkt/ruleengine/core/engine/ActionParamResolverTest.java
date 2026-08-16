package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.event.MarketingEvent;
import com.mkt.ruleengine.core.rule.EvaluationContext;
import com.mkt.ruleengine.core.rule.RuleAction;
import com.mkt.ruleengine.core.spi.ExpressionEvaluator;
import com.mkt.ruleengine.core.testutil.FakeExpressionEvaluator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 动作参数解析测试：${} 属性引用 + #{} 表达式。
 */
class ActionParamResolverTest {

    private final ExpressionEvaluator evaluator = new FakeExpressionEvaluator();
    private final ActionParamResolver resolver = new ActionParamResolver(evaluator);

    @Test
    void resolveRefAndExpression() {
        MarketingEvent event = new MarketingEvent("ORDER_CREATE", "u1", "APP",
                System.currentTimeMillis(), Map.of("orderAmount", 800));
        EvaluationContext ctx = new EvaluationContext(event, Map.of("rebateAmount", 40), Map.of());

        RuleAction action = new RuleAction("X", Map.of(
                "couponTemplateId", "CT-${orderAmount}-${rebateAmount}",
                "count", "#{rebateAmount * 2}",
                "plain", "literal"));
        Map<String, Object> resolved = resolver.resolve(action, ctx);
        assertEquals("CT-800-40", resolved.get("couponTemplateId"));
        Number count = (Number) resolved.get("count");
        assertEquals(80.0, count.doubleValue(), 0.001);
        assertEquals("literal", resolved.get("plain"));
    }

    @Test
    void resolveNestedStructures() {
        MarketingEvent event = new MarketingEvent("EVT", "u1", "APP", System.currentTimeMillis(), Map.of());
        EvaluationContext ctx = new EvaluationContext(event, Map.of("userId", "u1"), Map.of());
        RuleAction action = new RuleAction("X", Map.of(
                "nested", Map.of("a", "${userId}", "b", List.of("${userId}", 1))));
        Map<String, Object> resolved = resolver.resolve(action, ctx);
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) resolved.get("nested");
        assertEquals("u1", nested.get("a"));
        assertEquals(List.of("u1", 1), nested.get("b"));
    }
}
