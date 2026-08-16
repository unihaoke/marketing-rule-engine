package com.mkt.ruleengine.core.spi;

/**
 * 动态表达式求值 SPI：默认实现为 SpEL，可选实现为 QLExpress（rule-engine-ext-qlexpress）。
 */
public interface ExpressionEvaluator {

    /**
     * 求值表达式。
     *
     * @param expression 表达式文本（SpEL / QLExpress 语法）
     * @param variables  变量上下文
     * @return 求值结果
     */
    Object evaluate(String expression, java.util.Map<String, Object> variables);

    /**
     * 布尔表达式求值。
     */
    default boolean evaluateBoolean(String expression, java.util.Map<String, Object> variables) {
        Object result = evaluate(expression, variables);
        if (result instanceof Boolean b) {
            return b;
        }
        if (result instanceof Number n) {
            return n.doubleValue() != 0;
        }
        if (result instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        throw new IllegalStateException("expression result is not boolean: " + result);
    }
}
