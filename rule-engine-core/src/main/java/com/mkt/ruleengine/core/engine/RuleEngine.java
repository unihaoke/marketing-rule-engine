package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.event.MarketingEvent;
import com.mkt.ruleengine.core.exception.RuleEngineException;
import com.mkt.ruleengine.core.rule.RuleSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 规则引擎抽象基类（模板方法模式）。
 *
 * <p>算法骨架固定：校验事件 → 加载规则快照 → 组装责任链 → 执行链路 → 结果汇总；
 * 子类通过钩子方法定制：{@link #loadSnapshots}（快照来源，缓存/远程）、
 * {@link #buildChain}（责任链装配，可替换为 LiteFlow 等编排）、
 * {@link #onFinished}（结果落库/审计）。</p>
 */
public abstract class RuleEngine {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 执行一次事件触发（模板方法，final 锁定骨架）。
     */
    public final EngineResult execute(MarketingEvent event) {
        return executeInternal(event, false).toResult(System.currentTimeMillis());
    }

    /**
     * 模拟触发（模板方法）：与 {@link #execute} 同一算法骨架，
     * 额外开启执行追踪（每条规则的匹配结论 + 动作明细），并将动作强制同步执行，便于页面直接查看效果。
     */
    public final SimulationResult simulate(MarketingEvent event) {
        EngineContext ctx = executeInternal(event, true);
        EngineResult result = ctx.toResult(System.currentTimeMillis());
        EngineTrace trace = ctx.trace();
        return new SimulationResult(result, trace == null ? List.of() : trace.rules());
    }

    private EngineContext executeInternal(MarketingEvent event, boolean simulation) {
        long start = System.currentTimeMillis();
        validate(event);
        EngineContext ctx = new EngineContext(event, start, simulation);
        try {
            ctx.setSnapshots(loadSnapshots(event.getEventCode()));
            StageChain chain = buildChain();
            chain.start(ctx);
        } catch (Exception e) {
            handleError(ctx, e);
        } finally {
            onFinished(ctx);
        }
        return ctx;
    }

    /**
     * 钩子：加载事件绑定的规则快照（本地缓存读取，热更新由缓存层保证）。
     */
    protected abstract List<RuleSnapshot> loadSnapshots(String eventCode);

    /**
     * 钩子：组装责任链（工厂方法，默认各阶段实现可替换）。
     */
    protected abstract StageChain buildChain();

    /**
     * 校验事件输入。
     */
    protected void validate(MarketingEvent event) {
        if (event == null || event.getEventCode() == null || event.getEventCode().isBlank()) {
            throw new RuleEngineException("eventCode must not be blank");
        }
    }

    /**
     * 链路异常处理（默认记录并标记失败，子类可重写）。
     */
    protected void handleError(EngineContext ctx, Exception e) {
        log.error("rule engine execute error, eventId={}", ctx.getEvent().getEventId(), e);
        ctx.markFailed(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }

    /**
     * 收尾钩子：结果落库 / 审计。
     */
    protected void onFinished(EngineContext ctx) {
        // 默认空实现
    }
}
