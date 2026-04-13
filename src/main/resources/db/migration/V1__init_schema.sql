-- Audit records table with partition_year column for logical partitioning.
-- In production PostgreSQL, this would use native table partitioning (PARTITION BY RANGE).
-- For portability with H2/tests, we use a flat table with partition_year as an indexed column.

CREATE TABLE audit_records (
    id              BIGSERIAL       PRIMARY KEY,
    event_id        VARCHAR(36)     NOT NULL UNIQUE,
    source          VARCHAR(100)    NOT NULL,
    action          VARCHAR(100)    NOT NULL,
    actor_id        VARCHAR(255),
    actor_email     VARCHAR(255),
    resource_type   VARCHAR(100)    NOT NULL,
    resource_id     VARCHAR(255),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    outcome         VARCHAR(50)     NOT NULL DEFAULT 'SUCCESS',
    metadata        TEXT,
    partition_year  INTEGER         NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMP,
    kafka_topic     VARCHAR(255),
    kafka_partition INTEGER,
    kafka_offset    BIGINT
);

CREATE INDEX idx_audit_source ON audit_records(source);
CREATE INDEX idx_audit_action ON audit_records(action);
CREATE INDEX idx_audit_actor ON audit_records(actor_id);
CREATE INDEX idx_audit_partition_year ON audit_records(partition_year);
CREATE INDEX idx_audit_created_at ON audit_records(created_at);
CREATE INDEX idx_audit_source_action ON audit_records(source, action);

-- Production partitioning DDL (PostgreSQL only, run manually or via separate migration):
--
-- CREATE TABLE audit_records_2024 PARTITION OF audit_records FOR VALUES FROM (2024) TO (2025);
-- CREATE TABLE audit_records_2025 PARTITION OF audit_records FOR VALUES FROM (2025) TO (2026);
-- CREATE TABLE audit_records_2026 PARTITION OF audit_records FOR VALUES FROM (2026) TO (2027);
--
-- To drop expired partitions (older than 7 years):
-- DROP TABLE IF EXISTS audit_records_2017;
