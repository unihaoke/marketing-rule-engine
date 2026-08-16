package com.mkt.ruleengine.core.repository;

import com.mkt.ruleengine.core.event.EventDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 事件定义仓储接口。
 */
public interface EventDefinitionRepository {

    EventDefinition save(EventDefinition definition);

    EventDefinition update(EventDefinition definition);

    void delete(String eventCode);

    Optional<EventDefinition> findByCode(String eventCode);

    List<EventDefinition> findAll();
}
