package com.mkt.ruleengine.core.rule;

import com.mkt.ruleengine.core.gray.GrayConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则组：画布配置的完整规则（条件树 + 前置函数 + 动作 + 灰度）。
 * 编辑态对象，发布时转为不可变 {@link RuleSnapshot} 供引擎使用。
 */
public class RuleGroup {

    /** 主键（DB） */
    private Long id;

    /** 规则编码，全局唯一 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 绑定事件编码 */
    private String eventCode;

    /** 规则描述 */
    private String description;

    /** 优先级（数值越小优先级越高，同事件多规则按此排序） */
    private int priority = 100;

    /** 是否启用（上下线开关） */
    private boolean enabled = false;

    /** 条件树（AND / OR / NOT 嵌套） */
    private ConditionNode conditionTree;

    /** 前置增强函数列表 */
    private List<RuleFunction> functions = new ArrayList<>();

    /** 动作列表 */
    private List<RuleAction> actions = new ArrayList<>();

    /** 灰度配置 */
    private GrayConfig gray = GrayConfig.fullRelease();

    /** 最新版本号（展示用，非持久化；由规则列表接口填充） */
    private Long versionNo;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public RuleGroup() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ConditionNode getConditionTree() {
        return conditionTree;
    }

    public void setConditionTree(ConditionNode conditionTree) {
        this.conditionTree = conditionTree;
    }

    public List<RuleFunction> getFunctions() {
        return Collections.unmodifiableList(functions);
    }

    public void setFunctions(List<RuleFunction> functions) {
        this.functions = functions == null ? new ArrayList<>() : new ArrayList<>(functions);
    }

    public List<RuleAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public void setActions(List<RuleAction> actions) {
        this.actions = actions == null ? new ArrayList<>() : new ArrayList<>(actions);
    }

    public GrayConfig getGray() {
        return gray;
    }

    public void setGray(GrayConfig gray) {
        this.gray = gray == null ? GrayConfig.fullRelease() : gray;
    }

    public Long getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Long versionNo) {
        this.versionNo = versionNo;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    /** 基础校验：编码/事件/条件树/动作 */
    public void validate() {
        if (ruleCode == null || ruleCode.isBlank()) {
            throw new com.mkt.ruleengine.core.exception.RuleConfigException("ruleCode must not be blank");
        }
        if (eventCode == null || eventCode.isBlank()) {
            throw new com.mkt.ruleengine.core.exception.RuleConfigException("eventCode must not be blank");
        }
        if (conditionTree == null) {
            throw new com.mkt.ruleengine.core.exception.RuleConfigException("conditionTree must not be null");
        }
    }

    /** 画布字段 → 内容结构（版本快照 / 持久化 JSON） */
    public RuleContent toContent() {
        RuleContent content = new RuleContent();
        content.setRuleName(ruleName);
        content.setDescription(description);
        content.setConditionTree(conditionTree);
        content.setFunctions(functions);
        content.setActions(actions);
        content.setGray(gray);
        return content;
    }

    /** 内容结构 → 画布字段（版本回溯 / 缓存装载） */
    public void applyContent(RuleContent content) {
        if (content == null) {
            return;
        }
        this.ruleName = content.getRuleName();
        this.description = content.getDescription();
        this.conditionTree = content.getConditionTree();
        this.functions = content.getFunctions();
        this.actions = content.getActions();
        this.gray = content.getGray();
    }

    @Override
    public String toString() {
        return "RuleGroup{" + "ruleCode='" + ruleCode + '\'' + ", eventCode='" + eventCode + '\'' + '}';
    }
}
