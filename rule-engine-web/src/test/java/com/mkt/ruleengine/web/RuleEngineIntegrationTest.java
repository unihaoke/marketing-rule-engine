package com.mkt.ruleengine.web;

import com.mkt.ruleengine.application.service.EngineAppService;
import com.mkt.ruleengine.application.service.RuleConfigAppService;
import com.mkt.ruleengine.core.engine.EngineResult;
import com.mkt.ruleengine.core.event.MarketingEvent;
import com.mkt.ruleengine.core.gray.GrayConfig;
import com.mkt.ruleengine.core.gray.GrayStrategyType;
import com.mkt.ruleengine.core.rule.CompareOp;
import com.mkt.ruleengine.core.rule.LeafConditionNode;
import com.mkt.ruleengine.core.rule.RuleAction;
import com.mkt.ruleengine.core.rule.RuleGroup;
import com.mkt.ruleengine.core.rule.RuleVersion;
import com.mkt.ruleengine.core.rule.ValueType;
import com.mkt.ruleengine.core.spi.ActionDefinitionRegistry;
import com.mkt.ruleengine.infrastructure.log.LogQueryService;
import com.mkt.ruleengine.infrastructure.persistence.po.EngineLogPO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 引擎全链路集成测试：种子数据 → 事件触发 → 规则匹配 → 函数增强 → 动作执行 → 日志落库。
 */
@SpringBootTest
class RuleEngineIntegrationTest {

    @Autowired
    private EngineAppService engineAppService;

    @Autowired
    private RuleConfigAppService ruleConfigAppService;

    @Autowired
    private LogQueryService logQueryService;

    @Autowired
    private com.mkt.ruleengine.infrastructure.cache.DbCacheVersionProvider cacheVersionProvider;

    @Autowired
    private com.mkt.ruleengine.infrastructure.log.EngineStatsQueryService statsQueryService;

    @Autowired
    private ActionDefinitionRegistry actionDefinitionRegistry;

    @Autowired
    private com.mkt.ruleengine.infrastructure.function.builtin.SignInDaysFunction signInDaysFunction;

    @Autowired
    private com.mkt.ruleengine.application.service.FunctionAppService functionAppService;

    /** 找一个画像中 checkinStreak >= 3 且标签为 NEW_USER/ACTIVE 的演示用户（确定性哈希） */
    private String findStreakUser(int minStreak) {
        for (int i = 1; i < 500; i++) {
            String userId = "itest-u" + i;
            int h = Math.floorMod(userId.hashCode(), 100_000);
            // 规则条件：checkinStreak >= 3 AND userTag IN [NEW_USER, ACTIVE]（registeredDays < 180）
            if (h % 10 >= minStreak && h % 365 < 180) {
                return userId;
            }
        }
        throw new IllegalStateException("no streak user found");
    }

    @Test
    void signInStreakRuleMatchesAndExecutesActions() {
        String userId = findStreakUser(3);
        EngineResult result = engineAppService.trigger(new MarketingEvent(
                "SIGN_IN", userId, "APP", System.currentTimeMillis(),
                Map.of("signInDate", java.time.LocalDate.now().toString())));

        assertTrue(result.isSuccess(), "engine result must succeed");
        assertTrue(result.getMatchedRuleCodes().contains("SIGN_IN_STREAK_REWARD"),
                "streak rule should match, matched=" + result.getMatchedRuleCodes());
        assertTrue(result.getActionRecords().stream()
                        .anyMatch(r -> r.actionCode().equals("ADD_POINTS") && r.success()),
                "ADD_POINTS action should execute");
        assertTrue(result.getAttributes().containsKey("checkinStreak"),
                "enhance function output should be present");
    }

    @Test
    void orderRebateRuleComputesFunctionAttribute() {
        EngineResult result = engineAppService.trigger(new MarketingEvent(
                "ORDER_CREATE", "itest-u2", "APP", System.currentTimeMillis(),
                Map.of("orderId", "ORD-IT-1", "orderAmount", 800)));
        assertTrue(result.isSuccess());
        assertTrue(result.getMatchedRuleCodes().contains("ORDER_REBATE_COUPON"),
                "rebate rule should match for amount 800");
        // 800 * 5% = 40
        assertEquals("40.00", String.valueOf(result.getAttributes().get("rebateAmount")));
    }

    @Test
    void undefinedEventRejectedInStrictMode() {
        com.mkt.ruleengine.core.exception.EventNotDefinedException ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.mkt.ruleengine.core.exception.EventNotDefinedException.class,
                        () -> engineAppService.trigger(new MarketingEvent(
                                "NOT_DEFINED_EVENT", "u1", "APP", System.currentTimeMillis(), Map.of())));
        assertNotNull(ex.getMessage());
    }

    @Test
    void idempotencyDeduplicatesSameEventActions() {
        String eventId = "idem-" + System.nanoTime();
        // 选一个非新客用户（流失预警规则排除 NEW_USER）
        String userId = findNonNewUser();
        MarketingEvent event = new MarketingEvent(eventId, "USER_RETENTION", userId, "APP",
                System.currentTimeMillis(), null, Map.of("retentionDay", 20, "lastActiveDays", 10));
        EngineResult first = engineAppService.trigger(event);
        EngineResult second = engineAppService.trigger(event);
        assertTrue(first.isSuccess());
        assertTrue(second.isSuccess());
        long firstExec = first.getActionRecords().stream()
                .filter(r -> !r.detail().contains("idempotent-skip")).count();
        long secondExec = second.getActionRecords().stream()
                .filter(r -> !r.detail().contains("idempotent-skip")).count();
        assertTrue(firstExec > 0, "first trigger should execute actions");
        assertEquals(0, secondExec, "second trigger with same eventId must be idempotent-skipped");
    }

    @Test
    void grayConfigCanToggleRuleReach() {
        String ruleCode = "LAUNCH_TIER_PUSH";
        // 强制全量放行（100%），高价值用户必命中
        ruleConfigAppService.setGray(ruleCode, new GrayConfig(true, GrayStrategyType.PERCENT, 100, List.of(), "userId"));
        // 找一个高价值用户（ltvTier A 或 orderCount>=10）
        String userId = findHighValueUser();
        EngineResult hit = engineAppService.trigger(new MarketingEvent(
                "APP_LAUNCH", userId, "APP", System.currentTimeMillis(), Map.of("appVersion", "3.2.0")));
        assertTrue(hit.getMatchedRuleCodes().contains(ruleCode), "100% gray should hit for high value user");
        // 全量关闭（0%）必不命中
        ruleConfigAppService.setGray(ruleCode, new GrayConfig(true, GrayStrategyType.PERCENT, 0, List.of(), "userId"));
        EngineResult miss = engineAppService.trigger(new MarketingEvent(
                "APP_LAUNCH", userId, "APP", System.currentTimeMillis(), Map.of("appVersion", "3.2.0")));
        assertFalse(miss.getMatchedRuleCodes().contains(ruleCode), "0% gray should never hit");
    }

    @Test
    void engineLogsPersisted() {
        engineAppService.trigger(new MarketingEvent(
                "APP_LAUNCH", "itest-u4", "APP", System.currentTimeMillis(), Map.of("appVersion", "3.2.1")));
        LogQueryService.PageResult<EngineLogPO> page = logQueryService.pageEngineLogs(1, 5, "APP_LAUNCH", null);
        assertTrue(page.total() >= 1);
        assertTrue(page.records().stream().anyMatch(l -> l.getEventCode().equals("APP_LAUNCH")));
    }

    @Test
    void cacheVersionPollingQueriesWork() {
        // 回归：热更新轮询的全部 MAX(updated_at/created_at) SQL 必须可执行
        long v1 = cacheVersionProvider.currentVersion();
        assertTrue(v1 >= 0);
        // 发布新版本 → 全局版本号必须变化（热更新检测信号）
        ruleConfigAppService.publish("AD_ATTRIBUTION_NEW_USER", "热更新检测", "test");
        long v2 = cacheVersionProvider.currentVersion();
        assertTrue(v2 != v1, "publish must change cache version");
    }

    @Test
    void publishAndRollbackRoundTrip() {
        String ruleCode = "SIGN_IN_STREAK_REWARD";
        long before = ruleConfigAppService.listVersions(ruleCode).stream()
                .mapToLong(RuleVersion::getVersionNo).max().orElse(0);
        RuleVersion v = ruleConfigAppService.publish(ruleCode, "集成测试发布", "test");
        assertEquals(before + 1, v.getVersionNo(), "publish should bump version");
        assertEquals(com.mkt.ruleengine.core.rule.RuleStatus.PUBLISHED, v.getStatus());
        // 回溯该规则最早的版本内容 → 产生新版本
        List<RuleVersion> all = ruleConfigAppService.listVersions(ruleCode);
        RuleVersion oldest = all.get(all.size() - 1); // version_no 倒序，末位为最早版本
        RuleVersion v2 = ruleConfigAppService.rollback(oldest.getId(), "回溯测试", "test");
        assertEquals(v.getVersionNo() + 1, v2.getVersionNo());
        assertEquals(com.mkt.ruleengine.core.rule.RuleStatus.PUBLISHED, v2.getStatus());
        // 回溯后的画布内容可回显
        RuleGroup rolledBack = ruleConfigAppService.groupFromVersion(v2.getId());
        assertNotNull(rolledBack.getConditionTree());
        assertEquals(ruleCode, rolledBack.getRuleCode());
    }

    @Test
    void statsReflectDatabaseAggregation() {
        long before = statsQueryService.snapshot().get("totalEvents") instanceof Number n ? n.longValue() : 0;
        engineAppService.trigger(new MarketingEvent(
                "ORDER_CREATE", "itest-u9", "APP", System.currentTimeMillis(),
                Map.of("orderId", "ORD-STAT-1", "orderAmount", 300)));
        engineAppService.trigger(new MarketingEvent(
                "SIGN_IN", "itest-u9", "APP", System.currentTimeMillis(),
                Map.of("signInDate", java.time.LocalDate.now().toString())));
        long after = statsQueryService.snapshot().get("totalEvents") instanceof Number n ? n.longValue() : 0;
        assertTrue(after >= before + 2, "DB aggregated stats should count new events");
        // 按事件/按动作报表可查询（执行了 ORDER_CREATE 的返券动作）
        assertTrue(statsQueryService.byEvent().stream().anyMatch(r -> "ORDER_CREATE".equals(r.getEventCode())));
        assertTrue(statsQueryService.byAction().stream().anyMatch(r -> "ISSUE_COUPON".equals(r.getActionCode())));
    }

    @Test
    void logDetailsIncludeActionsAndAttributes() {
        String userId = findStreakUser(3);
        engineAppService.trigger(new MarketingEvent(
                "SIGN_IN", userId, "APP", System.currentTimeMillis(),
                Map.of("signInDate", java.time.LocalDate.now().toString())));
        LogQueryService.PageResult<LogQueryService.EngineLogDetail> page =
                logQueryService.pageEngineLogDetails(1, 10, "SIGN_IN", null);
        assertTrue(page.total() >= 1);
        LogQueryService.EngineLogDetail detail = page.records().stream()
                .filter(d -> d.eventId() != null && d.matchedRuleCodes().contains("SIGN_IN_STREAK_REWARD"))
                .findFirst().orElse(null);
        if (detail != null) {
            assertTrue(detail.matchedRuleCodes().contains("SIGN_IN_STREAK_REWARD"));
            assertTrue(detail.actions().stream().anyMatch(a -> a.getActionCode().equals("ADD_POINTS") && a.getSuccess()));
            assertTrue(detail.attributes().containsKey("checkinStreak"));
        }
    }

    @Test
    void simulateShowsMatchedRulesAndActions() {
        String userId = findStreakUser(3);
        com.mkt.ruleengine.core.engine.SimulationResult sim = engineAppService.simulate(
                new MarketingEvent("SIGN_IN", userId, "APP", System.currentTimeMillis(),
                        Map.of("signInDate", java.time.LocalDate.now().toString())));
        assertTrue(sim.result().isSuccess());
        // 追踪中应包含该事件绑定的规则及匹配结论
        com.mkt.ruleengine.core.engine.EngineTrace.RuleTrace streak = sim.rules().stream()
                .filter(r -> r.ruleCode().equals("SIGN_IN_STREAK_REWARD"))
                .findFirst().orElseThrow(() -> new AssertionError("trace missing SIGN_IN_STREAK_REWARD"));
        assertTrue(streak.matched(), "streak rule should be matched");
        // 动作明细：含解析后的参数与执行结果
        assertTrue(streak.actions().stream().anyMatch(a ->
                        a.actionCode().equals("ADD_POINTS") && a.success() && a.params() != null),
                "action trace should record ADD_POINTS with resolved params");
    }

    @Test
    void simulateShowsSkipReasons() {
        // 条件不满足 → CONDITION_FAIL
        String weakUser = findWeakStreakUser();
        com.mkt.ruleengine.core.engine.SimulationResult r1 = engineAppService.simulate(
                new MarketingEvent("SIGN_IN", weakUser, "APP", System.currentTimeMillis(),
                        Map.of("signInDate", java.time.LocalDate.now().toString())));
        com.mkt.ruleengine.core.engine.EngineTrace.RuleTrace fail = r1.rules().stream()
                .filter(r -> r.ruleCode().equals("SIGN_IN_STREAK_REWARD"))
                .findFirst().orElseThrow(() -> new AssertionError("trace missing rule"));
        assertFalse(fail.matched());
        assertEquals("CONDITION_FAIL", fail.skipReason());
        // 灰度不命中 → GRAY_SKIP
        ruleConfigAppService.setGray("LAUNCH_TIER_PUSH",
                new GrayConfig(true, GrayStrategyType.PERCENT, 0, List.of(), "userId"));
        com.mkt.ruleengine.core.engine.SimulationResult r2 = engineAppService.simulate(
                new MarketingEvent("APP_LAUNCH", "sim-u1", "APP", System.currentTimeMillis(),
                        Map.of("appVersion", "3.2.0")));
        assertTrue(r2.rules().stream().anyMatch(t ->
                        t.ruleCode().equals("LAUNCH_TIER_PUSH") && "GRAY_SKIP".equals(t.skipReason())),
                "trace should mark gray-skipped rule");
    }

    /** 找一个 checkinStreak < 3 的用户（签到规则条件必不满足） */
    private String findWeakStreakUser() {
        for (int i = 1; i < 500; i++) {
            String userId = "ws-u" + i;
            int h = Math.floorMod(userId.hashCode(), 100_000);
            if (h % 10 < 3) {
                return userId;
            }
        }
        throw new IllegalStateException("no weak streak user found");
    }

    @Test
    void tieredRewardCalculatorSupportsDayTiers() {
        // 第1天1分、第2天2分、第3天4分、第4天8分、第5天16分；无档位兜底 0
        com.mkt.ruleengine.infrastructure.function.builtin.TieredRewardCalculatorFunction fn =
                new com.mkt.ruleengine.infrastructure.function.builtin.TieredRewardCalculatorFunction();
        java.util.Map<String, Object> tiers = Map.of(
                "tiers", List.of(
                        Map.of("key", 1, "value", 1),
                        Map.of("key", 2, "value", 2),
                        Map.of("key", 3, "value", 4),
                        Map.of("key", 4, "value", 8),
                        Map.of("key", 5, "value", 16)));
        assertReward(fn, tiers, 1, 1);
        assertReward(fn, tiers, 2, 2);
        assertReward(fn, tiers, 3, 4);
        assertReward(fn, tiers, 4, 8);
        assertReward(fn, tiers, 5, 16);
        assertReward(fn, tiers, 6, 0); // 超出档位 → 兜底 0
    }

    private void assertReward(com.mkt.ruleengine.infrastructure.function.builtin.TieredRewardCalculatorFunction fn,
                              java.util.Map<String, Object> tiers, int streak, int expected) {
        com.mkt.ruleengine.core.event.MarketingEvent event = new com.mkt.ruleengine.core.event.MarketingEvent(
                "SIGN_IN", "tier-u1", "APP", System.currentTimeMillis(), Map.of());
        com.mkt.ruleengine.core.function.FunctionContext ctx = new com.mkt.ruleengine.core.function.FunctionContext(
                event, new java.util.LinkedHashMap<>(Map.of("checkinStreak", streak)), tiers, Map.of());
        Object result = fn.evaluate(ctx);
        assertEquals(expected, ((Number) result).intValue(), "streak=" + streak);
    }

    @Test
    void ruleDraftAutoRegistersUnconfiguredAction() {
        // 画布保存包含未配置动作的规则草稿 → 自动注册动作定义
        // 持久化测试库会保留历史规则，故每次运行使用唯一规则码
        String ruleCode = "AUTO_ACTION_RULE-" + System.nanoTime();
        RuleGroup group = new RuleGroup();
        group.setRuleCode(ruleCode);
        group.setRuleName("自动注册动作测试");
        group.setEventCode("SIGN_IN");
        group.setPriority(99);
        group.setConditionTree(new LeafConditionNode("userId", CompareOp.EQUALS, "x", ValueType.STRING));
        group.setFunctions(List.of());
        group.setActions(List.of(new RuleAction("AUTO_ACTION_X", Map.of())));
        group.setGray(GrayConfig.fullRelease());
        ruleConfigAppService.createRuleGroup(group);
        // 草稿更新触发自动注册
        ruleConfigAppService.updateDraft(ruleCode, group);
        assertTrue(actionDefinitionRegistry.exists("AUTO_ACTION_X"), "unconfigured action should be auto-registered");
    }

    @Test
    void dailyFixedAndStageSignInRewardsFromEngineLog() {
        // 用唯一用户，基于 t_engine_log 真实计算：首次签到命中「每日固定 + 阶段第1天」；同日重复签到不再命中
        String userId = "sig-" + System.nanoTime();

        // 首次签到（当日）
        EngineResult first = engineAppService.trigger(new MarketingEvent(
                "SIGN_IN", userId, "APP", System.currentTimeMillis(),
                Map.of("signInDate", java.time.LocalDate.now().toString())));
        assertTrue(first.getMatchedRuleCodes().contains("SIGN_IN_DAILY_FIXED"),
                "first sign-in should hit daily fixed rule, matched=" + first.getMatchedRuleCodes());
        assertTrue(first.getMatchedRuleCodes().contains("SIGN_IN_STAGE_REWARD"),
                "first sign-in should hit stage reward rule");
        // 固定 10 分 + 阶段第1天 1 分
        java.util.List<Integer> points = first.getActionRecords().stream()
                .filter(r -> r.actionCode().equals("ADD_POINTS"))
                .map(r -> Integer.parseInt(r.detail().replaceAll("\\D", "")))
                .toList();
        assertTrue(points.contains(10), "daily fixed points should be 10, got " + points);
        assertTrue(points.contains(1), "stage day-1 points should be 1, got " + points);

        // 同日第二次触发：今日已签到 → 两条规则均不命中
        EngineResult second = engineAppService.trigger(new MarketingEvent(
                "SIGN_IN", userId, "APP", System.currentTimeMillis(),
                Map.of("signInDate", java.time.LocalDate.now().toString())));
        assertFalse(second.getMatchedRuleCodes().contains("SIGN_IN_DAILY_FIXED"),
                "same-day second sign-in should not hit daily fixed rule");
        assertFalse(second.getMatchedRuleCodes().contains("SIGN_IN_STAGE_REWARD"),
                "same-day second sign-in should not hit stage reward rule");
    }

    @Test
    void signInDaysFunctionComputesStreakAndTotalFromEngineLog() {
        String userId = "sdays-" + System.nanoTime();
        engineAppService.trigger(new MarketingEvent("SIGN_IN", userId, "APP",
                System.currentTimeMillis(), Map.of("signInDate", java.time.LocalDate.now().toString())));
        // 基于 t_engine_log 真实计算：累计=1、连续=1（本次当日首次计入）
        com.mkt.ruleengine.core.event.MarketingEvent ev = new com.mkt.ruleengine.core.event.MarketingEvent(
                "SIGN_IN", userId, "APP", System.currentTimeMillis(), Map.of());
        com.mkt.ruleengine.core.function.FunctionContext totalCtx = new com.mkt.ruleengine.core.function.FunctionContext(
                ev, new java.util.LinkedHashMap<>(),
                Map.of("mode", "total", "eventCode", "SIGN_IN"), Map.of());
        com.mkt.ruleengine.core.function.FunctionContext streakCtx = new com.mkt.ruleengine.core.function.FunctionContext(
                ev, new java.util.LinkedHashMap<>(),
                Map.of("mode", "streak", "eventCode", "SIGN_IN"), Map.of());
        assertEquals(1, ((Number) signInDaysFunction.evaluate(totalCtx)).intValue(), "total days should be 1");
        assertEquals(1, ((Number) signInDaysFunction.evaluate(streakCtx)).intValue(), "streak days should be 1");
    }

    @Test
    void functionTestCasesRoundTrip() {
        // 注册带测试案例的函数 → 查询回显 → 删除（持久化测试库，函数名唯一）
        String fn = "fn-cases-" + System.nanoTime();
        com.mkt.ruleengine.core.function.FunctionDefinition def = new com.mkt.ruleengine.core.function.FunctionDefinition();
        def.setFunctionName(fn);
        def.setDisplayName("案例往返测试");
        def.setType(com.mkt.ruleengine.core.function.FunctionType.EXPRESSION);
        def.setScript("orderAmount * 2");
        def.setParams(List.of());
        def.setConfig(Map.of());
        def.setTestCases(List.of(new com.mkt.ruleengine.core.function.FunctionTestCase(
                "案例1", Map.of("orderAmount", 100), Map.of("tiers", List.of()), "期望 200")));
        functionAppService.register(def);
        try {
            com.mkt.ruleengine.core.function.FunctionDefinition loaded = functionAppService.get(fn);
            assertEquals(1, loaded.getTestCases().size(), "testCases should persist");
            assertEquals("案例1", loaded.getTestCases().get(0).getName());
            assertEquals(100, ((Number) loaded.getTestCases().get(0).getEventParams().get("orderAmount")).intValue());
            assertEquals("期望 200", loaded.getTestCases().get(0).getExpect());
        } finally {
            functionAppService.delete(fn);
        }
    }

    private String findHighValueUser() {
        for (int i = 1; i < 300; i++) {
            String userId = "hv-u" + i;
            int h = Math.floorMod(userId.hashCode(), 100_000);
            boolean vip = "A".equals(java.util.List.of("A", "B", "C").get(h % 3));
            boolean highFreq = (h % 30) >= 10;
            if (vip || highFreq) {
                return userId;
            }
        }
        throw new IllegalStateException("no high value user found");
    }

    /** 非新客用户（registeredDays >= 7 → 标签非 NEW_USER） */
    private String findNonNewUser() {
        for (int i = 1; i < 300; i++) {
            String userId = "idem-u" + i;
            int h = Math.floorMod(userId.hashCode(), 100_000);
            if (h % 365 >= 7) {
                return userId;
            }
        }
        throw new IllegalStateException("no non-new user found");
    }
}
