CREATE TABLE external_annotation_node (
    node_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    provider_type VARCHAR(60) NOT NULL,
    base_url VARCHAR(512) NOT NULL,
    secret_ref VARCHAR(240) NOT NULL,
    storage_bucket VARCHAR(240),
    status VARCHAR(60) NOT NULL,
    last_known_version VARCHAR(120),
    last_verification_latency_ms BIGINT,
    last_verified_at TIMESTAMPTZ,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, node_id)
);

CREATE INDEX idx_external_annotation_node_tenant_status ON external_annotation_node(tenant_id, status);
CREATE INDEX idx_external_annotation_node_tenant_provider ON external_annotation_node(tenant_id, provider_type);
CREATE INDEX idx_external_annotation_node_tenant_updated ON external_annotation_node(tenant_id, updated_at DESC);
