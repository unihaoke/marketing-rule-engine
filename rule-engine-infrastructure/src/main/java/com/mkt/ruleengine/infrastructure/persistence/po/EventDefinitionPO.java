package com.mkt.ruleengine.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件定义表。
 */
@Data
@TableName("t_event_definition")
public class EventDefinitionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventCode;
    private String eventName;
    private String description;
    private Boolean enabled;
    /** 入参 schema JSON */
    private String paramsJson;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
