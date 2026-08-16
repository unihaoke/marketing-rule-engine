package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.function.FunctionContext;
import com.mkt.ruleengine.core.function.FunctionRegistry;
import com.mkt.ruleengine.core.function.MarketingFunction;
import com.mkt.ruleengine.core.rule.RuleFunction;
import com.mkt.ruleengine.core.rule.RuleSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 阶段二：前置函数增强（匹配前执行）。
 * 执行规则声明的自定义函数（连续打卡天数、阶梯返利核算等），
 * 结果写入运行时属性，供条件与动作参数引用；同名函数在本次链路内只执行一次。
 */
public class FunctionEnhanceStage implements EngineStage {

    private static final Logger log = LoggerFactory.getLogger(FunctionEnhanceStage.class);

    private final FunctionRegistry functionRegistry;

    public FunctionEnhanceStage(FunctionRegistry functionRegistry) {
        this.functionRegistry = functionRegistry;
    }

    @Override
    public void handle(EngineContext ctx, StageChain chain) {
        // 收集本次链路需要的函数（按别名去重）
        Map<String, RuleFunction> pending = new LinkedHashMap<>();
        for (RuleSnapshot snapshot : ctx.getSnapshots()) {
            for (RuleFunction fn : snapshot.getFunctions()) {
                pending.putIfAbsent(fn.getAlias() == null ? fn.getFunctionName() : fn.getAlias(), fn);
            }
        }
        for (Map.Entry<String, RuleFunction> entry : pending.entrySet()) {
            String alias = entry.getKey();
            RuleFunction ruleFn = entry.getValue();
            Optional<MarketingFunction> maybeFn = functionRegistry.get(ruleFn.getFunctionName());
            if (maybeFn.isEmpty()) {
                log.warn("function not registered, skip: {} (rule={})", ruleFn.getFunctionName(), ctx.getEvent().getEventCode());
                continue;
            }
            try {
                FunctionContext fnCtx = new FunctionContext(ctx.getEvent(), ctx.getAttributes(),
                        ruleFn.getBindings(), ctx.getUserProfile());
                Object result = maybeFn.get().evaluate(fnCtx);
                ctx.putAttribute(alias, result);
            } catch (Exception e) {
                log.error("function execute error: {} (eventId={})", ruleFn.getFunctionName(), ctx.getEvent().getEventId(), e);
            }
        }
        chain.proceed(ctx);
    }
}
