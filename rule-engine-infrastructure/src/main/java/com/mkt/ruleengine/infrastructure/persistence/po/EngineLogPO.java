package com.mkt.ruleengine.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 引擎执行日志表。
 */
@Data
@TableName("t_engine_log")
public class EngineLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventId;
    private String eventCode;
    private String userId;
    private String channelId;
    private String traceId;
    private Boolean success;
    private String errorMessage;
    /** 命中规则编码（逗号分隔） */
    private String matchedRuleCodes;
    private Long costMs;
    /** 增强属性 JSON */
    private String attributesJson;
    private LocalDateTime createdAt;
}
