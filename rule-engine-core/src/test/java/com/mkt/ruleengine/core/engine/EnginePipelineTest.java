package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.event.MarketingEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 责任链测试：顺序执行 / 提前中断。
 */
class EnginePipelineTest {

    private MarketingEvent event() {
        return new MarketingEvent("EVT", "u1", "APP", System.currentTimeMillis(), java.util.Map.of());
    }

    @Test
    void stagesRunInOrder() {
        List<String> trace = new ArrayList<>();
        EngineStage stageA = (ctx, chain) -> {
            trace.add("A");
            chain.proceed(ctx);
        };
        EngineStage stageB = (ctx, chain) -> {
            trace.add("B");
            chain.proceed(ctx);
        };
        EngineStage stageC = (ctx, chain) -> {
            trace.add("C");
            chain.proceed(ctx);
        };
        StageChain chain = EnginePipeline.of(stageA, stageB, stageC);
        chain.start(new EngineContext(event(), System.currentTimeMillis()));
        assertEquals(List.of("A", "B", "C"), trace);
    }

    @Test
    void breakChainStopsRemainingStages() {
        List<String> trace = new ArrayList<>();
        EngineStage stageA = (ctx, chain) -> {
            trace.add("A");
            chain.proceed(ctx);
        };
        EngineStage stageB = (ctx, chain) -> {
            trace.add("B");
            chain.breakChain(); // 中断
        };
        EngineStage stageC = (ctx, chain) -> {
            trace.add("C");
            chain.proceed(ctx);
        };
        StageChain chain = EnginePipeline.of(stageA, stageB, stageC);
        chain.start(new EngineContext(event(), System.currentTimeMillis()));
        assertEquals(List.of("A", "B"), trace);
    }

    @Test
    void engineContextResultAggregation() {
        EngineContext ctx = new EngineContext(event(), System.currentTimeMillis());
        ctx.putAttribute("score", 42);
        ctx.addMatchedRule(new com.mkt.ruleengine.core.rule.RuleSnapshot(
                "R1", "规则1", "EVT", null, 1, null, List.of(), List.of(),
                com.mkt.ruleengine.core.gray.GrayConfig.fullRelease(), 1));
        EngineResult result = ctx.toResult(System.currentTimeMillis());
        assertTrue(result.isSuccess());
        assertEquals(List.of("R1"), result.getMatchedRuleCodes());
        assertEquals(42, result.getAttributes().get("score"));
        assertTrue(result.getCostMs() >= 0);
    }
}
