CREATE TABLE annotation_sync_records (
    sync_id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(120) NOT NULL,
    annotation_task_id VARCHAR(120) NOT NULL,
    node_id VARCHAR(120) NOT NULL,
    node_type VARCHAR(60) NOT NULL,
    external_task_id VARCHAR(255),
    status VARCHAR(60) NOT NULL,
    external_url TEXT,
    failure_reason TEXT,
    metadata_json JSONB NOT NULL DEFAULT '{}',
    retry_count BIGINT NOT NULL DEFAULT 0,
    synced_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (sync_id)
);

CREATE INDEX idx_asr_tenant_status ON annotation_sync_records(tenant_id, status, synced_at DESC);
CREATE INDEX idx_asr_tenant_task ON annotation_sync_records(tenant_id, annotation_task_id, synced_at DESC);
CREATE INDEX idx_asr_tenant_node ON annotation_sync_records(tenant_id, node_id, synced_at DESC);
