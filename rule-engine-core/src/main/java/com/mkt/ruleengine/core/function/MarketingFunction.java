package com.mkt.ruleengine.core.function;

/**
 * 自定义函数 SPI：业务函数（连续打卡天数计算、阶梯返利核算等）的统一抽象。
 * 三种实现途径：JAVA_SPI（Spring Bean）/ JAR（上传 Jar 动态加载）/ EXPRESSION（在线脚本）。
 */
public interface MarketingFunction {

    /** 函数名 */
    String name();

    /**
     * 执行函数。
     *
     * @param ctx 函数上下文
     * @return 函数结果（写入规则运行时属性）
     */
    Object evaluate(FunctionContext ctx);
}
