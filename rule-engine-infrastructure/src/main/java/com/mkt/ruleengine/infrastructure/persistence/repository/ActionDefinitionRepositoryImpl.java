package com.mkt.ruleengine.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mkt.ruleengine.core.action.ActionDefinition;
import com.mkt.ruleengine.core.action.ActionParamDef;
import com.mkt.ruleengine.core.repository.ActionDefinitionRepository;
import com.mkt.ruleengine.infrastructure.expression.JacksonJsonCodec;
import com.mkt.ruleengine.infrastructure.persistence.mapper.ActionDefinitionMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.ActionDefinitionPO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 动作定义仓储实现。
 */
@Repository
public class ActionDefinitionRepositoryImpl implements ActionDefinitionRepository {

    private final ActionDefinitionMapper mapper;
    private final JacksonJsonCodec jsonCodec;

    public ActionDefinitionRepositoryImpl(ActionDefinitionMapper mapper, JacksonJsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public ActionDefinition save(ActionDefinition d) {
        ActionDefinitionPO po = toPO(d);
        po.setId(null);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        mapper.insert(po);
        d.setCreatedAt(po.getCreatedAt());
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }

    @Override
    public ActionDefinition update(ActionDefinition d) {
        ActionDefinitionPO po = toPO(d);
        po.setUpdatedAt(LocalDateTime.now());
        mapper.update(po, new LambdaQueryWrapper<ActionDefinitionPO>()
                .eq(ActionDefinitionPO::getActionCode, d.getActionCode()));
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }

    @Override
    public void delete(String actionCode) {
        mapper.delete(new LambdaQueryWrapper<ActionDefinitionPO>()
                .eq(ActionDefinitionPO::getActionCode, actionCode));
    }

    @Override
    public Optional<ActionDefinition> findByCode(String actionCode) {
        ActionDefinitionPO po = mapper.selectOne(new LambdaQueryWrapper<ActionDefinitionPO>()
                .eq(ActionDefinitionPO::getActionCode, actionCode));
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<ActionDefinition> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<ActionDefinitionPO>()
                        .orderByAsc(ActionDefinitionPO::getActionCode))
                .stream().map(this::toDomain).toList();
    }

    private ActionDefinitionPO toPO(ActionDefinition d) {
        ActionDefinitionPO po = new ActionDefinitionPO();
        po.setActionCode(d.getActionCode());
        po.setActionName(d.getActionName());
        po.setActionType(d.getActionType());
        po.setDescription(d.getDescription());
        po.setParamsJson(jsonCodec.toJson(d.getParams()));
        po.setDefaultsJson(d.getDefaults() == null ? null : jsonCodec.toJson(d.getDefaults()));
        po.setEnabled(d.isEnabled());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private ActionDefinition toDomain(ActionDefinitionPO po) {
        ActionDefinition d = new ActionDefinition();
        d.setActionCode(po.getActionCode());
        d.setActionName(po.getActionName());
        d.setActionType(po.getActionType());
        d.setDescription(po.getDescription());
        if (po.getParamsJson() != null) {
            d.setParams(jsonCodec.fromJson(po.getParamsJson(), new TypeReference<List<ActionParamDef>>() {
            }));
        }
        if (po.getDefaultsJson() != null) {
            d.setDefaults(jsonCodec.fromJson(po.getDefaultsJson(), new TypeReference<Map<String, Object>>() {
            }));
        }
        d.setEnabled(Boolean.TRUE.equals(po.getEnabled()));
        d.setCreatedAt(po.getCreatedAt());
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }
}
