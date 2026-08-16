package com.mkt.ruleengine.infrastructure.action.executor;

import com.mkt.ruleengine.core.action.ActionExecutionContext;
import com.mkt.ruleengine.core.action.ActionExecutor;
import com.mkt.ruleengine.core.action.ActionResult;
import com.mkt.ruleengine.core.action.ActionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 积分动作执行器（演示实现）。参数：points / reason
 */
@Component
public class PointsActionExecutor implements ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(PointsActionExecutor.class);

    @Override
    public String actionType() {
        return ActionTypes.POINTS;
    }

    @Override
    public ActionResult execute(ActionExecutionContext ctx) {
        long start = System.currentTimeMillis();
        Object points = ctx.param("points");
        String reason = String.valueOf(ctx.param("reason") == null ? "" : ctx.param("reason"));
        log.info("[MOCK-POINTS] userId={} rule={} points={} reason={}",
                ctx.getEvent().getUserId(), ctx.getRule().getRuleCode(), points, reason);
        return ActionResult.ok(ActionTypes.POINTS,
                "points added: " + points, System.currentTimeMillis() - start);
    }
}
