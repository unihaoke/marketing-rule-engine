package com.mkt.ruleengine.core.action;

/**
 * 动作执行结果。
 *
 * @param success    是否成功
 * @param actionCode 动作编码
 * @param detail     执行详情（返回码/消息/业务单号等）
 * @param costMs     耗时
 */
public record ActionResult(boolean success, String actionCode, String detail, long costMs) {

    public static ActionResult ok(String actionCode, String detail, long costMs) {
        return new ActionResult(true, actionCode, detail, costMs);
    }

    public static ActionResult fail(String actionCode, String detail, long costMs) {
        return new ActionResult(false, actionCode, detail, costMs);
    }
}
