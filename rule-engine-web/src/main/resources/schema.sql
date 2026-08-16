-- ============================================================
-- 营销规则引擎 Schema（仅支持 MySQL 5.7+ / 8.x）
-- 说明：全部使用 IF NOT EXISTS + 建表内联索引，可重复执行（幂等）；
--       MySQL 8 不支持 CREATE INDEX IF NOT EXISTS，故索引统一写在 CREATE TABLE 内
-- ============================================================

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
