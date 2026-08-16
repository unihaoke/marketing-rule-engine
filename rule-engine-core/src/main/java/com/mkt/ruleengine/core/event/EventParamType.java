package com.mkt.ruleengine.core.event;

/**
 * 事件入参字段类型。
 */
public enum EventParamType {
    /** 字符串 */
    STRING,
    /** 数字（整数/小数） */
    NUMBER,
    /** 布尔 */
    BOOLEAN,
    /** 日期时间（ISO-8601 字符串或毫秒时间戳） */
    DATETIME,
    /** JSON 对象（Map） */
    JSON,
    /** 数组 */
    LIST
}
