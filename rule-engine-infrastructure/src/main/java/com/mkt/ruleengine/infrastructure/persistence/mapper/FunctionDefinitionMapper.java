package com.mkt.ruleengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.FunctionDefinitionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FunctionDefinitionMapper extends BaseMapper<FunctionDefinitionPO> {
}
