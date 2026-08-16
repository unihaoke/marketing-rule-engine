package com.mkt.ruleengine.core.rule;

/**
 * 条件树求值 SPI（组合模式遍历）。
 */
public interface ConditionEvaluator {

    /**
     * 递归求值条件树。
     */
    boolean evaluate(ConditionNode node, EvaluationContext ctx);
}
