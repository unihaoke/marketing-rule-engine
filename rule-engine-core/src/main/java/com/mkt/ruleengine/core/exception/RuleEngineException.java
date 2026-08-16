package com.mkt.ruleengine.core.exception;

/**
 * 规则引擎统一运行时异常。
 */
public class RuleEngineException extends RuntimeException {

    public RuleEngineException(String message) {
        super(message);
    }

    public RuleEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
