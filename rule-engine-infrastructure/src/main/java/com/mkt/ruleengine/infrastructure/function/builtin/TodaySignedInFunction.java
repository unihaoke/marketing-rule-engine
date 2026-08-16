package com.mkt.ruleengine.infrastructure.function.builtin;

import com.mkt.ruleengine.core.function.FunctionContext;
import com.mkt.ruleengine.core.function.MarketingFunction;
import com.mkt.ruleengine.infrastructure.persistence.mapper.EngineLogMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 内置函数：今日是否已签到（基于 t_engine_log 历史，不含本次触发，本次日志在链路收尾才写入）。
 *
 * <p>绑定参数：{@code eventCode} 签到事件编码（默认 SIGN_IN）。
 * 返回 Boolean：今日已有签到记录为 true；用于「每日签到」规则限制每天只发一次。</p>
 */
@Component
public class TodaySignedInFunction implements MarketingFunction {

    public static final String NAME = "todaySignedIn";

    private final EngineLogMapper engineLogMapper;

    public TodaySignedInFunction(EngineLogMapper engineLogMapper) {
        this.engineLogMapper = engineLogMapper;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Object evaluate(FunctionContext ctx) {
        String eventCode = String.valueOf(ctx.binding("eventCode") == null ? "SIGN_IN" : ctx.binding("eventCode"));
        String userId = ctx.getEvent().getUserId();
        if (userId == null || userId.isBlank()) {
            return Boolean.FALSE;
        }
        LocalDate today = LocalDate.now();
        return engineLogMapper.selectEventDates(userId, eventCode).stream()
                .anyMatch(d -> d.toLocalDate().equals(today));
    }
}
