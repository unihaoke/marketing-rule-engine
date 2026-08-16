-- ============================================================
-- 营销规则引擎 MySQL 一键初始化脚本
-- 用法：mysql -uroot -p < sql/mysql-init.sql
-- 内容：DROP/创建 rule_engine 库 + 建表(schema.sql) + 初始化数据(data.sql)
-- 注意：会删除并重建 rule_engine 库（幂等脚本见 schema.sql / data.sql，应用启动时自动执行）
-- ============================================================

SET NAMES utf8mb4;

DROP DATABASE IF EXISTS rule_engine;
CREATE DATABASE rule_engine DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE rule_engine;

CREATE TABLE IF NOT EXISTS t_event_definition (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_code   VARCHAR(64)  NOT NULL UNIQUE,
    event_name   VARCHAR(128) NOT NULL,
    description  VARCHAR(512),
    enabled      TINYINT(1) DEFAULT 1,
    params_json  TEXT,
    created_by   VARCHAR(64),
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_rule_group (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code    VARCHAR(64)  NOT NULL UNIQUE,
    rule_name    VARCHAR(128) NOT NULL,
    event_code   VARCHAR(64)  NOT NULL,
    description  VARCHAR(512),
    priority     INT DEFAULT 100,
    enabled      TINYINT(1) DEFAULT 0,
    content_json TEXT,
    created_by   VARCHAR(64),
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_group_event (event_code)
);

CREATE TABLE IF NOT EXISTS t_rule_version (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code    VARCHAR(64) NOT NULL,
    version_no   BIGINT      NOT NULL,
    status       VARCHAR(16) NOT NULL,
    content_json TEXT,
    change_log   VARCHAR(512),
    published_by VARCHAR(64),
    published_at DATETIME,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rule_version_code_no (rule_code, version_no),
    INDEX idx_rule_version_code (rule_code)
);

CREATE TABLE IF NOT EXISTS t_function_definition (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    function_name VARCHAR(64)  NOT NULL UNIQUE,
    display_name  VARCHAR(128),
    type          VARCHAR(16)  NOT NULL,
    description   VARCHAR(512),
    output        VARCHAR(512) COMMENT '出参说明（函数结果含义，画布展示）',
    output_name   VARCHAR(64) COMMENT '默认出参名（规则画布固定别名，缺省=函数名）',
    class_name    VARCHAR(256),
    jar_path      VARCHAR(512),
    script        TEXT,
    params_json   TEXT,
    test_cases_json TEXT COMMENT '在线测试案例 JSON [{name,eventParams,bindings,expect}]',
    config_json   TEXT,
    enabled       TINYINT(1) DEFAULT 1,
    version       INT DEFAULT 1,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_action_definition (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_code   VARCHAR(64)  NOT NULL UNIQUE,
    action_name   VARCHAR(128),
    action_type   VARCHAR(32),
    description   VARCHAR(512),
    params_json   TEXT,
    defaults_json TEXT,
    enabled       TINYINT(1) DEFAULT 1,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_engine_log (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id           VARCHAR(64),
    event_code         VARCHAR(64),
    user_id            VARCHAR(64),
    channel_id         VARCHAR(64),
    trace_id           VARCHAR(64),
    success            TINYINT(1),
    error_message      VARCHAR(1024),
    matched_rule_codes VARCHAR(1024),
    cost_ms            BIGINT,
    attributes_json    TEXT,
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_engine_log_event_code (event_code),
    INDEX idx_engine_log_user_event (user_id, event_code),
    INDEX idx_engine_log_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS t_action_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    event_id        VARCHAR(64),
    rule_code       VARCHAR(64),
    action_code     VARCHAR(64),
    success         TINYINT(1),
    detail          VARCHAR(1024),
    params_json     TEXT,
    cost_ms         BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_action_log_event_id (event_id),
    INDEX idx_action_log_rule_code (rule_code)
);

-- ---------- 事件定义 ----------

INSERT INTO t_event_definition (event_code, event_name, description, enabled, params_json, created_by)
VALUES ('AD_CLICK', '广告点击', '广告点击归因事件', 1,
        '[{"code":"adSlotId","name":"广告位ID","type":"STRING","required":true,"description":null,"defaultValue":null},{"code":"campaignId","name":"活动ID","type":"STRING","required":true,"description":null,"defaultValue":null},{"code":"advertiserId","name":"广告主ID","type":"STRING","required":false,"description":null,"defaultValue":null},{"code":"region","name":"地域","type":"STRING","required":false,"description":null,"defaultValue":null},{"code":"deviceType","name":"设备类型","type":"STRING","required":false,"description":null,"defaultValue":null}]',
        'system')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_event_definition (event_code, event_name, description, enabled, params_json, created_by)
VALUES ('ORDER_CREATE', '下单', '订单创建事件', 1,
        '[{"code":"orderId","name":"订单号","type":"STRING","required":true,"description":null,"defaultValue":null},{"code":"orderAmount","name":"订单金额","type":"NUMBER","required":true,"description":null,"defaultValue":null},{"code":"skuId","name":"商品ID","type":"STRING","required":false,"description":null,"defaultValue":null},{"code":"region","name":"地域","type":"STRING","required":false,"description":null,"defaultValue":null},{"code":"paymentChannel","name":"支付渠道","type":"STRING","required":false,"description":null,"defaultValue":null}]',
        'system')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_event_definition (event_code, event_name, description, enabled, params_json, created_by)
VALUES ('SIGN_IN', '签到', '签到事件', 1,
        '[{"code":"signInDate","name":"签到日期","type":"DATETIME","required":true,"description":null,"defaultValue":null}]',
        'system')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_event_definition (event_code, event_name, description, enabled, params_json, created_by)
VALUES ('APP_LAUNCH', '启动', 'App 启动事件', 1,
        '[{"code":"appVersion","name":"App版本","type":"STRING","required":false,"description":null,"defaultValue":null},{"code":"region","name":"地域","type":"STRING","required":false,"description":null,"defaultValue":null},{"code":"launchSource","name":"启动来源","type":"STRING","required":false,"description":null,"defaultValue":null}]',
        'system')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_event_definition (event_code, event_name, description, enabled, params_json, created_by)
VALUES ('USER_RETENTION', '留存触达', '用户留存周期事件（流失预警触达）', 1,
        '[{"code":"retentionDay","name":"留存天数","type":"NUMBER","required":true,"description":null,"defaultValue":null},{"code":"lastActiveDays","name":"距上次活跃天数","type":"NUMBER","required":false,"description":null,"defaultValue":null}]',
        'system')
ON DUPLICATE KEY UPDATE id = id;

-- ---------- 函数定义 ----------

INSERT INTO t_function_definition (function_name, display_name, type, description, output, output_name, class_name, script, params_json, config_json, test_cases_json, enabled, version)
VALUES ('consecutiveCheckinDays', '连续打卡天数计算', 'JAVA_SPI', '计算用户连续打卡天数：优先取绑定/事件参数 checkinStreak，否则取用户画像 checkinStreak，缺省 0', '返回连续打卡天数（数字）', 'checkinStreak', 'consecutiveCheckinDaysFunction', NULL,
        '[{"code":"checkinStreak","name":"连续打卡天数","type":"NUMBER","required":false,"description":"无绑定/事件值时默认取用户画像 checkinStreak，缺省 0","editable":true}]',
        NULL,
        '[{"name":"事件参数取数","eventParams":{"checkinStreak":5},"bindings":{},"expect":"返回 5（事件参数优先于画像，当前画像为空）"},{"name":"无数据兜底","eventParams":{},"bindings":{},"expect":"返回 0（画像/事件/绑定均无 checkinStreak）"}]',
        1, 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_function_definition (function_name, display_name, type, description, output, output_name, class_name, script, params_json, config_json, test_cases_json, enabled, version)
VALUES ('rebateCalculator', '阶梯返利核算', 'JAVA_SPI', '按阶梯档位核算返利金额：读取事件参数 amountField（默认 orderAmount），按绑定参数 tiers 档位计算返利', '返回返利金额（数字，保留 2 位小数）', 'rebateAmount', 'rebateCalculatorFunction', NULL,
        '[{"code":"amountField","name":"金额字段","type":"STRING","required":false,"description":"参与返利核算的事件参数字段，默认 orderAmount","editable":false},{"code":"tiers","name":"阶梯档位","type":"LIST_OBJECT","required":true,"description":"按格式新增档位行：最低金额/最高金额/返利比例","editable":true,"itemSchema":[{"code":"min","name":"最低金额","type":"NUMBER","required":true,"description":"本档最低金额"},{"code":"max","name":"最高金额","type":"NUMBER","required":false,"description":"留空表示上不封顶"},{"code":"rate","name":"返利比例","type":"NUMBER","required":true,"description":"返利比例，如 0.05 表示 5%"}]}]',
        NULL,
        '[{"name":"低档返利 2%","eventParams":{"orderAmount":300},"bindings":{"tiers":[{"min":100,"max":499,"rate":0.02},{"min":500,"max":null,"rate":0.05}]},"expect":"返回 6.00（300×2%）"},{"name":"高档返利 5%","eventParams":{"orderAmount":800},"bindings":{"tiers":[{"min":100,"max":499,"rate":0.02},{"min":500,"max":null,"rate":0.05}]},"expect":"返回 40.00（800×5%）"},{"name":"低于门槛","eventParams":{"orderAmount":50},"bindings":{"tiers":[{"min":100,"max":499,"rate":0.02},{"min":500,"max":null,"rate":0.05}]},"expect":"返回 0.00（未命中任何档位）"}]',
        1, 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_function_definition (function_name, display_name, type, description, output, output_name, class_name, script, params_json, config_json, test_cases_json, enabled, version)
VALUES ('scoreCalculator', '活跃度积分计算', 'EXPRESSION', '根据用户画像计算活跃度得分：orderCount * 10 + checkinStreak * 5', '返回活跃度得分（数字）', 'score', NULL, 'orderCount * 10 + checkinStreak * 5',
        '[{"code":"orderCount","name":"下单数","type":"NUMBER","required":false,"description":"用户画像字段","editable":true},{"code":"checkinStreak","name":"连续打卡","type":"NUMBER","required":false,"description":"用户画像字段","editable":true}]',
        NULL,
        '[{"name":"活跃度 5 单 3 连签","eventParams":{"orderCount":5,"checkinStreak":3},"bindings":{},"expect":"返回 65（5×10+3×5）"}]',
        1, 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_function_definition (function_name, display_name, type, description, output, output_name, class_name, script, params_json, config_json, test_cases_json, enabled, version)
VALUES ('tieredRewardCalculator', '阶梯奖励核算', 'JAVA_SPI',
        '按档位 key 匹配返回奖励值（如连续签到阶梯：第1天1分、第2天2分、第3天4分…），绑定参数 keyField 指定匹配字段、tiers 定义档位、fallback 定义兜底',
        '返回档位奖励值（数字），无匹配返回 fallback',
        'rewardPoints',
        'tieredRewardCalculatorFunction', NULL,
        '[{"code":"keyField","name":"匹配字段","type":"STRING","required":false,"description":"取哪里的值匹配档位，默认 checkinStreak","editable":true},{"code":"tiers","name":"档位","type":"LIST_OBJECT","required":true,"description":"按格式新增档位行：键→奖励值","editable":true,"itemSchema":[{"code":"key","name":"键（天数/值）","type":"NUMBER","required":true,"description":"档位匹配键，如第 1 天填 1"},{"code":"value","name":"奖励值","type":"NUMBER","required":true,"description":"该档位奖励，如第 1 天 1 分"}]},{"code":"fallback","name":"兜底值","type":"NUMBER","required":false,"description":"无匹配时返回，默认 0","editable":true}]',
        NULL,
        '[{"name":"签到第3天","eventParams":{"checkinStreak":3},"bindings":{"keyField":"checkinStreak","tiers":[{"key":1,"value":1},{"key":2,"value":2},{"key":3,"value":4},{"key":4,"value":8},{"key":5,"value":16}]},"expect":"返回 4（第3天奖励翻倍）"},{"name":"超档兜底","eventParams":{"checkinStreak":9},"bindings":{"keyField":"checkinStreak","tiers":[{"key":1,"value":1},{"key":2,"value":2},{"key":3,"value":4},{"key":4,"value":8},{"key":5,"value":16}]},"expect":"返回 0（超出档位，fallback 默认 0）"},{"name":"区间档位","eventParams":{"orderAmount":800},"bindings":{"keyField":"orderAmount","tiers":[{"from":100,"to":499,"value":1},{"from":500,"to":null,"value":2}]},"expect":"返回 2（命中 from 500 上不封顶档）"}]',
        1, 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_function_definition (function_name, display_name, type, description, output, output_name, class_name, script, params_json, config_json, test_cases_json, enabled, version)
VALUES ('signInDays', '签到天数计算', 'JAVA_SPI',
        '基于 t_engine_log 真实签到历史按天去重计算：mode=streak 连续签到天数（默认）/ mode=total 累计签到天数；本次当日首次签到计入',
        '返回签到天数（数字）：streak=连续 / total=累计',
        'signInDays',
        'signInDaysFunction', NULL,
        '[{"code":"eventCode","name":"签到事件","type":"STRING","required":false,"description":"默认 SIGN_IN","editable":false},{"code":"mode","name":"模式","type":"STRING","required":false,"description":"streak 连续 / total 累计","editable":true}]',
        NULL,
        '[{"name":"累计天数 total","eventParams":{},"bindings":{"mode":"total","eventCode":"SIGN_IN"},"expect":"返回 test-user 在 t_engine_log 的累计签到天数（当日首次签到计入，无历史时首次为 1）"},{"name":"连续天数 streak","eventParams":{},"bindings":{"mode":"streak","eventCode":"SIGN_IN"},"expect":"返回 test-user 最近连续签到天数（无历史时首次为 1）"}]',
        1, 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_function_definition (function_name, display_name, type, description, output, output_name, class_name, script, params_json, config_json, test_cases_json, enabled, version)
VALUES ('todaySignedIn', '今日是否已签到', 'JAVA_SPI',
        '基于 t_engine_log 判断今日是否已有签到记录（不含本次触发），用于每日签到限发一次',
        '返回是否已签到（布尔值）',
        'todaySignedIn',
        'todaySignedInFunction', NULL,
        '[{"code":"eventCode","name":"签到事件","type":"STRING","required":false,"description":"默认 SIGN_IN","editable":false}]',
        NULL,
        '[{"name":"今日未签到","eventParams":{},"bindings":{"eventCode":"SIGN_IN"},"expect":"返回 false（test-user 今日无 SIGN_IN 日志时；已签到时为 true）"}]',
        1, 1)
ON DUPLICATE KEY UPDATE id = id;

-- ---------- 动作定义 ----------

INSERT INTO t_action_definition (action_code, action_name, action_type, description, params_json, defaults_json, enabled)
VALUES ('ISSUE_COUPON', '发放优惠券', 'COUPON', '发放优惠券（券中心）',
        '[{"code":"couponTemplateId","name":"券模板ID","type":"STRING","required":true,"defaultValue":null,"description":"券中心模板"},{"code":"count","name":"发放数量","type":"NUMBER","required":false,"defaultValue":1,"description":"默认 1 张"},{"code":"expireDays","name":"有效期天数","type":"NUMBER","required":false,"defaultValue":30,"description":"默认 30 天"}]',
        '{"count":1,"expireDays":30}', 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_action_definition (action_code, action_name, action_type, description, params_json, defaults_json, enabled)
VALUES ('SEND_SMS', '发送短信', 'SMS', '短信触达（短信平台）',
        '[{"code":"smsTemplateId","name":"短信模板ID","type":"STRING","required":true,"defaultValue":null,"description":"短信平台模板"},{"code":"mobile","name":"手机号","type":"STRING","required":false,"defaultValue":null,"description":"缺省取用户中心手机号"},{"code":"content","name":"短信内容","type":"STRING","required":false,"defaultValue":null,"description":"可选，覆盖模板"}]',
        '{}', 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_action_definition (action_code, action_name, action_type, description, params_json, defaults_json, enabled)
VALUES ('ADD_POINTS', '增加积分', 'POINTS', '积分账户加积分',
        '[{"code":"points","name":"积分数量","type":"NUMBER","required":true,"defaultValue":null,"description":"支持 #{expr} 表达式"},{"code":"reason","name":"积分原因","type":"STRING","required":false,"defaultValue":null,"description":"流水备注"}]',
        '{}', 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_action_definition (action_code, action_name, action_type, description, params_json, defaults_json, enabled)
VALUES ('SEND_PUSH', '发送推送', 'PUSH', 'App 推送触达（推送平台）',
        '[{"code":"pushTemplateId","name":"推送模板ID","type":"STRING","required":true,"defaultValue":null,"description":"推送平台模板"},{"code":"title","name":"标题","type":"STRING","required":false,"defaultValue":null,"description":""},{"code":"body","name":"内容","type":"STRING","required":false,"defaultValue":null,"description":""}]',
        '{}', 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_action_definition (action_code, action_name, action_type, description, params_json, defaults_json, enabled)
VALUES ('AUDIT_LOG', '审计日志', 'LOG', '链路审计记录（追踪/演示）',
        '[{"code":"message","name":"消息","type":"STRING","required":false,"defaultValue":null,"description":""}]',
        '{}', 1)
ON DUPLICATE KEY UPDATE id = id;

-- ---------- 规则组（画布配置）----------

-- 1. 广告归因-新客送券
INSERT INTO t_rule_group (rule_code, rule_name, event_code, description, priority, enabled, content_json, created_by)
VALUES ('AD_ATTRIBUTION_NEW_USER', '广告归因-新客送券', 'AD_CLICK', '广告归因-新客送券', 10, 1,
        '{"ruleName":"广告归因-新客送券","description":"广告归因-新客送券","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"userTag","operator":"CONTAINS","value":"NEW_USER","valueType":"STRING","expression":null,"not":false},{"nodeType":"LEAF","field":"channelId","operator":"EQUALS","value":"AD-ZHITONG","valueType":"STRING","expression":null,"not":false}]},"functions":[],"actions":[{"actionCode":"ISSUE_COUPON","params":{"couponTemplateId":"CT-AD-NEW","count":1,"expireDays":30},"async":false},{"actionCode":"AUDIT_LOG","params":{"message":"广告归因-新客送券"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        'system')
ON DUPLICATE KEY UPDATE id = id;

-- 2. 广告归因-老客召回
INSERT INTO t_rule_group (rule_code, rule_name, event_code, description, priority, enabled, content_json, created_by)
VALUES ('AD_ATTRIBUTION_OLD_USER', '广告归因-老客召回', 'AD_CLICK', '广告归因-老客召回', 20, 1,
        '{"ruleName":"广告归因-老客召回","description":"广告归因-老客召回","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"userTag","operator":"CONTAINS","value":"OLD_USER","valueType":"STRING","expression":null,"not":false},{"nodeType":"LEAF","field":"channelId","operator":"EQUALS","value":"AD-ZHITONG","valueType":"STRING","expression":null,"not":false}]},"functions":[],"actions":[{"actionCode":"ADD_POINTS","params":{"reason":"老客召回","points":20},"async":false},{"actionCode":"SEND_SMS","params":{"content":"老友回归，专属好礼已备好","smsTemplateId":"ST-AD-OLD"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        'system')
ON DUPLICATE KEY UPDATE id = id;

-- 3. 签到拉新-连续打卡阶梯返积分（基于画像演示；新库默认停用，启用请用下方基于真实日志的每日/阶段签到规则）
INSERT INTO t_rule_group (rule_code, rule_name, event_code, description, priority, enabled, content_json, created_by)
VALUES ('SIGN_IN_STREAK_REWARD', '签到拉新-连续打卡阶梯返积分', 'SIGN_IN', '签到拉新-连续打卡阶梯返积分', 30, 0,
        '{"ruleName":"签到拉新-连续打卡阶梯返积分","description":"签到拉新-连续打卡阶梯返积分","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"checkinStreak","operator":"GTE","value":1,"valueType":"NUMBER","expression":null,"not":false},{"nodeType":"LEAF","field":"userTag","operator":"IN","value":["NEW_USER","ACTIVE"],"valueType":"STRING","expression":null,"not":false}]},"functions":[{"functionName":"consecutiveCheckinDays","alias":"checkinStreak","bindings":{}},{"functionName":"tieredRewardCalculator","alias":"rewardPoints","bindings":{"keyField":"checkinStreak","tiers":[{"key":1,"value":1},{"key":2,"value":2},{"key":3,"value":4},{"key":4,"value":8},{"key":5,"value":16}]}}],"actions":[{"actionCode":"ADD_POINTS","params":{"reason":"连续打卡阶梯奖励","points":"#{rewardPoints}"},"async":false},{"actionCode":"ISSUE_COUPON","params":{"count":1,"couponTemplateId":"CT-SIGN-7"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        'system')
ON DUPLICATE KEY UPDATE id = id;

-- 3.1 每日签到-固定积分（基于 t_engine_log 今日首次签到）
INSERT INTO t_rule_group (rule_code, rule_name, event_code, description, priority, enabled, content_json, created_by)
VALUES ('SIGN_IN_DAILY_FIXED', '每日签到-固定积分', 'SIGN_IN', '每日签到获取固定积分（今日首次签到发放，同日不重复）', 31, 1,
        '{"ruleName":"每日签到-固定积分","description":"每日签到获取固定积分（今日首次签到发放，同日不重复）","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"todaySignedIn","operator":"EQUALS","value":false,"valueType":"BOOLEAN","expression":null,"not":false}]},"functions":[{"functionName":"todaySignedIn","alias":"todaySignedIn","bindings":{}}],"actions":[{"actionCode":"ADD_POINTS","params":{"reason":"每日签到固定积分","points":10},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        'system')
ON DUPLICATE KEY UPDATE id = id;

-- 3.2 签到新人-阶段积分（基于真实累计签到天数：第1天1分、第2天2分、第3天4分、第4天8分、第5天16分，最多5天活动结束）
INSERT INTO t_rule_group (rule_code, rule_name, event_code, description, priority, enabled, content_json, created_by)
VALUES ('SIGN_IN_STAGE_REWARD', '签到新人-阶段积分', 'SIGN_IN', '第1天1分、第2天2分、第3天4分、第4天8分、第5天16分，累计满5天后活动对该用户结束', 32, 1,
        '{"ruleName":"签到新人-阶段积分","description":"第1天1分、第2天2分、第3天4分、第4天8分、第5天16分，累计满5天后活动对该用户结束","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"signInDays","operator":"GTE","value":1,"valueType":"NUMBER","expression":null,"not":false},{"nodeType":"LEAF","field":"signInDays","operator":"LTE","value":5,"valueType":"NUMBER","expression":null,"not":false},{"nodeType":"LEAF","field":"todaySignedIn","operator":"EQUALS","value":false,"valueType":"BOOLEAN","expression":null,"not":false}]},"functions":[{"functionName":"signInDays","alias":"signInDays","bindings":{"mode":"total","eventCode":"SIGN_IN"}},{"functionName":"tieredRewardCalculator","alias":"rewardPoints","bindings":{"keyField":"signInDays","tiers":[{"key":1,"value":1},{"key":2,"value":2},{"key":3,"value":4},{"key":4,"value":8},{"key":5,"value":16}]}},{"functionName":"todaySignedIn","alias":"todaySignedIn","bindings":{}}],"actions":[{"actionCode":"ADD_POINTS","params":{"reason":"阶段签到奖励","points":"#{rewardPoints}"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        'system')
ON DUPLICATE KEY UPDATE id = id;

-- 4. 下单返券-阶梯返利
INSERT INTO t_rule_group (rule_code, rule_name, event_code, description, priority, enabled, content_json, created_by)
VALUES ('ORDER_REBATE_COUPON', '下单返券-阶梯返利', 'ORDER_CREATE', '下单返券-阶梯返利', 40, 1,
        '{"ruleName":"下单返券-阶梯返利","description":"下单返券-阶梯返利","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"orderAmount","operator":"GTE","value":100,"valueType":"NUMBER","expression":null,"not":false},{"nodeType":"LEAF","field":"rebateAmount","operator":"GT","value":0,"valueType":"NUMBER","expression":null,"not":false}]},"functions":[{"functionName":"rebateCalculator","alias":"rebateAmount","bindings":{"tiers":[{"rate":0.02,"max":499,"min":100},{"min":500,"max":null,"rate":0.05}],"amountField":"orderAmount"}}],"actions":[{"actionCode":"ISSUE_COUPON","params":{"count":"#{rebateAmount >= 10 ? 2 : 1}","couponTemplateId":"CT-ORDER-REBATE"},"async":false},{"actionCode":"AUDIT_LOG","params":{"message":"下单返利=${rebateAmount}"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        'system')
ON DUPLICATE KEY UPDATE id = id;

-- 5. 活动分层推送-高价值用户（带 30% 灰度）
INSERT INTO t_rule_group (rule_code, rule_name, event_code, description, priority, enabled, content_json, created_by)
VALUES ('LAUNCH_TIER_PUSH', '活动分层推送-高价值用户', 'APP_LAUNCH', '活动分层推送-高价值用户', 50, 1,
        '{"ruleName":"活动分层推送-高价值用户","description":"活动分层推送-高价值用户","conditionTree":{"nodeType":"LOGIC","logic":"OR","children":[{"nodeType":"LEAF","field":"ltvTier","operator":"EQUALS","value":"A","valueType":"STRING","expression":null,"not":false},{"nodeType":"LEAF","field":"orderCount","operator":"GTE","value":10,"valueType":"NUMBER","expression":null,"not":false}]},"functions":[],"actions":[{"actionCode":"SEND_PUSH","params":{"title":"会员专享活动","pushTemplateId":"PT-VIP-ACT","body":"点我查看专属权益"},"async":false}],"gray":{"enabled":true,"strategy":"PERCENT","percent":30,"channels":[],"bucketKey":"userId"}}',
        'system')
ON DUPLICATE KEY UPDATE id = id;

-- 6. 用户触达-流失预警
INSERT INTO t_rule_group (rule_code, rule_name, event_code, description, priority, enabled, content_json, created_by)
VALUES ('RETENTION_WARNING', '用户触达-流失预警', 'USER_RETENTION', '用户触达-流失预警', 60, 1,
        '{"ruleName":"用户触达-流失预警","description":"用户触达-流失预警","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"retentionDay","operator":"GTE","value":7,"valueType":"NUMBER","expression":null,"not":false},{"nodeType":"LEAF","field":"userTag","operator":"NOT_IN","value":["NEW_USER"],"valueType":"STRING","expression":null,"not":false}]},"functions":[],"actions":[{"actionCode":"SEND_SMS","params":{"content":"好久不见，回归好礼等你领","smsTemplateId":"ST-RETENTION"},"async":false},{"actionCode":"SEND_PUSH","params":{"title":"回归礼包","pushTemplateId":"PT-RETENTION","body":"领取专属福利"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        'system')
ON DUPLICATE KEY UPDATE id = id;

-- ---------- 规则版本（发布快照，status=PUBLISHED 即时生效）----------

INSERT INTO t_rule_version (rule_code, version_no, status, content_json, change_log, published_by, published_at)
VALUES ('AD_ATTRIBUTION_NEW_USER', 1, 'PUBLISHED',
        '{"ruleName":"广告归因-新客送券","description":"广告归因-新客送券","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"userTag","operator":"CONTAINS","value":"NEW_USER","valueType":"STRING","expression":null,"not":false},{"nodeType":"LEAF","field":"channelId","operator":"EQUALS","value":"AD-ZHITONG","valueType":"STRING","expression":null,"not":false}]},"functions":[],"actions":[{"actionCode":"ISSUE_COUPON","params":{"couponTemplateId":"CT-AD-NEW","count":1,"expireDays":30},"async":false},{"actionCode":"AUDIT_LOG","params":{"message":"广告归因-新客送券"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        '初始化发布', 'system', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_rule_version (rule_code, version_no, status, content_json, change_log, published_by, published_at)
VALUES ('AD_ATTRIBUTION_OLD_USER', 1, 'PUBLISHED',
        '{"ruleName":"广告归因-老客召回","description":"广告归因-老客召回","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"userTag","operator":"CONTAINS","value":"OLD_USER","valueType":"STRING","expression":null,"not":false},{"nodeType":"LEAF","field":"channelId","operator":"EQUALS","value":"AD-ZHITONG","valueType":"STRING","expression":null,"not":false}]},"functions":[],"actions":[{"actionCode":"ADD_POINTS","params":{"reason":"老客召回","points":20},"async":false},{"actionCode":"SEND_SMS","params":{"content":"老友回归，专属好礼已备好","smsTemplateId":"ST-AD-OLD"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        '初始化发布', 'system', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_rule_version (rule_code, version_no, status, content_json, change_log, published_by, published_at)
VALUES ('SIGN_IN_STREAK_REWARD', 1, 'PUBLISHED',
        '{"ruleName":"签到拉新-连续打卡阶梯返积分","description":"签到拉新-连续打卡阶梯返积分","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"checkinStreak","operator":"GTE","value":1,"valueType":"NUMBER","expression":null,"not":false},{"nodeType":"LEAF","field":"userTag","operator":"IN","value":["NEW_USER","ACTIVE"],"valueType":"STRING","expression":null,"not":false}]},"functions":[{"functionName":"consecutiveCheckinDays","alias":"checkinStreak","bindings":{}},{"functionName":"tieredRewardCalculator","alias":"rewardPoints","bindings":{"keyField":"checkinStreak","tiers":[{"key":1,"value":1},{"key":2,"value":2},{"key":3,"value":4},{"key":4,"value":8},{"key":5,"value":16}]}}],"actions":[{"actionCode":"ADD_POINTS","params":{"reason":"连续打卡阶梯奖励","points":"#{rewardPoints}"},"async":false},{"actionCode":"ISSUE_COUPON","params":{"count":1,"couponTemplateId":"CT-SIGN-7"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        '初始化发布', 'system', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_rule_version (rule_code, version_no, status, content_json, change_log, published_by, published_at)
VALUES ('SIGN_IN_DAILY_FIXED', 1, 'PUBLISHED',
        '{"ruleName":"每日签到-固定积分","description":"每日签到获取固定积分（今日首次签到发放，同日不重复）","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"todaySignedIn","operator":"EQUALS","value":false,"valueType":"BOOLEAN","expression":null,"not":false}]},"functions":[{"functionName":"todaySignedIn","alias":"todaySignedIn","bindings":{}}],"actions":[{"actionCode":"ADD_POINTS","params":{"reason":"每日签到固定积分","points":10},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        '初始化发布', 'system', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_rule_version (rule_code, version_no, status, content_json, change_log, published_by, published_at)
VALUES ('SIGN_IN_STAGE_REWARD', 1, 'PUBLISHED',
        '{"ruleName":"签到新人-阶段积分","description":"第1天1分、第2天2分、第3天4分、第4天8分、第5天16分，累计满5天后活动对该用户结束","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"signInDays","operator":"GTE","value":1,"valueType":"NUMBER","expression":null,"not":false},{"nodeType":"LEAF","field":"signInDays","operator":"LTE","value":5,"valueType":"NUMBER","expression":null,"not":false},{"nodeType":"LEAF","field":"todaySignedIn","operator":"EQUALS","value":false,"valueType":"BOOLEAN","expression":null,"not":false}]},"functions":[{"functionName":"signInDays","alias":"signInDays","bindings":{"mode":"total","eventCode":"SIGN_IN"}},{"functionName":"tieredRewardCalculator","alias":"rewardPoints","bindings":{"keyField":"signInDays","tiers":[{"key":1,"value":1},{"key":2,"value":2},{"key":3,"value":4},{"key":4,"value":8},{"key":5,"value":16}]}},{"functionName":"todaySignedIn","alias":"todaySignedIn","bindings":{}}],"actions":[{"actionCode":"ADD_POINTS","params":{"reason":"阶段签到奖励","points":"#{rewardPoints}"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        '初始化发布', 'system', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_rule_version (rule_code, version_no, status, content_json, change_log, published_by, published_at)
VALUES ('ORDER_REBATE_COUPON', 1, 'PUBLISHED',
        '{"ruleName":"下单返券-阶梯返利","description":"下单返券-阶梯返利","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"orderAmount","operator":"GTE","value":100,"valueType":"NUMBER","expression":null,"not":false},{"nodeType":"LEAF","field":"rebateAmount","operator":"GT","value":0,"valueType":"NUMBER","expression":null,"not":false}]},"functions":[{"functionName":"rebateCalculator","alias":"rebateAmount","bindings":{"tiers":[{"rate":0.02,"max":499,"min":100},{"min":500,"max":null,"rate":0.05}],"amountField":"orderAmount"}}],"actions":[{"actionCode":"ISSUE_COUPON","params":{"count":"#{rebateAmount >= 10 ? 2 : 1}","couponTemplateId":"CT-ORDER-REBATE"},"async":false},{"actionCode":"AUDIT_LOG","params":{"message":"下单返利=${rebateAmount}"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        '初始化发布', 'system', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_rule_version (rule_code, version_no, status, content_json, change_log, published_by, published_at)
VALUES ('LAUNCH_TIER_PUSH', 1, 'PUBLISHED',
        '{"ruleName":"活动分层推送-高价值用户","description":"活动分层推送-高价值用户","conditionTree":{"nodeType":"LOGIC","logic":"OR","children":[{"nodeType":"LEAF","field":"ltvTier","operator":"EQUALS","value":"A","valueType":"STRING","expression":null,"not":false},{"nodeType":"LEAF","field":"orderCount","operator":"GTE","value":10,"valueType":"NUMBER","expression":null,"not":false}]},"functions":[],"actions":[{"actionCode":"SEND_PUSH","params":{"title":"会员专享活动","pushTemplateId":"PT-VIP-ACT","body":"点我查看专属权益"},"async":false}],"gray":{"enabled":true,"strategy":"PERCENT","percent":30,"channels":[],"bucketKey":"userId"}}',
        '初始化发布', 'system', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO t_rule_version (rule_code, version_no, status, content_json, change_log, published_by, published_at)
VALUES ('RETENTION_WARNING', 1, 'PUBLISHED',
        '{"ruleName":"用户触达-流失预警","description":"用户触达-流失预警","conditionTree":{"nodeType":"LOGIC","logic":"AND","children":[{"nodeType":"LEAF","field":"retentionDay","operator":"GTE","value":7,"valueType":"NUMBER","expression":null,"not":false},{"nodeType":"LEAF","field":"userTag","operator":"NOT_IN","value":["NEW_USER"],"valueType":"STRING","expression":null,"not":false}]},"functions":[],"actions":[{"actionCode":"SEND_SMS","params":{"content":"好久不见，回归好礼等你领","smsTemplateId":"ST-RETENTION"},"async":false},{"actionCode":"SEND_PUSH","params":{"title":"回归礼包","pushTemplateId":"PT-RETENTION","body":"领取专属福利"},"async":false}],"gray":{"enabled":false,"strategy":"OFF","percent":0,"channels":[],"bucketKey":"userId"}}',
        '初始化发布', 'system', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE id = id;
