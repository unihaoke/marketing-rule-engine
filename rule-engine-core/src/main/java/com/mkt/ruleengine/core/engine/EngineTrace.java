package com.mkt.ruleengine.core.engine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 引擎执行追踪（模拟触发 / 排障）：记录每条被评估规则的匹配结论，
 * 以及每个动作的执行明细（含解析后的参数），帮助运营确认"规则为何命中/未命中、动作是否触发"。
 */
public class EngineTrace {

    /** 动作执行追踪 */
    public record ActionTrace(String actionCode, boolean success, String detail,
                              Map<String, Object> params, long costMs) {
    }

    /** 规则评估追踪 */
    public record RuleTrace(String ruleCode, String ruleName, long versionNo, boolean matched,
                            String skipReason, long costMs, List<ActionTrace> actions) {
    }

    private final List<RuleTrace> rules = new CopyOnWriteArrayList<>();
    private final Map<String, List<ActionTrace>> actionsByRule = new ConcurrentHashMap<>();

    public void recordRule(RuleTrace trace) {
        rules.add(trace);
    }

    public void recordAction(String ruleCode, ActionTrace action) {
        actionsByRule.computeIfAbsent(ruleCode, k -> new CopyOnWriteArrayList<>()).add(action);
    }

    /** 规则评估明细（按评估顺序，动作按执行顺序合并） */
    public List<RuleTrace> rules() {
        return rules.stream()
                .map(r -> new RuleTrace(r.ruleCode(), r.ruleName(), r.versionNo(), r.matched(),
                        r.skipReason(), r.costMs(),
                        actionsByRule.getOrDefault(r.ruleCode(), List.of())))
                .toList();
    }
}
