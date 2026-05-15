CREATE TABLE connector_health (
    connector_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    status VARCHAR(60) NOT NULL,
    last_ingestion_at TIMESTAMPTZ,
    last_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    last_failure_reason TEXT,
    retry_count BIGINT NOT NULL DEFAULT 0,
    lag_millis BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, connector_id)
);

CREATE INDEX idx_connector_health_tenant_status ON connector_health(tenant_id, status, last_ingestion_at DESC);
