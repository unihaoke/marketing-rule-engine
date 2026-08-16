package com.mkt.ruleengine.ext.liteflow;

import com.mkt.ruleengine.core.engine.EngineContext;
import com.mkt.ruleengine.core.engine.EngineStage;
import com.mkt.ruleengine.core.engine.EventNormalizeStage;
import com.mkt.ruleengine.core.engine.FunctionEnhanceStage;
import com.mkt.ruleengine.core.engine.RuleMatchStage;
import com.mkt.ruleengine.core.engine.StageChain;
import com.mkt.ruleengine.core.engine.ActionExecuteStage;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import java.util.List;

/**
 * LiteFlow 流程节点：将默认责任链四阶段包装为 LiteFlow NodeComponent。
 * 每个组件从 LiteFlow 上下文中取 {@link EngineContext}，以终止链执行对应阶段逻辑。
 */
public class LiteFlowStageComponents {

    /** 终止链：LiteFlow 编排天然提供"下一个"，阶段内不再推进 */
    private static final StageChain TERMINAL = new StageChain(List.of());

    private static <T extends EngineStage> T find(List<EngineStage> stages, Class<T> type) {
        return stages.stream().filter(type::isInstance).map(type::cast)
                .findFirst().orElseThrow(() -> new IllegalStateException("stage not found: " + type.getSimpleName()));
    }

    @LiteflowComponent("normalizeStage")
    public static class NormalizeComponent extends NodeComponent {
        private final EventNormalizeStage stage;

        public NormalizeComponent(List<EngineStage> stages) {
            this.stage = find(stages, EventNormalizeStage.class);
        }

        @Override
        public void process() {
            stage.handle(getContextBean(EngineContext.class), TERMINAL);
        }
    }

    @LiteflowComponent("enhanceStage")
    public static class EnhanceComponent extends NodeComponent {
        private final FunctionEnhanceStage stage;

        public EnhanceComponent(List<EngineStage> stages) {
            this.stage = find(stages, FunctionEnhanceStage.class);
        }

        @Override
        public void process() {
            stage.handle(getContextBean(EngineContext.class), TERMINAL);
        }
    }

    @LiteflowComponent("matchStage")
    public static class MatchComponent extends NodeComponent {
        private final RuleMatchStage stage;

        public MatchComponent(List<EngineStage> stages) {
            this.stage = find(stages, RuleMatchStage.class);
        }

        @Override
        public void process() {
            stage.handle(getContextBean(EngineContext.class), TERMINAL);
        }
    }

    @LiteflowComponent("actionStage")
    public static class ActionComponent extends NodeComponent {
        private final ActionExecuteStage stage;

        public ActionComponent(List<EngineStage> stages) {
            this.stage = find(stages, ActionExecuteStage.class);
        }

        @Override
        public void process() {
            stage.handle(getContextBean(EngineContext.class), TERMINAL);
        }
    }
}
