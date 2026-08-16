package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.action.ActionDefinition;
import com.mkt.ruleengine.core.action.ActionExecutionContext;
import com.mkt.ruleengine.core.action.ActionExecutor;
import com.mkt.ruleengine.core.action.ActionExecutorFactory;
import com.mkt.ruleengine.core.action.ActionResult;
import com.mkt.ruleengine.core.rule.EvaluationContext;
import com.mkt.ruleengine.core.rule.RuleAction;
import com.mkt.ruleengine.core.rule.RuleSnapshot;
import com.mkt.ruleengine.core.spi.ActionDefinitionRegistry;
import com.mkt.ruleengine.core.spi.ActionDispatchExecutor;
import com.mkt.ruleengine.core.spi.ActionLogWriter;
import com.mkt.ruleengine.core.spi.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 阶段四：动作执行。
 * 对命中规则的每个动作：动作定义解析（类型 + 默认参数合并）→ 参数解析（${} / #{}）→
 * 幂等校验 → 执行器分发（同步/异步）。
 */
public class ActionExecuteStage implements EngineStage {

    private static final Logger log = LoggerFactory.getLogger(ActionExecuteStage.class);

    private final ActionDefinitionRegistry actionDefinitionRegistry;
    private final ActionExecutorFactory actionExecutorFactory;
    private final ActionParamResolver paramResolver;
    private final ActionDispatchExecutor dispatchExecutor;
    private final IdempotencyStore idempotencyStore;
    private final ActionLogWriter actionLogWriter;

    public ActionExecuteStage(ActionDefinitionRegistry actionDefinitionRegistry,
                              ActionExecutorFactory actionExecutorFactory,
                              ActionParamResolver paramResolver,
                              ActionDispatchExecutor dispatchExecutor,
                              IdempotencyStore idempotencyStore,
                              ActionLogWriter actionLogWriter) {
        this.actionDefinitionRegistry = actionDefinitionRegistry;
        this.actionExecutorFactory = actionExecutorFactory;
        this.paramResolver = paramResolver;
        this.dispatchExecutor = dispatchExecutor;
        this.idempotencyStore = idempotencyStore;
        this.actionLogWriter = actionLogWriter;
    }

    @Override
    public void handle(EngineContext ctx, StageChain chain) {
        EvaluationContext evalCtx = new EvaluationContext(ctx.getEvent(), ctx.getAttributes(), ctx.getUserProfile());
        for (RuleSnapshot rule : ctx.getMatchedRules()) {
            for (RuleAction action : rule.getActions()) {
                executeAction(ctx, rule, action, evalCtx);
            }
        }
        chain.proceed(ctx);
    }

    private void executeAction(EngineContext ctx, RuleSnapshot rule, RuleAction action, EvaluationContext evalCtx) {
        String key = ctx.getEvent().getEventId() + ":" + rule.getRuleCode() + ":" + action.getActionCode();
        if (!idempotencyStore.tryAcquire(key)) {
            ctx.addActionRecord(new EngineResult.ActionExecutionRecord(
                    rule.getRuleCode(), action.getActionCode(), true, "idempotent-skip", 0));
            recordActionTrace(ctx, rule, action, null, true, "idempotent-skip", 0);
            return;
        }
        try {
            Optional<ActionDefinition> defOpt = actionDefinitionRegistry.findByCode(action.getActionCode());
            if (defOpt.isEmpty()) {
                idempotencyStore.release(key);
                ctx.addActionRecord(new EngineResult.ActionExecutionRecord(
                        rule.getRuleCode(), action.getActionCode(), false, "action not configured", 0));
                recordActionTrace(ctx, rule, action, null, false, "action not configured", 0);
                return;
            }
            ActionDefinition def = defOpt.get();
            ActionExecutor executor = actionExecutorFactory.get(def.getActionType());
            // 默认参数 + 规则参数（规则参数覆盖默认值）
            Map<String, Object> merged = new LinkedHashMap<>(def.getDefaults());
            merged.putAll(paramResolver.resolve(action, evalCtx));
            ActionExecutionContext actionCtx = new ActionExecutionContext(
                    ctx.getEvent(), rule, action, merged, ctx.getAttributes(), ctx.getUserProfile());
            // 模拟触发时动作强制同步执行，保证响应即可看到执行结果
            boolean async = !ctx.isSimulation() && action.isAsync();
            dispatchExecutor.dispatch(async, actionCtx, executor, result -> {
                ctx.addActionRecord(new EngineResult.ActionExecutionRecord(
                        rule.getRuleCode(), action.getActionCode(), result.success(), result.detail(), result.costMs()));
                recordActionTrace(ctx, rule, action, merged, result.success(), result.detail(), result.costMs());
                try {
                    actionLogWriter.write(actionCtx, result);
                } catch (Exception e) {
                    log.warn("action log write failed: {}", key, e);
                }
            });
        } catch (Exception e) {
            idempotencyStore.release(key);
            ctx.addActionRecord(new EngineResult.ActionExecutionRecord(
                    rule.getRuleCode(), action.getActionCode(), false, "error: " + e.getMessage(), 0));
            recordActionTrace(ctx, rule, action, null, false, "error: " + e.getMessage(), 0);
            log.error("action execute error: {}", key, e);
        }
    }

    /** 模拟模式下记录动作执行明细（含解析后参数） */
    private void recordActionTrace(EngineContext ctx, RuleSnapshot rule, RuleAction action,
                                   Map<String, Object> params, boolean success, String detail, long costMs) {
        if (!ctx.isSimulation()) {
            return;
        }
        ctx.trace().recordAction(rule.getRuleCode(),
                new EngineTrace.ActionTrace(action.getActionCode(), success, detail, params, costMs));
    }
}
