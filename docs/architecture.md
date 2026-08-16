# 架构设计

## 1 总体架构

```
┌─────────────────────────────── 运营端（Vue3 管理台） ───────────────────────────────┐
│  概览(统计报表) │ 事件管理(含模拟触发) │ 规则画布(分步向导) │ 函数注册 │ 动作配置      │
│  版本&灰度 │ 执行日志(明细/引擎/动作)                                                │
└──────────────────────────────────────────┬──────────────────────────────────────────┘
                                           │ REST
┌────────────────────────── interfaces 接口层（rule-engine-web） ──────────────────────┐
│  EventController / RuleController / FunctionController / ActionController            │
│  EngineController（trigger/simulate/统计/日志） + ApiResponse / 全局异常             │
└──────────────────────────────────────────┬──────────────────────────────────────────┘
┌────────────────────────── application 应用层（rule-engine-application） ─────────────┐
│  EventAppService / RuleConfigAppService / FunctionAppService / ActionAppService      │
│  EngineAppService（触发入口 + 吞吐统计）                                               │
└──────────────────────────────────────────┬──────────────────────────────────────────┘
┌────────────────────────── infrastructure 基础设施层（rule-engine-infrastructure） ────┐
│  持久化：MyBatis-Plus（PO / Mapper / Repository 实现）                                 │
│  表达式：SpEL（默认）/ QLExpress（可选扩展） │ 函数加载：Bean / Jar(URLClassLoader) / 脚本│
│  缓存热更新：RuleSnapshotCache + 版本轮询 Watcher + 事件/动作定义注册表                  │
│  执行器：发券 / 短信 / 积分 / 推送 / 审计（演示） │ 日志落库：EngineLog / ActionLog      │
└──────────────────────────────────────────┬──────────────────────────────────────────┘
┌────────────────────────── domain 领域层（rule-engine-core，纯 Java） ─────────────────┐
│  引擎：RuleEngine(模板方法) + StageChain(责任链) + EngineContext                       │
│  模型：事件 / 条件树(组合) / 规则组&版本 / 函数 / 动作 / 灰度                           │
│  SPI：条件求值 / 表达式 / 函数注册 / 动作执行器 / 快照缓存 / 用户画像 / 幂等 / 日志      │
│  仓储接口：Event / RuleConfig / Function / Action                                     │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

## 2 核心链路（模板方法 + 责任链）

```
事件触发 MarketingEvent
   │
   ▼
RuleEngine.execute()  ← 模板方法：校验 → 加载快照 → 装配责任链 → 执行 → 结果汇总
   │
   ▼  StageChain（责任链，支持中断）
┌──────────────────────────────────────────────────────────────────────┐
│ ① EventNormalizeStage  事件归一化：定义校验 / 参数并入属性 / 用户画像     │
│ ② FunctionEnhanceStage 函数增强：执行规则声明的前置函数 → attributes      │
│ ③ RuleMatchStage       规则匹配：灰度放行判定 → 条件树递归求值            │
│ ④ ActionExecuteStage   动作执行：动作解析 → 参数解析 → 幂等 → 异步分发    │
└──────────────────────────────────────────────────────────────────────┘
   │
   ▼
EngineResult（命中规则 / 动作记录 / 增强属性 / 耗时）→ EngineLog / ActionLog 落库
```

- **匹配前增强**：条件可以引用函数计算结果（如 `checkinStreak >= 3`），因此函数增强阶段在规则匹配之前执行；函数在同一链路内按别名去重只执行一次。
- **动作参数可编程**：`${field}` 引用增强属性/事件参数/画像，`#{expr}` 执行表达式（如 `#{rebateAmount >= 10 ? 2 : 1}`）。
- **幂等**：`eventId:ruleCode:actionCode` 幂等键，内存 + DB 唯一索引双保险，重复投递不重复执行。
- **异步化**：动作默认异步分发（线程池，队列满降级同步背压），事件主链路只做内存匹配，保证高并发吞吐。

## 3 高并发事件吞吐设计

| 手段 | 实现 |
|---|---|
| 本地缓存 | 规则快照 / 事件定义 / 动作定义全量内存缓存，运行时零触库 |
| 纯内存匹配 | 条件树递归求值 + 灰度哈希分桶，单事件微秒~毫秒级 |
| 动作异步化 | 内置线程池分发，`async=false` 可切同步（对账场景） |
| 幂等去重 | 内存幂等表（TTL）+ `t_action_log.idempotency_key` 唯一索引 |
| 日志异步 | EngineLog 落库在链路收尾，ActionLog 由动作回调写入 |
| 可扩展 | `ActionDispatchExecutor` SPI 可替换为 MQ（Kafka/RocketMQ）异步消费 |

## 4 热更新

- **主动刷新**：配置变更（发布/灰度/上下线/函数注册）后应用服务立即 `snapshotCache.refresh(...)` / `registry.refresh()`。
- **轮询兜底**：`RuleCacheVersionWatcher` 周期比对 DB 全局版本号（各表 MAX(updated_at) 组合哈希），变更即全量刷新缓存并重载函数，多实例部署下同样生效。
- **函数热更新**：Jar 函数按 `jarPath+className+version` 缓存 ClassLoader，新版本自动新建；脚本函数每次注册即重载。

## 5 版本与灰度

```
编辑(草稿 DRAFT) ──publish──▶ 线上(PUBLISHED) ──offline──▶ 下线(OFFLINE)
     ▲                            │
     └────────rollback────────────┘   （历史版本重新发布为新版本号）
```

- 版本号规则内递增，内容为 `RuleContent` JSON 快照（条件树 + 函数 + 动作 + 灰度）。
- 灰度策略（策略模式）：`PERCENT` 一致性哈希分桶（同用户同规则永远同桶）/ `CHANNEL` 渠道白名单 / `PERCENT_AND_CHANNEL` 组合；灰度修改对线上版本即时生效。
- 上下线：`t_rule_group.enabled` 开关 + 快照缓存刷新，秒级生效。

## 6 DDD 分层与依赖方向

```
rule-engine-core（领域，无框架依赖）
      ▲
      │
rule-engine-infrastructure（基础设施：实现仓储/缓存/加载器/执行器）
      ▲
      │
rule-engine-application（应用服务：编排用例）
      ▲
      │
rule-engine-web（接口层：REST + 启动 + 种子数据）

rule-engine-ext-qlexpress / rule-engine-ext-liteflow（可选扩展，profile 门控）
```

- 领域层只依赖 `slf4j-api`，定义仓储接口与全部 SPI；基础设施实现它们并注入 Spring 容器。
- 条件树多态序列化通过 Jackson Mixin 注入，领域对象保持零注解。
