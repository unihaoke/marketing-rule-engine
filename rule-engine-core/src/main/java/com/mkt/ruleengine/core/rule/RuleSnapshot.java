package com.mkt.ruleengine.core.rule;

import com.mkt.ruleengine.core.gray.GrayConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则快照：规则组发布后的不可变副本，引擎运行时只读使用（缓存 + 热更新单元）。
 */
public final class RuleSnapshot {

    private final String ruleCode;
    private final String ruleName;
    private final String eventCode;
    private final String description;
    private final int priority;
    private final ConditionNode conditionTree;
    private final List<RuleFunction> functions;
    private final List<RuleAction> actions;
    private final GrayConfig gray;
    /** 版本号（对应 RuleVersion.versionNo） */
    private final long versionNo;

    public RuleSnapshot(String ruleCode, String ruleName, String eventCode, String description,
                        int priority, ConditionNode conditionTree, List<RuleFunction> functions,
                        List<RuleAction> actions, GrayConfig gray, long versionNo) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.eventCode = eventCode;
        this.description = description;
        this.priority = priority;
        this.conditionTree = conditionTree;
        this.functions = functions == null ? List.of() : List.copyOf(functions);
        this.actions = actions == null ? List.of() : List.copyOf(actions);
        this.gray = gray == null ? GrayConfig.fullRelease() : gray;
        this.versionNo = versionNo;
    }

    /** 从规则组构建快照 */
    public static RuleSnapshot from(RuleGroup group, long versionNo) {
        return new RuleSnapshot(
                group.getRuleCode(),
                group.getRuleName(),
                group.getEventCode(),
                group.getDescription(),
                group.getPriority(),
                group.getConditionTree(),
                new ArrayList<>(group.getFunctions()),
                new ArrayList<>(group.getActions()),
                group.getGray(),
                versionNo);
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getEventCode() {
        return eventCode;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public ConditionNode getConditionTree() {
        return conditionTree;
    }

    public List<RuleFunction> getFunctions() {
        return Collections.unmodifiableList(functions);
    }

    public List<RuleAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public GrayConfig getGray() {
        return gray;
    }

    public long getVersionNo() {
        return versionNo;
    }

    @Override
    public String toString() {
        return "RuleSnapshot{" + ruleCode + "@v" + versionNo + ", event=" + eventCode + '}';
    }
}
