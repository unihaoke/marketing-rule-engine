package com.mkt.ruleengine.core.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 动作配置：可复用的动作模板（券模板、短信模板、积分数量等参数化配置）。
 */
public class ActionDefinition {

    /** 动作编码，全局唯一，如 ISSUE_COUPON */
    private String actionCode;

    /** 动作名称 */
    private String actionName;

    /** 动作类型（对应 ActionExecutor.actionType()） */
    private String actionType;

    /** 描述 */
    private String description;

    /** 参数 schema */
    private List<ActionParamDef> params = new ArrayList<>();

    /** 参数默认值 */
    private Map<String, Object> defaults = new LinkedHashMap<>();

    /** 是否启用 */
    private boolean enabled = true;

    private java.time.LocalDateTime createdAt;

    private java.time.LocalDateTime updatedAt;

    public ActionDefinition() {
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ActionParamDef> getParams() {
        return Collections.unmodifiableList(params);
    }

    public void setParams(List<ActionParamDef> params) {
        this.params = params == null ? new ArrayList<>() : new ArrayList<>(params);
    }

    public Map<String, Object> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, Object> defaults) {
        this.defaults = defaults == null ? new LinkedHashMap<>() : new LinkedHashMap<>(defaults);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ActionDefinition{" + actionCode + "(" + actionType + ")" + '}';
    }
}
