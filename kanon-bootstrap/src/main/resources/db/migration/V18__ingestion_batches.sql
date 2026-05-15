CREATE TABLE ingestion_batches (
    import_batch_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    connector_id VARCHAR(120) NOT NULL,
    source_system VARCHAR(120) NOT NULL,
    status VARCHAR(60) NOT NULL,
    received_count BIGINT NOT NULL DEFAULT 0,
    accepted_count BIGINT NOT NULL DEFAULT 0,
    rejected_count BIGINT NOT NULL DEFAULT 0,
    retry_count BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, import_batch_id)
);

CREATE INDEX idx_ingestion_batches_tenant_connector_status ON ingestion_batches(tenant_id, connector_id, status, started_at DESC);
CREATE INDEX idx_ingestion_batches_tenant_source ON ingestion_batches(tenant_id, source_system);
