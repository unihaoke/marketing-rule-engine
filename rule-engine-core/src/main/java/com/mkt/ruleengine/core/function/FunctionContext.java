package com.mkt.ruleengine.core.function;

import com.mkt.ruleengine.core.event.MarketingEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 函数执行上下文：事件 + 运行时属性（可读写，前序函数结果可见）+ 函数参数绑定 + 用户画像。
 */
public class FunctionContext {

    private final MarketingEvent event;
    private final Map<String, Object> attributes;
    private final Map<String, Object> bindings;
    private final Map<String, Object> userProfile;

    public FunctionContext(MarketingEvent event, Map<String, Object> attributes,
                           Map<String, Object> bindings, Map<String, Object> userProfile) {
        this.event = event;
        this.attributes = attributes == null ? new LinkedHashMap<>() : attributes;
        this.bindings = bindings == null ? new LinkedHashMap<>() : bindings;
        this.userProfile = userProfile == null ? Map.of() : userProfile;
    }

    public MarketingEvent getEvent() {
        return event;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Map<String, Object> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    public Map<String, Object> getUserProfile() {
        return Collections.unmodifiableMap(userProfile);
    }

    /** 事件参数取值 */
    public Object param(String key) {
        return event.getParams().get(key);
    }

    /** 绑定参数取值（规则画布中配置的函数参数） */
    public Object binding(String key) {
        return bindings.get(key);
    }

    /** 读取前序函数写入的属性 */
    public Object attribute(String key) {
        return attributes.get(key);
    }

    /** 写入属性（供条件与动作参数引用） */
    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /** 表达式变量集（供脚本函数使用） */
    public Map<String, Object> expressionVariables() {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.putAll(event.getParams());
        vars.putAll(userProfile);
        vars.putAll(bindings);
        vars.putAll(attributes);
        vars.put("userId", event.getUserId());
        vars.put("channelId", event.getChannelId());
        vars.put("eventTime", event.getEventTime());
        return vars;
    }
}
