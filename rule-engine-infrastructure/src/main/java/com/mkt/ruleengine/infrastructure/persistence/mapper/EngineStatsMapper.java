package com.mkt.ruleengine.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 引擎统计查询（实时从数据库聚合，不依赖内存计数）。
 */
@Mapper
public interface EngineStatsMapper {

    /** 事件触发总数（t_engine_log 行数） */
    @Select("SELECT COUNT(*) FROM t_engine_log")
    long countEvents();

    /** 命中规则的事件数（matched_rule_codes 非空） */
    @Select("SELECT COUNT(*) FROM t_engine_log WHERE matched_rule_codes IS NOT NULL AND matched_rule_codes <> ''")
    long countMatchedEvents();

    /** 执行成功的事件数 */
    @Select("SELECT COUNT(*) FROM t_engine_log WHERE success = TRUE")
    long countSuccessEvents();

    /** 动作执行总数 */
    @Select("SELECT COUNT(*) FROM t_action_log")
    long countActions();

    /** 平均事件耗时（ms） */
    @Select("SELECT COALESCE(AVG(cost_ms), 0) FROM t_engine_log")
    double avgCostMs();

    /** 按事件聚合：触发量 / 命中量 / 成功量 / 平均耗时 */
    @Select("""
            SELECT event_code,
                   COUNT(*)                                                AS events,
                   SUM(CASE WHEN matched_rule_codes IS NOT NULL AND matched_rule_codes <> '' THEN 1 ELSE 0 END) AS matched_events,
                   SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END)         AS success_events,
                   COALESCE(AVG(cost_ms), 0)                               AS avg_cost_ms
            FROM t_engine_log
            GROUP BY event_code
            ORDER BY events DESC
            """)
    List<EventStatRow> groupByEvent();

    /** 按动作聚合：执行量 / 成功量 / 平均耗时 */
    @Select("""
            SELECT action_code,
                   COUNT(*)                                AS actions,
                   SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END) AS success_actions,
                   COALESCE(AVG(cost_ms), 0)               AS avg_cost_ms
            FROM t_action_log
            GROUP BY action_code
            ORDER BY actions DESC
            """)
    List<ActionStatRow> groupByAction();

    /** 最近 N 天每日触发量（按事件时间分组，兼容 MySQL / H2；时间下限由调用方计算传入） */
    @Select("""
            SELECT DATE(created_at) AS stat_date, COUNT(*) AS events
            FROM t_engine_log
            WHERE created_at >= #{since}
            GROUP BY DATE(created_at)
            ORDER BY stat_date
            """)
    List<DailyStatRow> groupByDay(@Param("since") java.time.LocalDateTime since);

    /** 按事件聚合结果行 */
    class EventStatRow {
        private String eventCode;
        private Long events;
        private Long matchedEvents;
        private Long successEvents;
        private Double avgCostMs;

        public String getEventCode() {
            return eventCode;
        }

        public void setEventCode(String eventCode) {
            this.eventCode = eventCode;
        }

        public Long getEvents() {
            return events;
        }

        public void setEvents(Long events) {
            this.events = events;
        }

        public Long getMatchedEvents() {
            return matchedEvents;
        }

        public void setMatchedEvents(Long matchedEvents) {
            this.matchedEvents = matchedEvents;
        }

        public Long getSuccessEvents() {
            return successEvents;
        }

        public void setSuccessEvents(Long successEvents) {
            this.successEvents = successEvents;
        }

        public Double getAvgCostMs() {
            return avgCostMs;
        }

        public void setAvgCostMs(Double avgCostMs) {
            this.avgCostMs = avgCostMs;
        }
    }

    /** 按动作聚合结果行 */
    class ActionStatRow {
        private String actionCode;
        private Long actions;
        private Long successActions;
        private Double avgCostMs;

        public String getActionCode() {
            return actionCode;
        }

        public void setActionCode(String actionCode) {
            this.actionCode = actionCode;
        }

        public Long getActions() {
            return actions;
        }

        public void setActions(Long actions) {
            this.actions = actions;
        }

        public Long getSuccessActions() {
            return successActions;
        }

        public void setSuccessActions(Long successActions) {
            this.successActions = successActions;
        }

        public Double getAvgCostMs() {
            return avgCostMs;
        }

        public void setAvgCostMs(Double avgCostMs) {
            this.avgCostMs = avgCostMs;
        }
    }

    /** 每日统计结果行 */
    class DailyStatRow {
        private java.time.LocalDate statDate;
        private Long events;

        public java.time.LocalDate getStatDate() {
            return statDate;
        }

        public void setStatDate(java.time.LocalDate statDate) {
            this.statDate = statDate;
        }

        public Long getEvents() {
            return events;
        }

        public void setEvents(Long events) {
            this.events = events;
        }
    }
}
