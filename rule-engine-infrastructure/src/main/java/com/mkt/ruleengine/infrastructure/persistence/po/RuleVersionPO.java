package com.mkt.ruleengine.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则版本表（发布快照 + 状态机）。
 */
@Data
@TableName("t_rule_version")
public class RuleVersionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleCode;
    private Long versionNo;
    /** DRAFT / PUBLISHED / OFFLINE / ARCHIVED */
    private String status;
    private String contentJson;
    private String changeLog;
    private String publishedBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    // ---------- 联表查询附加字段（快照装载） ----------

    @TableField(exist = false)
    private String eventCode;

    @TableField(exist = false)
    private Integer priority;

    @TableField(exist = false)
    private String groupRuleName;
}
