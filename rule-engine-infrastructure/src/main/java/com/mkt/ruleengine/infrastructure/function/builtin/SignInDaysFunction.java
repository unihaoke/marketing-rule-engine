package com.mkt.ruleengine.infrastructure.function.builtin;

import com.mkt.ruleengine.core.function.FunctionContext;
import com.mkt.ruleengine.core.function.MarketingFunction;
import com.mkt.ruleengine.infrastructure.persistence.mapper.EngineLogMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 内置函数：签到天数计算（基于 t_engine_log 真实历史，按天去重）。
 *
 * <p>绑定参数：
 * <ul>
 *   <li>{@code eventCode}：签到事件编码（默认 SIGN_IN）</li>
 *   <li>{@code mode}：streak=连续签到天数（默认）/ total=累计签到天数</li>
 * </ul>
 *
 * <p>语义：查询该用户该事件的历史触发日期（按天去重）；
 * 若本次触发为当日首次签到（历史无当日记录），则把当日计入（累计 = 历史天数 + 1），
 * 同日重复触发不再增加；连续天数以最近签到日为起点向前连续计数。</p>
 */
@Component
public class SignInDaysFunction implements MarketingFunction {

    public static final String NAME = "signInDays";

    private final EngineLogMapper engineLogMapper;

    public SignInDaysFunction(EngineLogMapper engineLogMapper) {
        this.engineLogMapper = engineLogMapper;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Object evaluate(FunctionContext ctx) {
        String eventCode = String.valueOf(ctx.binding("eventCode") == null ? "SIGN_IN" : ctx.binding("eventCode"));
        String mode = String.valueOf(ctx.binding("mode") == null ? "streak" : ctx.binding("mode"));
        String userId = ctx.getEvent().getUserId();
        if (userId == null || userId.isBlank()) {
            return BigDecimal.ZERO;
        }

        List<LocalDate> dates = new ArrayList<>();
        engineLogMapper.selectEventDates(userId, eventCode).forEach(d -> dates.add(d.toLocalDate()));

        // 本次为当日首次签到（历史无当日记录）→ 计入当日
        LocalDate today = LocalDate.now();
        boolean includeToday = !dates.contains(today) && eventCode.equals(ctx.getEvent().getEventCode());
        if (includeToday) {
            dates.add(today);
            dates.sort(LocalDate::compareTo);
        }

        if ("total".equalsIgnoreCase(mode)) {
            return BigDecimal.valueOf(dates.size());
        }
        // streak：从最近签到日往前连续计数
        if (dates.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int streak = 0;
        LocalDate prev = null;
        for (int i = dates.size() - 1; i >= 0; i--) {
            LocalDate d = dates.get(i);
            if (prev == null || ChronoUnit.DAYS.between(d, prev) == 1) {
                streak++;
            } else {
                break;
            }
            prev = d;
        }
        return BigDecimal.valueOf(streak);
    }
}
