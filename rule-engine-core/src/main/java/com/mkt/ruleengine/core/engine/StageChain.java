package com.mkt.ruleengine.core.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 责任链（Chain of Responsibility）：按序推进各处理阶段，支持提前中断。
 */
public class StageChain {

    private final List<EngineStage> stages;
    private int index = 0;

    public StageChain(List<EngineStage> stages) {
        this.stages = new ArrayList<>(stages);
    }

    /** 启动链路 */
    public void start(EngineContext ctx) {
        proceed(ctx);
    }

    /** 推进到下一阶段 */
    public void proceed(EngineContext ctx) {
        if (index < stages.size()) {
            EngineStage stage = stages.get(index++);
            stage.handle(ctx, this);
        }
    }

    /** 中断链路（后续阶段不再执行） */
    public void breakChain() {
        index = stages.size();
    }
}
