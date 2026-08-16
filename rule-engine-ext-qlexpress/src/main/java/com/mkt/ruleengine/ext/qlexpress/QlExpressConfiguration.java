package com.mkt.ruleengine.ext.qlexpress;

import com.mkt.ruleengine.core.spi.ExpressionEvaluator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * QLExpress 表达式引擎装配：作为主 ExpressionEvaluator（SpEL 保留为兜底）。
 */
@Configuration
@ConditionalOnClass(name = "com.ql.util.express.ExpressRunner")
public class QlExpressConfiguration {

    @Bean
    @Primary
    public ExpressionEvaluator qlExpressEvaluator() {
        return new QlExpressEvaluator();
    }
}
