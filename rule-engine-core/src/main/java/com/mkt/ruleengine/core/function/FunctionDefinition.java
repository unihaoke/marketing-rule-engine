package com.mkt.ruleengine.core.function;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 函数注册定义：自定义函数元数据（Jar 上传 / 在线脚本 / Java SPI）。
 */
public class FunctionDefinition {

    /** 函数名（规则画布中引用），全局唯一 */
    private String functionName;

    /** 展示名 */
    private String displayName;

    /** 函数类型 */
    private FunctionType type;

    /** 函数描述 */
    private String description;

    /** Jar 类型：实现类的全限定名；EXPRESSION 类型：脚本内容；JAVA_SPI：Bean 名或类名 */
    private String className;

    /** Jar 类型：Jar 文件存储路径（URLClassLoader 加载） */
    private String jarPath;

    /** 脚本/表达式内容（EXPRESSION 类型） */
    private String script;

    /** 入参定义（用于画布提示与运行时绑定校验） */
    private List<FunctionParamDef> params = new ArrayList<>();

    /** 在线测试案例（函数管理一键填入示例入参） */
    private List<FunctionTestCase> testCases = new ArrayList<>();

    /** 扩展配置（阶梯返利档位等业务参数默认值） */
    private Map<String, Object> config;

    /** 是否启用 */
    private boolean enabled = true;

    /** 函数版本（热更新） */
    private int version = 1;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public FunctionDefinition() {
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public FunctionType getType() {
        return type;
    }

    public void setType(FunctionType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getJarPath() {
        return jarPath;
    }

    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public List<FunctionParamDef> getParams() {
        return Collections.unmodifiableList(params);
    }

    public void setParams(List<FunctionParamDef> params) {
        this.params = params == null ? new ArrayList<>() : new ArrayList<>(params);
    }

    public List<FunctionTestCase> getTestCases() {
        return Collections.unmodifiableList(testCases);
    }

    public void setTestCases(List<FunctionTestCase> testCases) {
        this.testCases = testCases == null ? new ArrayList<>() : new ArrayList<>(testCases);
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "FunctionDefinition{" + functionName + "(" + type + ")@" + version + '}';
    }
}
