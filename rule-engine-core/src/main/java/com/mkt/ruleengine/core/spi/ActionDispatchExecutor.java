package com.mkt.ruleengine.core.spi;

import com.mkt.ruleengine.core.action.ActionExecutionContext;
import com.mkt.ruleengine.core.action.ActionExecutor;
import com.mkt.ruleengine.core.action.ActionResult;

import java.util.function.Consumer;

/**
 * 动作分发执行器 SPI：动作执行异步化（高并发事件吞吐的关键），支持同步/异步两种模式。
 * 默认实现为框架内置线程池；业务侧可替换为 MQ / 自研异步框架。
 */
public interface ActionDispatchExecutor {

    /**
     * 分发动作执行。
     *
     * @param async     是否异步执行
     * @param ctx       动作执行上下文
     * @param executor  动作执行器
     * @param onResult  结果回调（可为空）
     */
    void dispatch(boolean async, ActionExecutionContext ctx, ActionExecutor executor, Consumer<ActionResult> onResult);
}
