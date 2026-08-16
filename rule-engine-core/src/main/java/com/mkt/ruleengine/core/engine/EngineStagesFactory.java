package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.action.ActionExecutorFactory;
import com.mkt.ruleengine.core.rule.ConditionEvaluator;
import com.mkt.ruleengine.core.spi.ActionDispatchExecutor;
import com.mkt.ruleengine.core.spi.ActionLogWriter;
import com.mkt.ruleengine.core.spi.EventDefinitionRegistry;
import com.mkt.ruleengine.core.spi.IdempotencyStore;
import com.mkt.ruleengine.core.spi.UserProfileResolver;

import java.util.Comparator;
import java.util.List;

/**
 * 责任链阶段装配器（工厂方法模式）：按固定顺序组装内置阶段。
 */
public final class EngineStagesFactory {

    private EngineStagesFactory() {
    }

    /**
     * 创建默认责任链：归一化 → 函数增强 → 规则匹配 → 动作执行。
     */
    public static List<EngineStage> createDefaultStages(EventDefinitionRegistry eventRegistry,
                                                        UserProfileResolver userProfileResolver,
                                                        com.mkt.ruleengine.core.function.FunctionRegistry functionRegistry,
                                                        ConditionEvaluator conditionEvaluator,
                                                        ActionExecutorFactory actionExecutorFactory,
                                                        ActionParamResolver paramResolver,
                                                        ActionDispatchExecutor dispatchExecutor,
                                                        IdempotencyStore idempotencyStore,
                                                        ActionLogWriter actionLogWriter,
                                                        com.mkt.ruleengine.core.spi.ActionDefinitionRegistry actionDefinitionRegistry,
                                                        boolean strictEventValidation) {
        List<StageHolder> holders = new java.util.ArrayList<>();
        holders.add(new StageHolder(EngineStageOrder.NORMALIZE, new EventNormalizeStage(
                eventRegistry, userProfileResolver, strictEventValidation)));
        holders.add(new StageHolder(EngineStageOrder.ENHANCE, new FunctionEnhanceStage(functionRegistry)));
        holders.add(new StageHolder(EngineStageOrder.MATCH, new RuleMatchStage(conditionEvaluator)));
        holders.add(new StageHolder(EngineStageOrder.ACTION, new ActionExecuteStage(
                actionDefinitionRegistry, actionExecutorFactory, paramResolver, dispatchExecutor,
                idempotencyStore, actionLogWriter)));
        holders.sort(Comparator.comparingInt(StageHolder::order));
        return holders.stream().map(StageHolder::stage).toList();
    }

    /** 阶段顺序包装 */
    private record StageHolder(int order, EngineStage stage) {
    }
}
