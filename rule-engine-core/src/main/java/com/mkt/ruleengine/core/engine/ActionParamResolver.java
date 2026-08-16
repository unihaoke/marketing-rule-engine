package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.rule.EvaluationContext;
import com.mkt.ruleengine.core.rule.RuleAction;
import com.mkt.ruleengine.core.spi.ExpressionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动作参数解析器：支持三类取值方式
 * <ul>
 *   <li>字面量：直接值</li>
 *   <li>属性引用：{@code ${field}} —— 引用增强属性 / 事件参数 / 用户画像字段</li>
 *   <li>表达式：{@code #{expr}} —— SpEL / QLExpress 动态计算（如 #{rebateAmount * 100}）</li>
 * </ul>
 */
public class ActionParamResolver {

    private static final Logger log = LoggerFactory.getLogger(ActionParamResolver.class);

    private static final Pattern REF_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern EXPR_PATTERN = Pattern.compile("^#\\{(.+)}$");

    private final ExpressionEvaluator expressionEvaluator;

    public ActionParamResolver(ExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }

    /**
     * 解析动作参数。
     *
     * @param action 规则动作配置
     * @param ctx    求值上下文（属性/事件/画像）
     * @return 解析后的参数 Map
     */
    public Map<String, Object> resolve(RuleAction action, EvaluationContext ctx) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        action.getParams().forEach((key, value) -> resolved.put(key, resolveValue(value, ctx)));
        return resolved;
    }

    public Object resolveValue(Object value, EvaluationContext ctx) {
        if (value instanceof String s) {
            // 整串表达式 #{...}
            Matcher exprMatcher = EXPR_PATTERN.matcher(s.trim());
            if (exprMatcher.matches()) {
                return expressionEvaluator.evaluate(exprMatcher.group(1), ctx.expressionVariables());
            }
            // 内含 ${...} 引用则替换
            if (s.contains("${")) {
                StringBuffer sb = new StringBuffer();
                Matcher refMatcher = REF_PATTERN.matcher(s);
                while (refMatcher.find()) {
                    Object v = ctx.resolveField(refMatcher.group(1));
                    refMatcher.appendReplacement(sb, Matcher.quoteReplacement(v == null ? "" : String.valueOf(v)));
                }
                refMatcher.appendTail(sb);
                return sb.toString();
            }
            return s;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            map.forEach((k, v) -> nested.put(String.valueOf(k), resolveValue(v, ctx)));
            return nested;
        }
        if (value instanceof Iterable<?> iterable) {
            java.util.List<Object> nested = new java.util.ArrayList<>();
            for (Object item : iterable) {
                nested.add(resolveValue(item, ctx));
            }
            return nested;
        }
        return value;
    }
}
