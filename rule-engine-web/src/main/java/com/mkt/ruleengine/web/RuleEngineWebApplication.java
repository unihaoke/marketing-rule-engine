package com.mkt.ruleengine.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 营销规则引擎 Web 启动入口。
 */
@SpringBootApplication(scanBasePackages = "com.mkt.ruleengine")
public class RuleEngineWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuleEngineWebApplication.class, args);
    }
}
