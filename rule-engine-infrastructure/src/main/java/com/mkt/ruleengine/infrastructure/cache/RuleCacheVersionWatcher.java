package com.mkt.ruleengine.infrastructure.cache;

import com.mkt.ruleengine.core.function.FunctionRegistry;
import com.mkt.ruleengine.core.spi.CacheVersionProvider;
import com.mkt.ruleengine.core.spi.EventDefinitionRegistry;
import com.mkt.ruleengine.core.spi.RuleSnapshotCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 配置版本轮询监听器：周期比对全局版本号，变更即触发缓存刷新（热更新）。
 * 规则发布 / 灰度调整 / 上下线 / 函数注册后，无需重启即可生效。
 */
@Component
public class RuleCacheVersionWatcher {

    private static final Logger log = LoggerFactory.getLogger(RuleCacheVersionWatcher.class);

    private final CacheVersionProvider versionProvider;
    private final RuleSnapshotCache snapshotCache;
    private final EventDefinitionRegistry eventDefinitionRegistry;
    private final FunctionRegistry functionRegistry;
    private final AtomicLong lastVersion = new AtomicLong(-1);

    public RuleCacheVersionWatcher(CacheVersionProvider versionProvider,
                                   RuleSnapshotCache snapshotCache,
                                   EventDefinitionRegistry eventDefinitionRegistry,
                                   FunctionRegistry functionRegistry) {
        this.versionProvider = versionProvider;
        this.snapshotCache = snapshotCache;
        this.eventDefinitionRegistry = eventDefinitionRegistry;
        this.functionRegistry = functionRegistry;
    }

    @Scheduled(fixedDelayString = "${rule-engine.cache.poll-interval-ms:5000}", initialDelayString = "${rule-engine.cache.poll-initial-delay-ms:3000}")
    public void poll() {
        try {
            long current = versionProvider.currentVersion();
            long last = lastVersion.getAndSet(current);
            if (last != -1 && last != current) {
                log.info("rule config changed ({} -> {}), hot refresh caches", last, current);
                snapshotCache.refreshAll();
                eventDefinitionRegistry.refresh();
                functionRegistry.reloadAll();
            }
        } catch (Exception e) {
            log.warn("cache version poll failed", e);
        }
    }
}
