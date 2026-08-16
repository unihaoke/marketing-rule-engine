package com.mkt.ruleengine.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.RuleVersionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RuleVersionMapper extends BaseMapper<RuleVersionPO> {

    /** 查询某事件下已启用规则的最新线上版本（引擎快照装载） */
    @Select("""
            SELECT v.*, g.event_code AS event_code, g.priority AS priority, g.rule_name AS group_rule_name
            FROM t_rule_version v
            JOIN t_rule_group g ON g.rule_code = v.rule_code
            WHERE g.event_code = #{eventCode} AND g.enabled = TRUE AND v.status = 'PUBLISHED'
            """)
    List<RuleVersionPO> selectPublishedByEvent(@Param("eventCode") String eventCode);

    @Select("""
            SELECT * FROM t_rule_version
            WHERE rule_code = #{ruleCode}
            ORDER BY version_no DESC
            """)
    List<RuleVersionPO> selectByRuleCode(@Param("ruleCode") String ruleCode);

    @Select("""
            SELECT * FROM t_rule_version
            WHERE rule_code = #{ruleCode} AND status = 'PUBLISHED'
            ORDER BY version_no DESC LIMIT 1
            """)
    RuleVersionPO selectPublished(@Param("ruleCode") String ruleCode);

    @Select("""
            SELECT * FROM t_rule_version
            WHERE rule_code = #{ruleCode} AND status = 'DRAFT'
            ORDER BY version_no DESC LIMIT 1
            """)
    RuleVersionPO selectDraft(@Param("ruleCode") String ruleCode);

    @Select("""
            SELECT * FROM t_rule_version
            WHERE rule_code = #{ruleCode} AND status IN ('PUBLISHED','OFFLINE')
            ORDER BY version_no DESC LIMIT 1
            """)
    RuleVersionPO selectLatestPublishedOrOffline(@Param("ruleCode") String ruleCode);

    /** 各规则最新版本号（规则列表展示） */
    @Select("""
            SELECT rule_code AS ruleCode, MAX(version_no) AS versionNo
            FROM t_rule_version
            GROUP BY rule_code
            """)
    List<LatestVersionRow> selectLatestVersionNos();

    /** 最新版本号行 */
    class LatestVersionRow {
        private String ruleCode;
        private Long versionNo;

        public String getRuleCode() {
            return ruleCode;
        }

        public void setRuleCode(String ruleCode) {
            this.ruleCode = ruleCode;
        }

        public Long getVersionNo() {
            return versionNo;
        }

        public void setVersionNo(Long versionNo) {
            this.versionNo = versionNo;
        }
    }
}
