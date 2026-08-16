package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.spi.EventDefinitionRegistry;
import com.mkt.ruleengine.core.spi.UserProfileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 阶段一：事件归一化。
 * <ul>
 *   <li>校验事件已定义（严格模式）</li>
 *   <li>事件参数并入运行时属性</li>
 *   <li>解析用户画像（供条件 / 函数引用）</li>
 * </ul>
 */
public class EventNormalizeStage implements EngineStage {

    private static final Logger log = LoggerFactory.getLogger(EventNormalizeStage.class);

    private final EventDefinitionRegistry eventRegistry;
    private final UserProfileResolver userProfileResolver;
    private final boolean strictEventValidation;

    public EventNormalizeStage(EventDefinitionRegistry eventRegistry,
                               UserProfileResolver userProfileResolver,
                               boolean strictEventValidation) {
        this.eventRegistry = eventRegistry;
        this.userProfileResolver = userProfileResolver == null ? UserProfileResolver.NOOP : userProfileResolver;
        this.strictEventValidation = strictEventValidation;
    }

    @Override
    public void handle(EngineContext ctx, StageChain chain) {
        var event = ctx.getEvent();
        if (strictEventValidation && !eventRegistry.exists(event.getEventCode())) {
            ctx.markFailed("event not defined: " + event.getEventCode());
            chain.breakChain();
            return;
        }
        // 事件参数并入运行时属性（字段解析优先级最高层）
        event.getParams().forEach(ctx::putAttribute);
        // 用户画像
        if (event.getUserId() != null && !event.getUserId().isBlank()) {
            try {
                ctx.setUserProfile(userProfileResolver.resolve(event.getUserId()));
            } catch (Exception e) {
                log.warn("resolve user profile failed, userId={}", event.getUserId(), e);
            }
        }
        chain.proceed(ctx);
    }
}
