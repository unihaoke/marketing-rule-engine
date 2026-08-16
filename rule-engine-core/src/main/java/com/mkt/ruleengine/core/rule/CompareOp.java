package com.mkt.ruleengine.core.rule;

/**
 * 叶子条件比较操作符。
 */
public enum CompareOp {
    /** 等于 */
    EQUALS("="),
    /** 不等于 */
    NOT_EQUALS("!="),
    /** 大于 */
    GT(">"),
    /** 大于等于 */
    GTE(">="),
    /** 小于 */
    LT("<"),
    /** 小于等于 */
    LTE("<="),
    /** 属于集合 */
    IN("IN"),
    /** 不属于集合 */
    NOT_IN("NOT IN"),
    /** 区间 [min, max] */
    BETWEEN("BETWEEN"),
    /** 字符串包含 */
    CONTAINS("CONTAINS"),
    /** 字符串前缀 */
    STARTS_WITH("STARTS_WITH"),
    /** 字段存在 */
    EXISTS("EXISTS"),
    /** 字段不存在 */
    NOT_EXISTS("NOT EXISTS"),
    /** 自定义表达式（QLExpress / SpEL），结果必须为布尔 */
    EXPRESSION("EXPRESSION");

    private final String symbol;

    CompareOp(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
