package com.mkt.ruleengine.core.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 嵌套逻辑条件节点：AND / OR / NOT（组合模式中的复合节点）。
 */
public class LogicConditionNode extends ConditionNode {

    private LogicOp logic = LogicOp.AND;

    private List<ConditionNode> children = new ArrayList<>();

    public LogicConditionNode() {
    }

    public LogicConditionNode(LogicOp logic, List<ConditionNode> children) {
        this.logic = logic;
        this.children = children == null ? new ArrayList<>() : new ArrayList<>(children);
    }

    public LogicOp getLogic() {
        return logic;
    }

    public void setLogic(LogicOp logic) {
        this.logic = logic;
    }

    public List<ConditionNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void setChildren(List<ConditionNode> children) {
        this.children = children == null ? new ArrayList<>() : new ArrayList<>(children);
    }

    public LogicConditionNode addChild(ConditionNode child) {
        this.children.add(child);
        return this;
    }

    @Override
    public NodeType nodeType() {
        return NodeType.LOGIC;
    }

    @Override
    public String toString() {
        return "LogicConditionNode{" + logic + ", children=" + children + '}';
    }
}
