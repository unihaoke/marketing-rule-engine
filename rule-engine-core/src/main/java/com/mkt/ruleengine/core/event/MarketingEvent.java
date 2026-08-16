package com.mkt.ruleengine.core.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 运行时营销事件信封：一次事件触发的完整输入。
 *
 * <p>固定字段（userId / channelId / eventTime 等）为通用营销上下文，
 * 业务差异字段统一放入 {@code params}（由事件管理的入参 schema 定义）。</p>
 */
public class MarketingEvent {

    /** 事件实例 ID（幂等去重键） */
    private final String eventId;

    /** 事件编码 */
    private final String eventCode;

    /** 用户 ID（可为空，例如未登录广告点击） */
    private final String userId;

    /** 渠道 ID */
    private final String channelId;

    /** 事件发生时间（毫秒时间戳） */
    private final long eventTime;

    /** 链路追踪 ID */
    private final String traceId;

    /** 业务参数（事件入参字段） */
    private final Map<String, Object> params;

    public MarketingEvent(String eventCode, String userId, String channelId,
                          long eventTime, Map<String, Object> params) {
        this(UUID.randomUUID().toString().replace("-", ""), eventCode, userId, channelId,
                eventTime, null, params);
    }

    public MarketingEvent(String eventId, String eventCode, String userId, String channelId,
                          long eventTime, String traceId, Map<String, Object> params) {
        this.eventId = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString().replace("-", "") : eventId;
        this.eventCode = eventCode;
        this.userId = userId;
        this.channelId = channelId;
        this.eventTime = eventTime == 0 ? System.currentTimeMillis() : eventTime;
        this.traceId = traceId == null ? this.eventId : traceId;
        this.params = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventCode() {
        return eventCode;
    }

    public String getUserId() {
        return userId;
    }

    public String getChannelId() {
        return channelId;
    }

    public long getEventTime() {
        return eventTime;
    }

    public String getTraceId() {
        return traceId;
    }

    public Map<String, Object> getParams() {
        return Collections.unmodifiableMap(params);
    }

    /** 从 params 取参数值 */
    public Object param(String key) {
        return params.get(key);
    }

    public Instant eventTimeInstant() {
        return Instant.ofEpochMilli(eventTime);
    }

    @Override
    public String toString() {
        return "MarketingEvent{" + "eventId='" + eventId + '\'' + ", eventCode='" + eventCode + '\''
                + ", userId='" + userId + '\'' + ", channelId='" + channelId + '\'' + '}';
    }
}
