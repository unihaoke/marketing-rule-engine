package com.mkt.ruleengine.core.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 事件定义：所有触发事件（广告点击、下单、签到等）的元数据。
 */
public class EventDefinition {

    /** 事件编码，全局唯一，如 AD_CLICK / ORDER_CREATE / SIGN_IN */
    private String eventCode;

    /** 事件名称 */
    private String eventName;

    /** 事件描述 */
    private String description;

    /** 是否启用（停用后引擎不再受理该事件） */
    private boolean enabled = true;

    /** 入参字段定义 */
    private List<EventParamDef> params = new ArrayList<>();

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    public EventDefinition() {
    }

    public EventDefinition(String eventCode, String eventName, String description,
                           boolean enabled, List<EventParamDef> params) {
        this.eventCode = eventCode;
        this.eventName = eventName;
        this.description = description;
        this.enabled = enabled;
        this.params = params == null ? new ArrayList<>() : new ArrayList<>(params);
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<EventParamDef> getParams() {
        return Collections.unmodifiableList(params);
    }

    public void setParams(List<EventParamDef> params) {
        this.params = params == null ? new ArrayList<>() : new ArrayList<>(params);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EventDefinition that)) {
            return false;
        }
        return Objects.equals(eventCode, that.eventCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventCode);
    }

    @Override
    public String toString() {
        return "EventDefinition{" + "eventCode='" + eventCode + '\'' + ", eventName='" + eventName + '\'' + '}';
    }
}
