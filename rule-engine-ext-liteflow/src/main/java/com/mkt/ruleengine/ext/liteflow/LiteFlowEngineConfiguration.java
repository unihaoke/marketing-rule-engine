package com.mkt.ruleengine.ext.liteflow;

import com.mkt.ruleengine.core.spi.EngineLogRecorder;
import com.mkt.ruleengine.core.spi.EventDefinitionRegistry;
import com.mkt.ruleengine.core.spi.RuleSnapshotCache;
import com.mkt.ruleengine.infrastructure.config.RuleEngineProperties;
import com.yomahub.liteflow.core.FlowExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * LiteFlow 编排引擎装配：通过 rule-engine.liteflow.enabled=true 启用，
 * 替换默认内存责任链引擎（模板方法骨架不变，仅换编排实现）。
 */
@Configuration
@ConditionalOnClass(name = "com.yomahub.liteflow.core.FlowExecutor")
@ConditionalOnProperty(prefix = "rule-engine.liteflow", name = "enabled", havingValue = "true")
public class LiteFlowEngineConfiguration {

    @Bean
    @Primary
    public com.mkt.ruleengine.core.engine.RuleEngine liteFlowRuleEngine(RuleSnapshotCache snapshotCache,
                                                                        EventDefinitionRegistry eventDefinitionRegistry,
                                                                        FlowExecutor flowExecutor,
                                                                        EngineLogRecorder engineLogRecorder,
                                                                        RuleEngineProperties properties) {
        return new LiteFlowRuleEngine(snapshotCache, eventDefinitionRegistry,
                properties.strictEventValidation(), flowExecutor, engineLogRecorder);
    }
}
