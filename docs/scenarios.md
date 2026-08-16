# 业务场景适配

框架不绑定具体业务，场景差异全部落在「事件定义 + 条件 + 函数 + 动作 + 灰度」的配置与 SPI 实现上。
以下为种子数据内置的五类主流营销场景及扩展指引。

## 1 广告归因

- 事件：`AD_CLICK`（adSlotId / campaignId / advertiserId / region / deviceType）
- 条件：渠道白名单 + 用户标签（新客/老客）→ 命中不同投放策略
- 动作：发券（新客）、积分+短信召回（老客）
- 扩展：真实归因可把「点击后是否转化」作为函数增强（SPI `MarketingFunction`），或将归因结果写入事件参数

```
规则 AD_ATTRIBUTION_NEW_USER（优先级 10）
  条件: userTag CONTAINS 'NEW_USER' AND channelId = 'AD-ZHITONG'
  动作: ISSUE_COUPON {couponTemplateId: CT-AD-NEW}
```

## 2 签到拉新

- 事件：`SIGN_IN`
- 函数：
  - `signInDays`（签到天数计算，**基于 t_engine_log 真实签到历史按天去重**；`mode=streak` 连续 / `mode=total` 累计，本次当日首次签到计入）
  - `todaySignedIn`（今日是否已签到，查历史记录不含本次，用于"每日限发一次"）
  - `consecutiveCheckinDays`（连续打卡天数计算，画像/事件/绑定参数取数，演示用）
- 条件：签到天数区间 + 今日未签到
- 动作：积分（数量引用阶梯奖励函数结果）

### 2.1 每日签到-固定积分（基于真实日志，推荐）

种子规则 `SIGN_IN_DAILY_FIXED`（优先级 31，已发布）：今日首次签到发 10 分，同日重复签到不再命中。

```
函数: todaySignedIn → alias=todaySigned
条件: todaySigned = false
动作: ADD_POINTS {points: 10, reason: 每日签到固定积分}
```

### 2.2 阶段积分（第 N 天奖励翻倍，最多 5 天）

种子规则 `SIGN_IN_STAGE_REWARD`（优先级 32，已发布）：累计签到 **第1天1分、第2天2分、第3天4分、第4天8分、第5天16分**，
累计满 5 天后该用户不再命中（条件限定 `signInTotal` 1..5）。签到天数由 `signInDays(mode=total)` 从 `t_engine_log` 实时计算。

```
函数: signInDays → alias=signInTotal   bindings: {mode: total, eventCode: SIGN_IN}
      tieredRewardCalculator → alias=rewardPoints
      bindings: {keyField: signInTotal, tiers: [{key:1,value:1},{key:2,value:2},{key:3,value:4},{key:4,value:8},{key:5,value:16}]}
      todaySignedIn → alias=todaySigned
条件: signInTotal >= 1 AND signInTotal <= 5 AND todaySigned = false
动作: ADD_POINTS {points: "#{rewardPoints}", reason: 阶段签到奖励}
```

> 说明：签到计数基于 `t_engine_log` 中该用户 `SIGN_IN` 事件按日期去重（`DATE(created_at)`），
> 因此**多次触发不重复计数**，且按真实日志可回溯任意用户的连续/累计天数。
> 旧规则 `SIGN_IN_STREAK_REWARD`（基于画像 `consecutiveCheckinDays`）在新库默认停用（enabled=0），
> 老库如需切换请在画布停用旧规则，避免同日叠加发放。

### 2.3 阶梯式奖励函数 `tieredRewardCalculator`

框架通过**内置阶梯核算函数 `tieredRewardCalculator`** 支持任意阶梯档位（奖励/返利/折扣等）：

| 配置 | 说明 |
|---|---|
| `keyField` | 档位匹配字段（默认 `checkinStreak`，取增强属性/事件参数/绑定参数） |
| `tiers` | 精确档位 `[{key,value}]`：`[{key:1,value:1},{key:2,value:2},{key:3,value:4}]` |
| `tiers` | 区间档位 `[{from,to,value}]`：`[{from:1,to:2,value:1},{from:3,to:null,value:4}]`（to 空=上不封顶） |
| `fallback` | 无匹配兜底值（默认 0） |

配置步骤（画布）：
1. 步骤②添加前置函数 `tieredRewardCalculator`，别名如 `rewardPoints`，绑定参数填上述 `tiers`
2. 动作参数引用 `#{rewardPoints}`（或 `#{rewardPoints * 100}` 放大）

同类函数：`rebateCalculator`（按金额区间 `[{min,max,rate}]` 算阶梯返利）。

## 3 下单返券

- 事件：`ORDER_CREATE`（orderAmount）
- 函数：`rebateCalculator`（阶梯返利核算，绑定参数 `tiers` 定义档位 `[{min,max,rate}]`）
- 条件：金额门槛 + 返利>0
- 动作：券数量按返利额度表达式计算、审计日志

```
函数: rebateCalculator → alias=rebateAmount
     bindings: {amountField: orderAmount, tiers: [{min:100,max:499,rate:0.02},{min:500,rate:0.05}]}
条件: orderAmount >= 100 AND rebateAmount > 0
动作: ISSUE_COUPON {count: "#{rebateAmount >= 10 ? 2 : 1}"}
```

## 4 活动分层推送

- 事件：`APP_LAUNCH`
- 条件：LTV 分层（画像 ltvTier）或高频用户（orderCount）→ 高价值用户专享推送
- 灰度：`PERCENT` 按 userId 分桶（种子默认 30%），渠道白名单可组合 `PERCENT_AND_CHANNEL`

```
条件: ltvTier = 'A' OR orderCount >= 10
动作: SEND_PUSH {pushTemplateId: PT-VIP-ACT}
灰度: enabled=true, strategy=PERCENT, percent=30, bucketKey=userId
```

## 5 用户触达（流失预警）

- 事件：`USER_RETENTION`（retentionDay / lastActiveDays）
- 条件：留存天数阈值 + 排除新客
- 动作：短信 + 推送组合触达

## 扩展新场景的步骤

1. **事件管理**新增事件与入参字段（如 `PROMO_VIEW`）
2. **函数注册**：业务函数 → JAVA_SPI（实现 `MarketingFunction` 的 Spring Bean）或上传 Jar / 在线脚本
3. **动作配置**：如对接企业微信 → 实现 `ActionExecutor`（actionType=WECHAT）并注册为 Bean，画布动作类型下拉自动出现
4. **规则画布**拖拽配置条件树、绑定函数与动作，灰度小流量 → 发布上线
5. 触发：`POST /api/engine/trigger` 或接入 MQ 消费后调用 `EngineAppService.trigger`

## 与业务系统的对接点（全部 SPI）

| 对接 | SPI | 默认实现 |
|---|---|---|
| 用户标签/行为/画像 | `UserProfileResolver` | 演示画像（确定性哈希） |
| 发券/短信/推送/积分通道 | `ActionExecutor` | Mock 记录实现 |
| 表达式/脚本引擎 | `ExpressionEvaluator` | SpEL（可选 QLExpress） |
| 幂等存储 | `IdempotencyStore` | JVM 内存（可换 Redis） |
| 动作异步分发 | `ActionDispatchExecutor` | 内置线程池（可换 MQ） |
| 规则快照缓存 | `RuleSnapshotCache` | 内存 + DB 轮询热更新 |
