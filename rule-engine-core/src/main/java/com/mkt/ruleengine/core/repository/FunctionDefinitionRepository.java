package com.mkt.ruleengine.core.repository;

import com.mkt.ruleengine.core.function.FunctionDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 函数定义仓储接口。
 */
public interface FunctionDefinitionRepository {

    FunctionDefinition save(FunctionDefinition definition);

    FunctionDefinition update(FunctionDefinition definition);

    void delete(String functionName);

    Optional<FunctionDefinition> findByName(String functionName);

    List<FunctionDefinition> findAll();
}
