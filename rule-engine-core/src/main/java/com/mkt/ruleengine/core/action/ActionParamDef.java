package com.mkt.ruleengine.core.action;

/**
 * 动作参数定义。
 *
 * @param code         参数编码（如 couponTemplateId / smsTemplateId / points）
 * @param name         参数名称
 * @param type         参数类型（STRING / NUMBER / BOOLEAN / JSON）
 * @param required     是否必填
 * @param defaultValue 默认值
 * @param description  说明
 * @param frontDisplay 是否前端展示（规则画布动作参数编辑中展示给运营填写；false 则隐藏，仅用默认值/内部传参）
 */
public record ActionParamDef(
        String code,
        String name,
        String type,
        boolean required,
        Object defaultValue,
        String description,
        Boolean frontDisplay) {

    public ActionParamDef {
        if (frontDisplay == null) {
            frontDisplay = true;
        }
    }

    /** 兼容旧构造（缺省前端展示） */
    public ActionParamDef(String code, String name, String type, boolean required,
                          Object defaultValue, String description) {
        this(code, name, type, required, defaultValue, description, true);
    }
}
