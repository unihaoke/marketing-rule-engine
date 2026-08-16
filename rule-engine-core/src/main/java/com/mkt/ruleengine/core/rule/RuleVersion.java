package com.mkt.ruleengine.core.rule;

import java.time.LocalDateTime;

/**
 * 规则版本：发布快照（内容为 RuleGroup 序列化），支持回溯与灰度上下线。
 */
public class RuleVersion {

    private Long id;

    /** 规则编码 */
    private String ruleCode;

    /** 版本号（规则内递增，从 1 开始） */
    private long versionNo;

    /** 状态：DRAFT 草稿 / PUBLISHED 线上 / OFFLINE 下线 / ARCHIVED 历史 */
    private RuleStatus status;

    /** 内容（RuleGroup JSON：条件树 + 前置函数 + 动作 + 灰度） */
    private String contentJson;

    /** 变更说明 */
    private String changeLog;

    /** 发布人 */
    private String publishedBy;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    public RuleVersion() {
    }

    public RuleVersion(String ruleCode, long versionNo, RuleStatus status, String contentJson, String changeLog) {
        this.ruleCode = ruleCode;
        this.versionNo = versionNo;
        this.status = status;
        this.contentJson = contentJson;
        this.changeLog = changeLog;
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

    public long getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(long versionNo) {
        this.versionNo = versionNo;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "RuleVersion{" + ruleCode + "@v" + versionNo + "(" + status + ")" + '}';
    }
}
