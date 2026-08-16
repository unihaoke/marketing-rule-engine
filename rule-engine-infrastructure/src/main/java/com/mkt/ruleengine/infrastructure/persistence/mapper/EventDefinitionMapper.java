package com.mkt.ruleengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.EventDefinitionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventDefinitionMapper extends BaseMapper<EventDefinitionPO> {
}
