package com.mkt.ruleengine.ext.liteflow;

import com.mkt.ruleengine.core.engine.EngineContext;
import com.mkt.ruleengine.core.engine.StageChain;
import com.mkt.ruleengine.core.event.MarketingEvent;
import com.mkt.ruleengine.core.rule.RuleSnapshot;
import com.mkt.ruleengine.core.spi.EngineLogRecorder;
import com.mkt.ruleengine.core.spi.EventDefinitionRegistry;
import com.mkt.ruleengine.core.spi.RuleSnapshotCache;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * LiteFlow 编排版规则引擎：与默认引擎同一模板方法骨架（{@link com.mkt.ruleengine.core.engine.RuleEngine}），
 * 仅将「责任链执行」替换为 LiteFlow EL 流程（THEN(normalize, enhance, match, action)）。
 * 启用：添加 rule-engine-ext-liteflow 依赖并配置 liteflow.rule-source=config/flow.el.xml。
 */
public class LiteFlowRuleEngine extends com.mkt.ruleengine.core.engine.RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(LiteFlowRuleEngine.class);

    /** LiteFlow EL 链名（与 flow.el.xml 中 chain name 对应） */
    public static final String ENGINE_CHAIN = "engineChain";

    private final RuleSnapshotCache snapshotCache;
    private final EventDefinitionRegistry eventDefinitionRegistry;
    private final boolean strictEventValidation;
    private final FlowExecutor flowExecutor;
    private final EngineLogRecorder logRecorder;

    public LiteFlowRuleEngine(RuleSnapshotCache snapshotCache,
                              EventDefinitionRegistry eventDefinitionRegistry,
                              boolean strictEventValidation,
                              FlowExecutor flowExecutor,
                              EngineLogRecorder logRecorder) {
        this.snapshotCache = snapshotCache;
        this.eventDefinitionRegistry = eventDefinitionRegistry;
        this.strictEventValidation = strictEventValidation;
        this.flowExecutor = flowExecutor;
        this.logRecorder = logRecorder;
    }

    @Override
    protected List<RuleSnapshot> loadSnapshots(String eventCode) {
        return snapshotCache.get(eventCode);
    }

    /**
     * 责任链装配钩子 → LiteFlow EL 流程（单阶段桥接，实际编排在 LiteFlow 组件内）。
     */
    @Override
    protected StageChain buildChain() {
        return com.mkt.ruleengine.core.engine.EnginePipeline.of((ctx, chain) -> {
            try {
                LiteflowResponse response = flowExecutor.execute2Resp(ENGINE_CHAIN, ctx, EngineContext.class);
                if (!response.isSuccess()) {
                    ctx.markFailed("liteflow chain failed: " + response.getMessage());
                }
            } catch (Exception e) {
                ctx.markFailed("liteflow execute error: " + e.getMessage());
                log.error("liteflow execute error, eventId={}", ctx.getEvent().getEventId(), e);
            }
            chain.proceed(ctx);
        });
    }

    @Override
    protected void validate(MarketingEvent event) {
        super.validate(event);
        if (strictEventValidation && !eventDefinitionRegistry.exists(event.getEventCode())) {
            throw new com.mkt.ruleengine.core.exception.EventNotDefinedException(event.getEventCode());
        }
    }

    @Override
    protected void onFinished(EngineContext ctx) {
        if (logRecorder != null) {
            logRecorder.record(ctx.toResult(System.currentTimeMillis()));
        }
    }
}
