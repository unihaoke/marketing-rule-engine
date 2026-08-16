package com.mkt.ruleengine.core.repository;

import com.mkt.ruleengine.core.rule.RuleGroup;
import com.mkt.ruleengine.core.rule.RuleVersion;

import java.util.List;
import java.util.Optional;

/**
 * 规则配置仓储接口（规则组 + 版本管理）。
 */
public interface RuleConfigRepository {

    // ---------- 规则组 ----------

    RuleGroup saveRuleGroup(RuleGroup group);

    RuleGroup updateRuleGroup(RuleGroup group);

    Optional<RuleGroup> findRuleGroupByCode(String ruleCode);

    List<RuleGroup> findAllRuleGroups();

    /** 各规则最新版本号（ruleCode -> max(version_no)，规则列表展示用） */
    java.util.Map<String, Long> latestVersionNos();

    // ---------- 版本 ----------

    RuleVersion saveVersion(RuleVersion version);

    RuleVersion updateVersion(RuleVersion version);

    Optional<RuleVersion> findVersionById(Long id);

    List<RuleVersion> listVersions(String ruleCode);

    /** 某规则当前线上版本 */
    Optional<RuleVersion> findPublishedVersion(String ruleCode);

    /** 某规则当前草稿版本 */
    Optional<RuleVersion> findDraftVersion(String ruleCode);

    /** 某规则最近发布的版本（线上或历史，用于回溯） */
    Optional<RuleVersion> findLatestPublishedOrOfflineVersion(String ruleCode);

    /** 由版本内容重建规则组（版本回溯），事件/优先级/启停取自规则组当前记录 */
    RuleGroup rebuildGroupFromVersion(RuleVersion version);
}
