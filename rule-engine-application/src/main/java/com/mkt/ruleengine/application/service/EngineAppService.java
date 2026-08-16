package com.mkt.ruleengine.application.service;

import com.mkt.ruleengine.core.engine.EngineResult;
import com.mkt.ruleengine.core.engine.RuleEngine;
import com.mkt.ruleengine.core.event.MarketingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 引擎运行时应用服务：事件触发入口。
 * 吞吐统计不在此处做内存计数，统一由数据库实时聚合查询（见 /api/engine/stats 与 /stats/by-*）。
 */
@Service
public class EngineAppService {

    private static final Logger log = LoggerFactory.getLogger(EngineAppService.class);

    private final RuleEngine ruleEngine;

    public EngineAppService(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * 事件触发（同步）：事件归一化 → 函数增强 → 规则匹配 → 动作执行。
     */
    public EngineResult trigger(MarketingEvent event) {
        long start = System.nanoTime();
        try {
            return ruleEngine.execute(event);
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            if (costMs > 100) {
                log.warn("slow engine execution: {} ms, event={}", costMs, event.getEventCode());
            }
        }
    }

    /**
     * 事件模拟触发（事件管理页测试用）：返回完整执行结果 + 每条规则的匹配结论与动作明细。
     */
    public com.mkt.ruleengine.core.engine.SimulationResult simulate(MarketingEvent event) {
        return ruleEngine.simulate(event);
    }
}
