package com.mkt.ruleengine.infrastructure.action.executor;

import com.mkt.ruleengine.core.action.ActionExecutionContext;
import com.mkt.ruleengine.core.action.ActionExecutor;
import com.mkt.ruleengine.core.action.ActionResult;
import com.mkt.ruleengine.core.action.ActionTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 发券动作执行器（演示实现：记录 + 返回结果；生产替换为券中心接口）。
 * 参数：couponTemplateId / count / expireDays
 */
@Component
public class CouponActionExecutor implements ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(CouponActionExecutor.class);

    @Override
    public String actionType() {
        return ActionTypes.COUPON;
    }

    @Override
    public ActionResult execute(ActionExecutionContext ctx) {
        long start = System.currentTimeMillis();
        String templateId = String.valueOf(ctx.param("couponTemplateId"));
        Object count = ctx.param("count") == null ? 1 : ctx.param("count");
        String expireDays = String.valueOf(ctx.param("expireDays") == null ? "" : ctx.param("expireDays"));
        log.info("[MOCK-COUPON] userId={} rule={} couponTemplateId={} count={} expireDays={}",
                ctx.getEvent().getUserId(), ctx.getRule().getRuleCode(), templateId, count, expireDays);
        return ActionResult.ok(ActionTypes.COUPON,
                "coupon issued: template=" + templateId + ", count=" + count, System.currentTimeMillis() - start);
    }
}
