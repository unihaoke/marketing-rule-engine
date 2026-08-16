package com.mkt.ruleengine.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.mkt.ruleengine.core.action.ActionExecutor;
import com.mkt.ruleengine.core.action.ActionExecutorFactory;
import com.mkt.ruleengine.core.engine.ActionParamResolver;
import com.mkt.ruleengine.core.engine.DefaultActionDispatchExecutor;
import com.mkt.ruleengine.core.engine.DefaultIdempotencyStore;
import com.mkt.ruleengine.core.engine.DefaultRuleEngine;
import com.mkt.ruleengine.core.engine.EngineStage;
import com.mkt.ruleengine.core.engine.EngineStagesFactory;
import com.mkt.ruleengine.core.function.FunctionRegistry;
import com.mkt.ruleengine.core.rule.ConditionEvaluator;
import com.mkt.ruleengine.core.rule.DefaultConditionEvaluator;
import com.mkt.ruleengine.core.spi.ActionDispatchExecutor;
import com.mkt.ruleengine.core.spi.ActionLogWriter;
import com.mkt.ruleengine.core.spi.EngineLogRecorder;
import com.mkt.ruleengine.core.spi.EventDefinitionRegistry;
import com.mkt.ruleengine.core.spi.ExpressionEvaluator;
import com.mkt.ruleengine.core.spi.IdempotencyStore;
import com.mkt.ruleengine.core.spi.RuleSnapshotCache;
import com.mkt.ruleengine.core.spi.UserProfileResolver;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

/**
 * 规则引擎基础设施装配：引擎 Bean + 责任链阶段 + 持久化插件。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(RuleEngineProperties.class)
@MapperScan("com.mkt.ruleengine.infrastructure.persistence.mapper")
public class RuleEngineInfraConfiguration {

    /** 动作分发执行器（内置线程池，异步化保证事件吞吐） */
    @Bean
    public ActionDispatchExecutor actionDispatchExecutor() {
        return new DefaultActionDispatchExecutor(64, 256, 10_000);
    }

    /** 幂等存储 */
    @Bean
    public IdempotencyStore idempotencyStore() {
        return new DefaultIdempotencyStore();
    }

    /** 动作参数解析器 */
    @Bean
    public ActionParamResolver actionParamResolver(ExpressionEvaluator expressionEvaluator) {
        return new ActionParamResolver(expressionEvaluator);
    }

    /** 条件树求值器 */
    @Bean
    public ConditionEvaluator conditionEvaluator(ExpressionEvaluator expressionEvaluator) {
        return new DefaultConditionEvaluator(expressionEvaluator);
    }

    /** 动作执行器工厂（注册全部内置执行器） */
    @Bean
    public ActionExecutorFactory actionExecutorFactory(List<ActionExecutor> executors) {
        return new ActionExecutorFactory(executors);
    }

    /** 责任链阶段（模板方法钩子之一） */
    @Bean
    public List<EngineStage> engineStages(EventDefinitionRegistry eventDefinitionRegistry,
                                          UserProfileResolver userProfileResolver,
                                          FunctionRegistry functionRegistry,
                                          ConditionEvaluator conditionEvaluator,
                                          ActionExecutorFactory actionExecutorFactory,
                                          ActionParamResolver actionParamResolver,
                                          ActionDispatchExecutor actionDispatchExecutor,
                                          IdempotencyStore idempotencyStore,
                                          ActionLogWriter actionLogWriter,
                                          com.mkt.ruleengine.core.spi.ActionDefinitionRegistry actionDefinitionRegistry,
                                          RuleEngineProperties properties) {
        return EngineStagesFactory.createDefaultStages(
                eventDefinitionRegistry,
                userProfileResolver,
                functionRegistry,
                conditionEvaluator,
                actionExecutorFactory,
                actionParamResolver,
                actionDispatchExecutor,
                idempotencyStore,
                actionLogWriter,
                actionDefinitionRegistry,
                properties.strictEventValidation());
    }

    /** 默认规则引擎（可选扩展模块可替换，如 LiteFlow 编排版） */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(com.mkt.ruleengine.core.engine.RuleEngine.class)
    public DefaultRuleEngine ruleEngine(RuleSnapshotCache snapshotCache,
                                        EventDefinitionRegistry eventDefinitionRegistry,
                                        List<EngineStage> engineStages,
                                        EngineLogRecorder engineLogRecorder,
                                        RuleEngineProperties properties) {
        return new DefaultRuleEngine(snapshotCache, eventDefinitionRegistry,
                properties.strictEventValidation(), engineStages, engineLogRecorder);
    }

    /** MyBatis-Plus 分页插件（仅 MySQL 方言） */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /** REST JSON 序列化：条件树多态类型信息（与持久化编解码一致） */
    @Bean
    public org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer conditionNodeMixinCustomizer() {
        return builder -> builder.mixIn(
                com.mkt.ruleengine.core.rule.ConditionNode.class,
                com.mkt.ruleengine.infrastructure.persistence.ConditionNodeMixin.class);
    }
}
