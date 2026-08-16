package com.mkt.ruleengine.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 函数注册表。
 */
@Data
@TableName("t_function_definition")
public class FunctionDefinitionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String functionName;
    private String displayName;
    /** JAVA_SPI / JAR / EXPRESSION */
    private String type;
    private String description;
    private String className;
    private String jarPath;
    private String script;
    /** 入参 schema JSON */
    private String paramsJson;
    /** 在线测试案例 JSON [{name,eventParams,bindings,expect}] */
    private String testCasesJson;
    /** 扩展配置 JSON */
    private String configJson;
    private Boolean enabled;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
