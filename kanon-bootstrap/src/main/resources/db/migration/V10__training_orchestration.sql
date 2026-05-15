CREATE TABLE compute_backend (
    backend_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    backend_type VARCHAR(60) NOT NULL,
    name VARCHAR(240) NOT NULL,
    endpoint_url VARCHAR(512),
    credential_ref VARCHAR(240),
    enabled BOOLEAN NOT NULL,
    healthy BOOLEAN NOT NULL,
    last_health_check_at VARCHAR(60),
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, backend_id)
);

CREATE TABLE training_job (
    training_job_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    dataset_version_id VARCHAR(120),
    dataset_definition_id VARCHAR(120),
    compute_backend_id VARCHAR(120),
    model_name VARCHAR(240) NOT NULL,
    status VARCHAR(60) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    failure_reason TEXT,
    checkpoint_uri VARCHAR(1024),
    output_model_artifact_uri VARCHAR(1024),
    total_duration_seconds BIGINT,
    external_job_id VARCHAR(240),
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, training_job_id)
);

CREATE INDEX idx_training_job_tenant_status ON training_job(tenant_id, status);
CREATE INDEX idx_training_job_tenant_dataset ON training_job(tenant_id, dataset_version_id);
CREATE INDEX idx_training_job_tenant_requested ON training_job(tenant_id, requested_at DESC);
CREATE INDEX idx_compute_backend_tenant_type ON compute_backend(tenant_id, backend_type);
