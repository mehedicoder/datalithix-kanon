CREATE TABLE agent_profile (
    agent_id VARCHAR(160) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    name VARCHAR(180) NOT NULL,
    agent_type VARCHAR(80) NOT NULL,
    description TEXT,
    status VARCHAR(60) NOT NULL,
    enabled BOOLEAN NOT NULL,
    supported_domains TEXT,
    supported_task_types TEXT,
    supported_asset_types TEXT,
    supported_source_types TEXT,
    supported_annotation_types TEXT,
    required_policies TEXT,
    required_permissions TEXT,
    input_schema_ref VARCHAR(240),
    output_schema_ref VARCHAR(240),
    execution_mode VARCHAR(80) NOT NULL,
    timeout_seconds INTEGER NOT NULL,
    retry_max_attempts INTEGER,
    retry_initial_backoff_ms BIGINT,
    retry_max_backoff_ms BIGINT,
    max_attempts INTEGER NOT NULL,
    concurrency_limit INTEGER NOT NULL,
    priority INTEGER NOT NULL,
    queue_name VARCHAR(160),
    runtime_profile VARCHAR(160),
    configuration_ref VARCHAR(240),
    model_route_policy VARCHAR(160),
    preferred_model_profile VARCHAR(160),
    fallback_model_profile VARCHAR(160),
    allowed_model_profile_ids TEXT,
    required_llm_service_capabilities TEXT,
    allow_cloud_models BOOLEAN NOT NULL,
    allow_local_models BOOLEAN NOT NULL,
    max_cost_class VARCHAR(80),
    max_latency_class VARCHAR(80),
    compliance_tags TEXT,
    evidence_required BOOLEAN NOT NULL,
    trace_enabled BOOLEAN NOT NULL,
    last_run_at TIMESTAMPTZ,
    last_success_at TIMESTAMPTZ,
    last_failure_at TIMESTAMPTZ,
    last_failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, agent_id)
);

CREATE INDEX idx_agent_profile_tenant_type_enabled
    ON agent_profile (tenant_id, agent_type, enabled);

CREATE INDEX idx_agent_profile_tenant_status_updated
    ON agent_profile (tenant_id, status, updated_at DESC);

CREATE TABLE workflow_definition (
    workflow_id VARCHAR(160) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    name VARCHAR(180) NOT NULL,
    workflow_type VARCHAR(80) NOT NULL,
    description TEXT,
    status VARCHAR(60) NOT NULL,
    enabled BOOLEAN NOT NULL,
    domain_type VARCHAR(80),
    task_type VARCHAR(80),
    asset_type VARCHAR(80),
    source_type VARCHAR(80),
    policy_profile VARCHAR(160),
    regulatory_act VARCHAR(160),
    data_residency VARCHAR(80),
    goal TEXT,
    planner_type VARCHAR(80) NOT NULL,
    planner_version VARCHAR(80),
    action_set_ref VARCHAR(240),
    preconditions TEXT,
    constraints_text TEXT,
    fallback_workflow_ref VARCHAR(160),
    model_route_policy VARCHAR(160),
    allowed_model_profile_ids TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, workflow_id)
);

CREATE INDEX idx_workflow_definition_tenant_type_enabled
    ON workflow_definition (tenant_id, workflow_type, enabled);

CREATE INDEX idx_workflow_definition_tenant_status_updated
    ON workflow_definition (tenant_id, status, updated_at DESC);

CREATE TABLE workflow_instance (
    workflow_instance_id VARCHAR(180) NOT NULL,
    workflow_id VARCHAR(160) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    case_id VARCHAR(180),
    media_asset_id VARCHAR(180),
    current_step VARCHAR(160),
    current_state VARCHAR(160),
    assigned_agent_id VARCHAR(160),
    assigned_user_id VARCHAR(160),
    priority INTEGER NOT NULL,
    due_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    failure_reason TEXT,
    review_required BOOLEAN NOT NULL,
    review_status VARCHAR(80) NOT NULL,
    reviewer_id VARCHAR(160),
    approval_status VARCHAR(80) NOT NULL,
    approved_by VARCHAR(160),
    approved_at TIMESTAMPTZ,
    escalation_reason TEXT,
    export_ready BOOLEAN NOT NULL,
    evidence_event_ids TEXT,
    trace_id VARCHAR(180),
    correlation_id VARCHAR(180),
    model_invocation_ids TEXT,
    input_asset_ids TEXT,
    output_asset_ids TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, workflow_instance_id)
);

CREATE INDEX idx_workflow_instance_tenant_case
    ON workflow_instance (tenant_id, case_id);

CREATE INDEX idx_workflow_instance_tenant_workflow_updated
    ON workflow_instance (tenant_id, workflow_id, updated_at DESC);

CREATE INDEX idx_workflow_instance_tenant_review_due
    ON workflow_instance (tenant_id, review_required, assigned_user_id, due_at);

CREATE INDEX idx_workflow_instance_tenant_correlation
    ON workflow_instance (tenant_id, correlation_id);
