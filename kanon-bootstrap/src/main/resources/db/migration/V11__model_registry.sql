CREATE TABLE model_entry (
    model_entry_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    model_name VARCHAR(240) NOT NULL,
    description TEXT,
    framework VARCHAR(60),
    task_type VARCHAR(60),
    domain_type VARCHAR(60),
    latest_version_number INTEGER NOT NULL DEFAULT 0,
    latest_lifecycle_stage VARCHAR(60),
    latest_version_id VARCHAR(120),
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, model_entry_id)
);

CREATE TABLE model_version (
    model_version_id VARCHAR(120) NOT NULL,
    model_entry_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    version_number INTEGER NOT NULL,
    training_job_id VARCHAR(120),
    dataset_version_id VARCHAR(120),
    dataset_definition_id VARCHAR(120),
    artifact_uri VARCHAR(1024),
    artifact_size BIGINT,
    artifact_type VARCHAR(60),
    artifact_storage_backend VARCHAR(60),
    lifecycle_stage VARCHAR(60) NOT NULL,
    promoted_by VARCHAR(120),
    promoted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, model_version_id),
    FOREIGN KEY (tenant_id, model_entry_id) REFERENCES model_entry(tenant_id, model_entry_id)
);

CREATE TABLE evaluation_run (
    evaluation_run_id VARCHAR(120) NOT NULL,
    model_version_id VARCHAR(120) NOT NULL,
    model_entry_id VARCHAR(120),
    tenant_id VARCHAR(120) NOT NULL,
    test_dataset_version_id VARCHAR(120),
    status VARCHAR(60) NOT NULL,
    failure_reason TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    passed_threshold BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, evaluation_run_id),
    FOREIGN KEY (tenant_id, model_version_id) REFERENCES model_version(tenant_id, model_version_id)
);

CREATE TABLE deployment_target (
    deployment_target_id VARCHAR(120) NOT NULL,
    model_version_id VARCHAR(120) NOT NULL,
    model_entry_id VARCHAR(120),
    tenant_id VARCHAR(120) NOT NULL,
    target_type VARCHAR(60),
    endpoint_url VARCHAR(512),
    health_status VARCHAR(60),
    healthy BOOLEAN NOT NULL,
    last_health_check_at TIMESTAMPTZ,
    credential_ref VARCHAR(240),
    active BOOLEAN NOT NULL,
    deployed_at TIMESTAMPTZ,
    rolled_back_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, deployment_target_id),
    FOREIGN KEY (tenant_id, model_version_id) REFERENCES model_version(tenant_id, model_version_id)
);

CREATE INDEX idx_model_entry_tenant_name ON model_entry(tenant_id, model_name);
CREATE INDEX idx_model_entry_tenant_stage ON model_entry(tenant_id, latest_lifecycle_stage);
CREATE INDEX idx_model_version_entry ON model_version(tenant_id, model_entry_id, version_number DESC);
CREATE INDEX idx_model_version_stage ON model_version(tenant_id, lifecycle_stage);
CREATE INDEX idx_evaluation_run_version ON evaluation_run(tenant_id, model_version_id);
CREATE INDEX idx_deployment_target_active ON deployment_target(tenant_id, model_entry_id, active);
CREATE INDEX idx_deployment_target_version ON deployment_target(tenant_id, model_version_id);
