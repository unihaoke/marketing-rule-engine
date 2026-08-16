package com.mkt.ruleengine.core.action;

/**
 * 动作执行器 SPI（策略模式）：每种动作类型一个实现，按 actionType 注册。
 *
 * <p>内置类型见 {@link ActionTypes}；业务侧可实现本接口扩展（如发券 / 短信 / 推送 / 积分）。</p>
 */
public interface ActionExecutor {

    /** 动作类型标识 */
    String actionType();

    /**
     * 执行动作。
     *
     * @param ctx 动作执行上下文（含解析后的参数）
     * @return 执行结果
     */
    ActionResult execute(ActionExecutionContext ctx);
}
