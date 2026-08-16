package com.mkt.ruleengine.infrastructure.action.executor;

import com.mkt.ruleengine.core.action.ActionExecutionContext;
import com.mkt.ruleengine.core.action.ActionExecutor;
import com.mkt.ruleengine.core.action.ActionResult;
import com.mkt.ruleengine.core.action.ActionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 审计日志动作执行器：动作链路的可观测节点（演示/追踪）。
 * 参数：message
 */
@Component
public class LogActionExecutor implements ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(LogActionExecutor.class);

    @Override
    public String actionType() {
        return ActionTypes.LOG;
    }

    @Override
    public ActionResult execute(ActionExecutionContext ctx) {
        long start = System.currentTimeMillis();
        String message = String.valueOf(ctx.param("message") == null ? "" : ctx.param("message"));
        log.info("[AUDIT-LOG] eventId={} userId={} rule={} action={} message={}",
                ctx.getEvent().getEventId(), ctx.getEvent().getUserId(),
                ctx.getRule().getRuleCode(), ctx.getAction().getActionCode(), message);
        return ActionResult.ok(ActionTypes.LOG, "audit logged", System.currentTimeMillis() - start);
    }
}
