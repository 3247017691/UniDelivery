-- 全域投放核心 schema。唯一标的约束：active_flag=1 表示在投/有效，终态置 NULL，
-- 借助 MySQL/H2 唯一索引对 NULL 不去重的语义实现「同一 aweme_id x asset_id 仅一个活跃全域项目」。

CREATE TABLE oauth_token (
    advertiser_id        BIGINT       NOT NULL PRIMARY KEY,
    access_token         VARCHAR(512) NOT NULL,
    refresh_token        VARCHAR(512) NOT NULL,
    expires_at           TIMESTAMP    NOT NULL,
    refresh_expires_at   TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL
);

CREATE TABLE advertiser (
    advertiser_id      BIGINT       NOT NULL PRIMARY KEY,
    name               VARCHAR(128) NOT NULL,
    whitelisted        BOOLEAN      NOT NULL DEFAULT FALSE,
    whitelist_evidence VARCHAR(512),
    brand_info_json    TEXT,
    brand_checked_at   TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL
);

CREATE TABLE aweme_auth (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    advertiser_id   BIGINT       NOT NULL,
    aweme_id        BIGINT       NOT NULL,
    auth_scenarios  VARCHAR(512),
    uni_authorized  BOOLEAN      NOT NULL DEFAULT FALSE,
    checked_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uk_aweme_auth UNIQUE (advertiser_id, aweme_id)
);

CREATE TABLE product_playlet (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    advertiser_id   BIGINT       NOT NULL,
    asset_id        BIGINT       NOT NULL,
    playlet_id      BIGINT,
    album_id        BIGINT,
    org_id          BIGINT,
    platform_id     BIGINT,
    library_version VARCHAR(32)  NOT NULL,
    deliverable     BOOLEAN      NOT NULL DEFAULT FALSE,
    checked_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uk_product UNIQUE (advertiser_id, asset_id)
);

CREATE TABLE uni_project (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    advertiser_id      BIGINT        NOT NULL,
    project_id         BIGINT,
    aweme_id           BIGINT        NOT NULL,
    asset_id           BIGINT        NOT NULL,
    active_flag        INT,
    stage              VARCHAR(32)   NOT NULL,
    status             VARCHAR(32),
    monetization_mode  VARCHAR(32),
    budget             DECIMAL(14, 2),
    bid                DECIMAL(14, 2),
    client_request_id  VARCHAR(64)   NOT NULL,
    confirmed_conflict BOOLEAN       NOT NULL DEFAULT FALSE,
    reject_reason_json TEXT,
    last_error         VARCHAR(512),
    created_at         TIMESTAMP     NOT NULL,
    updated_at         TIMESTAMP     NOT NULL,
    CONSTRAINT uk_client_request UNIQUE (client_request_id),
    CONSTRAINT uk_project_id UNIQUE (project_id),
    CONSTRAINT uk_target UNIQUE (aweme_id, asset_id, active_flag)
);

CREATE TABLE project_material (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_local_id BIGINT      NOT NULL,
    video_id      VARCHAR(64)  NOT NULL,
    material_id   BIGINT,
    status        VARCHAR(32)  NOT NULL,
    opt_status    VARCHAR(16),
    labels        VARCHAR(256),
    batch_no      INT          NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    CONSTRAINT uk_project_video UNIQUE (project_local_id, video_id)
);

CREATE TABLE upload_task (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    advertiser_id BIGINT       NOT NULL,
    file_md5      VARCHAR(32)  NOT NULL,
    filename      VARCHAR(256) NOT NULL,
    task_id       BIGINT,
    video_id      VARCHAR(64),
    status        VARCHAR(32)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    CONSTRAINT uk_upload_md5 UNIQUE (advertiser_id, file_md5)
);

CREATE TABLE report_snapshot (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    advertiser_id BIGINT       NOT NULL,
    topic         VARCHAR(64)  NOT NULL,
    channel       VARCHAR(16)  NOT NULL,
    granularity   VARCHAR(16)  NOT NULL,
    window_start  TIMESTAMP    NOT NULL,
    project_id    BIGINT       NOT NULL DEFAULT 0,
    material_id   BIGINT       NOT NULL DEFAULT 0,
    metrics_json  TEXT         NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    CONSTRAINT uk_snapshot UNIQUE (advertiser_id, topic, channel, granularity, window_start, project_id, material_id)
);

CREATE TABLE subscription_event (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id     VARCHAR(128) NOT NULL,
    event_type   VARCHAR(64)  NOT NULL,
    payload_json TEXT         NOT NULL,
    consumed     BOOLEAN      NOT NULL DEFAULT FALSE,
    received_at  TIMESTAMP    NOT NULL,
    consumed_at  TIMESTAMP,
    CONSTRAINT uk_event UNIQUE (event_id)
);

CREATE TABLE op_journal (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_request_id  VARCHAR(64)  NOT NULL,
    endpoint           VARCHAR(64)  NOT NULL,
    advertiser_id      BIGINT       NOT NULL,
    project_local_id   BIGINT,
    request_summary    TEXT         NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    platform_request_id VARCHAR(64),
    response_summary   TEXT,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP    NOT NULL,
    CONSTRAINT uk_journal_request UNIQUE (client_request_id)
);

CREATE TABLE mapi_audit (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    endpoint        VARCHAR(64)  NOT NULL,
    path            VARCHAR(256) NOT NULL,
    advertiser_id   BIGINT       NOT NULL,
    request_summary TEXT,
    http_status     INT          NOT NULL,
    code            INT          NOT NULL,
    request_id      VARCHAR(64),
    elapsed_ms      BIGINT       NOT NULL,
    attempt         INT          NOT NULL,
    outcome         VARCHAR(24)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL
);

-- 平台操作日志无独立 log_id（契约核实），以 对象+时间+操作内容 作幂等键
CREATE TABLE platform_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    advertiser_id BIGINT       NOT NULL,
    object_id     BIGINT       NOT NULL,
    object_type   VARCHAR(64),
    content_title VARCHAR(256) NOT NULL,
    second_log_id BIGINT,
    detail_json   TEXT,
    op_time       TIMESTAMP    NOT NULL,
    synced_at     TIMESTAMP    NOT NULL,
    CONSTRAINT uk_platform_log UNIQUE (advertiser_id, object_id, op_time, content_title)
);

CREATE TABLE precheck_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    advertiser_id BIGINT       NOT NULL,
    aweme_id      BIGINT       NOT NULL,
    asset_id      BIGINT       NOT NULL,
    item          VARCHAR(32)  NOT NULL,
    passed        BOOLEAN      NOT NULL,
    reason        VARCHAR(512) NOT NULL,
    evidence      VARCHAR(512),
    created_at    TIMESTAMP    NOT NULL
);

CREATE INDEX idx_project_adv ON uni_project (advertiser_id, stage);
CREATE INDEX idx_material_project ON project_material (project_local_id);
CREATE INDEX idx_snapshot_window ON report_snapshot (advertiser_id, topic, channel, window_start);
CREATE INDEX idx_audit_adv_time ON mapi_audit (advertiser_id, created_at);
CREATE INDEX idx_journal_status ON op_journal (status, created_at);
