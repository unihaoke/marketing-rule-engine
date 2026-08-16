package com.mkt.ruleengine.infrastructure.function;

import com.mkt.ruleengine.core.function.FunctionContext;
import com.mkt.ruleengine.core.function.FunctionDefinition;
import com.mkt.ruleengine.core.function.FunctionType;
import com.mkt.ruleengine.core.function.MarketingFunction;
import com.mkt.ruleengine.core.spi.ExpressionEvaluator;
import org.springframework.stereotype.Component;

/**
 * 表达式/脚本函数加载器：在线编辑的脚本（默认 SpEL，可选 QLExpress 扩展模块）封装为函数。
 * 脚本可引用：事件参数、用户画像、函数绑定参数、前序函数属性。
 */
@Component
public class ExpressionFunctionLoader implements FunctionLoader {

    private final ExpressionEvaluator expressionEvaluator;

    public ExpressionFunctionLoader(ExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public boolean supports(FunctionType type) {
        return type == FunctionType.EXPRESSION;
    }

    @Override
    public MarketingFunction load(FunctionDefinition definition) {
        final String script = definition.getScript();
        return new MarketingFunction() {
            @Override
            public String name() {
                return definition.getFunctionName();
            }

            @Override
            public Object evaluate(FunctionContext ctx) {
                return expressionEvaluator.evaluate(script, ctx.expressionVariables());
            }
        };
    }
}
