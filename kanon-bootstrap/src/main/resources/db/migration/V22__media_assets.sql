CREATE TABLE media_assets (
    media_asset_id VARCHAR(120) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    case_id VARCHAR(120),
    asset_type VARCHAR(60) NOT NULL,
    source_type VARCHAR(60) NOT NULL,
    source_trace_id VARCHAR(120),
    storage_uri VARCHAR(1024) NOT NULL,
    checksum_sha256 VARCHAR(64),
    content_type VARCHAR(120),
    size_bytes BIGINT NOT NULL,
    duration_ms BIGINT,
    frame_rate DOUBLE PRECISION,
    width INTEGER,
    height INTEGER,
    capture_timestamp TIMESTAMPTZ,
    data_residency VARCHAR(60),
    source_device_id VARCHAR(120),
    mission_id VARCHAR(120),
    technical_metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, media_asset_id)
);

CREATE INDEX idx_media_assets_tenant_case ON media_assets(tenant_id, case_id);
CREATE INDEX idx_media_assets_tenant_source_trace ON media_assets(tenant_id, source_trace_id);
CREATE INDEX idx_media_assets_tenant_updated ON media_assets(tenant_id, updated_at DESC);
