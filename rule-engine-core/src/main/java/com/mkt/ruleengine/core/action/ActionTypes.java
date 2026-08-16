package com.mkt.ruleengine.core.action;

/**
 * 内置动作类型常量（框架内置的演示/通用执行器）。
 */
public final class ActionTypes {

    /** 发券 */
    public static final String COUPON = "COUPON";

    /** 短信 */
    public static final String SMS = "SMS";

    /** 积分 */
    public static final String POINTS = "POINTS";

    /** 推送 */
    public static final String PUSH = "PUSH";

    /** 审计日志（演示/追踪用） */
    public static final String LOG = "LOG";

    private ActionTypes() {
    }
}
