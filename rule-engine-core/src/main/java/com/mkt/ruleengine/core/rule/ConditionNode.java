package com.mkt.ruleengine.core.rule;

/**
 * 条件节点抽象（组合模式）：{@link LogicConditionNode} 组合节点 / {@link LeafConditionNode} 叶子节点。
 */
public abstract class ConditionNode {

    /** 节点类型：LOGIC / LEAF */
    public enum NodeType {
        LOGIC, LEAF
    }

    public abstract NodeType nodeType();
}
