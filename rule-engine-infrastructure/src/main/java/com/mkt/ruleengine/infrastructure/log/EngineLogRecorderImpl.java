package com.mkt.ruleengine.infrastructure.log;

import com.mkt.ruleengine.core.engine.EngineResult;
import com.mkt.ruleengine.core.spi.EngineLogRecorder;
import com.mkt.ruleengine.infrastructure.expression.JacksonJsonCodec;
import com.mkt.ruleengine.infrastructure.persistence.mapper.EngineLogMapper;
import com.mkt.ruleengine.infrastructure.persistence.po.EngineLogPO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 引擎执行日志落库实现。
 */
@Component
public class EngineLogRecorderImpl implements EngineLogRecorder {

    private final EngineLogMapper mapper;
    private final JacksonJsonCodec jsonCodec;

    public EngineLogRecorderImpl(EngineLogMapper mapper, JacksonJsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public void record(EngineResult result) {
        EngineLogPO po = new EngineLogPO();
        po.setEventId(result.getEventId());
        po.setEventCode(result.getEventCode());
        po.setUserId(result.getUserId());
        po.setChannelId(result.getChannelId());
        po.setTraceId(result.getTraceId());
        po.setSuccess(result.isSuccess());
        po.setErrorMessage(result.getErrorMessage());
        po.setMatchedRuleCodes(result.getMatchedRuleCodes().stream()
                .collect(Collectors.joining(",")));
        po.setCostMs(result.getCostMs());
        po.setAttributesJson(jsonCodec.toJson(result.getAttributes()));
        po.setCreatedAt(LocalDateTime.now());
        mapper.insert(po);
    }
}
