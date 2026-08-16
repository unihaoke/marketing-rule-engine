package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.event.MarketingEvent;
import com.mkt.ruleengine.core.rule.RuleSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 引擎执行上下文：贯穿责任链的共享状态（事件 + 快照 + 增强属性 + 命中规则 + 动作记录）。
 */
public class EngineContext {

    private final MarketingEvent event;
    private final long startTime;

    /** 待匹配规则快照（按优先级排序） */
    private List<RuleSnapshot> snapshots = List.of();

    /** 运行时属性：事件参数 + 前置函数增强结果 */
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    /** 用户画像 */
    private Map<String, Object> userProfile = Map.of();

    /** 命中规则 */
    private final List<RuleSnapshot> matchedRules = new ArrayList<>();

    /** 动作执行记录 */
    private final List<EngineResult.ActionExecutionRecord> actionRecords = new ArrayList<>();

    private boolean failed = false;
    private String errorMessage;

    /** 是否模拟触发（开启执行追踪 + 动作强制同步，便于页面查看效果） */
    private final boolean simulation;

    /** 执行追踪（simulation 模式下非空） */
    private final EngineTrace trace;

    public EngineContext(MarketingEvent event, long startTime) {
        this(event, startTime, false);
    }

    public EngineContext(MarketingEvent event, long startTime, boolean simulation) {
        this.event = event;
        this.startTime = startTime;
        this.simulation = simulation;
        this.trace = simulation ? new EngineTrace() : null;
    }

    public boolean isSimulation() {
        return simulation;
    }

    public EngineTrace trace() {
        return trace;
    }

    public MarketingEvent getEvent() {
        return event;
    }

    public long getStartTime() {
        return startTime;
    }

    public List<RuleSnapshot> getSnapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    public void setSnapshots(List<RuleSnapshot> snapshots) {
        this.snapshots = snapshots == null ? List.of() : snapshots;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object attribute(String key) {
        return attributes.get(key);
    }

    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Map<String, Object> getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(Map<String, Object> userProfile) {
        this.userProfile = userProfile == null ? Map.of() : userProfile;
    }

    public List<RuleSnapshot> getMatchedRules() {
        return Collections.unmodifiableList(matchedRules);
    }

    public void addMatchedRule(RuleSnapshot rule) {
        matchedRules.add(rule);
    }

    public List<EngineResult.ActionExecutionRecord> getActionRecords() {
        return Collections.unmodifiableList(actionRecords);
    }

    public void addActionRecord(EngineResult.ActionExecutionRecord record) {
        actionRecords.add(record);
    }

    public boolean isFailed() {
        return failed;
    }

    public void markFailed(String message) {
        this.failed = true;
        this.errorMessage = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** 汇总为执行结果 */
    public EngineResult toResult(long finishTime) {
        EngineResult result = new EngineResult();
        result.setEventId(event.getEventId());
        result.setEventCode(event.getEventCode());
        result.setUserId(event.getUserId());
        result.setChannelId(event.getChannelId());
        result.setTraceId(event.getTraceId());
        result.setStartedAt(startTime);
        result.setFinishedAt(finishTime);
        result.setCostMs(finishTime - startTime);
        result.setSuccess(!failed);
        result.setErrorMessage(errorMessage);
        matchedRules.forEach(rule -> result.addMatchedRule(rule.getRuleCode()));
        actionRecords.forEach(result::addActionRecord);
        attributes.forEach(result::putAttribute);
        return result;
    }
}
