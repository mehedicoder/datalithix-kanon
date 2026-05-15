CREATE TABLE connector_configurations (
    connector_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    display_name VARCHAR(240),
    source_category VARCHAR(60) NOT NULL,
    source_type VARCHAR(60) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    execution_policy_json JSONB,
    properties_json JSONB,
    secret_refs_json JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, connector_id)
);

CREATE INDEX idx_connector_configurations_tenant_type ON connector_configurations(tenant_id, source_category, source_type);
