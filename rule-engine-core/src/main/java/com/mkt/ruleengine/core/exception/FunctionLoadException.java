package com.mkt.ruleengine.core.exception;

/**
 * 自定义函数加载/执行异常。
 */
public class FunctionLoadException extends RuleEngineException {

    public FunctionLoadException(String message) {
        super(message);
    }

    public FunctionLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
