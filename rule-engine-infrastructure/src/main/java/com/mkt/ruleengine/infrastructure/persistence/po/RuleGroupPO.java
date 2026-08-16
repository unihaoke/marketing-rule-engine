package com.mkt.ruleengine.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则组表（当前编辑态 + 上下线开关）。
 */
@Data
@TableName("t_rule_group")
public class RuleGroupPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleCode;
    private String ruleName;
    private String eventCode;
    private String description;
    private Integer priority;
    private Boolean enabled;
    /** 规则内容 JSON（条件树 + 前置函数 + 动作 + 灰度） */
    private String contentJson;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
