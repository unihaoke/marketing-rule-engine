package com.mkt.ruleengine.core.rule;

/**
 * 规则生命周期状态。
 */
public enum RuleStatus {
    /** 草稿 */
    DRAFT,
    /** 已发布（线上生效） */
    PUBLISHED,
    /** 已下线 */
    OFFLINE,
    /** 归档（历史版本） */
    ARCHIVED
}
