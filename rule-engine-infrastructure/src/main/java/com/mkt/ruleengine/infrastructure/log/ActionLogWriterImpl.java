package com.mkt.ruleengine.infrastructure.log;

import com.mkt.ruleengine.core.action.ActionExecutionContext;
import com.mkt.ruleengine.core.action.ActionResult;
import com.mkt.ruleengine.core.spi.ActionLogWriter;
import com.mkt.ruleengine.infrastructure.expression.JacksonJsonCodec;
import com.mkt.ruleengine.infrastructure.persistence.mapper.ActionLogMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.ActionLogPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 动作执行记录落库实现（幂等键唯一约束兜底去重）。
 */
@Component
public class ActionLogWriterImpl implements ActionLogWriter {

    private static final Logger log = LoggerFactory.getLogger(ActionLogWriterImpl.class);

    private final ActionLogMapper mapper;
    private final JacksonJsonCodec jsonCodec;

    public ActionLogWriterImpl(ActionLogMapper mapper, JacksonJsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void write(ActionExecutionContext ctx, ActionResult result) {
        ActionLogPO po = new ActionLogPO();
        po.setIdempotencyKey(ctx.idempotencyKey());
        po.setEventId(ctx.getEvent().getEventId());
        po.setRuleCode(ctx.getRule().getRuleCode());
        po.setActionCode(ctx.getAction().getActionCode());
        po.setSuccess(result.success());
        po.setDetail(result.detail());
        po.setParamsJson(jsonCodec.toJson(ctx.getResolvedParams()));
        po.setCostMs(result.costMs());
        po.setCreatedAt(LocalDateTime.now());
        try {
            mapper.insert(po);
        } catch (DuplicateKeyException e) {
            // 重复执行（并发/重放）直接忽略，保证幂等
            log.debug("duplicate action execution ignored: {}", po.getIdempotencyKey());
        }
    }
}
