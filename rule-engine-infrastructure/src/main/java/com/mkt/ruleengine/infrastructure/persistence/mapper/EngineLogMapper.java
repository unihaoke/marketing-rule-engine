package com.mkt.ruleengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.EngineLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EngineLogMapper extends BaseMapper<EngineLogPO> {

    /** 查询用户某事件的历史触发日期（去重升序），用于签到天数/连续天数计算 */
    @Select("""
            SELECT DISTINCT DATE(created_at)
            FROM t_engine_log
            WHERE user_id = #{userId} AND event_code = #{eventCode}
            ORDER BY DATE(created_at)
            """)
    List<java.sql.Date> selectEventDates(@Param("userId") String userId, @Param("eventCode") String eventCode);
}
