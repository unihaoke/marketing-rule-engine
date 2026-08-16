# 营销规则引擎 · 运营后台

通用营销规则引擎（Java/Spring Boot 后端）的前端管理界面。技术栈：**Vue 3 + Element Plus + Pinia + Vue Router + Axios + ECharts + Vite**。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 概览 | 引擎吞吐统计（数据库实时聚合）、数据统计报表（ECharts 图表：按事件/按动作/每日趋势）、最近执行日志明细（10 条） |
| 事件管理 | 事件定义 CRUD、入参 schema 动态行编辑、启停、**事件模拟触发**（按入参填值 → 真实请求引擎全链路 → 展示命中规则/规则追踪/动作明细/增强属性，日志落库可跳转查看） |
| 规则管理 | 规则组列表（展示最新版本号）、新建、上线/下线、发布、灰度入口、跳转编辑（分步向导） |
| 规则画布 | 分步向导（①基础信息&灰度 → ②条件与函数 → ③动作）；条件树拖拽编辑、前置函数绑定参数（大文本域）、动作"名称-值"参数（按前端展示开关过滤，仅填值）；函数/动作从左侧面板点击或拖拽添加；每步可校验，保存草稿 |
| 函数管理 | 函数注册（JAVA_SPI / JAR / EXPRESSION）、Jar 上传（multipart）、在线测试、启停 |
| 动作配置 | 动作定义 CRUD、参数 schema 行编辑（默认值 + **前端展示开关**，默认值自动派生 defaults）、启停 |
| 版本&灰度 | 版本列表、回溯（rollback）、灰度配置（gray） |
| 执行日志 | 执行明细分页（命中规则/执行动作/耗时/**增强属性**/错误）、引擎日志、动作日志 |

## 快速开始

```bash
npm install        # 含 echarts
npm run dev
```

默认访问 `http://localhost:5173`，`/api` 请求由 Vite 代理到 `http://localhost:8080`（后端 Spring Boot 服务）。

生产构建：

```bash
npm run build
```

## 目录结构

```
rule-engine-admin-ui/
├── package.json
├── vite.config.js          # /api → http://localhost:8080 代理
├── index.html
└── src/
    ├── main.js             # 应用入口：ElementPlus(中文语言包) + Pinia + Router + 全局图标
    ├── App.vue             # 深色侧边栏 + 顶栏 + 主内容区布局
    ├── assets/main.css     # 全局样式
    ├── router/index.js     # 路由表
    ├── api/index.js        # axios 实例 + {code,message,data} 响应解包
    ├── api/modules.js      # 按模块分组的全部 API 调用
    ├── stores/engine.js    # 引擎运行时 Pinia store（统计/报表/日志）
    ├── utils/index.js      # 分页归一化、JSON 解析、时间格式化等工具
    ├── utils/condition.js  # 条件树节点工厂 / 操作符 / 值归一化
    ├── components/         # 通用组件（条件树递归编辑器、参数行编辑器等）
    └── views/              # 页面视图
```

## 说明

- 后端不可用时页面仍可打开，接口失败仅通过 `ElMessage` 提示，不影响浏览。
- 规则画布条件树遵循后端契约：`nodeType: LOGIC|LEAF`，逻辑节点 `logic: AND|OR|NOT` 递归嵌套，叶子节点 `field + operator + value + valueType + not`，表达式叶子使用 `expression` 字段。
- 叶子操作符为 `EXPRESSION` 时隐藏 field/value 并显示表达式文本域；`IN / NOT_IN / BETWEEN` 时值以英文逗号分隔编辑、提交前自动转为数组。
