package com.mkt.ruleengine.core.rule;

import com.mkt.ruleengine.core.spi.ExpressionEvaluator;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 默认条件树求值器：
 * <ul>
 *   <li>组合模式递归遍历 {@link LogicConditionNode} / {@link LeafConditionNode}</li>
 *   <li>操作符求值采用策略模式（{@link OperatorEvaluator} 策略表）</li>
 *   <li>EXPRESSION 操作符委托 {@link ExpressionEvaluator}（默认 SpEL，可选 QLExpress）</li>
 * </ul>
 */
public class DefaultConditionEvaluator implements ConditionEvaluator {

    /** 操作符求值策略（策略模式） */
    public interface OperatorEvaluator {
        boolean eval(Object fieldValue, Object threshold, ValueType valueType);
    }

    private final Map<CompareOp, OperatorEvaluator> operators = new EnumMap<>(CompareOp.class);
    private final ExpressionEvaluator expressionEvaluator;

    public DefaultConditionEvaluator(ExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
        registerDefaults();
    }

    private void registerDefaults() {
        operators.put(CompareOp.EQUALS, (field, threshold, vt) -> ValueCoercer.valueEquals(coerce(field, vt), coerce(threshold, vt)));
        operators.put(CompareOp.NOT_EQUALS, (field, threshold, vt) -> !ValueCoercer.valueEquals(coerce(field, vt), coerce(threshold, vt)));
        operators.put(CompareOp.GT, (field, threshold, vt) -> compareField(field, threshold, vt) > 0);
        operators.put(CompareOp.GTE, (field, threshold, vt) -> compareField(field, threshold, vt) >= 0);
        operators.put(CompareOp.LT, (field, threshold, vt) -> compareField(field, threshold, vt) < 0);
        operators.put(CompareOp.LTE, (field, threshold, vt) -> compareField(field, threshold, vt) <= 0);
        operators.put(CompareOp.IN, (field, threshold, vt) -> in(field, threshold, vt, false));
        operators.put(CompareOp.NOT_IN, (field, threshold, vt) -> in(field, threshold, vt, true));
        operators.put(CompareOp.BETWEEN, this::between);
        operators.put(CompareOp.CONTAINS, (field, threshold, vt) -> {
            String f = str(field);
            return f != null && f.contains(String.valueOf(coerce(threshold, ValueType.STRING)));
        });
        operators.put(CompareOp.STARTS_WITH, (field, threshold, vt) -> {
            String f = str(field);
            return f != null && f.startsWith(String.valueOf(coerce(threshold, ValueType.STRING)));
        });
        operators.put(CompareOp.EXISTS, (field, threshold, vt) -> field != null);
        operators.put(CompareOp.NOT_EXISTS, (field, threshold, vt) -> field == null);
        operators.put(CompareOp.EXPRESSION, (field, threshold, vt) -> {
            throw new IllegalStateException("EXPRESSION operator handled separately");
        });
    }

    @Override
    public boolean evaluate(ConditionNode node, EvaluationContext ctx) {
        if (node == null) {
            return true;
        }
        return switch (node.nodeType()) {
            case LOGIC -> evaluateLogic((LogicConditionNode) node, ctx);
            case LEAF -> evaluateLeaf((LeafConditionNode) node, ctx);
        };
    }

    private boolean evaluateLogic(LogicConditionNode node, EvaluationContext ctx) {
        return switch (node.getLogic()) {
            case AND -> node.getChildren().stream().allMatch(child -> evaluate(child, ctx));
            case OR -> node.getChildren().stream().anyMatch(child -> evaluate(child, ctx));
            case NOT -> {
                if (node.getChildren().size() != 1) {
                    throw new IllegalStateException("NOT node must have exactly one child");
                }
                yield !evaluate(node.getChildren().get(0), ctx);
            }
        };
    }

    private boolean evaluateLeaf(LeafConditionNode leaf, EvaluationContext ctx) {
        if (leaf.getOperator() == CompareOp.EXPRESSION) {
            boolean result = expressionEvaluator.evaluateBoolean(leaf.getExpression(), ctx.expressionVariables());
            return leaf.isNot() != result;
        }
        Object fieldValue = ctx.resolveField(leaf.getField());
        OperatorEvaluator evaluator = Optional.ofNullable(operators.get(leaf.getOperator()))
                .orElseThrow(() -> new IllegalStateException("unsupported operator: " + leaf.getOperator()));
        boolean result = evaluator.eval(fieldValue, leaf.getValue(), leaf.getValueType());
        return leaf.isNot() != result;
    }

    // ---------- 操作符实现 ----------

    private Object coerce(Object raw, ValueType vt) {
        if (raw == null) {
            return null;
        }
        // 字段值类型未知时按阈值类型强转
        return switch (vt) {
            case STRING -> raw instanceof String ? raw : String.valueOf(raw);
            case NUMBER -> ValueCoercer.toBigDecimal(raw);
            case BOOLEAN -> ValueCoercer.toBoolean(raw);
            case DATETIME -> ValueCoercer.toEpochMillis(raw);
            case LIST -> ValueCoercer.toList(raw);
        };
    }

    private int compareField(Object field, Object threshold, ValueType vt) {
        Object f = coerce(field, vt);
        Object t = coerce(threshold, vt);
        if (f == null || t == null) {
            return -1;
        }
        return ValueCoercer.compare(f, t);
    }

    private boolean in(Object field, Object threshold, ValueType vt, boolean negate) {
        if (field == null || threshold == null) {
            return false;
        }
        List<Object> list = ValueCoercer.toList(threshold);
        boolean hit;
        if (field instanceof List<?> fieldList) {
            // 字段为集合（如用户标签数组）：任一元素命中即命中
            hit = fieldList.stream().anyMatch(f ->
                    list.stream().anyMatch(item -> ValueCoercer.valueEquals(coerce(f, vt), coerce(item, vt))));
        } else {
            hit = list.stream().anyMatch(item -> ValueCoercer.valueEquals(coerce(field, vt), coerce(item, vt)));
        }
        return negate != hit;
    }

    private boolean between(Object field, Object threshold, ValueType vt) {
        if (field == null || !(threshold instanceof List<?> list) || list.size() != 2) {
            return false;
        }
        Object f = coerce(field, vt);
        Object min = coerce(list.get(0), vt);
        Object max = coerce(list.get(1), vt);
        if (f == null || min == null || max == null) {
            return false;
        }
        return ValueCoercer.compare(f, min) >= 0 && ValueCoercer.compare(f, max) <= 0;
    }

    private String str(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }
}
