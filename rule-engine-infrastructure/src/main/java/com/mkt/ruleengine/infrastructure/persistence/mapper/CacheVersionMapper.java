package com.mkt.ruleengine.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 缓存版本查询（热更新轮询检测配置变更）。
 */
@Mapper
public interface CacheVersionMapper {

    @Select("SELECT MAX(updated_at) FROM t_rule_group")
    LocalDateTime maxRuleGroupUpdatedAt();

    @Select("SELECT COALESCE(MAX(version_no),0) FROM t_rule_version")
    Long maxRuleVersionNo();

    /**
     * 版本表为追加式（每次发布/回溯插入新行），新增版本即配置变更，故取 MAX(created_at)。
     */
    @Select("SELECT MAX(created_at) FROM t_rule_version")
    LocalDateTime maxRuleVersionCreatedAt();

    @Select("SELECT MAX(updated_at) FROM t_event_definition")
    LocalDateTime maxEventUpdatedAt();

    @Select("SELECT MAX(updated_at) FROM t_function_definition")
    LocalDateTime maxFunctionUpdatedAt();

    @Select("SELECT MAX(updated_at) FROM t_action_definition")
    LocalDateTime maxActionUpdatedAt();

    @Select("SELECT DISTINCT event_code FROM t_rule_group WHERE enabled = TRUE")
    List<String> distinctEnabledEventCodes();
}
