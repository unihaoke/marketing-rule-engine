package com.mkt.ruleengine.application.service;

import com.mkt.ruleengine.core.exception.RuleConfigException;
import com.mkt.ruleengine.core.function.FunctionRegistry;
import com.mkt.ruleengine.core.gray.GrayConfig;
import com.mkt.ruleengine.core.repository.RuleConfigRepository;
import com.mkt.ruleengine.core.rule.RuleContent;
import com.mkt.ruleengine.core.rule.RuleGroup;
import com.mkt.ruleengine.core.rule.RuleStatus;
import com.mkt.ruleengine.core.rule.RuleVersion;
import com.mkt.ruleengine.core.spi.ActionDefinitionRegistry;
import com.mkt.ruleengine.core.spi.JsonCodec;
import com.mkt.ruleengine.core.spi.RuleSnapshotCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 规则配置应用服务：画布配置 / 发布 / 版本回溯 / 灰度 / 上下线。
 * 所有变更后主动刷新快照缓存，同时轮询热更新兜底。
 */
@Service
public class RuleConfigAppService {

    private static final Logger log = LoggerFactory.getLogger(RuleConfigAppService.class);

    private final RuleConfigRepository repository;
    private final JsonCodec jsonCodec;
    private final RuleSnapshotCache snapshotCache;
    private final ActionDefinitionRegistry actionDefinitionRegistry;
    private final FunctionRegistry functionRegistry;
    private final com.mkt.ruleengine.core.repository.ActionDefinitionRepository actionDefinitionRepository;

    public RuleConfigAppService(RuleConfigRepository repository,
                                JsonCodec jsonCodec,
                                RuleSnapshotCache snapshotCache,
                                ActionDefinitionRegistry actionDefinitionRegistry,
                                FunctionRegistry functionRegistry,
                                com.mkt.ruleengine.core.repository.ActionDefinitionRepository actionDefinitionRepository) {
        this.repository = repository;
        this.jsonCodec = jsonCodec;
        this.snapshotCache = snapshotCache;
        this.actionDefinitionRegistry = actionDefinitionRegistry;
        this.functionRegistry = functionRegistry;
        this.actionDefinitionRepository = actionDefinitionRepository;
    }

    // ---------- 创建 / 编辑 ----------

    @Transactional
    public RuleGroup createRuleGroup(RuleGroup group) {
        group.validate();
        if (repository.findRuleGroupByCode(group.getRuleCode()).isPresent()) {
            throw new RuleConfigException("rule already exists: " + group.getRuleCode());
        }
        validateReferences(group);
        group.setEnabled(false);
        repository.saveRuleGroup(group);
        // 初始草稿版本
        RuleVersion draft = new RuleVersion(group.getRuleCode(), 1L, RuleStatus.DRAFT,
                jsonCodec.toJson(group.toContent()), "初始草稿");
        repository.saveVersion(draft);
        snapshotCache.refresh(group.getEventCode());
        return group;
    }

    @Transactional
    public RuleGroup updateDraft(String ruleCode, RuleGroup updated) {
        RuleGroup existing = getRule(ruleCode);
        updated.setRuleCode(ruleCode);
        updated.setId(existing.getId());
        updated.setEnabled(existing.isEnabled());
        updated.setCreatedBy(existing.getCreatedBy());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.validate();
        validateReferences(updated);
        // 画布直接添加的动作自动注册（保证添加的动作可执行）
        autoRegisterActions(updated);
        repository.updateRuleGroup(updated);
        // 同步草稿版本内容
        Optional<RuleVersion> draftOpt = repository.findDraftVersion(ruleCode);
        if (draftOpt.isPresent()) {
            RuleVersion draft = draftOpt.get();
            draft.setContentJson(jsonCodec.toJson(updated.toContent()));
            repository.updateVersion(draft);
        } else {
            long next = nextVersionNo(ruleCode);
            repository.saveVersion(new RuleVersion(ruleCode, next, RuleStatus.DRAFT,
                    jsonCodec.toJson(updated.toContent()), "编辑草稿"));
        }
        snapshotCache.refresh(updated.getEventCode());
        return updated;
    }

    // ---------- 发布 / 回溯 ----------

    /**
     * 发布为线上版本：当前线上版本归档 → 新版本上线 → 规则启用。
     */
    @Transactional
    public RuleVersion publish(String ruleCode, String changeLog, String operator) {
        RuleGroup group = getRule(ruleCode);
        validateReferences(group);
        repository.findPublishedVersion(ruleCode).ifPresent(v -> {
            v.setStatus(RuleStatus.ARCHIVED);
            repository.updateVersion(v);
        });
        long next = nextVersionNo(ruleCode);
        RuleVersion version = new RuleVersion(ruleCode, next, RuleStatus.PUBLISHED,
                jsonCodec.toJson(group.toContent()), changeLog == null ? "发布" : changeLog);
        version.setPublishedBy(operator);
        version.setPublishedAt(LocalDateTime.now());
        repository.saveVersion(version);
        // 发布即上线
        if (!group.isEnabled()) {
            group.setEnabled(true);
            repository.updateRuleGroup(group);
        }
        snapshotCache.refresh(group.getEventCode());
        log.info("rule published: {} v{} by {}", ruleCode, next, operator);
        return version;
    }

    /**
     * 版本回溯：将指定历史/线上版本内容重新发布为新版本。
     */
    @Transactional
    public RuleVersion rollback(Long versionId, String changeLog, String operator) {
        RuleVersion source = repository.findVersionById(versionId)
                .orElseThrow(() -> new RuleConfigException("version not found: " + versionId));
        RuleGroup group = repository.rebuildGroupFromVersion(source);
        validateReferences(group);
        repository.findPublishedVersion(group.getRuleCode()).ifPresent(v -> {
            v.setStatus(RuleStatus.ARCHIVED);
            repository.updateVersion(v);
        });
        long next = nextVersionNo(group.getRuleCode());
        RuleVersion version = new RuleVersion(group.getRuleCode(), next, RuleStatus.PUBLISHED,
                source.getContentJson(), changeLog == null ? "回溯 v" + source.getVersionNo() : changeLog);
        version.setPublishedBy(operator);
        version.setPublishedAt(LocalDateTime.now());
        repository.saveVersion(version);
        group.setEnabled(true);
        repository.updateRuleGroup(group);
        snapshotCache.refresh(group.getEventCode());
        log.info("rule rollback: {} -> v{} by {}", group.getRuleCode(), next, operator);
        return version;
    }

    // ---------- 灰度 / 上下线 ----------

    /**
     * 灰度配置：同步更新规则组与线上版本（灰度即时生效），热刷新缓存。
     */
    @Transactional
    public RuleGroup setGray(String ruleCode, GrayConfig gray) {
        RuleGroup group = getRule(ruleCode);
        group.setGray(gray);
        repository.updateRuleGroup(group);
        repository.findPublishedVersion(ruleCode).ifPresent(v -> {
            v.setContentJson(jsonCodec.toJson(group.toContent()));
            repository.updateVersion(v);
        });
        snapshotCache.refresh(group.getEventCode());
        log.info("rule gray updated: {} strategy={} percent={} channels={}",
                ruleCode, gray.getStrategy(), gray.getPercent(), gray.getChannels());
        return group;
    }

    @Transactional
    public RuleGroup online(String ruleCode) {
        RuleGroup group = getRule(ruleCode);
        group.setEnabled(true);
        repository.updateRuleGroup(group);
        snapshotCache.refresh(group.getEventCode());
        return group;
    }

    @Transactional
    public RuleGroup offline(String ruleCode) {
        RuleGroup group = getRule(ruleCode);
        group.setEnabled(false);
        repository.updateRuleGroup(group);
        snapshotCache.refresh(group.getEventCode());
        return group;
    }

    // ---------- 查询 ----------

    public RuleGroup getRule(String ruleCode) {
        return repository.findRuleGroupByCode(ruleCode)
                .orElseThrow(() -> new RuleConfigException("rule not found: " + ruleCode));
    }

    public List<RuleGroup> listRules() {
        List<RuleGroup> groups = repository.findAllRuleGroups();
        java.util.Map<String, Long> versions = repository.latestVersionNos();
        groups.forEach(g -> g.setVersionNo(versions.get(g.getRuleCode())));
        return groups;
    }

    public List<RuleVersion> listVersions(String ruleCode) {
        getRule(ruleCode);
        return repository.listVersions(ruleCode);
    }

    public RuleVersion getVersion(Long versionId) {
        return repository.findVersionById(versionId)
                .orElseThrow(() -> new RuleConfigException("version not found: " + versionId));
    }

    /** 版本内容解析（前端回显历史版本画布） */
    public RuleGroup groupFromVersion(Long versionId) {
        RuleVersion version = getVersion(versionId);
        return repository.rebuildGroupFromVersion(version);
    }

    // ---------- 私有 ----------

    private long nextVersionNo(String ruleCode) {
        return repository.listVersions(ruleCode).stream()
                .mapToLong(RuleVersion::getVersionNo)
                .max().orElse(0L) + 1;
    }

    /** 发布前引用校验：动作已配置、函数已注册（缺失仅告警不阻断） */
    private void validateReferences(RuleGroup group) {
        group.getActions().forEach(action -> {
            if (!actionDefinitionRegistry.exists(action.getActionCode())) {
                log.warn("rule {} references unconfigured action: {}", group.getRuleCode(), action.getActionCode());
            }
        });
        group.getFunctions().forEach(fn -> {
            if (!functionRegistry.contains(fn.getFunctionName())) {
                log.warn("rule {} references unregistered function: {}", group.getRuleCode(), fn.getFunctionName());
            }
        });
    }

    /**
     * 画布中直接添加的动作自动注册：未在动作配置中定义的动作编码，保存画布时自动创建基础动作定义，
     * 保证"在画布添加的动作都会被执行"（actionType 取动作编码本身，需存在对应执行器）。
     */
    private void autoRegisterActions(RuleGroup group) {
        group.getActions().forEach(action -> {
            if (!actionDefinitionRegistry.exists(action.getActionCode())) {
                com.mkt.ruleengine.core.action.ActionDefinition def = new com.mkt.ruleengine.core.action.ActionDefinition();
                def.setActionCode(action.getActionCode());
                def.setActionName(action.getActionCode());
                def.setActionType(action.getActionCode());
                def.setDescription("画布保存时自动注册");
                def.setEnabled(true);
                actionDefinitionRepository.save(def);
                actionDefinitionRegistry.refresh();
                log.info("auto registered action from rule canvas: {}", action.getActionCode());
            }
        });
    }
}
