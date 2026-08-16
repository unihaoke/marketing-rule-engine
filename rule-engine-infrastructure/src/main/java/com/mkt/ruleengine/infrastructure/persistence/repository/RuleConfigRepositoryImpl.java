package com.mkt.ruleengine.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mkt.ruleengine.core.rule.RuleContent;
import com.mkt.ruleengine.core.rule.RuleGroup;
import com.mkt.ruleengine.core.rule.RuleStatus;
import com.mkt.ruleengine.core.rule.RuleVersion;
import com.mkt.ruleengine.core.repository.RuleConfigRepository;
import com.mkt.ruleengine.infrastructure.expression.JacksonJsonCodec;
import com.mkt.ruleengine.infrastructure.persistence.mapper.RuleGroupMapper;
import com.mkt.ruleengine.infrastructure.persistence.mapper.RuleVersionMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.RuleGroupPO;
import com.mkt.ruleengine.infrastructure.persistence.po.RuleVersionPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 规则配置仓储实现（规则组 + 版本）。
 */
@Repository
public class RuleConfigRepositoryImpl implements RuleConfigRepository {

    private final RuleGroupMapper groupMapper;
    private final RuleVersionMapper versionMapper;
    private final JacksonJsonCodec jsonCodec;

    public RuleConfigRepositoryImpl(RuleGroupMapper groupMapper, RuleVersionMapper versionMapper,
                                    JacksonJsonCodec jsonCodec) {
        this.groupMapper = groupMapper;
        this.versionMapper = versionMapper;
        this.jsonCodec = jsonCodec;
    }

    // ---------- 规则组 ----------

    @Override
    @Transactional
    public RuleGroup saveRuleGroup(RuleGroup group) {
        RuleGroupPO po = toGroupPO(group);
        po.setId(null);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        groupMapper.insert(po);
        group.setId(po.getId());
        group.setCreatedAt(po.getCreatedAt());
        group.setUpdatedAt(po.getUpdatedAt());
        return group;
    }

    @Override
    @Transactional
    public RuleGroup updateRuleGroup(RuleGroup group) {
        RuleGroupPO po = toGroupPO(group);
        po.setUpdatedAt(LocalDateTime.now());
        groupMapper.update(po, new LambdaQueryWrapper<RuleGroupPO>()
                .eq(RuleGroupPO::getRuleCode, group.getRuleCode()));
        group.setUpdatedAt(po.getUpdatedAt());
        return group;
    }

    @Override
    public Optional<RuleGroup> findRuleGroupByCode(String ruleCode) {
        RuleGroupPO po = groupMapper.selectOne(new LambdaQueryWrapper<RuleGroupPO>()
                .eq(RuleGroupPO::getRuleCode, ruleCode));
        return Optional.ofNullable(po).map(this::toGroupDomain);
    }

    @Override
    public List<RuleGroup> findAllRuleGroups() {
        return groupMapper.selectList(new LambdaQueryWrapper<RuleGroupPO>()
                        .orderByAsc(RuleGroupPO::getRuleCode))
                .stream().map(this::toGroupDomain).toList();
    }

    @Override
    public java.util.Map<String, Long> latestVersionNos() {
        java.util.Map<String, Long> result = new java.util.HashMap<>();
        versionMapper.selectLatestVersionNos().forEach(row -> result.put(row.getRuleCode(), row.getVersionNo()));
        return result;
    }

    // ---------- 版本 ----------

    @Override
    @Transactional
    public RuleVersion saveVersion(RuleVersion version) {
        RuleVersionPO po = toVersionPO(version);
        po.setCreatedAt(LocalDateTime.now());
        versionMapper.insert(po);
        version.setId(po.getId());
        version.setCreatedAt(po.getCreatedAt());
        return version;
    }

    @Override
    @Transactional
    public RuleVersion updateVersion(RuleVersion version) {
        RuleVersionPO po = toVersionPO(version);
        versionMapper.updateById(po);
        return version;
    }

    @Override
    public Optional<RuleVersion> findVersionById(Long id) {
        return Optional.ofNullable(versionMapper.selectById(id)).map(this::toVersionDomain);
    }

    @Override
    public List<RuleVersion> listVersions(String ruleCode) {
        return versionMapper.selectByRuleCode(ruleCode).stream().map(this::toVersionDomain).toList();
    }

    @Override
    public Optional<RuleVersion> findPublishedVersion(String ruleCode) {
        return Optional.ofNullable(versionMapper.selectPublished(ruleCode)).map(this::toVersionDomain);
    }

    @Override
    public Optional<RuleVersion> findDraftVersion(String ruleCode) {
        return Optional.ofNullable(versionMapper.selectDraft(ruleCode)).map(this::toVersionDomain);
    }

    @Override
    public Optional<RuleVersion> findLatestPublishedOrOfflineVersion(String ruleCode) {
        return Optional.ofNullable(versionMapper.selectLatestPublishedOrOffline(ruleCode)).map(this::toVersionDomain);
    }

    // ---------- 转换 ----------

    private RuleGroupPO toGroupPO(RuleGroup g) {
        RuleGroupPO po = new RuleGroupPO();
        po.setId(g.getId());
        po.setRuleCode(g.getRuleCode());
        po.setRuleName(g.getRuleName());
        po.setEventCode(g.getEventCode());
        po.setDescription(g.getDescription());
        po.setPriority(g.getPriority());
        po.setEnabled(g.isEnabled());
        po.setCreatedBy(g.getCreatedBy());
        po.setContentJson(jsonCodec.toJson(g.toContent()));
        po.setCreatedAt(g.getCreatedAt());
        po.setUpdatedAt(g.getUpdatedAt());
        return po;
    }

    private RuleGroup toGroupDomain(RuleGroupPO po) {
        RuleGroup g = new RuleGroup();
        g.setId(po.getId());
        g.setRuleCode(po.getRuleCode());
        g.setEventCode(po.getEventCode());
        g.setPriority(po.getPriority() == null ? 100 : po.getPriority());
        g.setEnabled(Boolean.TRUE.equals(po.getEnabled()));
        g.setCreatedBy(po.getCreatedBy());
        g.setCreatedAt(po.getCreatedAt());
        g.setUpdatedAt(po.getUpdatedAt());
        if (po.getContentJson() != null) {
            g.applyContent(jsonCodec.fromJson(po.getContentJson(), RuleContent.class));
        }
        return g;
    }

    private RuleVersionPO toVersionPO(RuleVersion v) {
        RuleVersionPO po = new RuleVersionPO();
        po.setId(v.getId());
        po.setRuleCode(v.getRuleCode());
        po.setVersionNo(v.getVersionNo());
        po.setStatus(v.getStatus() == null ? null : v.getStatus().name());
        po.setContentJson(v.getContentJson());
        po.setChangeLog(v.getChangeLog());
        po.setPublishedBy(v.getPublishedBy());
        po.setPublishedAt(v.getPublishedAt());
        po.setCreatedAt(v.getCreatedAt());
        return po;
    }

    private RuleVersion toVersionDomain(RuleVersionPO po) {
        RuleVersion v = new RuleVersion();
        v.setId(po.getId());
        v.setRuleCode(po.getRuleCode());
        v.setVersionNo(po.getVersionNo());
        v.setStatus(po.getStatus() == null ? null : RuleStatus.valueOf(po.getStatus()));
        v.setContentJson(po.getContentJson());
        v.setChangeLog(po.getChangeLog());
        v.setPublishedBy(po.getPublishedBy());
        v.setPublishedAt(po.getPublishedAt());
        v.setCreatedAt(po.getCreatedAt());
        return v;
    }

    /** 版本内容 JSON → RuleGroup（版本回溯用） */
    @Override
    public RuleGroup rebuildGroupFromVersion(RuleVersion version) {
        RuleGroup g = findRuleGroupByCode(version.getRuleCode())
                .orElseGet(RuleGroup::new);
        g.setRuleCode(version.getRuleCode());
        if (g.getEventCode() == null) {
            g.setEventCode("UNKNOWN");
        }
        g.applyContent(jsonCodec.fromJson(version.getContentJson(), RuleContent.class));
        return g;
    }
}
