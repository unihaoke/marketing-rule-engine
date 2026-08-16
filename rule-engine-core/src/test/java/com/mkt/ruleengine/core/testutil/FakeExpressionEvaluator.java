package com.mkt.ruleengine.core.testutil;

import com.mkt.ruleengine.core.spi.ExpressionEvaluator;

import java.util.Map;

/**
 * 测试用简化表达式求值器：支持算术（+ - * /）、比较（> >= < <= == !=）、
 * && 、|| 、! 与括号组合。
 */
public class FakeExpressionEvaluator implements ExpressionEvaluator {

    @Override
    public Object evaluate(String expression, Map<String, Object> variables) {
        String expr = expression.trim();
        // 算术优先级从高到低
        for (String op : new String[]{"*", "/", "+", "-"}) {
            int idx = topLevelIndexOf(expr, op);
            if (idx > 0) {
                double a = toNumber(resolve(expr.substring(0, idx).trim(), variables));
                double b = toNumber(resolve(expr.substring(idx + 1).trim(), variables));
                return switch (op) {
                    case "*" -> a * b;
                    case "/" -> a / b;
                    case "+" -> a + b;
                    default -> a - b;
                };
            }
        }
        return evalBoolean(expr, variables);
    }

    private Object resolve(String token, Map<String, Object> vars) {
        if (vars.containsKey(token)) {
            return vars.get(token);
        }
        return token;
    }

    private boolean evalBoolean(String expr, Map<String, Object> vars) {
        expr = expr.trim();
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return evalBoolean(expr.substring(1, expr.length() - 1), vars);
        }
        int orIdx = topLevelIndexOf(expr, "||");
        if (orIdx >= 0) {
            return evalBoolean(expr.substring(0, orIdx), vars)
                    || evalBoolean(expr.substring(orIdx + 2), vars);
        }
        int andIdx = topLevelIndexOf(expr, "&&");
        if (andIdx >= 0) {
            return evalBoolean(expr.substring(0, andIdx), vars)
                    && evalBoolean(expr.substring(andIdx + 2), vars);
        }
        if (expr.startsWith("!")) {
            return !evalBoolean(expr.substring(1), vars);
        }
        if ("true".equals(expr)) {
            return true;
        }
        if ("false".equals(expr)) {
            return false;
        }
        for (String op : new String[]{">=", "<=", "==", "!=", ">", "<"}) {
            int idx = expr.indexOf(op);
            if (idx > 0) {
                double fv = toNumber(resolve(expr.substring(0, idx).trim(), vars));
                double rv = toNumber(resolve(expr.substring(idx + op.length()).trim(), vars));
                return switch (op) {
                    case ">=" -> fv >= rv;
                    case "<=" -> fv <= rv;
                    case "==" -> fv == rv;
                    case "!=" -> fv != rv;
                    case ">" -> fv > rv;
                    default -> fv < rv;
                };
            }
        }
        throw new IllegalArgumentException("unsupported expr: " + expr);
    }

    /** 顶层（括号外）查找操作符 */
    private int topLevelIndexOf(String s, String token) {
        int depth = 0;
        for (int i = 0; i < s.length() - token.length() + 1; i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && s.startsWith(token, i)) {
                return i;
            }
        }
        return -1;
    }

    private double toNumber(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(String.valueOf(v));
    }
}
