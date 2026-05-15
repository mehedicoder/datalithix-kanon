CREATE TABLE dataset_definition (
    dataset_definition_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    name VARCHAR(240) NOT NULL,
    description TEXT,
    domain_type VARCHAR(60),
    split_strategy VARCHAR(60) NOT NULL,
    train_ratio NUMERIC(5,4) NOT NULL,
    val_ratio NUMERIC(5,4) NOT NULL,
    test_ratio NUMERIC(5,4) NOT NULL,
    data_residency VARCHAR(60),
    enabled BOOLEAN NOT NULL,
    latest_version_number INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, dataset_definition_id)
);

CREATE TABLE dataset_version (
    dataset_version_id VARCHAR(120) NOT NULL,
    dataset_definition_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    version_number INTEGER NOT NULL,
    split_strategy VARCHAR(60),
    total_record_count BIGINT NOT NULL DEFAULT 0,
    curated_at TIMESTAMPTZ,
    curated_by VARCHAR(120),
    export_status VARCHAR(60),
    export_format VARCHAR(60),
    export_artifact_uri VARCHAR(1024),
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, dataset_version_id),
    FOREIGN KEY (tenant_id, dataset_definition_id) REFERENCES dataset_definition(tenant_id, dataset_definition_id)
);

CREATE INDEX idx_dataset_definition_tenant_name ON dataset_definition(tenant_id, name);
CREATE INDEX idx_dataset_definition_tenant_domain ON dataset_definition(tenant_id, domain_type);
CREATE INDEX idx_dataset_version_definition ON dataset_version(tenant_id, dataset_definition_id, version_number DESC);
