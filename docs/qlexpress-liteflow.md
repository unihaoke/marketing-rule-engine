# QLExpress / LiteFlow 可选扩展

两个扩展模块默认**不参与构建**（本仓库离线环境无对应依赖缓存），启用需在有网络的机器上执行：

```bash
# QLExpress 动态规则表达式
mvn -Pqlexpress clean package

# LiteFlow 流程编排
mvn -Pliteflow clean package
```

## 1 QLExpress（动态规则表达式）— `rule-engine-ext-qlexpress`

- 依赖：`com.alibaba:QLExpress:3.3.1`
- 适配：`QlExpressEvaluator implements ExpressionEvaluator`（适配器模式），
  `QlExpressConfiguration` 将其注册为 **@Primary** 的 `ExpressionEvaluator`。
- 生效范围（无需改任何业务代码）：
  - 条件树 `EXPRESSION` 操作符的表达式（原 SpEL → QLExpress）
  - `EXPRESSION` 类型脚本函数（在线编辑脚本支持函数定义、分支、循环、赋值语句）
  - 动作参数 `#{...}` 动态计算
- QLExpress 相对 SpEL 的优势：完整脚本语法、预编译执行（高并发）、别名/宏、与 Java 无缝互调。

示例（脚本函数）：
```
// 阶梯返利（QLExpress 脚本）
function rebate(amount){
  if(amount >= 500) return amount * 0.05;
  if(amount >= 100) return amount * 0.02;
  return 0;
}
return rebate(orderAmount);
```

## 2 LiteFlow（流程编排）— `rule-engine-ext-liteflow`

- 依赖：`com.yomahub:liteflow-spring-boot-starter:2.11.4`
- 设计：默认四阶段责任链（归一化 → 增强 → 匹配 → 动作）等价地表达为 LiteFlow EL：

```xml
<flow>
  <chain name="engineChain">
    THEN(normalizeStage, enhanceStage, matchStage, actionStage)
  </chain>
</flow>
```

- 实现：
  - `LiteFlowStageComponents`：四个 `NodeComponent`，从 LiteFlow 上下文取 `EngineContext`，复用默认阶段的逻辑（组合而非重写）。
  - `LiteFlowRuleEngine extends RuleEngine`：**模板方法骨架不变**，仅覆盖 `buildChain()` 钩子——把"责任链执行"替换为 `FlowExecutor.execute2Resp("engineChain", ctx, EngineContext.class)`。
  - `LiteFlowEngineConfiguration`：`rule-engine.liteflow.enabled=true` 时以 `@Primary` 替换默认引擎（默认引擎 Bean 带 `@ConditionalOnMissingBean` 守卫）。

启用配置（rule-engine-web 的 application.yml）：
```yaml
liteflow:
  rule-source: config/flow.el.xml   # 来自 rule-engine-ext-liteflow 模块资源
rule-engine:
  liteflow:
    enabled: true
```

- 收益：LiteFlow 的规则热刷新（DB/zk 等）、组件级可观测（耗时/降级）、复杂链路分支（IF/WHEN/SWITCH）扩展能力。

## 3 两者可叠加

QLExpress 管「表达式/脚本」，LiteFlow 管「流程编排」，互不冲突：
```bash
mvn -Pqlexpress -Pliteflow clean package
```

## 4 验证清单（联网环境）

```bash
mvn -Pqlexpress,iteflow install
cd rule-engine-web
mvn -Pqlexpress,iteflow spring-boot:run
curl -X POST http://localhost:8080/api/engine/trigger -H "Content-Type: application/json" -d '{"eventCode":"ORDER_CREATE","userId":"u1","channelId":"APP","params":{"orderId":"O1","orderAmount":800}}'   # 全链路仍应正常
```
