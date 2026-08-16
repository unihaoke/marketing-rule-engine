package com.mkt.ruleengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.ActionDefinitionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActionDefinitionMapper extends BaseMapper<ActionDefinitionPO> {
}
