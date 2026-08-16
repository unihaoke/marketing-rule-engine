package com.mkt.ruleengine.core.spi;

import com.mkt.ruleengine.core.rule.RuleSnapshot;

import java.util.List;

/**
 * 规则快照缓存 SPI：引擎运行时只从本地缓存读取（热更新单元）。
 */
public interface RuleSnapshotCache {

    /**
     * 获取某事件下已启用的规则快照（按优先级排序）。
     */
    List<RuleSnapshot> get(String eventCode);

    /**
     * 刷新某事件的快照（配置/发布/灰度变更后调用）。
     */
    void refresh(String eventCode);

    /**
     * 全量刷新。
     */
    void refreshAll();

    /**
     * 清空缓存。
     */
    void clear();
}
