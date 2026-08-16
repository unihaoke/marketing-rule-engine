package com.mkt.ruleengine.core.function;

/**
 * 自定义函数类型。
 */
public enum FunctionType {
    /** Java SPI：内置 Spring Bean 函数（框架内置或业务通过 @Component 扩展） */
    JAVA_SPI,
    /** 上传 Jar 包动态加载 */
    JAR,
    /** 在线编辑脚本/表达式（默认 SpEL，可选 QLExpress） */
    EXPRESSION
}
