# API 契约（REST）

Base URL: `http://localhost:8080`，统一响应包装：

```json
{ "code": 0, "message": "ok", "data": { ... } }
```

## 1 事件管理 `/api/events`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/events` | 事件列表 |
| GET | `/api/events/{eventCode}` | 事件详情 |
| POST | `/api/events` | 创建事件（body: EventDefinition） |
| PUT | `/api/events/{eventCode}` | 更新事件 |
| DELETE | `/api/events/{eventCode}` | 删除事件 |
| POST | `/api/events/{eventCode}/enable?enabled=true` | 启用/停用 |

EventDefinition：
```json
{
  "eventCode": "AD_CLICK",
  "eventName": "广告点击",
  "description": "广告点击归因事件",
  "enabled": true,
  "params": [
    { "code": "adSlotId", "name": "广告位ID", "type": "STRING", "required": true, "description": "", "defaultValue": null },
    { "code": "orderAmount", "name": "订单金额", "type": "NUMBER", "required": false, "description": "", "defaultValue": null }
  ],
  "createdBy": "system"
}
```
`type` ∈ STRING | NUMBER | BOOLEAN | DATETIME | JSON | LIST

## 2 规则配置 `/api/rules`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/rules` | 规则列表 |
| GET | `/api/rules/{ruleCode}` | 规则详情（含条件树/函数/动作/灰度） |
| POST | `/api/rules` | 创建规则（含初始草稿版本 v1） |
| PUT | `/api/rules/{ruleCode}` | 更新画布草稿 |
| POST | `/api/rules/{ruleCode}/publish?changeLog=&operator=` | 发布为线上版本 |
| POST | `/api/rules/versions/{versionId}/rollback?changeLog=&operator=` | 版本回溯（重新发布） |
| POST | `/api/rules/{ruleCode}/gray` | 灰度配置（body: GrayConfig，即时生效） |
| POST | `/api/rules/{ruleCode}/online` | 上线 |
| POST | `/api/rules/{ruleCode}/offline` | 下线 |
| GET | `/api/rules/{ruleCode}/versions` | 版本列表 |
| GET | `/api/rules/versions/{versionId}` | 版本详情 |
| GET | `/api/rules/versions/{versionId}/content` | 版本内容（画布回显） |

RuleGroup：
```json
{
  "id": 1,
  "ruleCode": "SIGN_IN_STREAK_REWARD",
  "ruleName": "签到拉新-连续打卡返积分",
  "eventCode": "SIGN_IN",
  "description": "...",
  "priority": 30,
  "enabled": true,
  "conditionTree": {
    "nodeType": "LOGIC",
    "logic": "AND",
    "children": [
      { "nodeType": "LEAF", "field": "checkinStreak", "operator": "GTE", "value": 3, "valueType": "NUMBER", "not": false },
      { "nodeType": "LEAF", "field": "userTag", "operator": "IN", "value": ["NEW_USER", "ACTIVE"], "valueType": "STRING", "not": false },
      { "nodeType": "LEAF", "operator": "EXPRESSION", "expression": "orderAmount >= 100 && userId != null", "valueType": "STRING" }
    ]
  },
  "functions": [
    { "functionName": "consecutiveCheckinDays", "alias": "checkinStreak", "bindings": {} }
  ],
  "actions": [
    { "actionCode": "ADD_POINTS", "async": false, "params": { "points": "#{checkinStreak * 10}", "reason": "连续打卡奖励" } }
  ],
  "gray": { "enabled": false, "strategy": "OFF", "percent": 0, "channels": [], "bucketKey": "userId" }
}
```

节点类型：
- `nodeType: "LOGIC"` → `logic` ∈ AND | OR | NOT，`children` 递归
- `nodeType: "LEAF"` → `field` + `operator` + `value` + `valueType`
- 操作符：EQUALS / NOT_EQUALS / GT / GTE / LT / LTE / IN / NOT_IN / BETWEEN / CONTAINS / STARTS_WITH / EXISTS / NOT_EXISTS / EXPRESSION
- EXPRESSION 叶子：`expression` 字段写表达式（默认 SpEL，可切 QLExpress）

GrayConfig：`{ "enabled": bool, "strategy": "OFF|PERCENT|CHANNEL|PERCENT_AND_CHANNEL", "percent": 0-100, "channels": [], "bucketKey": "userId" }`

## 3 自定义函数 `/api/functions`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/functions` | 函数列表 |
| GET | `/api/functions/{functionName}` | 函数详情 |
| POST | `/api/functions` | 注册（body: FunctionDefinition） |
| PUT | `/api/functions/{functionName}` | 更新（热更新） |
| DELETE | `/api/functions/{functionName}` | 删除 |
| POST | `/api/functions/{functionName}/enable?enabled=` | 启用/停用 |
| POST | `/api/functions/{functionName}/test` | 在线测试：`{ "eventParams": {...}, "bindings": {...} }` |
| POST | `/api/functions/upload-jar` | multipart：`file` + `functionName` + `className` (+`displayName`/`description`) |
| GET | `/api/functions/executor-types` | 动作执行器类型列表 |

FunctionDefinition：
```json
{
  "functionName": "rebateCalculator",
  "displayName": "阶梯返利核算",
  "type": "JAVA_SPI",
  "description": "按阶梯档位核算返利金额：读取事件参数 amountField（默认 orderAmount），按绑定参数 tiers 档位计算返利",
  "output": "返回返利金额（数字，保留 2 位小数）",
  "outputName": "rebateAmount",
  "className": "rebateCalculatorFunction",
  "script": "orderCount * 10 + checkinStreak * 5",
  "jarPath": "./data/functions/xxx.jar",
  "params": [
    { "code": "amountField", "name": "金额字段", "type": "STRING", "required": false, "description": "", "editable": false },
    {
      "code": "tiers",
      "name": "阶梯档位",
      "type": "LIST_OBJECT",
      "required": true,
      "description": "按格式新增档位行",
      "editable": true,
      "itemSchema": [
        { "code": "min", "name": "最低金额", "type": "NUMBER", "required": true, "description": "" },
        { "code": "max", "name": "最高金额", "type": "NUMBER", "required": false, "description": "留空表示上不封顶" },
        { "code": "rate", "name": "返利比例", "type": "NUMBER", "required": true, "description": "0.05=5%" }
      ]
    }
  ],
  "config": {},
  "testCases": [
    {
      "name": "低档返利 2%",
      "eventParams": { "orderAmount": 300 },
      "bindings": { "tiers": [{ "min": 100, "max": 499, "rate": 0.02 }] },
      "expect": "返回 6.00（300×2%）"
    }
  ],
  "enabled": true
}
```
`type` ∈ JAVA_SPI | JAR | EXPRESSION
- `output`：出参说明（函数结果的含义/类型，规则画布中展示给运营）
- `outputName`：默认出参名（规则画布中的**固定别名，不可修改**，缺省=函数名；条件/动作以 `#{outputName}` 引用）
- `params[]`：绑定参数 schema，`type` ∈ STRING | NUMBER | BOOLEAN | DATETIME | LIST | USER（用户 ID） | LIST_OBJECT（对象数组） | JSON；
  `LIST_OBJECT` 需用 `itemSchema` 声明元素子字段（画布中按格式新增/填写，如阶梯档位 [{key,value}]）；
  `editable=false` 的参数不在规则画布中展示赋值（由函数内部/默认值决定，加载旧规则时保留原值）
- `testCases`：在线测试案例数组（函数管理弹窗一键填入入参），`{ name, eventParams, bindings, expect }`；注册/更新时随 body 一并提交，存储于 `t_function_definition.test_cases_json`

## 4 动作配置 `/api/actions`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/actions` | 动作列表 |
| GET | `/api/actions/{actionCode}` | 动作详情 |
| POST | `/api/actions` | 创建 |
| PUT | `/api/actions/{actionCode}` | 更新 |
| DELETE | `/api/actions/{actionCode}` | 删除 |
| POST | `/api/actions/{actionCode}/enable?enabled=` | 启用/停用 |

ActionDefinition：
```json
{
  "actionCode": "ISSUE_COUPON",
  "actionName": "发放优惠券",
  "actionType": "COUPON",
  "description": "发放优惠券（券中心）",
  "params": [
    { "code": "couponTemplateId", "name": "券模板ID", "type": "STRING", "required": true, "defaultValue": null, "description": "", "frontDisplay": true }
  ],
  "defaults": { "count": 1, "expireDays": 30 },
  "enabled": true
}
```
- `params[].frontDisplay`：是否在规则画布动作参数中展示给运营填写（默认 `true`；`false` 隐藏，仅用默认值/内部传参）
- `defaults` 由参数定义中的 `defaultValue` **自动派生**（保存动作时后端生成），无需单独维护
- 规则画布中直接添加的动作编码若未在动作配置中定义，保存画布时会**自动注册**为基础动作定义

## 5 引擎运行时 `/api/engine`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/engine/trigger` | 单事件触发 |
| POST | `/api/engine/trigger-batch` | 批量触发 |
| **POST** | **`/api/engine/simulate`** | **事件模拟触发（事件管理页测试）：返回执行结果 + 规则评估追踪明细（命中/灰度跳过/条件失败 + 动作解析参数），动作强制同步执行** |
| GET | `/api/engine/stats` | 吞吐统计总览（实时从数据库聚合：总事件/命中/成功/失败/动作数/平均耗时） |
| GET | `/api/engine/stats/by-event` | 按事件统计报表（触发量/命中量/成功量/平均耗时） |
| GET | `/api/engine/stats/by-action` | 按动作统计报表（执行量/成功量/平均耗时） |
| GET | `/api/engine/stats/by-day?days=7` | 最近 N 天每日触发趋势 |
| GET | `/api/engine/logs?page=&size=&eventCode=&userId=` | 执行日志分页 |
| GET | `/api/engine/logs/detail?page=&size=&eventCode=&userId=` | 执行明细分页（日志 + 关联动作 + 解析属性：命中规则 / 执行动作 / 耗时 / 属性） |
| GET | `/api/engine/action-logs?page=&size=&eventId=&ruleCode=&actionCode=` | 动作记录分页 |

触发请求：
```json
{
  "eventId": "可选，缺省自动生成（幂等键）",
  "eventCode": "AD_CLICK",
  "userId": "u1001",
  "channelId": "AD-ZHITONG",
  "eventTime": 1710000000000,
  "traceId": "可选",
  "params": { "adSlotId": "SLOT-A", "campaignId": "CAMP-001", "region": "BEIJING" }
}
```

EngineResult：
```json
{
  "eventId": "...",
  "eventCode": "AD_CLICK",
  "userId": "u1001",
  "channelId": "AD-ZHITONG",
  "traceId": "...",
  "startedAt": 1710000000000,
  "finishedAt": 1710000000012,
  "costMs": 12,
  "success": true,
  "errorMessage": null,
  "matchedRuleCodes": ["AD_ATTRIBUTION_NEW_USER"],
  "actionRecords": [
    { "ruleCode": "AD_ATTRIBUTION_NEW_USER", "actionCode": "ISSUE_COUPON", "success": true, "detail": "coupon issued: template=CT-AD-NEW, count=1", "costMs": 1 }
  ],
  "attributes": { "adSlotId": "SLOT-A", "campaignId": "CAMP-001", "region": "BEIJING", "userId": "u1001", "channelId": "AD-ZHITONG", "eventCode": "AD_CLICK", "eventTime": 1710000000000 }
}
```

模拟触发响应（`POST /api/engine/simulate`，请求体同 trigger）：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "result": { ...同 EngineResult... },
    "rules": [
      {
        "ruleCode": "SIGN_IN_STREAK_REWARD",
        "ruleName": "签到拉新-连续打卡返积分",
        "versionNo": 2,
        "matched": true,
        "skipReason": null,
        "costMs": 1,
        "actions": [
          { "actionCode": "ADD_POINTS", "success": true, "detail": "points added: 30", "params": { "points": 30, "reason": "连续打卡奖励" }, "costMs": 0 }
        ]
      },
      { "ruleCode": "AD_ATTRIBUTION_NEW_USER", "ruleName": "广告归因-新客送券", "versionNo": 2, "matched": false, "skipReason": "GRAY_SKIP", "costMs": 0, "actions": [] }
    ]
  }
}
```
- `rules` 为事件绑定的每条规则的评估结论：`matched=true` 命中；`skipReason` ∈ `GRAY_SKIP`（灰度未放行）/ `CONDITION_FAIL`（条件不满足）/ `EVALUATE_ERROR`（求值异常）
- `actions[].params` 为解析后的动作参数（`${}`/`#{}` 表达式已求值），可直接核对动作将发送的内容
- 模拟模式下动作强制同步执行，响应即可看到完整动作结果

执行明细响应（`GET /api/engine/logs/detail`）：```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "total": 6,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 6,
        "eventId": "...",
        "eventCode": "ORDER_CREATE",
        "userId": "u1001",
        "channelId": "APP",
        "traceId": "...",
        "success": true,
        "errorMessage": null,
        "matchedRuleCodes": ["ORDER_REBATE_COUPON"],
        "actions": [
          { "id": 1, "idempotencyKey": "...", "eventId": "...", "ruleCode": "ORDER_REBATE_COUPON", "actionCode": "ISSUE_COUPON", "success": true, "detail": "coupon issued: template=CT-ORDER-REBATE, count=2", "paramsJson": "{...}", "costMs": 1, "createdAt": "..." }
        ],
        "attributes": { "orderAmount": 800, "rebateAmount": 40.00, "orderId": "..." },
        "costMs": 41,
        "createdAt": "..."
      }
    ]
  }
}
```
- `matchedRuleCodes` 为命中规则列表；`actions` 为该事件关联的动作执行记录（含解析后参数与结果）；`attributes` 为运行时属性（函数增强输出等）

统计响应（`GET /api/engine/stats`，实时数据库聚合）：
```json
{ "code": 0, "message": "ok", "data": { "totalEvents": 6, "matchedEvents": 3, "successEvents": 6, "failedEvents": 0, "executedActions": 8, "avgCostMs": 29.4 } }
```
