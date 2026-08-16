package com.mkt.ruleengine.ext.qlexpress;

import com.mkt.ruleengine.core.spi.ExpressionEvaluator;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * QLExpress 动态规则表达式引擎（适配器模式）。
 *
 * <p>QLExpress 相对 SpEL 的优势：完整脚本语法（函数定义、循环、分支、赋值语句）、
 * 高并发预编译、别名与宏支持，适合运营在线编辑复杂脚本函数与规则表达式。</p>
 *
 * <p>启用方式：将 rule-engine-ext-qlexpress 加入依赖（mvn -Pqlexpress），
 * 本实现自动成为 @Primary ExpressionEvaluator，条件 EXPRESSION 与脚本函数无缝切换。</p>
 */
public class QlExpressEvaluator implements ExpressionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(QlExpressEvaluator.class);

    /** QLExpress 线程安全的预编译执行器 */
    private final ExpressRunner runner = new ExpressRunner();

    @Override
    public Object evaluate(String expression, Map<String, Object> variables) {
        try {
            DefaultContext<String, Object> context = new DefaultContext<>();
            if (variables != null) {
                variables.forEach(context::put);
            }
            // isPrecision = true（高精度小数），isTrace = false（关闭轨迹，性能优先）
            return runner.execute(expression, context, null, true, false);
        } catch (Exception e) {
            log.error("QLExpress evaluate error, expr={}", expression, e);
            throw new IllegalStateException("QLExpress evaluate failed: " + e.getMessage(), e);
        }
    }
}
