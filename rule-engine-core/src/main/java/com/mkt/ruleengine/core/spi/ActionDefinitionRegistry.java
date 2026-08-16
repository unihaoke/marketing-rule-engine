package com.mkt.ruleengine.core.spi;

import com.mkt.ruleengine.core.action.ActionDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 动作定义注册表 SPI：动作编码 → 动作类型 + 参数默认值（缓存实现，热更新）。
 */
public interface ActionDefinitionRegistry {

    Optional<ActionDefinition> findByCode(String actionCode);

    List<ActionDefinition> findAll();

    boolean exists(String actionCode);

    /** 配置变更后刷新缓存 */
    void refresh();
}
