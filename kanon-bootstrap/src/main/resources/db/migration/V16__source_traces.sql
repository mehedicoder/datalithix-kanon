CREATE TABLE source_traces (
    source_trace_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    case_id VARCHAR(120),
    source_system VARCHAR(120) NOT NULL,
    source_identifier VARCHAR(240) NOT NULL,
    source_category VARCHAR(60) NOT NULL,
    source_type VARCHAR(60) NOT NULL,
    source_uri VARCHAR(1024),
    asset_type VARCHAR(60),
    data_classification VARCHAR(60),
    compliance_classification VARCHAR(60),
    data_residency VARCHAR(60),
    retention_policy VARCHAR(120),
    consent_ref VARCHAR(240),
    original_payload_hash VARCHAR(128),
    actor_type VARCHAR(60) NOT NULL,
    actor_id VARCHAR(120) NOT NULL,
    ingestion_timestamp TIMESTAMPTZ NOT NULL,
    correlation_id VARCHAR(120),
    evidence_event_id VARCHAR(120),
    details_json JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, source_trace_id)
);

CREATE INDEX idx_source_traces_tenant_case_ingested ON source_traces(tenant_id, case_id, ingestion_timestamp DESC);
CREATE INDEX idx_source_traces_tenant_source_identity ON source_traces(tenant_id, source_system, source_identifier);
CREATE INDEX idx_source_traces_tenant_correlation ON source_traces(tenant_id, correlation_id);
CREATE INDEX idx_source_traces_tenant_category ON source_traces(tenant_id, source_category);
