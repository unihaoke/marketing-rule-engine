package com.mkt.ruleengine.infrastructure.action.executor;

import com.mkt.ruleengine.core.action.ActionExecutionContext;
import com.mkt.ruleengine.core.action.ActionExecutor;
import com.mkt.ruleengine.core.action.ActionResult;
import com.mkt.ruleengine.core.action.ActionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 推送动作执行器（演示实现）。参数：pushTemplateId / title / body
 */
@Component
public class PushActionExecutor implements ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(PushActionExecutor.class);

    @Override
    public String actionType() {
        return ActionTypes.PUSH;
    }

    @Override
    public ActionResult execute(ActionExecutionContext ctx) {
        long start = System.currentTimeMillis();
        String templateId = String.valueOf(ctx.param("pushTemplateId"));
        String title = String.valueOf(ctx.param("title") == null ? "" : ctx.param("title"));
        String body = String.valueOf(ctx.param("body") == null ? "" : ctx.param("body"));
        log.info("[MOCK-PUSH] userId={} rule={} pushTemplateId={} title={} body={}",
                ctx.getEvent().getUserId(), ctx.getRule().getRuleCode(), templateId, title, body);
        return ActionResult.ok(ActionTypes.PUSH,
                "push sent: template=" + templateId, System.currentTimeMillis() - start);
    }
}
