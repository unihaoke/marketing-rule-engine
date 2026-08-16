package com.mkt.ruleengine.core.spi;

import java.lang.reflect.Type;

/**
 * JSON 编解码 SPI（基础设施层用 Jackson 实现），core 不依赖具体 JSON 库。
 */
public interface JsonCodec {

    String toJson(Object value);

    <T> T fromJson(String json, Class<T> type);

    <T> T fromJson(String json, Type type);
}
