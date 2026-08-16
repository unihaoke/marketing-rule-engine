package com.mkt.ruleengine.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 规则引擎配置项。
 *
 * @param strictEventValidation 严格事件校验（未定义/停用事件拒绝受理）
 * @param functionJarDir        上传函数 Jar 存储目录
 * @param cachePollIntervalMs   热更新轮询间隔
 */
@ConfigurationProperties(prefix = "rule-engine")
public record RuleEngineProperties(
        boolean strictEventValidation,
        String functionJarDir,
        long cachePollIntervalMs) {

    public RuleEngineProperties {
        if (functionJarDir == null || functionJarDir.isBlank()) {
            functionJarDir = "./data/functions";
        }
        if (cachePollIntervalMs <= 0) {
            cachePollIntervalMs = 5000;
        }
    }
}
