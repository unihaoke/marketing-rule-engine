package com.mkt.ruleengine.core.spi;

import com.mkt.ruleengine.core.action.ActionExecutionContext;
import com.mkt.ruleengine.core.action.ActionResult;

/**
 * 动作执行记录 SPI：同步/异步动作完成后的持久化（审计、对账、幂等留痕）。
 */
public interface ActionLogWriter {

    void write(ActionExecutionContext ctx, ActionResult result);
}
