package com.mkt.ruleengine.core.event;

/**
 * 事件入参字段定义（事件管理模块核心模型）。
 *
 * @param code        字段编码，规则条件中直接引用，如 channelId / adSlotId / orderAmount
 * @param name        字段名称
 * @param type        字段类型
 * @param required    是否必填
 * @param description 字段说明
 * @param defaultValue 默认值（可选）
 */
public record EventParamDef(
        String code,
        String name,
        EventParamType type,
        boolean required,
        String description,
        Object defaultValue) {

    public EventParamDef {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("event param code must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("event param type must not be null");
        }
    }

    public static EventParamDef of(String code, String name, EventParamType type, boolean required) {
        return new EventParamDef(code, name, type, required, null, null);
    }
}
