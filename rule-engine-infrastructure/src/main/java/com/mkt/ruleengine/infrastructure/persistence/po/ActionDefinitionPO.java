package com.mkt.ruleengine.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动作配置表。
 */
@Data
@TableName("t_action_definition")
public class ActionDefinitionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String actionCode;
    private String actionName;
    private String actionType;
    private String description;
    /** 参数 schema JSON */
    private String paramsJson;
    /** 默认值 JSON */
    private String defaultsJson;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
