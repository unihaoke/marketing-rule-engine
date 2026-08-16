package com.mkt.ruleengine.infrastructure.function.builtin;

import com.mkt.ruleengine.core.function.FunctionContext;
import com.mkt.ruleengine.core.function.MarketingFunction;
import com.mkt.ruleengine.core.rule.ValueCoercer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 内置函数：阶梯奖励核算（按档位 key 匹配返回对应奖励值）。
 *
 * <p>典型场景：连续签到阶梯奖励——第 1 天 1 积分、第 2 天 2 积分、第 3 天 4 积分……
 * <pre>
 * 绑定参数：
 *   keyField: 档位匹配字段（默认 checkinStreak，取自增强属性/事件参数/绑定参数）
 *   tiers:    [{ "key": 1, "value": 1 }, { "key": 2, "value": 2 }, { "key": 3, "value": 4 }, ...]
 *             也支持区间档位 [{ "from": 1, "to": 2, "value": 1 }, { "from": 3, "to": null, "value": 4 }]
 *   fallback: 无匹配时的兜底值（默认 0）
 * </pre>
 * 动作参数中引用：#{rewardPoints}（规则内 alias）。</p>
 */
@Component
public class TieredRewardCalculatorFunction implements MarketingFunction {

    public static final String NAME = "tieredRewardCalculator";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object evaluate(FunctionContext ctx) {
        String keyField = String.valueOf(ctx.binding("keyField") == null ? "checkinStreak" : ctx.binding("keyField"));
        Object keyRaw = ctx.attribute(keyField);
        if (keyRaw == null) {
            keyRaw = ctx.param(keyField);
        }
        if (keyRaw == null) {
            keyRaw = ctx.binding(keyField);
        }
        BigDecimal key = ValueCoercer.toBigDecimal(keyRaw == null ? "-1" : keyRaw);

        Object tiersRaw = ctx.binding("tiers");
        if (!(tiersRaw instanceof List<?> tiers) || tiers.isEmpty()) {
            return fallback(ctx);
        }
        for (Object item : tiers) {
            if (!(item instanceof Map<?, ?> tier)) {
                continue;
            }
            // 精确档位：key 相等
            if (tier.containsKey("key") && tier.get("key") != null) {
                BigDecimal tierKey = ValueCoercer.toBigDecimal(String.valueOf(tier.get("key")));
                if (key.compareTo(tierKey) == 0) {
                    return ValueCoercer.toBigDecimal(String.valueOf(tier.get("value")));
                }
                continue;
            }
            // 区间档位：from <= key <= to（to 为空表示上不封顶）
            if (tier.containsKey("from") && tier.get("from") != null) {
                BigDecimal from = ValueCoercer.toBigDecimal(String.valueOf(tier.get("from")));
                BigDecimal to = tier.get("to") == null ? null : ValueCoercer.toBigDecimal(String.valueOf(tier.get("to")));
                if (key.compareTo(from) >= 0 && (to == null || key.compareTo(to) <= 0)) {
                    return ValueCoercer.toBigDecimal(String.valueOf(tier.get("value")));
                }
            }
        }
        return fallback(ctx);
    }

    private BigDecimal fallback(FunctionContext ctx) {
        Object fb = ctx.binding("fallback");
        return fb == null ? BigDecimal.ZERO : ValueCoercer.toBigDecimal(String.valueOf(fb));
    }
}
