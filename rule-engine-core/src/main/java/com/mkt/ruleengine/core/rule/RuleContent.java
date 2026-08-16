package com.mkt.ruleengine.core.rule;

import com.mkt.ruleengine.core.gray.GrayConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则画布内容（版本快照 / 持久化 JSON 结构）：条件树 + 前置函数 + 动作 + 灰度。
 */
public class RuleContent {

    private String ruleName;
    private String description;
    private ConditionNode conditionTree;
    private List<RuleFunction> functions = new ArrayList<>();
    private List<RuleAction> actions = new ArrayList<>();
    private GrayConfig gray = GrayConfig.fullRelease();

    public RuleContent() {
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ConditionNode getConditionTree() {
        return conditionTree;
    }

    public void setConditionTree(ConditionNode conditionTree) {
        this.conditionTree = conditionTree;
    }

    public List<RuleFunction> getFunctions() {
        return Collections.unmodifiableList(functions);
    }

    public void setFunctions(List<RuleFunction> functions) {
        this.functions = functions == null ? new ArrayList<>() : new ArrayList<>(functions);
    }

    public List<RuleAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public void setActions(List<RuleAction> actions) {
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
    }

    public GrayConfig getGray() {
        return gray;
    }

    public void setGray(GrayConfig gray) {
        this.gray = gray == null ? GrayConfig.fullRelease() : gray;
    }
}
