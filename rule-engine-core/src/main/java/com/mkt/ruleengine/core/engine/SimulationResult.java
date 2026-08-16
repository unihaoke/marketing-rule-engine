package com.mkt.ruleengine.core.engine;

import java.util.List;

/**
 * 模拟触发结果：完整执行结果 + 规则评估追踪明细。
 */
public record SimulationResult(EngineResult result, List<EngineTrace.RuleTrace> rules) {
}
