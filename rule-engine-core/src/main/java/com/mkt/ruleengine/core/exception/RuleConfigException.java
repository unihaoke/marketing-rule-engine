package com.mkt.ruleengine.core.exception;

/**
 * 规则配置异常：规则内容非法 / 发布冲突 / 版本操作错误等。
 */
public class RuleConfigException extends RuleEngineException {

    public RuleConfigException(String message) {
        super(message);
    }

    public RuleConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
