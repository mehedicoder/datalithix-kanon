CREATE TABLE kanon_schema_version_marker (
    id BIGSERIAL PRIMARY KEY,
    marker_key VARCHAR(120) NOT NULL UNIQUE,
    marker_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    version BIGINT NOT NULL
);

CREATE INDEX idx_kanon_schema_version_marker_key
    ON kanon_schema_version_marker (marker_key);

CREATE TABLE active_configuration_version (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    configuration_type VARCHAR(80) NOT NULL,
    configuration_id VARCHAR(160) NOT NULL,
    template_id VARCHAR(160) NOT NULL,
    version INTEGER NOT NULL,
    activation_state VARCHAR(40) NOT NULL,
    activated_by VARCHAR(120) NOT NULL,
    activated_at TIMESTAMPTZ NOT NULL,
    deactivated_by VARCHAR(120),
    deactivated_at TIMESTAMPTZ,
    change_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    CONSTRAINT uq_active_configuration_version_key
        UNIQUE (tenant_id, configuration_type, configuration_id)
);

CREATE INDEX idx_active_configuration_version_tenant_state
    ON active_configuration_version (tenant_id, activation_state, configuration_type);

CREATE INDEX idx_active_configuration_version_tenant_updated
    ON active_configuration_version (tenant_id, updated_at DESC);
