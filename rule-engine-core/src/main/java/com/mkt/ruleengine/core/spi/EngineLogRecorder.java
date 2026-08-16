package com.mkt.ruleengine.core.spi;

import com.mkt.ruleengine.core.engine.EngineResult;

/**
 * 引擎执行日志 SPI：记录事件触发 -> 规则命中 -> 动作执行全链路，供运营审计与排障。
 */
public interface EngineLogRecorder {

    void record(EngineResult result);
}
