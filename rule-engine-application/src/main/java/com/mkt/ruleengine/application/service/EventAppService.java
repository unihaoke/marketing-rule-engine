package com.mkt.ruleengine.application.service;

import com.mkt.ruleengine.core.event.EventDefinition;
import com.mkt.ruleengine.core.exception.RuleConfigException;
import com.mkt.ruleengine.core.repository.EventDefinitionRepository;
import com.mkt.ruleengine.core.spi.EventDefinitionRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 事件管理应用服务：事件定义 CRUD + 入参 schema + 启停。
 */
@Service
public class EventAppService {

    private final EventDefinitionRepository repository;
    private final EventDefinitionRegistry registry;

    public EventAppService(EventDefinitionRepository repository, EventDefinitionRegistry registry) {
        this.repository = repository;
        this.registry = registry;
    }

    public EventDefinition create(EventDefinition definition) {
        if (definition.getEventCode() == null || definition.getEventCode().isBlank()) {
            throw new RuleConfigException("eventCode must not be blank");
        }
        if (repository.findByCode(definition.getEventCode()).isPresent()) {
            throw new RuleConfigException("event already exists: " + definition.getEventCode());
        }
        repository.save(definition);
        registry.refresh();
        return definition;
    }

    public EventDefinition update(String eventCode, EventDefinition definition) {
        EventDefinition existing = get(eventCode);
        definition.setEventCode(eventCode);
        definition.setCreatedBy(existing.getCreatedBy());
        definition.setCreatedAt(existing.getCreatedAt());
        repository.update(definition);
        registry.refresh();
        return definition;
    }

    public void delete(String eventCode) {
        repository.delete(eventCode);
        registry.refresh();
    }

    public EventDefinition get(String eventCode) {
        return repository.findByCode(eventCode)
                .orElseThrow(() -> new RuleConfigException("event not found: " + eventCode));
    }

    public List<EventDefinition> list() {
        return repository.findAll();
    }

    public EventDefinition toggleEnabled(String eventCode, boolean enabled) {
        EventDefinition def = get(eventCode);
        def.setEnabled(enabled);
        repository.update(def);
        registry.refresh();
        return def;
    }
}
