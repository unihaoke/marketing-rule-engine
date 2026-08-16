package com.mkt.ruleengine.infrastructure.function;

import com.mkt.ruleengine.core.exception.FunctionLoadException;
import com.mkt.ruleengine.core.function.FunctionDefinition;
import com.mkt.ruleengine.core.function.FunctionType;
import com.mkt.ruleengine.core.function.MarketingFunction;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Java SPI 函数加载器：从 Spring 容器获取实现 {@link MarketingFunction} 的 Bean。
 * className 为 Bean 名或类全限定名。
 */
@Component
public class BeanFunctionLoader implements FunctionLoader {

    private final ApplicationContext applicationContext;

    public BeanFunctionLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean supports(FunctionType type) {
        return type == FunctionType.JAVA_SPI;
    }

    @Override
    public MarketingFunction load(FunctionDefinition definition) {
        String target = definition.getClassName();
        if (target == null || target.isBlank()) {
            // 缺省按函数名找 Bean
            target = definition.getFunctionName();
        }
        Object bean = null;
        try {
            bean = applicationContext.getBean(target);
        } catch (Exception e) {
            try {
                bean = applicationContext.getBean(Class.forName(target));
            } catch (Exception ex) {
                throw new FunctionLoadException("JAVA_SPI bean not found: " + target, ex);
            }
        }
        if (!(bean instanceof MarketingFunction function)) {
            throw new FunctionLoadException("bean is not a MarketingFunction: " + target);
        }
        return function;
    }
}
