package com.mkt.ruleengine.core.function;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 函数在线测试案例：一键填入事件参数/绑定参数，并给出预期结果说明。
 *
 * <p>存储于 t_function_definition.test_cases_json（SQL 种子/画布维护），
 * 函数管理「在线测试」弹窗渲染为可点击案例，点击自动填充入参并提示预期结果。</p>
 */
public class FunctionTestCase {

    /** 案例名（展示用） */
    private String name;

    /** 事件参数（函数测试入口 eventParams） */
    private Map<String, Object> eventParams = new LinkedHashMap<>();

    /** 绑定参数（函数测试入口 bindings） */
    private Map<String, Object> bindings = new LinkedHashMap<>();

    /** 预期结果说明（提示用，不参与断言） */
    private String expect;

    public FunctionTestCase() {
    }

    public FunctionTestCase(String name, Map<String, Object> eventParams, Map<String, Object> bindings, String expect) {
        this.name = name;
        this.eventParams = eventParams == null ? new LinkedHashMap<>() : eventParams;
        this.bindings = bindings == null ? new LinkedHashMap<>() : bindings;
        this.expect = expect;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getEventParams() {
        return Collections.unmodifiableMap(eventParams);
    }

    public void setEventParams(Map<String, Object> eventParams) {
        this.eventParams = eventParams == null ? new LinkedHashMap<>() : new LinkedHashMap<>(eventParams);
    }

    public Map<String, Object> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    public void setBindings(Map<String, Object> bindings) {
        this.bindings = bindings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(bindings);
    }

    public String getExpect() {
        return expect;
    }

    public void setExpect(String expect) {
        this.expect = expect;
    }
}
