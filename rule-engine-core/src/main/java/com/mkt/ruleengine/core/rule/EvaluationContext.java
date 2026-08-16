package com.mkt.ruleengine.core.rule;

import com.mkt.ruleengine.core.event.MarketingEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 条件求值上下文：事件 + 运行时属性（函数增强结果）+ 用户画像。
 */
public class EvaluationContext {

    private final MarketingEvent event;
    /** 运行时属性：基础字段 + 前置函数增强结果，字段解析优先级最高 */
    private final Map<String, Object> attributes;
    /** 用户画像字段 */
    private final Map<String, Object> userProfile;

    public EvaluationContext(MarketingEvent event, Map<String, Object> attributes, Map<String, Object> userProfile) {
        this.event = event;
        this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
        this.userProfile = userProfile == null ? Map.of() : userProfile;
    }

    public MarketingEvent getEvent() {
        return event;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public Map<String, Object> getUserProfile() {
        return Collections.unmodifiableMap(userProfile);
    }

    /**
     * 字段解析：attributes（函数增强）→ 事件参数 → 事件固定字段 → 用户画像。
     */
    public Object resolveField(String field) {
        if (field == null || field.isBlank()) {
            return null;
        }
        if (attributes.containsKey(field)) {
            return attributes.get(field);
        }
        if (event.getParams().containsKey(field)) {
            return event.getParams().get(field);
        }
        switch (field) {
            case "userId" -> {
                return event.getUserId();
            }
            case "channelId" -> {
                return event.getChannelId();
            }
            case "eventId" -> {
                return event.getEventId();
            }
            case "eventCode" -> {
                return event.getEventCode();
            }
            case "eventTime" -> {
                return event.getEventTime();
            }
            default -> {
            }
        }
        return userProfile.get(field);
    }

    /** 构建表达式求值变量集：事件参数 + 画像 + 增强属性（属性优先级最高） */
    public Map<String, Object> expressionVariables() {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.putAll(event.getParams());
        vars.putAll(userProfile);
        vars.putAll(attributes);
        vars.put("userId", event.getUserId());
        vars.put("channelId", event.getChannelId());
        vars.put("eventId", event.getEventId());
        vars.put("eventCode", event.getEventCode());
        vars.put("eventTime", event.getEventTime());
        return vars;
    }
}
