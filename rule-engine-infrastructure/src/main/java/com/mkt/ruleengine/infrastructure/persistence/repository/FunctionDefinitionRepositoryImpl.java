package com.mkt.ruleengine.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mkt.ruleengine.core.function.FunctionDefinition;
import com.mkt.ruleengine.core.function.FunctionParamDef;
import com.mkt.ruleengine.core.function.FunctionType;
import com.mkt.ruleengine.core.repository.FunctionDefinitionRepository;
import com.mkt.ruleengine.infrastructure.expression.JacksonJsonCodec;
import com.mkt.ruleengine.infrastructure.persistence.mapper.FunctionDefinitionMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.FunctionDefinitionPO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 函数定义仓储实现。
 */
@Repository
public class FunctionDefinitionRepositoryImpl implements FunctionDefinitionRepository {

    private final FunctionDefinitionMapper mapper;
    private final JacksonJsonCodec jsonCodec;

    public FunctionDefinitionRepositoryImpl(FunctionDefinitionMapper mapper, JacksonJsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public FunctionDefinition save(FunctionDefinition d) {
        FunctionDefinitionPO po = toPO(d);
        po.setId(null);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        mapper.insert(po);
        d.setCreatedAt(po.getCreatedAt());
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }

    @Override
    public FunctionDefinition update(FunctionDefinition d) {
        FunctionDefinitionPO po = toPO(d);
        po.setUpdatedAt(LocalDateTime.now());
        mapper.update(po, new LambdaQueryWrapper<FunctionDefinitionPO>()
                .eq(FunctionDefinitionPO::getFunctionName, d.getFunctionName()));
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }

    @Override
    public void delete(String functionName) {
        mapper.delete(new LambdaQueryWrapper<FunctionDefinitionPO>()
                .eq(FunctionDefinitionPO::getFunctionName, functionName));
    }

    @Override
    public Optional<FunctionDefinition> findByName(String functionName) {
        FunctionDefinitionPO po = mapper.selectOne(new LambdaQueryWrapper<FunctionDefinitionPO>()
                .eq(FunctionDefinitionPO::getFunctionName, functionName));
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<FunctionDefinition> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<FunctionDefinitionPO>()
                        .orderByAsc(FunctionDefinitionPO::getFunctionName))
                .stream().map(this::toDomain).toList();
    }

    private FunctionDefinitionPO toPO(FunctionDefinition d) {
        FunctionDefinitionPO po = new FunctionDefinitionPO();
        po.setFunctionName(d.getFunctionName());
        po.setDisplayName(d.getDisplayName());
        po.setType(d.getType() == null ? null : d.getType().name());
        po.setDescription(d.getDescription());
        po.setClassName(d.getClassName());
        po.setJarPath(d.getJarPath());
        po.setScript(d.getScript());
        po.setParamsJson(jsonCodec.toJson(d.getParams()));
        po.setTestCasesJson(d.getTestCases().isEmpty() ? null : jsonCodec.toJson(d.getTestCases()));
        po.setConfigJson(d.getConfig() == null ? null : jsonCodec.toJson(d.getConfig()));
        po.setEnabled(d.isEnabled());
        po.setVersion(d.getVersion());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private FunctionDefinition toDomain(FunctionDefinitionPO po) {
        FunctionDefinition d = new FunctionDefinition();
        d.setFunctionName(po.getFunctionName());
        d.setDisplayName(po.getDisplayName());
        d.setType(po.getType() == null ? null : FunctionType.valueOf(po.getType()));
        d.setDescription(po.getDescription());
        d.setClassName(po.getClassName());
        d.setJarPath(po.getJarPath());
        d.setScript(po.getScript());
        if (po.getParamsJson() != null) {
            d.setParams(jsonCodec.fromJson(po.getParamsJson(), new TypeReference<List<FunctionParamDef>>() {
            }));
        }
        if (po.getTestCasesJson() != null) {
            d.setTestCases(jsonCodec.fromJson(po.getTestCasesJson(), new TypeReference<List<com.mkt.ruleengine.core.function.FunctionTestCase>>() {
            }));
        }
        if (po.getConfigJson() != null) {
            d.setConfig(jsonCodec.fromJson(po.getConfigJson(), new TypeReference<java.util.Map<String, Object>>() {
            }));
        }
        d.setEnabled(Boolean.TRUE.equals(po.getEnabled()));
        d.setVersion(po.getVersion() == null ? 1 : po.getVersion());
        d.setCreatedAt(po.getCreatedAt());
        d.setUpdatedAt(po.getUpdatedAt());
        return d;
    }
}
