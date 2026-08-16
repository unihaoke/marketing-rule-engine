package com.mkt.ruleengine.infrastructure.function.builtin;

import com.mkt.ruleengine.core.function.FunctionContext;
import com.mkt.ruleengine.core.function.MarketingFunction;
import com.mkt.ruleengine.core.rule.ValueCoercer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 内置函数：阶梯返利核算。
 *
 * <p>绑定参数：
 * <ul>
 *   <li>{@code amountField}：参与返利核算的事件参数字段（如 orderAmount），缺省 orderAmount</li>
 *   <li>{@code tiers}：阶梯档位列表 [{min, max, rate}]，rate 为返利比例（0.05 = 5%）</li>
 * </ul>
 * 返回 BigDecimal 返利金额。</p>
 */
@Component
public class RebateCalculatorFunction implements MarketingFunction {

    public static final String NAME = "rebateCalculator";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object evaluate(FunctionContext ctx) {
        String amountField = String.valueOf(ctx.binding("amountField") == null ? "orderAmount" : ctx.binding("amountField"));
        Object amountRaw = ctx.param(amountField);
        if (amountRaw == null) {
            amountRaw = ctx.binding(amountField);
        }
        BigDecimal amount = ValueCoercer.toBigDecimal(amountRaw == null ? "0" : amountRaw);
        Object tiersRaw = ctx.binding("tiers");
        if (!(tiersRaw instanceof List<?> tiers) || tiers.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        for (Object item : tiers) {
            if (!(item instanceof Map<?, ?> tier)) {
                continue;
            }
            BigDecimal min = ValueCoercer.toBigDecimal(String.valueOf(tier.get("min")));
            BigDecimal max = tier.get("max") == null ? null : ValueCoercer.toBigDecimal(String.valueOf(tier.get("max")));
            BigDecimal rate = ValueCoercer.toBigDecimal(String.valueOf(tier.get("rate")));
            if (amount.compareTo(min) >= 0 && (max == null || amount.compareTo(max) <= 0)) {
                return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
