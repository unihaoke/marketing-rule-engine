package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.gray.GrayContext;
import com.mkt.ruleengine.core.gray.GrayStrategyFactory;
import com.mkt.ruleengine.core.rule.ConditionEvaluator;
import com.mkt.ruleengine.core.rule.EvaluationContext;
import com.mkt.ruleengine.core.rule.RuleSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 阶段三：规则匹配。
 * 对绑定事件的全部规则快照（按优先级排序）依次：灰度放行判定 → 条件树求值，命中则记录。
 */
public class RuleMatchStage implements EngineStage {

    private static final Logger log = LoggerFactory.getLogger(RuleMatchStage.class);

    private final ConditionEvaluator conditionEvaluator;

    public RuleMatchStage(ConditionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
    }

    @Override
    public void handle(EngineContext ctx, StageChain chain) {
        EvaluationContext evalCtx = new EvaluationContext(ctx.getEvent(), ctx.getAttributes(), ctx.getUserProfile());
        for (RuleSnapshot snapshot : ctx.getSnapshots()) {
            long ruleStart = System.nanoTime();
            try {
                // 1. 灰度放行
                boolean grayHit = GrayStrategyFactory.decide(snapshot.getGray(),
                        new GrayContext(ctx.getEvent(), snapshot.getRuleCode()));
                if (!grayHit) {
                    log.debug("rule gray skip: {} eventId={}", snapshot.getRuleCode(), ctx.getEvent().getEventId());
                    recordTrace(ctx, snapshot, false, "GRAY_SKIP", ruleStart);
                    continue;
                }
                // 2. 条件树求值
                boolean matched = conditionEvaluator.evaluate(snapshot.getConditionTree(), evalCtx);
                if (matched) {
                    ctx.addMatchedRule(snapshot);
                    log.debug("rule matched: {} eventId={}", snapshot.getRuleCode(), ctx.getEvent().getEventId());
                    recordTrace(ctx, snapshot, true, null, ruleStart);
                } else {
                    recordTrace(ctx, snapshot, false, "CONDITION_FAIL", ruleStart);
                }
            } catch (Exception e) {
                log.error("rule evaluate error: {} eventId={}", snapshot.getRuleCode(), ctx.getEvent().getEventId(), e);
                recordTrace(ctx, snapshot, false, "EVALUATE_ERROR", ruleStart);
            }
        }
        chain.proceed(ctx);
    }

    /** 模拟模式下记录规则评估结论（含耗时） */
    private void recordTrace(EngineContext ctx, RuleSnapshot snapshot, boolean matched, String skipReason, long ruleStart) {
        if (!ctx.isSimulation()) {
            return;
        }
        long costMs = (System.nanoTime() - ruleStart) / 1_000_000;
        ctx.trace().recordRule(new EngineTrace.RuleTrace(
                snapshot.getRuleCode(), snapshot.getRuleName(), snapshot.getVersionNo(),
                matched, skipReason, costMs, null));
    }
}
