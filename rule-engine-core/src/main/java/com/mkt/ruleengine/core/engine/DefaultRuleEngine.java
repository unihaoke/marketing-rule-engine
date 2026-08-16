package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.exception.EventNotDefinedException;
import com.mkt.ruleengine.core.rule.RuleSnapshot;
import com.mkt.ruleengine.core.spi.EngineLogRecorder;
import com.mkt.ruleengine.core.spi.EventDefinitionRegistry;
import com.mkt.ruleengine.core.spi.RuleSnapshotCache;

import java.util.List;

/**
 * 默认规则引擎：本地快照缓存 + 内置责任链阶段。
 */
public class DefaultRuleEngine extends RuleEngine {

    private final RuleSnapshotCache snapshotCache;
    private final EventDefinitionRegistry eventDefinitionRegistry;
    private final boolean strictEventValidation;
    private final List<EngineStage> stages;
    private final EngineLogRecorder logRecorder;

    public DefaultRuleEngine(RuleSnapshotCache snapshotCache,
                             EventDefinitionRegistry eventDefinitionRegistry,
                             boolean strictEventValidation,
                             List<EngineStage> stages,
                             EngineLogRecorder logRecorder) {
        this.snapshotCache = snapshotCache;
        this.eventDefinitionRegistry = eventDefinitionRegistry;
        this.strictEventValidation = strictEventValidation;
        this.stages = stages;
        this.logRecorder = logRecorder;
    }

    @Override
    protected List<RuleSnapshot> loadSnapshots(String eventCode) {
        return snapshotCache.get(eventCode);
    }

    @Override
    protected StageChain buildChain() {
        return EnginePipeline.of(stages);
    }

    @Override
    protected void validate(com.mkt.ruleengine.core.event.MarketingEvent event) {
        super.validate(event);
        if (strictEventValidation && !eventDefinitionRegistry.exists(event.getEventCode())) {
            throw new EventNotDefinedException(event.getEventCode());
        }
    }

    @Override
    protected void onFinished(EngineContext ctx) {
        if (logRecorder != null) {
            try {
                logRecorder.record(ctx.toResult(System.currentTimeMillis()));
            } catch (Exception e) {
                log.warn("record engine log failed, eventId={}", ctx.getEvent().getEventId(), e);
            }
        }
    }
}
