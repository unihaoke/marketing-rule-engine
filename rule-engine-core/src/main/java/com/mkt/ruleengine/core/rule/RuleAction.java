package com.mkt.ruleengine.core.rule;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 规则动作：勾选的动作 + 参数（券模板 / 短信模板 / 积分数量等）。
 */
public class RuleAction {

    /** 动作编码，对应动作配置中的 ActionDefinition.actionCode，如 ISSUE_COUPON / SEND_SMS / ADD_POINTS */
    private String actionCode;

    /** 动作参数（键值对） */
    private Map<String, Object> params = new LinkedHashMap<>();

    /** 是否异步执行（默认 true，异步不阻塞事件主链路） */
    private boolean async = true;

    public RuleAction() {
    }

    public RuleAction(String actionCode, Map<String, Object> params) {
        this.actionCode = actionCode;
        this.params = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    @Override
    public String toString() {
        return "RuleAction{" + actionCode + ", params=" + params + '}';
    }
}
