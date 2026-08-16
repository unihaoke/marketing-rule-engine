package com.mkt.ruleengine.core.spi;

import com.mkt.ruleengine.core.event.EventDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 事件定义注册表 SPI：运行时校验事件 + 提供入参 schema（基础设施层实现，支持热更新缓存）。
 */
public interface EventDefinitionRegistry {

    Optional<EventDefinition> findByCode(String eventCode);

    boolean exists(String eventCode);

    List<EventDefinition> findAll();

    /** 配置变更后刷新缓存 */
    void refresh();
}
