package com.mkt.ruleengine.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 条件树多态序列化 Mixin：core 保持零注解，JSON 编解码层注入类型信息。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "nodeType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = com.mkt.ruleengine.core.rule.LogicConditionNode.class, name = "LOGIC"),
        @JsonSubTypes.Type(value = com.mkt.ruleengine.core.rule.LeafConditionNode.class, name = "LEAF")
})
public abstract class ConditionNodeMixin {
}
