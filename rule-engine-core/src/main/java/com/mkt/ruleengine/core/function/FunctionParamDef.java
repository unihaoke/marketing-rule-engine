package com.mkt.ruleengine.core.function;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 函数入参定义。
 *
 * @param code        参数编码
 * @param name        参数名称
 * @param type        参数类型
 * @param required    是否必填
 * @param description 说明
 */
public record FunctionParamDef(
        String code,
        String name,
        String type,
        boolean required,
        String description) {
}
