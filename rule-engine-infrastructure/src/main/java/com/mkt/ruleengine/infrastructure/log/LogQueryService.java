package com.mkt.ruleengine.infrastructure.log;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mkt.ruleengine.infrastructure.expression.JacksonJsonCodec;
import com.mkt.ruleengine.infrastructure.persistence.mapper.ActionLogMapper;
import com.mkt.ruleengine.infrastructure.persistence.mapper.EngineLogMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.ActionLogPO;
import com.mkt.ruleengine.infrastructure.persistence.po.EngineLogPO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日志查询服务：引擎日志 / 动作日志 / 执行明细（日志 + 关联动作 + 解析属性）分页查询。
 */
@Component
public class LogQueryService {

    private final EngineLogMapper engineLogMapper;
    private final ActionLogMapper actionLogMapper;
    private final JacksonJsonCodec jsonCodec;

    public LogQueryService(EngineLogMapper engineLogMapper, ActionLogMapper actionLogMapper,
                           JacksonJsonCodec jsonCodec) {
        this.engineLogMapper = engineLogMapper;
        this.actionLogMapper = actionLogMapper;
        this.jsonCodec = jsonCodec;
    }

    public PageResult<EngineLogPO> pageEngineLogs(long page, long size, String eventCode, String userId) {
        Page<EngineLogPO> p = new Page<>(page, size);
        LambdaQueryWrapper<EngineLogPO> wrapper = new LambdaQueryWrapper<EngineLogPO>()
                .eq(StringUtils.hasText(eventCode), EngineLogPO::getEventCode, eventCode)
                .eq(StringUtils.hasText(userId), EngineLogPO::getUserId, userId)
                .orderByDesc(EngineLogPO::getId);
        engineLogMapper.selectPage(p, wrapper);
        return new PageResult<>(p.getTotal(), page, size, p.getRecords());
    }

    public PageResult<ActionLogPO> pageActionLogs(long page, long size, String eventId, String ruleCode, String actionCode) {
        Page<ActionLogPO> p = new Page<>(page, size);
        LambdaQueryWrapper<ActionLogPO> wrapper = new LambdaQueryWrapper<ActionLogPO>()
                .eq(StringUtils.hasText(eventId), ActionLogPO::getEventId, eventId)
                .eq(StringUtils.hasText(ruleCode), ActionLogPO::getRuleCode, ruleCode)
                .eq(StringUtils.hasText(actionCode), ActionLogPO::getActionCode, actionCode)
                .orderByDesc(ActionLogPO::getId);
        actionLogMapper.selectPage(p, wrapper);
        return new PageResult<>(p.getTotal(), page, size, p.getRecords());
    }

    /**
     * 执行明细分页：引擎日志 + 该事件关联的动作执行记录 + 解析后的运行时属性。
     * 字段对齐运营视角：命中规则 / 执行动作 / 耗时 / 属性。
     */
    public PageResult<EngineLogDetail> pageEngineLogDetails(long page, long size, String eventCode, String userId) {
        Page<EngineLogPO> p = new Page<>(page, size);
        LambdaQueryWrapper<EngineLogPO> wrapper = new LambdaQueryWrapper<EngineLogPO>()
                .eq(StringUtils.hasText(eventCode), EngineLogPO::getEventCode, eventCode)
                .eq(StringUtils.hasText(userId), EngineLogPO::getUserId, userId)
                .orderByDesc(EngineLogPO::getId);
        engineLogMapper.selectPage(p, wrapper);
        List<EngineLogPO> logs = p.getRecords();

        // 批量查询关联动作（按 event_id）
        List<String> eventIds = logs.stream().map(EngineLogPO::getEventId).toList();
        Map<String, List<ActionLogPO>> actionsByEvent = new LinkedHashMap<>();
        if (!eventIds.isEmpty()) {
            actionLogMapper.selectByEventIds(eventIds)
                    .forEach(a -> actionsByEvent.computeIfAbsent(a.getEventId(), k -> new ArrayList<>()).add(a));
        }

        List<EngineLogDetail> details = logs.stream().map(log -> new EngineLogDetail(
                log.getId(),
                log.getEventId(),
                log.getEventCode(),
                log.getUserId(),
                log.getChannelId(),
                log.getTraceId(),
                Boolean.TRUE.equals(log.getSuccess()),
                log.getErrorMessage(),
                splitMatchedRules(log.getMatchedRuleCodes()),
                actionsByEvent.getOrDefault(log.getEventId(), List.of()),
                parseAttributes(log.getAttributesJson()),
                log.getCostMs(),
                log.getCreatedAt())).toList();
        return new PageResult<>(p.getTotal(), page, size, details);
    }

    private List<String> splitMatchedRules(String codes) {
        if (!StringUtils.hasText(codes)) {
            return List.of();
        }
        return List.of(codes.split(","));
    }

    private Map<String, Object> parseAttributes(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            Map<String, Object> attrs = jsonCodec.fromJson(json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
            return attrs == null ? Map.of() : attrs;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** 执行明细行（日志 + 关联动作 + 解析属性） */
    public record EngineLogDetail(Long id, String eventId, String eventCode, String userId, String channelId,
                                  String traceId, boolean success, String errorMessage,
                                  List<String> matchedRuleCodes, List<ActionLogPO> actions,
                                  Map<String, Object> attributes, Long costMs, LocalDateTime createdAt) {
    }

    /** 分页结果 */
    public record PageResult<T>(long total, long page, long size, List<T> records) {
    }
}
