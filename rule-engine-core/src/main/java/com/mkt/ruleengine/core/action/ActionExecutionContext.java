package com.mkt.ruleengine.core.action;

import com.mkt.ruleengine.core.event.MarketingEvent;
import com.mkt.ruleengine.core.rule.RuleAction;
import com.mkt.ruleengine.core.rule.RuleSnapshot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动作执行上下文：事件 + 命中规则 + 动作配置 + 解析后的参数 + 运行时属性。
 */
public class ActionExecutionContext {

    private final MarketingEvent event;
    private final RuleSnapshot rule;
    private final RuleAction action;
    private final Map<String, Object> resolvedParams;
    private final Map<String, Object> attributes;
    private final Map<String, Object> userProfile;

    public ActionExecutionContext(MarketingEvent event, RuleSnapshot rule, RuleAction action,
                                  Map<String, Object> resolvedParams, Map<String, Object> attributes,
                                  Map<String, Object> userProfile) {
        this.event = event;
        this.rule = rule;
        this.action = action;
        this.resolvedParams = resolvedParams == null ? new LinkedHashMap<>() : new LinkedHashMap<>(resolvedParams);
        this.attributes = attributes;
        this.userProfile = userProfile == null ? Map.of() : userProfile;
    }

    public MarketingEvent getEvent() {
        return event;
    }

    public RuleSnapshot getRule() {
        return rule;
    }

    public RuleAction getAction() {
        return action;
    }

    public Map<String, Object> getResolvedParams() {
        return Collections.unmodifiableMap(resolvedParams);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Map<String, Object> getUserProfile() {
        return userProfile;
    }

    /** 参数取值 */
    public Object param(String key) {
        return resolvedParams.get(key);
    }

    /** 幂等键：eventId + ruleCode + actionCode */
    public String idempotencyKey() {
        return event.getEventId() + ":" + rule.getRuleCode() + ":" + action.getActionCode();
    }
}
