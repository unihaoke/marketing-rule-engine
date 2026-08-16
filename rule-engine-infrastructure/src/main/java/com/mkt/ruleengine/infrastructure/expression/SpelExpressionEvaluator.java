package com.mkt.ruleengine.infrastructure.expression;

import com.mkt.ruleengine.core.spi.ExpressionEvaluator;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认表达式引擎：Spring Expression Language（SpEL）。
 * <ul>
 *   <li>变量可直接引用（orderAmount &gt; 100），也支持 #orderAmount 形式</li>
 *   <li>可调用注册函数 / 静态方法（T(...)）</li>
 *   <li>表达式编译缓存，高并发友好</li>
 * </ul>
 */
@Component
public class SpelExpressionEvaluator implements ExpressionEvaluator {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    @Override
    public Object evaluate(String expression, Map<String, Object> variables) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Expression expr = expressionCache.computeIfAbsent(expression, parser::parseExpression);
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        // Map 属性访问（root 对象）+ 变量引用两种方式均可用
        ctx.addPropertyAccessor(new MapAccessor());
        if (variables != null) {
            ctx.setRootObject(variables);
            variables.forEach(ctx::setVariable);
        }
        return expr.getValue(ctx);
    }
}
