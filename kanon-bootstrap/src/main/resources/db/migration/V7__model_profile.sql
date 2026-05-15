CREATE TABLE model_profile (
    profile_key VARCHAR(160) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    provider VARCHAR(80) NOT NULL,
    backend_type VARCHAR(80) NOT NULL,
    model_id VARCHAR(160) NOT NULL,
    model_name VARCHAR(180) NOT NULL,
    base_url VARCHAR(512),
    is_local BOOLEAN NOT NULL,
    supports_tools BOOLEAN NOT NULL,
    supports_structured_output BOOLEAN NOT NULL,
    task_capabilities TEXT,
    cost_class VARCHAR(80),
    latency_class VARCHAR(80),
    locality VARCHAR(80),
    compliance_tags TEXT,
    enabled BOOLEAN NOT NULL,
    health_status VARCHAR(80) NOT NULL,
    secret_ref VARCHAR(240),
    priority INTEGER NOT NULL,
    fallback_profile_key VARCHAR(160),
    async_required BOOLEAN NOT NULL,
    health_check_required BOOLEAN NOT NULL,
    evidence_required BOOLEAN NOT NULL,
    timeout_ms BIGINT NOT NULL,
    max_attempts INTEGER NOT NULL,
    concurrency_limit INTEGER NOT NULL,
    rate_limit_per_minute INTEGER NOT NULL,
    retry_max_attempts INTEGER NOT NULL,
    retry_initial_backoff_ms BIGINT NOT NULL,
    retry_max_backoff_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, profile_key)
);

CREATE INDEX idx_model_profile_enabled ON model_profile(tenant_id, enabled);
CREATE INDEX idx_model_profile_provider ON model_profile(tenant_id, provider);
CREATE INDEX idx_model_profile_backend_type ON model_profile(tenant_id, backend_type);
CREATE INDEX idx_model_profile_updated_at ON model_profile(tenant_id, updated_at DESC);

COMMENT ON TABLE model_profile IS 'AI model profiles for routing and execution';
COMMENT ON COLUMN model_profile.profile_key IS 'Unique identifier for the model profile';
COMMENT ON COLUMN model_profile.provider IS 'Model provider (OpenAI, Anthropic, Ollama, etc.)';
COMMENT ON COLUMN model_profile.backend_type IS 'Backend type (LOCAL_SERVER, API)';
COMMENT ON COLUMN model_profile.model_id IS 'Provider-specific model identifier';
COMMENT ON COLUMN model_profile.model_name IS 'Human-readable model name';
COMMENT ON COLUMN model_profile.base_url IS 'Base URL for API endpoints';
COMMENT ON COLUMN model_profile.is_local IS 'Whether the model runs locally';
COMMENT ON COLUMN model_profile.supports_tools IS 'Whether the model supports function calling';
COMMENT ON COLUMN model_profile.supports_structured_output IS 'Whether the model supports structured output';
COMMENT ON COLUMN model_profile.task_capabilities IS 'Comma-separated list of supported AI task types';
COMMENT ON COLUMN model_profile.cost_class IS 'Cost classification (FREE, LOW, MEDIUM, HIGH, PREMIUM)';
COMMENT ON COLUMN model_profile.latency_class IS 'Latency classification (REALTIME, FAST, NORMAL, SLOW, BATCH)';
COMMENT ON COLUMN model_profile.locality IS 'Data locality (LOCAL, CLOUD, HYBRID)';
COMMENT ON COLUMN model_profile.compliance_tags IS 'Comma-separated compliance tags';
COMMENT ON COLUMN model_profile.enabled IS 'Whether the model is enabled for routing';
COMMENT ON COLUMN model_profile.health_status IS 'Current health status (HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN)';
COMMENT ON COLUMN model_profile.secret_ref IS 'Reference to secret store for API keys';
COMMENT ON COLUMN model_profile.priority IS 'Routing priority (higher values preferred)';
COMMENT ON COLUMN model_profile.fallback_profile_key IS 'Fallback model profile key';
COMMENT ON COLUMN model_profile.async_required IS 'Whether async execution is required';
COMMENT ON COLUMN model_profile.health_check_required IS 'Whether health checks are required';
COMMENT ON COLUMN model_profile.evidence_required IS 'Whether evidence logging is required';
COMMENT ON COLUMN model_profile.timeout_ms IS 'Execution timeout in milliseconds';
COMMENT ON COLUMN model_profile.max_attempts IS 'Maximum execution attempts';
COMMENT ON COLUMN model_profile.concurrency_limit IS 'Maximum concurrent requests';
COMMENT ON COLUMN model_profile.rate_limit_per_minute IS 'Rate limit per minute';
COMMENT ON COLUMN model_profile.retry_max_attempts IS 'Maximum retry attempts';
COMMENT ON COLUMN model_profile.retry_initial_backoff_ms IS 'Initial retry backoff in milliseconds';
COMMENT ON COLUMN model_profile.retry_max_backoff_ms IS 'Maximum retry backoff in milliseconds';

-- Made with Bob
