package com.mkt.ruleengine.core.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 函数入参/绑定参数定义。
 *
 * <p>类型：
 * <ul>
 *   <li>STRING / NUMBER / BOOLEAN / DATETIME / LIST：基础类型</li>
 *   <li>USER：用户 ID（画布中提示填用户 ID 或 ${userId} 引用）</li>
 *   <li>LIST_OBJECT：对象数组（如阶梯档位 [{key,value}]），元素格式由 {@code itemSchema} 定义，
 *       画布中按子字段结构化新增/填写</li>
 * </ul>
 *
 * @param code        参数编码
 * @param name        参数名称
 * @param type        参数类型
 * @param required    是否必填
 * @param description 说明
 * @param itemSchema  LIST_OBJECT 类型的元素子字段定义（其他类型为 null）
 */
public class FunctionParamDef {

    private String code;
    private String name;
    private String type;
    private boolean required;
    private String description;
    private List<FunctionParamDef> itemSchema;
    /** 是否允许在规则画布中由用户赋值（默认 true；false 表示仅函数内部/默认值使用，画布不展示） */
    private boolean editable = true;

    public FunctionParamDef() {
    }

    public FunctionParamDef(String code, String name, String type, boolean required, String description,
                            List<FunctionParamDef> itemSchema) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.required = required;
        this.description = description;
        this.itemSchema = itemSchema == null ? null : new ArrayList<>(itemSchema);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<FunctionParamDef> getItemSchema() {
        return itemSchema == null ? null : Collections.unmodifiableList(itemSchema);
    }

    public void setItemSchema(List<FunctionParamDef> itemSchema) {
        this.itemSchema = itemSchema == null ? null : new ArrayList<>(itemSchema);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
