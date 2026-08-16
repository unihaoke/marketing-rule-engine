package com.mkt.ruleengine.infrastructure.expression;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mkt.ruleengine.core.spi.JsonCodec;
import com.mkt.ruleengine.infrastructure.persistence.ConditionNodeMixin;
import com.mkt.ruleengine.core.rule.ConditionNode;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;

/**
 * Jackson JSON 编解码实现。
 */
@Component
public class JacksonJsonCodec implements JsonCodec {

    private final ObjectMapper objectMapper;

    public JacksonJsonCodec() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 条件树多态
        this.objectMapper.addMixIn(ConditionNode.class, ConditionNodeMixin.class);
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    @Override
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("json serialize failed", e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("json deserialize failed: " + json, e);
        }
    }

    @Override
    public <T> T fromJson(String json, Type type) {
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructType(type));
        } catch (Exception e) {
            throw new IllegalStateException("json deserialize failed: " + json, e);
        }
    }

    public <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            throw new IllegalStateException("json deserialize failed: " + json, e);
        }
    }
}
