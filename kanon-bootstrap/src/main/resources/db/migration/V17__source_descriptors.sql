CREATE TABLE source_descriptors (
    source_system VARCHAR(120) NOT NULL,
    source_identifier VARCHAR(240) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    source_category VARCHAR(60) NOT NULL,
    source_type VARCHAR(60) NOT NULL,
    source_uri VARCHAR(1024),
    asset_type VARCHAR(60),
    data_classification VARCHAR(60),
    compliance_classification VARCHAR(60),
    data_residency VARCHAR(60),
    retention_policy VARCHAR(120),
    consent_ref VARCHAR(240),
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, source_system, source_identifier)
);

CREATE INDEX idx_source_descriptors_tenant_category ON source_descriptors(tenant_id, source_category);
