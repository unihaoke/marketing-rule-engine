package com.mkt.ruleengine.core.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 引擎执行结果：一次事件触发的完整产出。
 */
public class EngineResult {

    /** 单条动作执行记录 */
    public record ActionExecutionRecord(String ruleCode, String actionCode, boolean success, String detail, long costMs) {
    }

    private String eventId;
    private String eventCode;
    private String userId;
    private String channelId;
    private String traceId;
    private long startedAt;
    private long finishedAt;
    private long costMs;
    private boolean success = true;
    private String errorMessage;
    private final List<String> matchedRuleCodes = new ArrayList<>();
    private final List<ActionExecutionRecord> actionRecords = new ArrayList<>();
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(long finishedAt) {
        this.finishedAt = finishedAt;
    }

    public long getCostMs() {
        return costMs;
    }

    public void setCostMs(long costMs) {
        this.costMs = costMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getMatchedRuleCodes() {
        return Collections.unmodifiableList(matchedRuleCodes);
    }

    public void addMatchedRule(String ruleCode) {
        matchedRuleCodes.add(ruleCode);
    }

    public List<ActionExecutionRecord> getActionRecords() {
        return Collections.unmodifiableList(actionRecords);
    }

    public void addActionRecord(ActionExecutionRecord record) {
        actionRecords.add(record);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }
}
