package com.mkt.ruleengine.infrastructure.action.executor;

import com.mkt.ruleengine.core.action.ActionExecutionContext;
import com.mkt.ruleengine.core.action.ActionExecutor;
import com.mkt.ruleengine.core.action.ActionResult;
import com.mkt.ruleengine.core.action.ActionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 短信动作执行器（演示实现）。参数：smsTemplateId / mobile / content
 */
@Component
public class SmsActionExecutor implements ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(SmsActionExecutor.class);

    @Override
    public String actionType() {
        return ActionTypes.SMS;
    }

    @Override
    public ActionResult execute(ActionExecutionContext ctx) {
        long start = System.currentTimeMillis();
        String templateId = String.valueOf(ctx.param("smsTemplateId"));
        String mobile = String.valueOf(ctx.param("mobile") == null ? "" : ctx.param("mobile"));
        String content = String.valueOf(ctx.param("content") == null ? "" : ctx.param("content"));
        log.info("[MOCK-SMS] userId={} rule={} smsTemplateId={} mobile={} content={}",
                ctx.getEvent().getUserId(), ctx.getRule().getRuleCode(), templateId, mobile, content);
        return ActionResult.ok(ActionTypes.SMS,
                "sms sent: template=" + templateId, System.currentTimeMillis() - start);
    }
}
