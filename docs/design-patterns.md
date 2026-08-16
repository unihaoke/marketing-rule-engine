# 设计模式落地

| 模式 | 落地点 | 说明 |
|---|---|---|
| **模板方法** | `RuleEngine`（抽象基类）+ `DefaultRuleEngine` | 算法骨架固定：校验 → 加载快照 → `buildChain()` 装配责任链 → 执行 → 结果汇总；子类只实现钩子（快照来源 / 链装配 / 收尾日志）。LiteFlow 扩展即提供另一种 `buildChain` 实现 |
| **责任链** | `EngineStage` + `StageChain` + 四阶段 | 事件归一化 → 函数增强 → 规则匹配 → 动作执行，支持 `breakChain()` 提前中断；阶段可插拔、可排序（`EngineStageOrder`） |
| **工厂方法** | `EngineStagesFactory.createDefaultStages` / `EnginePipeline.of` | 阶段装配器集中创建有序责任链；`GrayStrategyFactory` 按类型返回灰度策略 |
| **工厂** | `ActionExecutorFactory` | 动作执行器注册表，按 actionType 获取，支持运行时动态注册（热更新） |
| **策略** | `ConditionEvaluator` + `OperatorEvaluator` 策略表、`GrayStrategy`（Percent/Channel/组合）、`ActionExecutor`（发券/短信/积分/推送/审计） | 操作符、灰度、动作均为可替换策略 |
| **组合** | `ConditionNode`（`LogicConditionNode` / `LeafConditionNode`） | 条件树递归求值，AND/OR/NOT 任意嵌套；叶子 = 基础条件（用户标签、时间区间、行为次数、金额等） |
| **适配器/SPI** | `ExpressionEvaluator`（SpEL 默认，QLExpress 可选）、`JsonCodec`（Jackson）、`UserProfileResolver`、`IdempotencyStore`、`ActionDispatchExecutor` | 核心面向接口编程，替换实现不改领域逻辑 |
| **观察者（轮询）** | `RuleCacheVersionWatcher` | 配置版本号变更 → 缓存刷新（热更新） |
| **代理/门面** | `DefaultFunctionRegistry`（Bean 自动注册 + 按定义加载） | 统一函数注册入口，隐藏三种加载器（Bean/Jar/脚本） |

## 核心类关系速览

```
RuleEngine(模板)
 ├─ loadSnapshots()          → RuleSnapshotCache（本地缓存，热更新）
 └─ buildChain()             → StageChain（责任链）
      ├─ EventNormalizeStage ── EventDefinitionRegistry / UserProfileResolver
      ├─ FunctionEnhanceStage ─ FunctionRegistry（Bean/Jar/EXPRESSION 加载器）
      ├─ RuleMatchStage        ─ ConditionEvaluator（组合递归 + 操作符策略）+ GrayStrategyFactory
      └─ ActionExecuteStage    ─ ActionDefinitionRegistry + ActionParamResolver
                                + IdempotencyStore + ActionDispatchExecutor（异步）
                                + ActionExecutorFactory（策略）
```

## 为什么这样组合

1. **模板方法锁定主链路、责任链解耦阶段**：新增阶段（如风控、频控）只需实现 `EngineStage` 并加入装配器，主链路零改动；切换 LiteFlow 编排只换 `buildChain`。
2. **策略模式天然适配"条件/动作/灰度"三类多变点**：运营新增条件操作符、动作类型、灰度方式都不需要改引擎。
3. **组合模式表达规则画布**：与前端拖拽树（AND/OR/NOT 嵌套）一一对应，JSON 序列化结构即存储结构，配置即代码。
4. **SPI 隔离业务**：用户画像、动作通道、表达式引擎都是 SPI，框架通用、业务自适配（广告归因 / 签到拉新 / 下单返券 / 分层推送 / 用户触达只是配置与实现插件的差异）。
