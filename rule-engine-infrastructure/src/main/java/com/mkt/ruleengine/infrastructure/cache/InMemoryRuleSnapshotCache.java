package com.mkt.ruleengine.infrastructure.cache;

import com.mkt.ruleengine.core.rule.RuleContent;
import com.mkt.ruleengine.core.rule.RuleSnapshot;
import com.mkt.ruleengine.core.spi.RuleSnapshotCache;
import com.mkt.ruleengine.infrastructure.expression.JacksonJsonCodec;
import com.mkt.ruleengine.infrastructure.persistence.mapper.RuleVersionMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.RuleVersionPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存规则快照缓存：懒加载 + 事件级失效（热更新单元）。
 * 引擎运行时只读本地缓存，不触库，保证高并发事件吞吐。
 */
@Component
public class InMemoryRuleSnapshotCache implements RuleSnapshotCache {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRuleSnapshotCache.class);

    private final Map<String, List<RuleSnapshot>> cache = new ConcurrentHashMap<>();
    private final RuleVersionMapper versionMapper;
    private final JacksonJsonCodec jsonCodec;

    public InMemoryRuleSnapshotCache(RuleVersionMapper versionMapper, JacksonJsonCodec jsonCodec) {
        this.versionMapper = versionMapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public List<RuleSnapshot> get(String eventCode) {
        return cache.computeIfAbsent(eventCode, this::loadFromDb);
    }

    @Override
    public void refresh(String eventCode) {
        cache.remove(eventCode);
    }

    @Override
    public void refreshAll() {
        cache.clear();
        log.info("rule snapshot cache refreshed");
    }

    @Override
    public void clear() {
        cache.clear();
    }

    private List<RuleSnapshot> loadFromDb(String eventCode) {
        List<RuleVersionPO> rows = versionMapper.selectPublishedByEvent(eventCode);
        List<RuleSnapshot> snapshots = rows.stream().map(this::toSnapshot).toList();
        log.info("load {} rule snapshot(s) for event {}", snapshots.size(), eventCode);
        return snapshots;
    }

    private RuleSnapshot toSnapshot(RuleVersionPO po) {
        RuleContent content = jsonCodec.fromJson(po.getContentJson(), RuleContent.class);
        return new RuleSnapshot(
                po.getRuleCode(),
                content.getRuleName(),
                po.getEventCode() == null ? po.getRuleCode() : po.getEventCode(),
                content.getDescription(),
                po.getPriority() == null ? 100 : po.getPriority(),
                content.getConditionTree(),
                content.getFunctions(),
                content.getActions(),
                content.getGray(),
                po.getVersionNo());
    }
}
