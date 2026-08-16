package com.mkt.ruleengine.core.exception;

/**
 * 事件未定义异常：触发的事件未在事件管理中维护。
 */
public class EventNotDefinedException extends RuleEngineException {

    public EventNotDefinedException(String eventCode) {
        super("event not defined: " + eventCode);
    }
}
