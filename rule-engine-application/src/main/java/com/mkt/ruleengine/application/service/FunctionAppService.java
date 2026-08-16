package com.mkt.ruleengine.application.service;

import com.mkt.ruleengine.core.exception.RuleConfigException;
import com.mkt.ruleengine.core.function.FunctionContext;
import com.mkt.ruleengine.core.function.FunctionDefinition;
import com.mkt.ruleengine.core.function.FunctionRegistry;
import com.mkt.ruleengine.core.function.FunctionType;
import com.mkt.ruleengine.core.function.MarketingFunction;
import com.mkt.ruleengine.core.repository.FunctionDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 自定义函数注册应用服务：Jar 上传 / 在线脚本 / Java SPI 注册 + 热更新 + 在线测试。
 */
@Service
public class FunctionAppService {

    private static final Logger log = LoggerFactory.getLogger(FunctionAppService.class);

    private final FunctionDefinitionRepository repository;
    private final FunctionRegistry registry;

    public FunctionAppService(FunctionDefinitionRepository repository, FunctionRegistry registry) {
        this.repository = repository;
        this.registry = registry;
    }

    public FunctionDefinition register(FunctionDefinition definition) {
        if (definition.getFunctionName() == null || definition.getFunctionName().isBlank()) {
            throw new RuleConfigException("functionName must not be blank");
        }
        if (definition.getType() == null) {
            throw new RuleConfigException("function type must not be null");
        }
        if (repository.findByName(definition.getFunctionName()).isPresent()) {
            throw new RuleConfigException("function already exists: " + definition.getFunctionName());
        }
        validateDefinition(definition);
        definition.setVersion(1);
        repository.save(definition);
        // 注册后即时生效（热更新）
        try {
            MarketingFunction fn = registry.load(definition);
            registry.register(definition, fn);
        } catch (Exception e) {
            log.error("function register ok but load failed: {}", definition.getFunctionName(), e);
        }
        return definition;
    }

    public FunctionDefinition update(String functionName, FunctionDefinition definition) {
        FunctionDefinition existing = get(functionName);
        definition.setFunctionName(functionName);
        definition.setVersion(existing.getVersion() + 1);
        definition.setCreatedAt(existing.getCreatedAt());
        validateDefinition(definition);
        repository.update(definition);
        // 热更新
        try {
            MarketingFunction fn = registry.load(definition);
            registry.register(definition, fn);
        } catch (Exception e) {
            log.error("function hot reload failed: {}", functionName, e);
        }
        return definition;
    }

    public void delete(String functionName) {
        repository.delete(functionName);
        registry.unregister(functionName);
    }

    public FunctionDefinition get(String functionName) {
        return repository.findByName(functionName)
                .orElseThrow(() -> new RuleConfigException("function not found: " + functionName));
    }

    public List<FunctionDefinition> list() {
        return repository.findAll();
    }

    public FunctionDefinition toggleEnabled(String functionName, boolean enabled) {
        FunctionDefinition def = get(functionName);
        def.setEnabled(enabled);
        repository.update(def);
        if (enabled) {
            try {
                registry.register(def, registry.load(def));
            } catch (Exception e) {
                log.error("function enable load failed: {}", functionName, e);
            }
        } else {
            registry.unregister(functionName);
        }
        return def;
    }

    /**
     * 在线测试函数：以示例事件参数 + 绑定参数执行。
     * <p>绑定参数可携带 {@code eventCode} 指定事件编码（默认 functionName-test），
     * 使 signInDays / todaySignedIn 等基于 t_engine_log 的函数在测试时可按真实事件语义计算。</p>
     */
    public Object testRun(String functionName, Map<String, Object> eventParams, Map<String, Object> bindings) {
        MarketingFunction fn = registry.get(functionName)
                .orElseThrow(() -> new RuleConfigException("function not loaded: " + functionName));
        FunctionDefinition def = get(functionName);
        Map<String, Object> b = bindings == null ? Map.of() : bindings;
        String eventCode = b.get("eventCode") == null ? functionName + "-test" : String.valueOf(b.get("eventCode"));
        com.mkt.ruleengine.core.event.MarketingEvent event = new com.mkt.ruleengine.core.event.MarketingEvent(
                eventCode, "test-user", "test-channel",
                System.currentTimeMillis(), eventParams == null ? Map.of() : eventParams);
        FunctionContext ctx = new FunctionContext(event, new java.util.LinkedHashMap<>(),
                b, Map.of());
        // 绑定参数写入属性，脚本可引用
        ctx.getAttributes().putAll(b);
        return fn.evaluate(ctx);
    }

    private void validateDefinition(FunctionDefinition def) {
        if (def.getType() == FunctionType.JAR && (def.getJarPath() == null || def.getJarPath().isBlank())) {
            throw new RuleConfigException("JAR function requires jarPath");
        }
        if (def.getType() == FunctionType.JAR && (def.getClassName() == null || def.getClassName().isBlank())) {
            throw new RuleConfigException("JAR function requires className");
        }
        if (def.getType() == FunctionType.EXPRESSION && (def.getScript() == null || def.getScript().isBlank())) {
            throw new RuleConfigException("EXPRESSION function requires script");
        }
        if (def.getType() == FunctionType.JAVA_SPI && (def.getClassName() == null || def.getClassName().isBlank())) {
            throw new RuleConfigException("JAVA_SPI function requires className (bean name)");
        }
    }
}
