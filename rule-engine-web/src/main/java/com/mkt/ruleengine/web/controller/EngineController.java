package com.mkt.ruleengine.web.controller;

import com.mkt.ruleengine.application.service.EngineAppService;
import com.mkt.ruleengine.core.engine.EngineResult;
import com.mkt.ruleengine.core.event.MarketingEvent;
import com.mkt.ruleengine.infrastructure.log.EngineStatsQueryService;
import com.mkt.ruleengine.infrastructure.log.LogQueryService;
import com.mkt.ruleengine.infrastructure.persistence.mapper.EngineStatsMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.ActionLogPO;
import com.mkt.ruleengine.infrastructure.persistence.po.EngineLogPO;
import com.mkt.ruleengine.web.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 引擎运行时 API：事件触发 / 实时统计（数据库聚合）/ 执行日志。
 */
@RestController
@RequestMapping("/api/engine")
public class EngineController {

    /** 事件触发请求 */
    public record EventRequest(String eventId, String eventCode, String userId, String channelId,
                               Long eventTime, String traceId, Map<String, Object> params) {
    }

    private final EngineAppService engineAppService;
    private final LogQueryService logQueryService;
    private final EngineStatsQueryService statsQueryService;

    public EngineController(EngineAppService engineAppService,
                            LogQueryService logQueryService,
                            EngineStatsQueryService statsQueryService) {
        this.engineAppService = engineAppService;
        this.logQueryService = logQueryService;
        this.statsQueryService = statsQueryService;
    }

    /** 单事件触发 */
    @PostMapping("/trigger")
    public ApiResponse<EngineResult> trigger(@RequestBody EventRequest request) {
        return ApiResponse.ok(engineAppService.trigger(toEvent(request)));
    }

    /** 事件模拟触发（事件管理页测试）：返回执行结果 + 规则评估追踪（命中结论/灰度跳过/条件失败/动作明细） */
    @PostMapping("/simulate")
    public ApiResponse<com.mkt.ruleengine.core.engine.SimulationResult> simulate(@RequestBody EventRequest request) {
        return ApiResponse.ok(engineAppService.simulate(toEvent(request)));
    }

    /** 批量事件触发 */
    @PostMapping("/trigger-batch")
    public ApiResponse<List<EngineResult>> triggerBatch(@RequestBody List<EventRequest> requests) {
        List<EngineResult> results = requests.stream()
                .map(this::toEvent)
                .map(engineAppService::trigger)
                .toList();
        return ApiResponse.ok(results);
    }

    /** 吞吐统计总览（实时从数据库聚合） */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(statsQueryService.snapshot());
    }

    /** 按事件统计报表（触发量 / 命中量 / 成功量 / 平均耗时） */
    @GetMapping("/stats/by-event")
    public ApiResponse<List<EngineStatsMapper.EventStatRow>> statsByEvent() {
        return ApiResponse.ok(statsQueryService.byEvent());
    }

    /** 按动作统计报表（执行量 / 成功量 / 平均耗时） */
    @GetMapping("/stats/by-action")
    public ApiResponse<List<EngineStatsMapper.ActionStatRow>> statsByAction() {
        return ApiResponse.ok(statsQueryService.byAction());
    }

    /** 最近 N 天每日触发趋势 */
    @GetMapping("/stats/by-day")
    public ApiResponse<List<EngineStatsMapper.DailyStatRow>> statsByDay(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(statsQueryService.byDay(days));
    }

    /** 引擎执行日志 */
    @GetMapping("/logs")
    public ApiResponse<LogQueryService.PageResult<EngineLogPO>> logs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String eventCode,
            @RequestParam(required = false) String userId) {
        return ApiResponse.ok(logQueryService.pageEngineLogs(page, size, eventCode, userId));
    }

    /** 执行明细分页（日志 + 关联动作 + 解析属性：命中规则 / 执行动作 / 耗时 / 属性） */
    @GetMapping("/logs/detail")
    public ApiResponse<LogQueryService.PageResult<LogQueryService.EngineLogDetail>> logDetails(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String eventCode,
            @RequestParam(required = false) String userId) {
        return ApiResponse.ok(logQueryService.pageEngineLogDetails(page, size, eventCode, userId));
    }

    /** 动作执行记录 */
    @GetMapping("/action-logs")
    public ApiResponse<LogQueryService.PageResult<ActionLogPO>> actionLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) String actionCode) {
        return ApiResponse.ok(logQueryService.pageActionLogs(page, size, eventId, ruleCode, actionCode));
    }

    private MarketingEvent toEvent(EventRequest request) {
        return new MarketingEvent(request.eventId(), request.eventCode(), request.userId(),
                request.channelId(), request.eventTime() == null ? 0 : request.eventTime(),
                request.traceId(), request.params());
    }
}
