package com.mkt.ruleengine.core.action;

import com.mkt.ruleengine.core.exception.RuleEngineException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动作执行器工厂（工厂模式）：按动作类型返回执行器，支持动态注册（热更新）。
 */
public class ActionExecutorFactory {

    private final Map<String, ActionExecutor> executors = new ConcurrentHashMap<>();

    public ActionExecutorFactory(List<ActionExecutor> initialExecutors) {
        if (initialExecutors != null) {
            initialExecutors.forEach(this::register);
        }
    }

    public void register(ActionExecutor executor) {
        if (executor == null || executor.actionType() == null) {
            throw new RuleEngineException("executor with null actionType cannot be registered");
        }
        executors.put(executor.actionType(), executor);
    }

    public void unregister(String actionType) {
        executors.remove(actionType);
    }

    public ActionExecutor get(String actionType) {
        ActionExecutor executor = executors.get(actionType);
        if (executor == null) {
            throw new RuleEngineException("no ActionExecutor for actionType: " + actionType);
        }
        return executor;
    }

    public boolean contains(String actionType) {
        return executors.containsKey(actionType);
    }

    public Map<String, ActionExecutor> all() {
        return Map.copyOf(executors);
    }
}
