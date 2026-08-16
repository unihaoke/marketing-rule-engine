-- ============================================================
-- 升级脚本：为已有库添加「函数在线测试案例」支持
-- 适用：已存在的 rule_engine / rule_engine_test 库（schema.sql 的
--       CREATE TABLE IF NOT EXISTS 不会给旧表加列，需手动执行本脚本一次）
-- 用法：mysql -uroot -p rule_engine < sql/upgrade-add-function-test-cases.sql
--        mysql -uroot -p rule_engine_test < sql/upgrade-add-function-test-cases.sql
-- 注意：仅执行一次；重复执行会因列已存在而报错（可忽略/回滚）
-- ============================================================

SET NAMES utf8mb4;

ALTER TABLE t_function_definition
    ADD COLUMN test_cases_json TEXT COMMENT '在线测试案例 JSON [{name,eventParams,bindings,expect}]';

UPDATE t_function_definition SET test_cases_json =
'[{"name":"事件参数取数","eventParams":{"checkinStreak":5},"bindings":{},"expect":"返回 5（事件参数优先于画像，当前画像为空）"},{"name":"无数据兜底","eventParams":{},"bindings":{},"expect":"返回 0（画像/事件/绑定均无 checkinStreak）"}]'
WHERE function_name = 'consecutiveCheckinDays';

UPDATE t_function_definition SET test_cases_json =
'[{"name":"低档返利 2%","eventParams":{"orderAmount":300},"bindings":{"tiers":[{"min":100,"max":499,"rate":0.02},{"min":500,"max":null,"rate":0.05}]},"expect":"返回 6.00（300×2%）"},{"name":"高档返利 5%","eventParams":{"orderAmount":800},"bindings":{"tiers":[{"min":100,"max":499,"rate":0.02},{"min":500,"max":null,"rate":0.05}]},"expect":"返回 40.00（800×5%）"},{"name":"低于门槛","eventParams":{"orderAmount":50},"bindings":{"tiers":[{"min":100,"max":499,"rate":0.02},{"min":500,"max":null,"rate":0.05}]},"expect":"返回 0.00（未命中任何档位）"}]'
WHERE function_name = 'rebateCalculator';

UPDATE t_function_definition SET test_cases_json =
'[{"name":"活跃度 5 单 3 连签","eventParams":{"orderCount":5,"checkinStreak":3},"bindings":{},"expect":"返回 65（5×10+3×5）"}]'
WHERE function_name = 'scoreCalculator';

UPDATE t_function_definition SET test_cases_json =
'[{"name":"签到第3天","eventParams":{"checkinStreak":3},"bindings":{"keyField":"checkinStreak","tiers":[{"key":1,"value":1},{"key":2,"value":2},{"key":3,"value":4},{"key":4,"value":8},{"key":5,"value":16}]},"expect":"返回 4（第3天奖励翻倍）"},{"name":"超档兜底","eventParams":{"checkinStreak":9},"bindings":{"keyField":"checkinStreak","tiers":[{"key":1,"value":1},{"key":2,"value":2},{"key":3,"value":4},{"key":4,"value":8},{"key":5,"value":16}]},"expect":"返回 0（超出档位，fallback 默认 0）"},{"name":"区间档位","eventParams":{"orderAmount":800},"bindings":{"keyField":"orderAmount","tiers":[{"from":100,"to":499,"value":1},{"from":500,"to":null,"value":2}]},"expect":"返回 2（命中 from 500 上不封顶档）"}]'
WHERE function_name = 'tieredRewardCalculator';

UPDATE t_function_definition SET test_cases_json =
'[{"name":"累计天数 total","eventParams":{},"bindings":{"mode":"total","eventCode":"SIGN_IN"},"expect":"返回 test-user 在 t_engine_log 的累计签到天数（当日首次签到计入，无历史时首次为 1）"},{"name":"连续天数 streak","eventParams":{},"bindings":{"mode":"streak","eventCode":"SIGN_IN"},"expect":"返回 test-user 最近连续签到天数（无历史时首次为 1）"}]'
WHERE function_name = 'signInDays';

UPDATE t_function_definition SET test_cases_json =
'[{"name":"今日未签到","eventParams":{},"bindings":{"eventCode":"SIGN_IN"},"expect":"返回 false（test-user 今日无 SIGN_IN 日志时；已签到时为 true）"}]'
WHERE function_name = 'todaySignedIn';
