package com.mkt.ruleengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.ActionLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ActionLogMapper extends BaseMapper<ActionLogPO> {

    /** 按事件 ID 批量查询动作执行记录（日志明细关联用） */
    @Select("""
            <script>
            SELECT * FROM t_action_log
            WHERE event_id IN
            <foreach collection="eventIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            ORDER BY id
            </script>
            """)
    List<ActionLogPO> selectByEventIds(@Param("eventIds") List<String> eventIds);
}
