package com.mkt.ruleengine.infrastructure.function;

import com.mkt.ruleengine.core.function.FunctionDefinition;
import com.mkt.ruleengine.core.function.FunctionRegistry;
import com.mkt.ruleengine.core.function.MarketingFunction;
import com.mkt.ruleengine.core.repository.FunctionDefinitionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认函数注册中心：JAVA_SPI Bean 自动注册 + Jar/脚本按定义加载，支持热更新。
 */
@Component
public class DefaultFunctionRegistry implements FunctionRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultFunctionRegistry.class);

    private final Map<String, MarketingFunction> functions = new ConcurrentHashMap<>();
    private final Map<String, FunctionDefinition> definitions = new ConcurrentHashMap<>();
    private final List<FunctionLoader> loaders;
    private final FunctionDefinitionRepository repository;

    public DefaultFunctionRegistry(List<FunctionLoader> loaders,
                                   FunctionDefinitionRepository repository,
                                   ApplicationContext applicationContext) {
        this.loaders = loaders;
        this.repository = repository;
        // JAVA_SPI：容器内所有 MarketingFunction Bean 自动注册
        applicationContext.getBeansOfType(MarketingFunction.class)
                .forEach((beanName, fn) -> {
                    FunctionDefinition def = new FunctionDefinition();
                    def.setFunctionName(fn.name());
                    def.setDisplayName(fn.name());
                    def.setType(com.mkt.ruleengine.core.function.FunctionType.JAVA_SPI);
                    def.setClassName(beanName);
                    def.setEnabled(true);
                    register(def, fn);
                });
    }

    @PostConstruct
    public void init() {
        reloadAll();
    }

    @Override
    public Optional<MarketingFunction> get(String functionName) {
        return Optional.ofNullable(functions.get(functionName));
    }

    @Override
    public boolean contains(String functionName) {
        return functions.containsKey(functionName);
    }

    @Override
    public void register(FunctionDefinition definition, MarketingFunction function) {
        functions.put(definition.getFunctionName(), function);
        definitions.put(definition.getFunctionName(), definition);
    }

    @Override
    public void unregister(String functionName) {
        functions.remove(functionName);
        definitions.remove(functionName);
    }

    @Override
    public List<String> names() {
        return List.copyOf(functions.keySet());
    }

    @Override
    public void reloadAll() {
        List<FunctionDefinition> enabled = repository.findAll().stream()
                .filter(FunctionDefinition::isEnabled)
                .toList();
        for (FunctionDefinition def : enabled) {
            try {
                MarketingFunction fn = load(def);
                register(def, fn);
                log.info("function registered: {} ({})", def.getFunctionName(), def.getType());
            } catch (Exception e) {
                log.error("function load failed, skip: {}", def.getFunctionName(), e);
            }
        }
        // 注销已删除的定义
        definitions.keySet().removeIf(name -> enabled.stream().noneMatch(d -> d.getFunctionName().equals(name)));
    }

    /** 按类型找到加载器并加载（供应用服务注册后即时生效） */
    public MarketingFunction load(FunctionDefinition definition) {
        for (FunctionLoader loader : loaders) {
            if (loader.supports(definition.getType())) {
                return loader.load(definition);
            }
        }
        throw new IllegalArgumentException("no loader for function type: " + definition.getType());
    }
}
