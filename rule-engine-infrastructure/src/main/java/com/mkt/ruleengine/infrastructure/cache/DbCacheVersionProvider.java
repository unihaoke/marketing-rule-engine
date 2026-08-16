package com.mkt.ruleengine.infrastructure.cache;

import com.mkt.ruleengine.core.spi.CacheVersionProvider;
import com.mkt.ruleengine.infrastructure.persistence.mapper.CacheVersionMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 数据库版本号提供者：各配置表 MAX(updated_at) / MAX(version_no) 组合哈希，
 * 任一配置变更都会产生新版本号，供轮询热更新检测。
 */
@Component
public class DbCacheVersionProvider implements CacheVersionProvider {

    private final CacheVersionMapper mapper;

    public DbCacheVersionProvider(CacheVersionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long currentVersion() {
        long v = 0;
        v = v * 31 + ts(mapper.maxRuleGroupUpdatedAt());
        v = v * 31 + safe(mapper.maxRuleVersionNo());
        v = v * 31 + ts(mapper.maxRuleVersionCreatedAt());
        v = v * 31 + ts(mapper.maxEventUpdatedAt());
        v = v * 31 + ts(mapper.maxFunctionUpdatedAt());
        v = v * 31 + ts(mapper.maxActionUpdatedAt());
        return v;
    }

    private long ts(LocalDateTime t) {
        return t == null ? 0 : t.toEpochSecond(ZoneOffset.UTC);
    }

    private long safe(Long v) {
        return v == null ? 0 : v;
    }
}
