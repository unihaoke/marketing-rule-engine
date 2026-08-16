package com.mkt.ruleengine.core.rule;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 规则绑定的前置增强函数：匹配前执行，结果写入运行时属性供条件与动作参数引用。
 *
 * <p>例如：连续打卡天数计算 {@code checkinStreak}、阶梯返利核算 {@code rebateAmount}。</p>
 */
public class RuleFunction {

    /** 注册的函数名（对应函数注册中心 FunctionDefinition.functionName） */
    private String functionName;

    /** 结果写入运行时属性的别名（规则内可重命名，缺省等于 functionName） */
    private String alias;

    /** 函数参数绑定（阶梯返利档位、打卡窗口等业务参数） */
    private Map<String, Object> bindings = new LinkedHashMap<>();

    public RuleFunction() {
    }

    public RuleFunction(String functionName, String alias, Map<String, Object> bindings) {
        this.functionName = functionName;
        this.alias = alias == null || alias.isBlank() ? functionName : alias;
        this.bindings = bindings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(bindings);
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Map<String, Object> getBindings() {
        return bindings;
    }

    public void setBindings(Map<String, Object> bindings) {
        this.bindings = bindings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(bindings);
    }

    @Override
    public String toString() {
        return "RuleFunction{" + functionName + " -> " + alias + '}';
    }
}
