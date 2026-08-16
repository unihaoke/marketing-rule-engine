package com.mkt.ruleengine.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mkt.ruleengine.core.event.EventDefinition;
import com.mkt.ruleengine.core.event.EventParamDef;
import com.mkt.ruleengine.core.repository.EventDefinitionRepository;
import com.mkt.ruleengine.infrastructure.expression.JacksonJsonCodec;
import com.mkt.ruleengine.infrastructure.persistence.mapper.EventDefinitionMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.EventDefinitionPO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 事件定义仓储实现（MyBatis-Plus）。
 */
@Repository
public class EventDefinitionRepositoryImpl implements EventDefinitionRepository {

    private final EventDefinitionMapper mapper;
    private final JacksonJsonCodec jsonCodec;

    public EventDefinitionRepositoryImpl(EventDefinitionMapper mapper, JacksonJsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public EventDefinition save(EventDefinition definition) {
        EventDefinitionPO po = toPO(definition);
        po.setId(null);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        mapper.insert(po);
        definition.setCreatedAt(po.getCreatedAt());
        definition.setUpdatedAt(po.getUpdatedAt());
        return definition;
    }

    @Override
    public EventDefinition update(EventDefinition definition) {
        EventDefinitionPO po = toPO(definition);
        po.setUpdatedAt(LocalDateTime.now());
        mapper.update(po, new LambdaQueryWrapper<EventDefinitionPO>()
                .eq(EventDefinitionPO::getEventCode, definition.getEventCode()));
        definition.setUpdatedAt(po.getUpdatedAt());
        return definition;
    }

    @Override
    public void delete(String eventCode) {
        mapper.delete(new LambdaQueryWrapper<EventDefinitionPO>()
                .eq(EventDefinitionPO::getEventCode, eventCode));
    }

    @Override
    public Optional<EventDefinition> findByCode(String eventCode) {
        EventDefinitionPO po = mapper.selectOne(new LambdaQueryWrapper<EventDefinitionPO>()
                .eq(EventDefinitionPO::getEventCode, eventCode));
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<EventDefinition> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<EventDefinitionPO>()
                        .orderByAsc(EventDefinitionPO::getEventCode))
                .stream().map(this::toDomain).toList();
    }

    private EventDefinitionPO toPO(EventDefinition d) {
        EventDefinitionPO po = new EventDefinitionPO();
        po.setEventCode(d.getEventCode());
        po.setEventName(d.getEventName());
        po.setDescription(d.getDescription());
        po.setEnabled(d.isEnabled());
        po.setCreatedBy(d.getCreatedBy());
        po.setParamsJson(jsonCodec.toJson(d.getParams()));
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private EventDefinition toDomain(EventDefinitionPO po) {
        EventDefinition d = new EventDefinition(po.getEventCode(), po.getEventName(),
                po.getDescription(), Boolean.TRUE.equals(po.getEnabled()), List.of());
        d.setParams(jsonCodec.fromJson(po.getParamsJson(),
                new com.fasterxml.jackson.core.type.TypeReference<List<EventParamDef>>() {
                }));
        d.setCreatedBy(po.getCreatedBy());
        d.setCreatedAt(po.getCreatedAt());
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }
}
