package com.mkt.ruleengine.core.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 责任链装配器（工厂方法）：将有序阶段列表组装为执行链。
 */
public final class EnginePipeline {

    private EnginePipeline() {
    }

    public static StageChain of(List<EngineStage> stages) {
        return new StageChain(stages == null ? List.of() : new ArrayList<>(stages));
    }

    public static StageChain of(EngineStage... stages) {
        return new StageChain(List.of(stages));
    }
}
