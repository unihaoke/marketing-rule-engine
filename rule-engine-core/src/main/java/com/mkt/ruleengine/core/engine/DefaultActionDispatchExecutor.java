package com.mkt.ruleengine.core.engine;

import com.mkt.ruleengine.core.action.ActionExecutionContext;
import com.mkt.ruleengine.core.action.ActionExecutor;
import com.mkt.ruleengine.core.action.ActionResult;
import com.mkt.ruleengine.core.event.MarketingEvent;
import com.mkt.ruleengine.core.rule.RuleAction;
import com.mkt.ruleengine.core.spi.ActionDispatchExecutor;
import com.mkt.ruleengine.core.spi.IdempotencyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 默认动作分发执行器：同步直连 / 异步线程池（框架内置，可被业务侧 Bean 替换）。
 */
public class DefaultActionDispatchExecutor implements ActionDispatchExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultActionDispatchExecutor.class);

    private final ExecutorService asyncPool;
    private final List<Runnable> rejectedTasks = new CopyOnWriteArrayList<>();

    public DefaultActionDispatchExecutor() {
        this(64, 256, 10_000);
    }

    public DefaultActionDispatchExecutor(int core, int max, int queueSize) {
        this.asyncPool = new ThreadPoolExecutor(
                core, max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                new ThreadFactory() {
                    private final AtomicInteger seq = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "rule-engine-action-" + seq.getAndIncrement());
                        t.setDaemon(true);
                        return t;
                    }
                },
                (r, executor) -> {
                    // 队列满：降级为同步执行，保证动作不丢失（背压策略）
                    log.warn("action async queue full, fallback to sync execution");
                    r.run();
                });
    }

    @Override
    public void dispatch(boolean async, ActionExecutionContext ctx, ActionExecutor executor,
                         Consumer<ActionResult> onResult) {
        Runnable task = () -> {
            long start = System.currentTimeMillis();
            try {
                ActionResult result = executor.execute(ctx);
                if (onResult != null) {
                    onResult.accept(result);
                }
            } catch (Exception e) {
                log.error("action execute error: {}", ctx.idempotencyKey(), e);
                if (onResult != null) {
                    onResult.accept(ActionResult.fail(ctx.getAction().getActionCode(),
                            "exception: " + e.getMessage(), System.currentTimeMillis() - start));
                }
            }
        };
        if (async) {
            asyncPool.execute(task);
        } else {
            task.run();
        }
    }

    /** 当前异步队列积压数 */
    public int queuedCount() {
        return ((ThreadPoolExecutor) asyncPool).getQueue().size();
    }

    /** 被拒绝降级为同步执行的次数（仅供监控） */
    public int rejectedCount() {
        return rejectedTasks.size();
    }
}
