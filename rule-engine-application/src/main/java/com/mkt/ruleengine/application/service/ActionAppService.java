package com.mkt.ruleengine.application.service;

import com.mkt.ruleengine.core.action.ActionDefinition;
import com.mkt.ruleengine.core.exception.RuleConfigException;
import com.mkt.ruleengine.core.repository.ActionDefinitionRepository;
import com.mkt.ruleengine.core.spi.ActionDefinitionRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 动作配置应用服务：动作模板 CRUD（券模板/短信模板/积分数量等参数化）。
 */
@Service
public class ActionAppService {

    private final ActionDefinitionRepository repository;
    private final ActionDefinitionRegistry registry;

    public ActionAppService(ActionDefinitionRepository repository, ActionDefinitionRegistry registry) {
        this.repository = repository;
        this.registry = registry;
    }

    public ActionDefinition create(ActionDefinition definition) {
        if (definition.getActionCode() == null || definition.getActionCode().isBlank()) {
            throw new RuleConfigException("actionCode must not be blank");
        }
        if (definition.getActionType() == null || definition.getActionType().isBlank()) {
            throw new RuleConfigException("actionType must not be blank");
        }
        if (repository.findByCode(definition.getActionCode()).isPresent()) {
            throw new RuleConfigException("action already exists: " + definition.getActionCode());
        }
        // 默认值由参数定义中的 defaultValue 自动派生（前端不再单独维护 defaults）
        definition.setDefaults(deriveDefaults(definition.getParams()));
        repository.save(definition);
        registry.refresh();
        return definition;
    }

    public ActionDefinition update(String actionCode, ActionDefinition definition) {
        get(actionCode);
        definition.setActionCode(actionCode);
        definition.setDefaults(deriveDefaults(definition.getParams()));
        repository.update(definition);
        registry.refresh();
        return definition;
    }

    public void delete(String actionCode) {
        repository.delete(actionCode);
        registry.refresh();
    }

    public ActionDefinition get(String actionCode) {
        return repository.findByCode(actionCode)
                .orElseThrow(() -> new RuleConfigException("action not found: " + actionCode));
    }

    public List<ActionDefinition> list() {
        return repository.findAll();
    }

    public ActionDefinition toggleEnabled(String actionCode, boolean enabled) {
        ActionDefinition def = get(actionCode);
        def.setEnabled(enabled);
        repository.update(def);
        registry.refresh();
        return def;
    }

    /** 由参数定义中的 defaultValue 派生动作默认参数 */
    private java.util.Map<String, Object> deriveDefaults(java.util.List<com.mkt.ruleengine.core.action.ActionParamDef> params) {
        java.util.Map<String, Object> defaults = new java.util.LinkedHashMap<>();
        if (params != null) {
            params.forEach(p -> {
                if (p.defaultValue() != null) {
                    defaults.put(p.code(), p.defaultValue());
                }
            });
        }
        return defaults;
    }
}
