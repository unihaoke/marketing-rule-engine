package com.mkt.ruleengine.core.repository;

import com.mkt.ruleengine.core.action.ActionDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 动作定义仓储接口。
 */
public interface ActionDefinitionRepository {

    ActionDefinition save(ActionDefinition definition);

    ActionDefinition update(ActionDefinition definition);

    void delete(String actionCode);

    Optional<ActionDefinition> findByCode(String actionCode);

    List<ActionDefinition> findAll();
}
