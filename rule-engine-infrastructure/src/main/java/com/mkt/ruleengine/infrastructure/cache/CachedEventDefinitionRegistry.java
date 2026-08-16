package com.mkt.ruleengine.infrastructure.cache;

import com.mkt.ruleengine.core.event.EventDefinition;
import com.mkt.ruleengine.core.repository.EventDefinitionRepository;
import com.mkt.ruleengine.core.spi.EventDefinitionRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 事件定义注册表（缓存实现）：启动加载 + 配置变更刷新，运行时零触库。
 */
@Component
public class CachedEventDefinitionRegistry implements EventDefinitionRegistry {

    private final EventDefinitionRepository repository;
    private volatile Map<String, EventDefinition> cache = Map.of();

    public CachedEventDefinitionRegistry(EventDefinitionRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    @Override
    public Optional<EventDefinition> findByCode(String eventCode) {
        EventDefinition def = cache.get(eventCode);
        return Optional.ofNullable(def != null && def.isEnabled() ? def : null);
    }

    @Override
    public boolean exists(String eventCode) {
        EventDefinition def = cache.get(eventCode);
        return def != null && def.isEnabled();
    }

    @Override
    public List<EventDefinition> findAll() {
        return List.copyOf(cache.values());
    }

    @Override
    public void refresh() {
        Map<String, EventDefinition> fresh = repository.findAll().stream()
                .collect(Collectors.toConcurrentMap(EventDefinition::getEventCode, d -> d));
        cache = fresh;
    }
}
