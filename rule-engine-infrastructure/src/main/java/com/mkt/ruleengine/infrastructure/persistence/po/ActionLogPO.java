package com.mkt.ruleengine.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 动作执行记录表（幂等键唯一）。
 */
@Data
@TableName("t_action_log")
public class ActionLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幂等键 eventId:ruleCode:actionCode */
    private String idempotencyKey;
    private String eventId;
    private String ruleCode;
    private String actionCode;
    private Boolean success;
    private String detail;
    /** 解析后参数 JSON */
    private String paramsJson;
    private Long costMs;
    private LocalDateTime createdAt;
}
