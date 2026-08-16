package com.mkt.ruleengine.infrastructure.cache;

import com.mkt.ruleengine.core.action.ActionDefinition;
import com.mkt.ruleengine.core.repository.ActionDefinitionRepository;
import com.mkt.ruleengine.core.spi.ActionDefinitionRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 动作定义注册表（缓存实现）：运行时零触库。
 */
@Component
public class CachedActionDefinitionRegistry implements ActionDefinitionRegistry {

    private final ActionDefinitionRepository repository;
    private volatile Map<String, ActionDefinition> cache = Map.of();

    public CachedActionDefinitionRegistry(ActionDefinitionRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    @Override
    public Optional<ActionDefinition> findByCode(String actionCode) {
        ActionDefinition def = cache.get(actionCode);
        return Optional.ofNullable(def != null && def.isEnabled() ? def : null);
    }

    @Override
    public List<ActionDefinition> findAll() {
        return List.copyOf(cache.values());
    }

    @Override
    public boolean exists(String actionCode) {
        ActionDefinition def = cache.get(actionCode);
        return def != null && def.isEnabled();
    }

    @Override
    public void refresh() {
        cache = repository.findAll().stream()
                .collect(Collectors.toConcurrentMap(ActionDefinition::getActionCode, d -> d));
    }
}
