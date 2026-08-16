package com.mkt.ruleengine.core.engine;

/**
 * 引擎处理阶段 SPI（责任链模式节点）：事件归一化 / 函数增强 / 规则匹配 / 动作执行。
 */
public interface EngineStage {

    /**
     * 处理当前阶段。
     *
     * @param ctx   引擎上下文（共享状态）
     * @param chain 责任链（调用 chain.proceed(ctx) 进入下一阶段，或直接结束链路）
     */
    void handle(EngineContext ctx, StageChain chain);
}
