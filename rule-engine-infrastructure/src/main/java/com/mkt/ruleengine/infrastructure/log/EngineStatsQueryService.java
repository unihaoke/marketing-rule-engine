package com.mkt.ruleengine.infrastructure.log;

import com.mkt.ruleengine.infrastructure.persistence.mapper.EngineStatsMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 引擎统计查询服务：吞吐/命中/动作等指标全部实时从数据库聚合，不依赖内存计数。
 */
@Component
public class EngineStatsQueryService {

    private final EngineStatsMapper mapper;

    public EngineStatsQueryService(EngineStatsMapper mapper) {
        this.mapper = mapper;
    }

    /** 总览统计（实时） */
    public Map<String, Object> snapshot() {
        return Map.of(
                "totalEvents", mapper.countEvents(),
                "matchedEvents", mapper.countMatchedEvents(),
                "successEvents", mapper.countSuccessEvents(),
                "failedEvents", mapper.countEvents() - mapper.countSuccessEvents(),
                "executedActions", mapper.countActions(),
                "avgCostMs", Math.round(mapper.avgCostMs() * 100.0) / 100.0);
    }

    /** 按事件统计 */
    public List<EngineStatsMapper.EventStatRow> byEvent() {
        return mapper.groupByEvent();
    }

    /** 按动作统计 */
    public List<EngineStatsMapper.ActionStatRow> byAction() {
        return mapper.groupByAction();
    }

    /** 最近 N 天每日触发量（时间下限由 Java 计算，兼容 MySQL / H2） */
    public List<EngineStatsMapper.DailyStatRow> byDay(int days) {
        return mapper.groupByDay(LocalDateTime.now().minusDays(Math.max(1, days)));
    }
}
