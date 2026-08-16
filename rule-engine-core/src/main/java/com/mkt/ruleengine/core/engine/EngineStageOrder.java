package com.mkt.ruleengine.core.engine;

/**
 * 责任链阶段顺序常量。
 */
public final class EngineStageOrder {

    /** 事件归一化 / 校验 */
    public static final int NORMALIZE = 100;

    /** 前置函数增强（匹配前执行自定义函数，产出运行时属性） */
    public static final int ENHANCE = 200;

    /** 规则匹配（灰度 + 条件树） */
    public static final int MATCH = 300;

    /** 动作执行（参数解析 + 幂等 + 异步分发） */
    public static final int ACTION = 400;

    private EngineStageOrder() {
    }
}
