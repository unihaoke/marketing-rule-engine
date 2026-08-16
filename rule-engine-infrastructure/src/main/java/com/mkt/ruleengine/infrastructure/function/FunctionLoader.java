package com.mkt.ruleengine.infrastructure.function;

import com.mkt.ruleengine.core.function.FunctionDefinition;
import com.mkt.ruleengine.core.function.FunctionType;
import com.mkt.ruleengine.core.function.MarketingFunction;

/**
 * 函数加载器 SPI：按函数类型加载为可执行 {@link MarketingFunction}。
 */
public interface FunctionLoader {

    boolean supports(FunctionType type);

    MarketingFunction load(FunctionDefinition definition);
}
