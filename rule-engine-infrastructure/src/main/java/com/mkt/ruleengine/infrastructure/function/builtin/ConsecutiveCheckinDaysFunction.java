package com.mkt.ruleengine.infrastructure.function.builtin;

import com.mkt.ruleengine.core.function.FunctionContext;
import com.mkt.ruleengine.core.function.MarketingFunction;
import org.springframework.stereotype.Component;

/**
 * 内置函数：连续打卡天数计算。
 * <p>数据来源优先级：用户画像 checkinStreak → 事件参数 checkinStreak → 绑定参数 checkinStreak → 0。
 * 业务侧可实现自己的 {@link com.mkt.ruleengine.core.spi.UserProfileResolver} 提供真实数据。</p>
 */
@Component
public class ConsecutiveCheckinDaysFunction implements MarketingFunction {

    public static final String NAME = "consecutiveCheckinDays";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Object evaluate(FunctionContext ctx) {
        Object streak = ctx.getUserProfile().get("checkinStreak");
        if (streak == null) {
            streak = ctx.param("checkinStreak");
        }
        if (streak == null) {
            streak = ctx.binding("checkinStreak");
        }
        return streak == null ? 0 : com.mkt.ruleengine.core.rule.ValueCoercer.toBigDecimal(streak);
    }
}
