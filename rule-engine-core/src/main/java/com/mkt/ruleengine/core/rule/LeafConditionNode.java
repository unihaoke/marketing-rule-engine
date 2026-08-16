package com.mkt.ruleengine.core.rule;

import java.util.List;

/**
 * 叶子条件节点：field + operator + value。
 *
 * <p>示例：
 * <ul>
 *   <li>用户标签包含 NEW_USER：field=userTag, operator=IN, value=["NEW_USER"]</li>
 *   <li>订单金额 ≥ 100：field=orderAmount, operator=GTE, value=100</li>
 *   <li>连续打卡天数 ≥ 3（由自定义函数算出）：field=checkinStreak, operator=GTE, value=3</li>
 *   <li>自定义表达式：operator=EXPRESSION, expression="orderAmount >= 100 && userId != null"</li>
 * </ul>
 * </p>
 */
public class LeafConditionNode extends ConditionNode {

    /** 字段名：事件参数 / 函数增强属性 / 用户画像属性；EXPRESSION 操作符时可空 */
    private String field;

    /** 比较操作符 */
    private CompareOp operator = CompareOp.EQUALS;

    /** 阈值：标量或 List（IN / BETWEEN 用） */
    private Object value;

    /** 阈值类型 */
    private ValueType valueType = ValueType.STRING;

    /** 自定义表达式（operator == EXPRESSION 时生效，支持 SpEL / QLExpress） */
    private String expression;

    /** 整体取反（等价于 NOT 包裹，画布便捷项） */
    private boolean not;

    public LeafConditionNode() {
    }

    public LeafConditionNode(String field, CompareOp operator, Object value, ValueType valueType) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.valueType = valueType == null ? ValueType.STRING : valueType;
    }

    public static LeafConditionNode expression(String expression) {
        LeafConditionNode node = new LeafConditionNode();
        node.operator = CompareOp.EXPRESSION;
        node.expression = expression;
        return node;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public CompareOp getOperator() {
        return operator;
    }

    public void setOperator(CompareOp operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public boolean isNot() {
        return not;
    }

    public void setNot(boolean not) {
        this.not = not;
    }

    @Override
    public NodeType nodeType() {
        return NodeType.LEAF;
    }

    @Override
    public String toString() {
        if (operator == CompareOp.EXPRESSION) {
            return "expr(" + expression + ')';
        }
        return field + " " + operator.getSymbol() + " " + value;
    }
}
