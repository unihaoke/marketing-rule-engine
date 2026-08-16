package com.mkt.ruleengine.core.function;

import java.util.List;
import java.util.Optional;

/**
 * 函数注册中心 SPI：支持热更新（register / unregister / reload）。
 */
public interface FunctionRegistry {

    /**
     * 获取函数。
     */
    Optional<MarketingFunction> get(String functionName);

    /**
     * 是否存在。
     */
    boolean contains(String functionName);

    /**
     * 注册/热更新函数。
     */
    void register(FunctionDefinition definition, MarketingFunction function);

    /**
     * 按定义加载函数（供注册/更新后即时生效）。
     */
    MarketingFunction load(FunctionDefinition definition);

    /**
     * 注销函数。
     */
    void unregister(String functionName);

    /**
     * 已注册函数名列表。
     */
    List<String> names();

    /**
     * 全量重建（配置变更后触发）。
     */
    void reloadAll();
}
